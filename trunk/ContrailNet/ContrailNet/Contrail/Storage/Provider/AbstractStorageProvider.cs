using System;
using System.Threading.Tasks;
using System.Collections.Generic;
using Contrail.Tasks;


namespace Contrail.Storage.Provider {


/**
 * A convenient base class for implementing providers.
 * This class implements the nasty, complicated concurrency aspect.
 * All that needs to be implemented are the doXXXX methods that just 
 * implement the actual storage functions.   
 * 
 */
abstract public class AbstractStorageProvider : IStorageProvider
{
	abstract public class Session: IStorageSession
	{
		private TaskMaster _trackerSession= new TaskMaster();
		
		abstract protected bool exists(Identifier path);
		abstract protected void doStore(Identifier path, byte[] byteArray);
		abstract protected bool doCreate(Identifier path, byte[] byteArray);
		abstract protected byte[] doFetch(Identifier path);
		abstract protected void doDelete(Identifier path);
		abstract protected void doClose(); 
		abstract protected void doFlush();
		abstract protected ICollection<Identifier> doList(Identifier path);
		
		

		public Task<Void> flush() {
			try {
				_trackerSession.WaitAll();
			}
			finally {
				doFlush();
			}
		}

		public async Task close() {
			try {
				flush();
			}
			finally {
				try { _trackerSession.close(); } catch (Exception t) { Logging.warning(t); }
				doClose();
			}
		}
		
		public Task<Collection<Identifier>> listChildren( Identifier path) {
			return _trackerSession.submit(path, Operation.LIST, delegate() {
					return doList(path);
			});
		}
		
		public Task<byte[]> fetch(Identifier path) {
			return _trackerSession.submit(path, Operation.READ, delegate() {
					return doFetch(path);
			});
		}

		public void store(Identifier identifier, Task<byte[]> content) {
			return _trackerSession.submit(path, Operation.WRITE, delegate() {
					doStore(identifier, content.get());
			});
		}

		public void delete(Identifier path) {
			return _trackerSession.submit(path, Operation.DELETE, delegate() {
					doDelete(path);
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
		public Task<Boolean> create(Identifier path_, Task<byte[]> source_, long waitMillis_) 
		{
			return _trackerSession.submit(path_, Operation.CREATE, delegate {
					long startMillis= System.currentTimeMillis();
					bool success= false;
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
			});
		}
	}
}

}