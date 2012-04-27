package com.googlecode.contraildb.core.storage;

import java.io.IOException;
import java.util.Collection;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.storage.provider.IStorageProvider;
import com.googlecode.contraildb.core.utils.Receipt;


/**
 * An even more convenient API for storing Java objects than IObjectStore.
 * 
 * This API stores instances of IEntity instead of POJOs, thus simplifying the 
 * API somewhat.
 * Other than that, IEntityStorage is like IObjectStore, this API handles the 
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
	public Session connect() throws IOException;
	
	
	static public interface Session {
		
		/**
		 * MUST be called when the session is no longer needed.
		 * Any pending changed are flushed before closing.
		 */
		public void close() throws IOException; 
	
		/**
		 * Flush any pending changes to physical storage.
		 */
		public Receipt<Void> flush();
		
		public Receipt<Void> delete(Identifier path);
		
		public Receipt<Void> deleteAllChildren(Identifier path);

		public <E extends IEntity> Receipt<E> fetch(Identifier path);

		public <E extends IEntity> Receipt<Collection<E>> fetchChildren(final Identifier path);

		public <E extends IEntity> Receipt<Void> store(E entity);

		public Receipt<Collection<Identifier>> listChildren(Identifier path);

		public <E extends IEntity> Receipt<Boolean> create(E entity, long waitMillis);

	}


}
