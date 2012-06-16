package storage

import "contrail/id"


/**
 * Objects implement this interface when they need to perform 
 * some tasks when they are stored and/or fetched to/from ObjectStorage.
 */
type Lifecycle interface {
	// Called before an object is stored and after an object is loaded
	SetStorage(storage *ObjectStorageSession)

	// Called before an object is stored
	// The path parameter specifies the identifier used to store the object.
	OnInsert(path *id.Identifier)
	
	// Called after an object is loaded
	// The path parameter specifies the identifier used to load the object.
	OnLoad(path *id.Identifier)
	
	// Called after an object is deleted
	OnDelete()
}
