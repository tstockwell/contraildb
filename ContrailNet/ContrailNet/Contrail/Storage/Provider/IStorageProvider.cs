using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Contrail.Storage.Provider {


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
 * Some implementation of this interface may cache operations for performance 
 * reasons or, more likely, perform operations asynchronously in the background.  
 * The flush method must be called to make sure that all changes are flushed 
 * to physical storage at a particular point in time.
 * Note that IStorageProvider implementations are not required to implement caching 
 * since Contrail will cache objects it reads from the raw storage system.
 * 
 */
public interface IStorageProvider {
	
	/**
	 * Start a storage session. 
	 */
	Task<IStorageSession> connect();

}


public interface IStorageSession {
	
	/**
	 * MUST be called when the session is no longer needed.
	 * Any pending changed are flushed before closing.
	 */
	Task close(); 
	
	/**
	 * Returns the complete paths to all the children of the given path.
	 */
	Task<ICollection<Identifier>> listChildren(Identifier path);
	
	/**
	 * @return the contents of of the given path, or null if the file does not exist.
	 */
	Task<byte[]> fetch(Identifier path);

	/**
	 * Stores the given contents at the given location.
	 * The file is created if it does not already exist.
	 */
	Task store(Identifier path, Task<byte[]> content);

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
	Task<Boolean> create(Identifier i, Task<byte[]> content, long waitMillis);

	/**
	 * Deletes the contents stored at the given locations.
	 */
	Task delete(Identifier path);
	
	/**
	 * Flush any pending changes made by this session to physical storage.
	 */
	Task flush();


}

}