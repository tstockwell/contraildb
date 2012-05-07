package com.googlecode.contraildb.core.storage;

import java.io.IOException;
import java.util.Collection;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.storage.provider.IStorageProvider;


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
		public void flush() throws IOException;
		
		public void delete(Identifier path);
		
		public void deleteAllChildren(Identifier path);

		public <E extends IEntity> IResult<E> fetch(Identifier path);

		public <E extends IEntity> IResult<Collection<E>> fetchChildren(final Identifier path);

		public <E extends IEntity> void store(E entity);

		public IResult<Collection<Identifier>> listChildren(Identifier path);

		public <E extends IEntity> IResult<Boolean> create(E entity, long waitMillis);

	}


}
