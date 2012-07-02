package storage

import (
	"math/big"
)

/**
 * A marker that is written into a revision folder that indicates that the 
 * revision is committed. 
 */
type CommitMarker struct {
	storage *ObjectStorageSession
	path *id.Identifier
	
	FinalCommitNumber *big.Int
}

func CreateCommitMarker(revision *RevisionFolder, finalCommitNumber *big.Int) *CommitMarker {
	return &CommitMarker {
		path: revision.Identifier().Child("commitMarker")
		FinalCommitNumber: finalCommitNumber 
	}
}



////////////////////////////
//
// Implementation of Lifecycle interface
//


func (self *CommitMarker)	SetStorage(storage *ObjectStorageSession) {
	self.storage= storage
}
func (self *CommitMarker)	OnInsert(path *id.Identifier) {
	self.path= path
}
func (self *CommitMarker)	OnLoad(path *id.Identifier) {
	self.path= path
}
func (self *CommitMarker)	OnDelete() {
	// do nothing
}
func (self *CommitMarker)	Identifier() *id.Identifier {
	return self.identifier
}
func (self *CommitMarker)	Storage() *ObjectStorageSession {
	return self.storage
}
