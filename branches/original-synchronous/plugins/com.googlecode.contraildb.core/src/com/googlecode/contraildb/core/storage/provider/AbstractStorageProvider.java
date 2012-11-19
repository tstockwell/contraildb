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


/**
 * A convenient base class for implementing providers.
 * This class implements the concurrency aspect.
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
		/**
		 * @return true if the file was created, false if the file already exists.
		 */
		abstract protected boolean doCreate(Identifier path, byte[] byteArray) throws IOException;
		abstract protected byte[] doFetch(Identifier path) throws IOException;
		abstract protected void doDelete(Identifier path) throws IOException;
		abstract protected void doClose() throws IOException; 
		abstract protected void doFlush() throws IOException;
		abstract protected Collection<Identifier> doList(Identifier path) throws IOException;
		
		

		@Override
		public IResult<Void> flush() {
			return 	_trackerSession.submit(new ContrailAction(null, Operation.FLUSH) {
				@Override protected void action() throws Pausable, Exception {
					doFlush();
				}
			});
		}

		@Override
		public IResult<Void> close() {
			return new ContrailAction(null, Operation.FLUSH) {
				@Override protected void action() throws Pausable, Exception {
					try {
						doFlush();
					}
					finally {
						doClose();
					}
				}
			}.submit(); 
		}
		
		@Override
		public IResult<Collection<Identifier>> listChildren(final Identifier path) {
			return _trackerSession.submit(new ContrailTask<Collection<Identifier>>(path, Operation.LIST) {
				protected Collection<Identifier> run() throws Pausable, IOException {
					return doList(path);
				}
			});
		}
		
		@Override
		public IResult<byte[]> fetch(final Identifier path) {
			ContrailTask<byte[]> action= new ContrailTask<byte[]>(path, Operation.READ) {
				protected byte[] run() throws Pausable, IOException {
					return doFetch(path);
				}
			};
			return _trackerSession.submit(action);
		}

		@Override
		public IResult<Void> store(final Identifier identifier, final IResult<byte[]> content) {
			return _trackerSession.submit(new ContrailAction(identifier, Operation.WRITE) {
				protected void action() throws Pausable, IOException  {
					byte[] bs= content.get();
					doStore(identifier, bs);
				}
			});
		}

		@Override
		public IResult<Void> delete(final Identifier path) {
			return _trackerSession.submit(new ContrailAction(path, Operation.DELETE) {
				protected void action() throws Pausable, IOException {
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
		public IResult<Boolean> create(final Identifier id, final IResult<byte[]> content, final long waitMillis) 
		{
			return new ContrailTask<Boolean>() {
				@Override protected Boolean run() throws Pausable, Exception {
					final long end= System.currentTimeMillis()+waitMillis;
					while (true) {
						boolean created= _trackerSession.submit(new ContrailTask<Boolean>(id, Operation.CREATE) {
							protected Boolean run() throws Pausable, IOException {
								byte[] bs= content.get();  
								return doCreate(id, bs);  
							}
						}).get();
						if (created)
							return true;
						if (end < System.currentTimeMillis())
							return false;
					}
				}
			}.submit();
		}
	}
}
