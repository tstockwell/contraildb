package com.googlecode.contraildb.core.storage;

import java.io.IOException;
import java.util.Collection;

import kilim.Pausable;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.storage.provider.IStorageProvider;


/**
 * An even more convenient API for storing Java objects than ObjectStorage.
 * 
 * This API stores instances of IEntity instead of POJOs, thus simplifying the 
 * API somewhat.
 * Other than that, IEntityStorage is like ObjectStorage, this API handles the 
 * serialization of Java objects to byte streams, object caching, and ILifecycle 
 * lifecycle management.
 * 
 * Implementations should also cache objects in order to avoid as much 
 * serialization and deserialization as possible.
 * 
 * @param T the base type of objects stored in the repository 
 *   
 * @author Ted Stockwell
 */
public interface IEntityStorage {

	
	/**
	 * Return the underlying storage provider
	 */
	public IStorageProvider getStorageProvider();
	
	/**
	 * Start a storage session. 
	 */
	public Session connect() throws IOException, Pausable;
	
	
	static public interface Session {
		
		/**
		 * MUST be called when the session is no longer needed.
		 * Any pending changed are flushed before closing.
		 */
		public void close() throws IOException, Pausable;
	
		/**
		 * Flush any pending changes to physical storage.
		 */
		public void flush() throws IOException, Pausable;
		
		public void delete(Identifier path) throws IOException, Pausable;
		
		public <E extends IEntity> void delete(E entity) throws IOException, Pausable;
		
		public void deleteAllChildren(Identifier path) throws IOException, Pausable;

		public <E extends IEntity> E fetch(Identifier path) throws IOException, Pausable;

		public <E extends IEntity> Collection<E> fetchChildren(final Identifier path) throws IOException, Pausable;

		public <E extends IEntity> void store(E entity) throws IOException, Pausable;

		public Collection<Identifier> listChildren(Identifier path) throws IOException, Pausable;

		public <E extends IEntity> boolean create(E entity, long waitMillis) throws IOException, Pausable;

	}


}
