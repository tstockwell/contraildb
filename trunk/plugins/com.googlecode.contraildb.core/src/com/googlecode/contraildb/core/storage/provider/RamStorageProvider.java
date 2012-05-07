package com.googlecode.contraildb.core.storage.provider;

import java.io.IOException;
import java.util.Collection;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.utils.IdentifierIndexedStorage;
import com.googlecode.contraildb.core.utils.TaskUtils;


/**
 * Implementation of the IStorageProvider interface that stores items in memory.
 * 
 * @author Ted Stockwell
 */
public class RamStorageProvider extends AbstractStorageProvider {
	
	IdentifierIndexedStorage<byte[]> _storage= new IdentifierIndexedStorage<byte[]>(); 
	
	@Override
	public com.googlecode.contraildb.core.storage.provider.IStorageProvider.Session connect() throws IOException {
		return new RamStorageSession();
	}
	
	private class RamStorageSession 
	extends AbstractStorageProvider.Session 
	{

		@Override
		protected IResult<Void> doClose() {
			// do nothing
			return TaskUtils.asResult(null);
		}

		@Override
		protected IResult<byte[]> doFetch(Identifier path) {
			return TaskUtils.asResult(_storage.fetch(path));
		}

		@Override
		protected IResult<Void> doFlush() {
			// do nothing
			return TaskUtils.asResult(null);
		}

		@Override
		protected Collection<Identifier> doList(Identifier path) {
			return _storage.listChildren(path);
		}

		@Override
		protected IResult<Void> doStore(Identifier path, byte[] byteArray) {
			_storage.store(path, byteArray);
			return TaskUtils.asResult(null);
		}
		
		@Override
		protected IResult<Void> doDelete(Identifier path) {
			_storage.delete(path);
			return TaskUtils.asResult(null);
		}
		
		@Override
		protected IResult<Boolean> exists(Identifier path) {
			return TaskUtils.asResult(_storage.exists(path));
		}
	}
	
}
