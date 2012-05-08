package com.googlecode.contraildb.core.storage.provider;

import java.io.IOException;
import java.util.Collection;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.utils.ContrailAction;
import com.googlecode.contraildb.core.utils.ContrailTask;
import com.googlecode.contraildb.core.utils.ContrailTask.Operation;
import com.googlecode.contraildb.core.utils.ContrailTaskTracker;
import com.googlecode.contraildb.core.utils.ResultHandler;


/**
 * A convenient base class for implementing providers.
 * This class implements the concurrency aspect.
 * All that needs to be implemented are the doXXXX methods that just 
 * implement the actual storage functions.   
 * 
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
abstract public class AbstractStorageProvider
implements IStorageProvider
{
	
	private ContrailTaskTracker _tracker= new ContrailTaskTracker();
	
	abstract public class Session
	implements IStorageProvider.Session
	{
		private ContrailTaskTracker.Session _trackerSession= _tracker.beginSession();
		
		abstract protected IResult<Boolean> exists(Identifier path);
		abstract protected IResult<Void> doStore(Identifier path, byte[] byteArray);
		abstract protected IResult<Boolean> doCreate(Identifier path, byte[] byteArray, long waitMillis);
		abstract protected IResult<byte[]> doFetch(Identifier path);
		abstract protected IResult<Void> doDelete(Identifier path);
		abstract protected IResult<Void> doClose(); 
		abstract protected IResult<Void> doFlush();
		abstract protected Collection<Identifier> doList(Identifier path);
		
		

		@Override
		public IResult<Void> flush() throws IOException {
			return new ResultHandler(_trackerSession.complete()) {
				protected void onComplete() throws Exception {
					spawnChild(doFlush());
				};
			}.toResult();
		}

		@Override
		public IResult<Void> close() {
			return new ResultHandler(_trackerSession.complete()) {
				protected void onComplete() throws Exception {
					spawnChild(doClose());
				};
			}.toResult();
		}
		
		@Override
		public IResult<Collection<Identifier>> listChildren(final Identifier path) {
			return _trackerSession.submit(new ContrailTask(path, Operation.LIST) {
				protected Object run() throws IOException {
					return doList(path);
				}
			});
		}
		
		@Override
		public IResult<byte[]> fetch(final Identifier path) {
			return _trackerSession.submit(new ContrailTask(path, Operation.READ) {
				protected Object run() throws IOException {
					return doFetch(path).get();
				}
			});
		}

		@Override
		public IResult<Void> store(final Identifier identifier, final IResult<byte[]> content) {
			return _trackerSession.submit(new ContrailAction(identifier, Operation.WRITE) {
				protected void action() {
					try {
						doStore(identifier, content.get());
					}
					catch (Throwable t) {
						error(t);
					}
				}
			});
		}

		@Override
		public IResult<Void> delete(final Identifier path) {
			return _trackerSession.submit(new ContrailAction(path, Operation.DELETE) {
				protected void action() throws IOException {
						doDelete(path).get();
				}
			});
		}
		
		/**
		 * Stores the given contents at the given location if the file 
		 * does not already exist.  Otherwise does nothing.
		 * 
		 * @param _waitMillis
		 * 		if the file already exists and _waitMillis is greater than zero   
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
			return _trackerSession.submit(new ContrailTask(path_, Operation.CREATE) {
				protected Object run() throws IOException {
						return doCreate(path_, source_.get(), waitMillis_).get();
				}
			});
		}
	}
}
