package errors

import (
	
)


type FolderLocked struct {
	identifer string
}
func (self *FolderLocked) Error() {
	return "Folder is locked: "+self.identifer
}

type InternalError struct {
	msg string
}
func (self *InternalError) Error() {
	return "Internal Error: "+self.msg
}
