package com.googlecode.contraildb.core.storage.provider;

import java.io.IOException;
import java.util.Collection;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.utils.IdentifierIndexedStorage;


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
		protected void doClose() throws IOException {
			// do nothing
		}

		@Override
		protected byte[] doFetch(Identifier path) {
			return _storage.fetch(path);
		}

		@Override
		protected void doFlush() throws IOException {
			// do nothing
		}

		@Override
		protected Collection<Identifier> doList(Identifier path) {
			return _storage.listChildren(path);
		}

		@Override
		protected void doStore(Identifier path, byte[] byteArray) {
			_storage.store(path, byteArray);
		}
		
		@Override
		protected void doDelete(Identifier path) throws IOException {
			_storage.delete(path);
		}
		
		@Override
		protected boolean exists(Identifier path) throws IOException {
			return _storage.exists(path);
		}

		@Override
		protected boolean doCreate(Identifier path, byte[] byteArray) throws IOException {
			return _storage.create(path, byteArray);
		}
	}
	
}
