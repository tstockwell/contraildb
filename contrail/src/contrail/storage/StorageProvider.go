package storage

import (
	"time"
	cid "contrail/id"
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
 * Some implementations of this interface may cache operations for performance 
 * reasons or, more likely, perform operations asynchronously in the background.  
 * The flush method must be called to make sure that all changes are flushed 
 * to physical storage at a particular point in time.
 * Note that StorageProvider implementations may implement caching but they are
 * are not required to do so since Contrail will cache objects it reads from the
 * raw storage system.
 * 
 * A StorageProvider may be used by multiple clients.
 * StorageProvider implementations should define the circumstances in which it 
 * may be used (for instance, an implementation may only support clients in the 
 * same process on one machine or by an unlimited number of clients on multiple 
 * remote machines).    
 * StorageProviders must gaurantee that methods are executed in the sequential 
 * order in which requests are received.
 * StorageProviders are not required to execute method calls sequentially, they 
 * are just required to execute requests in a way that is *equivalent* to 
 * executing them sequentially.
 *
 * Like most APIs in Contrail, StorageProviders do not adhere to the Go idiom of 
 * returning error codes, instead failures cause runtime panics.
 * 
 * 
 */
type StorageSession interface  {
	
	/**
	 * MUST be called when the session is no longer needed.
	 * Any pending changes are flushed before closing.
	 */
	Close()
	
	/**
	 * Returns the complete paths to all the children of the given path.
	 */
	ListChildren(path *cid.Identifier) []*cid.Identifier
	
	/**
	 * @return the contents of of the given path, or null if the file does not exist.
	 */
	Fetch(path *cid.Identifier) []byte

	/**
	 * Stores the given contents at the given location.
	 * The file is created if it does not already exist.
	 */
	Store(path *cid.Identifier, content []byte)

	/**
	 * Stores the given contents at the given location if the file 
	 * does not already exist.  
	 * If the file already exists then this method does nothing.
	 * 
	 * @param wait
	 * 		if the file already exists and wait parameter is greater than 
	 *      zero then wait the denoted duration for some other 
	 * 		process to delete the file.
	 * 
	 * @return 
	 * 		true if the file was created, false if the file already exists 
	 * 		and was not deleted within the wait period.
	 */
	Create(path *cid.Identifier, content []byte, wait time.Duration) bool

	/**
	 * Deletes the contents stored at the given locations.
	 */
	Delete(path *cid.Identifier)
	
	/**
	 * Flush any pending changes made by this session to physical storage.
	 */
	Flush()
}

type StorageProvider interface {
	
	/**
	 * Start a storage session. 
	 */
	Connect() StorageSession 
	
	/**
	 * MUST be called when the provoder is no longer needed.
	 */
	Close()
	
}
