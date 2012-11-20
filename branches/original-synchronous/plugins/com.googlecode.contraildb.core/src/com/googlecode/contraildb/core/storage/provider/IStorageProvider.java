package com.googlecode.contraildb.core.storage.provider;

import java.util.Collection;

import kilim.Pausable;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.async.IResult;


/**
 * Contrail works with on any storage system that can implement the very basic 
 * file system described by this interface.
 * IStorageProvider provides access to a hierarchy of nested objects.
 * Each object has a name.
 * An object may have children.
 * Each object can be retrieved by its location in the hierarchy.
 * Also the names of the children of any object may be retrieved.
 * 
 * None of the methods are considered atomic, that is, if they fail 
 * they may have partially completed their operation.
 * 
 * Some implementation of this interface may choose to cache operations for 
 * performance reasons or, more likely, perform operations asynchronously in 
 * the background.  
 * The flush method must be called to make sure that all changes are flushed 
 * to physical storage at a particular point in time.
 * IStorageProvider implementations are not required to implement caching 
 * since Contrail will cache objects it reads from the raw storage system.
 * 
 * The methods in this interface throw Pausable, which identifies them as 
 * asynchronous methods. 
 * 
 */
public interface IStorageProvider {
	
	/**
	 * Start a storage session. 
	 */
	public IStorageProvider.Session connect() throws Pausable;
	public IResult<IStorageProvider.Session> connectA();
	
	
	static public interface Session {
		
		/**
		 * MUST be called when the session is no longer needed.
		 * Any pending changed are flushed before closing.
		 */
		public void close() throws Pausable;
		public IResult<Void> closeA(); 
		
		/**
		 * Returns the complete paths to all the children of the given path.
		 */
		public Collection<Identifier> listChildren(Identifier path) throws Pausable;
		public IResult<Collection<Identifier>> listChildrenA(Identifier path);
		
		/**
		 * @return the contents of of the given path, or null if the file does not exist.
		 */
		public byte[] fetch(Identifier path) throws Pausable;
		public IResult<byte[]> fetchA(Identifier path);

		/**
		 * Stores the given contents at the given location.
		 * The file is created if it does not already exist.
		 */
		public void store(Identifier path, IResult<byte[]> content) throws Pausable;
		public IResult<Void> storeA(Identifier path, IResult<byte[]> content);

		/**
		 * Stores the given contents at the given location if the file 
		 * does not already exist.  Otherwise does nothing.
		 * 
		 * @param waitMillis
		 * 		if the file already exists and parameter is greater than zero   
		 * 		then wait the denoted number of milliseconds for the file to be 
		 * 		deleted.
		 * 
		 * @return 
		 * 		true if the file was created, false if the file already exists 
		 * 		and was not deleted within the wait period.
		 */
		public boolean create(Identifier i, IResult<byte[]> content, long waitMillis) throws Pausable;
		public IResult<Boolean> createA(Identifier i, IResult<byte[]> content, long waitMillis);

		/**
		 * Deletes the contents stored at the given locations.
		 */
		public void delete(Identifier path) throws Pausable;
		public IResult<Void> deleteA(Identifier path);
		
		/**
		 * Flush any pending changes made by this session to physical storage.
		 */
		public void flush() throws Pausable;
		public IResult<Void> flushA();


	}
	
}
