package com.googlecode.contraildb.core.storage.provider;

import java.io.IOException;
import java.util.Collection;

import kilim.Pausable;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.async.ContrailAction;
import com.googlecode.contraildb.core.async.ContrailTask;
import com.googlecode.contraildb.core.async.IResult;
import com.googlecode.contraildb.core.async.Operation;
import com.googlecode.contraildb.core.async.TaskDomain;
import com.googlecode.contraildb.core.async.TaskUtils;
import com.googlecode.contraildb.core.utils.Logging;


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
	private TaskDomain _tracker= new TaskDomain();
	
	abstract public class Session
	implements IStorageProvider.Session
	{
		private TaskDomain.Session _trackerSession= _tracker.beginSession();
		
		abstract protected boolean exists(Identifier path) throws IOException;
		abstract protected void doStore(Identifier path, byte[] byteArray) throws IOException;
		abstract protected boolean doCreate(Identifier path, byte[] byteArray) throws IOException;
		abstract protected byte[] doFetch(Identifier path) throws IOException;
		abstract protected void doDelete(Identifier path) throws IOException;
		abstract protected void doClose() throws IOException; 
		abstract protected void doFlush() throws IOException;
		abstract protected Collection<Identifier> doList(Identifier path) throws IOException;
		
		

		@Override
		public IResult<Void> flush() {
			return 	_trackerSession.submit(new ContrailAction() {
				@Override
				protected void action() throws Pausable, Exception {
					try {
						// wait for previous operations to complete
						_trackerSession.complete().get(); 
					}
					finally {
						doFlush();
					}
				}
			});
		}

		@Override
		public IResult<Void> close() {
			return new ContrailAction() {
				@Override
				protected void action() throws Pausable, Exception {
					try {
						flush();
					}
					finally {
						try { _trackerSession.close(); } catch (Throwable t) { Logging.warning(t); }
						doClose();
					}
				}
			}.submit();
		}
		
		@Override
		public IResult<Collection<Identifier>> listChildren(final Identifier path) {
			ContrailTask<Collection<Identifier>> action= new ContrailTask<Collection<Identifier>>(path, Operation.LIST) {
				protected Collection<Identifier> run() throws IOException {
					return doList(path);
				}
			};
			return _trackerSession.submit(action);
		}
		
		@Override
		public IResult<byte[]> fetch(final Identifier path) {
			ContrailTask<byte[]> action= new ContrailTask<byte[]>(path, Operation.READ) {
				protected byte[] run() throws IOException {
					return doFetch(path);
				}
			};
			return _trackerSession.submit(action);
		}

		@Override
		public void store(final Identifier identifier, final IResult<byte[]> content) {
			_trackerSession.submit(new ContrailAction(identifier, Operation.WRITE) {
				protected void action() throws IOException {
					doStore(identifier, content.get());
				}
			});
		}

		@Override
		public void delete(final Identifier path) {
			_trackerSession.submit(new ContrailAction(path, Operation.DELETE) {
				protected void action() throws IOException {
					doDelete(path);
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
		public IResult<Boolean> create(final Identifier path_, final IResult<byte[]> source_, final long waitMillis_) 
		{
			return _trackerSession.submit(new ContrailTask<Boolean>(path_, Operation.CREATE) {
				protected Boolean run() throws IOException {
					long startMillis= System.currentTimeMillis();
					boolean success= false;
					while(true) {
						if (doCreate(path_, source_.get())) { 
							success= true;
							break;
						}
						if (waitMillis_ <= 0) 
							break;
						if (waitMillis_ < System.currentTimeMillis() - startMillis)
							break;
						yield(null);
					}
					return success;
				}
			});
		}
	}
}
