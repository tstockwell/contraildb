package storage

import (
	"contrail/id"
)


/**
 * Contrail works with on any storage system that can implement the very basic 
 * file system described by this interface.
 * StorageProvider provides access to a hierarchy of nested objects.
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
 * Note that StorageProvider implementations are not required to implement caching 
 * since Contrail will cache objects it reads from the raw storage system.
 * 
 */
type StorageSession interface  {
	
	/**
	 * MUST be called when the session is no longer needed.
	 * Any pending changes are flushed before closing.
	 */
	Close();
	
	/**
	 * Returns the complete paths to all the children of the given path.
	 */
	ListChildren(path *Identifier) []Identifier
	
	/**
	 * @return the contents of of the given path, or null if the file does not exist.
	 */
	Fetch(path *Identifier) []byte

	/**
	 * Stores the given contents at the given location.
	 * The file is created if it does not already exist.
	 */
	Store(path *Identifier, content []byte)

	/**
	 * Stores the given contents at the given location if the file 
	 * does not already exist.  
	 * If the file already exists then this method does nothing.
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
	Create(path *Identifier, content byte[], waitMillis uint64)

	/**
	 * Deletes the contents stored at the given locations.
	 */
	Delete(path *Identifier);
	
	/**
	 * Flush any pending changes made by this session to physical storage.
	 */
	Flush();


}
type StorageProvider interface {
	
	/**
	 * Start a storage session. 
	 */
	Connect() StorageSession 
	
}
