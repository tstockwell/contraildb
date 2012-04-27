package com.googlecode.contraildb.core.storage.provider;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.utils.Completion;
import com.googlecode.contraildb.core.utils.Completion;
import com.googlecode.contraildb.core.utils.Receipt;
import com.googlecode.contraildb.core.utils.ContrailAction;
import com.googlecode.contraildb.core.utils.ContrailTask;
import com.googlecode.contraildb.core.utils.ContrailTask.Operation;
import com.googlecode.contraildb.core.utils.ContrailTaskTracker;
import com.googlecode.contraildb.core.utils.Logging;
import com.googlecode.contraildb.core.utils.SignalHandler;
import com.googlecode.contraildb.core.utils.Signals;
import com.googlecode.contraildb.core.utils.TaskUtils;


/**
 * A convenient base class for implementing providers.
 * This class implements the nasty, complicated concurrency aspect.
 * All that needs to be implemented are the doXXXX methods that just 
 * implement the actual storage functions.   
 * 
 */
abstract public class AbstractStorageProvider
implements IStorageProvider
{
	
	// list of items for which we want notification of changes
	private HashSet<Identifier> _identifiersOfInterest= new HashSet<Identifier>(); 
	
	private ContrailTaskTracker _tracker= new ContrailTaskTracker();
	
	abstract public class Session
	implements IStorageProvider.Session
	{
		private ContrailTaskTracker.Session _trackerSession= _tracker.beginSession();
		
		abstract protected boolean exists(Identifier path) throws IOException;
		abstract protected void doStore(Identifier path, byte[] byteArray) throws IOException;
		abstract protected byte[] doFetch(Identifier path) throws IOException;
		abstract protected void doDelete(Identifier path) throws IOException;
		abstract protected void doClose() throws IOException; 
		abstract protected Receipt<Void> doFlush() throws IOException;
		abstract protected Collection<Identifier> doList(Identifier path) throws IOException;
		
		
		@Override
		public Receipt<Void> flush() throws IOException {
			return TaskUtils.<Void>sequence(
					_trackerSession.awaitCompletion(),
					new Completion<Void,Void>() {
						@Override public Receipt<Void> complete(Receipt<Void> future) {
							doFlush();
						}
					});
		}

		@Override
		public void close() throws IOException {
			try {
				_trackerSession.awaitCompletion(IOException.class);
			}
			finally {
				try { _trackerSession.close(); } catch (Throwable t) { Logging.warning(t); }
				doClose();
			}
		}
		
		@Override
		public Receipt<Collection<Identifier>> listChildren(final Identifier path) {
			ContrailTask<Collection<Identifier>> action= new ContrailTask<Collection<Identifier>>(path, Operation.LIST) {
				protected void run() throws IOException {
					setResult(doList(path));
				}
			};
			_trackerSession.submit(action);
			return action;
		}
		
		@Override
		public Receipt<byte[]> fetch(final Identifier path) {
			ContrailTask<byte[]> action= new ContrailTask<byte[]>(path, Operation.READ) {
				protected void run() throws IOException {
					setResult(doFetch(path));
				}
			};
			_trackerSession.submit(action);
			return action;
		}

		@Override
		public Receipt<Void> store(final Identifier identifier, final Receipt<byte[]> content) {
			_trackerSession.submit(new ContrailAction(identifier, Operation.WRITE) {
				protected void run() {
					try {
						doStore(identifier, content.get());
					}
					catch (Throwable t) {
						setError(t);
					}
				}
			});
		}

		@Override
		public Receipt<Void> delete(final Identifier path) {
			_trackerSession.submit(new ContrailAction(path, Operation.DELETE) {
				protected void run() throws IOException {
					try {
						doDelete(path);
					}
					finally {
						boolean signal= false;
						synchronized (_identifiersOfInterest) {
							if (_identifiersOfInterest.contains(path)) 
								signal= true;
						}
						if (signal) {
							Signals.signal(path);
						}
					}
				}
			});
		}
		
		/**
		 * Stores the given contents at the given location if the file 
		 * does not already exist.  Otherwise does nothing.
		 * 
		 * @param _waitMillis
		 * 		if the file already exists and parameter is greater than zero   
		 * 		then wait the denoted number of milliseconds for the file to be 
		 * 		deleted.
		 * 
		 * @return 
		 * 		true if the file was created, false if the file already exists 
		 * 		and was not deleted within the wait period.
		 */
		@Override
		public Receipt<Boolean> create(final Identifier path_, final Receipt<byte[]> source_, final long waitMillis_) 
		{
			ContrailTask<Boolean> task= new ContrailTask<Boolean>(path_, Operation.CREATE) {
				final Identifier _path= path_;
				final Receipt<byte[]> _source= source_;
				final long _waitMillis= waitMillis_;
				
				final long _start= System.currentTimeMillis();
				boolean _locked= false;
				
				SignalHandler signalHandler= new SignalHandler() {
					public void signal(Identifier signal) {
						process();
					}
				};
				
				{
					Signals.register(_path, signalHandler);
					
					// start this task on a background thread
					new ContrailAction() {
						public void run() {
							process();
						}
					}.submit();
				}
				protected void run() throws Exception {
					// do nothing
				}
				
				synchronized protected void process() {
					try {
						if (isDone()) { 
							return;
						}
						if (!getLock()) {
							return;
						}
						if (!doCreate()) { 
							return;
						}
						terminate();
					}
					catch (Throwable t) {
						setError(t);
						terminate();
					}
					finally {
						checkTimeOut();
					}
				}
				
				private boolean getLock() {
					if (!_locked) {
						synchronized (_identifiersOfInterest) {
							if (!_identifiersOfInterest.contains(_path)) {
								_identifiersOfInterest.add(_path);
								_locked= true;
							}
						}
					} 
					return _locked;
				}
				
				private void terminate() {
					if (getResult() == null)
						setResult(false);
					Signals.unregister(path_, signalHandler);
					releaseLock();
					done();
				}
				
				private void checkTimeOut() {
					long millisRemaining= _waitMillis - (System.currentTimeMillis() - _start);
					if (millisRemaining <= 0) {
						terminate();
					}
				}

				private boolean doCreate() throws IOException 
				{
					try {
						if (!exists(_path)) {
							doStore(_path, _source.get());
							setResult(true);
							return true;
						}
					}
					catch (Throwable t) {
						if (!isCancelled())
							TaskUtils.throwSomething(t, IOException.class);
					}
					return false;
				}

				private void releaseLock() {
					if (_locked) {
						synchronized (_identifiersOfInterest) {
							_identifiersOfInterest.remove(_path);
						}
						Signals.signal(_path);
					}
				}
			};
			return task;
		}
	}
}
