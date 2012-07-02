package storage

import (
	"contrail"
	"contrail/id"
	"contrail/errors"
)

/**
 * An object that serves as a folder in a storage system.
 *
 * Has methods for controlling access to the folder by locking/unlocking the 
 * folder. Uses a home-grown, file-based consensus algorithm which enables 
 * remote processes to decide amongst themselves who gets the lock.
 * 
 * @author ted stockwell
 */
type Folder struct {
	storage *ObjectStorageSession
	path *id.Identifier
}


func CreateFolder(path *id.Identifer) *Folder {
	return &Folder(path:path)
}


func CreateLockFolderId(parentId *id.Identifer) *id.Identifer {
	return parentId.Child("lockfolder")
}
	

/**
 * Locks this folder using a consensus algorithm.
 * The consensus algorithm enables many remote, anonymous processes to 
 * decide among themselves who gets the lock.
 * Locks expire after some amount of time, around one minute.
 * 
 * @param processId The id of the claiming process.
 * @param waitForNext wait until current lock expires and then try to break it
 * @return true if the process was awarded the lock   
 */
func (self *LockFolder) Lock(processId string, waitForNext bool) bool 
{
	lock:= CreateLock(id, processId)
	waitDuration= waitForNext? contrail.SESSION_MAX_ACTIVE : 0
	result= self.storage.Create(lock, waitDuration)
	if result {
		return true
	} else {
		// the session that holds the lock has timed out 
		if 0 < waitDuration {
			// break existing lock
			storage.Delete(lock.Identifier());
			if (storage.Create(lock, waitDuration)) {
				return true
			}
			else
				panic(&errors.FolderLocked{lock.Identifier()})
		}
	}
	return false
}


func (self *LockFolder) Unlock(processId string)  
{
	lockId:= self.path.Child("lock")
	lock:= self.storage.Fetch(lockId).(Lock)
	if lock == nil || lock.processId  != processId {
		panic(&errors.InternalError("Process "+processId+" tried to unlock a folder that it did not own: "+id);
	}
	self.storage.Delete(lock.Identifier())
	self.storage.Flush()
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
