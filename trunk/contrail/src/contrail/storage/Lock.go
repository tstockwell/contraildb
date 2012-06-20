package storage

import (
	"contrail"
)

type Lock struct {
	storage *ObjectStorageSession
	path *id.Identifier
}


func CreateLockId(parentId *id.Identifer) *id.Identifer {
	return parentId.Child("lock")
}

////////////////////////////
//
// Implementation of Lifecycle interface
//

func (self *LockFolder)	SetStorage(storage *ObjectStorageSession) {
	self.storage= storage
}
func (self *LockFolder)	OnInsert(path *id.Identifier) {
	self.path= path
}
func (self *LockFolder)	OnLoad(path *id.Identifier) {
	self.path= path
}
func (self *LockFolder)	OnDelete() {
	// do nothing
}
func (self *LockFolder)	Identifier() *id.Identifier {
	return self.identifier
}
func (self *LockFolder)	Storage() *ObjectStorageSession {
	return self.storage
}
	