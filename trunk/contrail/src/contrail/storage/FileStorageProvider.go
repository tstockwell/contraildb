package storage

import (
	"os"
	"io/ioutil"
	"path/filepath"
	"contrail/id"
	"contrail/tasks"
)

	
var lock_file string= ".lock"
var content_file string= ".content" 


/**
 * An of the StorageProvider interface that stores items in files 
 * on the local file system.
 * This implementation is very naive for a database storage facility, it just 
 * writes all items to separate files.
 * On the other hand, it works plenty fast and has some advantages.  For instance 
 * it makes it possible to do incremental backup of the database using  
 * off-the-shelf backup routines.  
 * 
 * This file system implementation is only meant to be used by a single process.
 * Another class, ServerStorageProvider, implements an HTTP API on top of 
 * self class that provides multi-user, client-server access to a file store. 
 *  
 * @see ServerStorageProvider for client/server access to a file store 
 * 
 * @author Ted Stockwell
 */

type FileStorageProvider struct {
	root *os.File // path the root of database file system
	taskMaster *tasks.TaskMaster
}
type FileStorageSession struct {
	provider *FileStorageProvider
}

func CreateFileStorageProvider(path string, clean bool) *FileStorageProvider {
	if clean {
		os.RemoveAll(path)
	}
	if err:= os.MkdirAll(path, os.FileMode(0777)); err != nil {
		panic(err)
	}
	file, err:= os.Open(path)
	if err != nil {
		panic(err)
	}
	return &FileStorageProvider{ 
		root:		file,
		taskMasetr: tasks.CreateTaskMaster(),
	}
}


/**
 * Start a storage session. 
 */
func (self *FileStorageProvider) Connect() StorageSession {
	return &FileStorageSession { 
		provider:self,
	}
} 

func (self *FileStorageProvider) Root() string {
	return self.root.Name()
} 

func (self *FileStorageProvider) Close() {
	self.root.Close() // ignore any error
	self.root= nil
} 


/**
 * MUST be called when the session is no longer needed.
 * Any pending changed are flushed before closing.
 */
func (self *FileStorageSession) Close() {
	self.Flush()
}
	
/**
 * Returns the complete paths to all the children of the given path.
 */
func (self *FileStorageSession) ListChildren(element *id.Identifier) []id.Identifier {
	return self.provider.taskMaster.Submit(tasks.LIST, element, func() interface{} {
		elementPath:= filepath.FromSlash(element.Path())
		filepath:= filepath.Join(self.provider.root.Name(), elementPath)
		
		dir, err:= os.Open(filepath)
		if err != nil { panic(err)}
		
		fi, err:= dir.Readdir(0)
		if err != nil {	panic(err) }
		
		children:= make([]id.Identifier, 0, len(fi))
		for i:= len(fi)-1; 0 <= i; i-= 1 {
			if fi[i].IsDir() {
				children= append(children, *element.Child(fi[i].Name()))
			}
		}
	}).Get().([]id.Identifier)
}
	
/**
 * @return the contents of of the given path, or null if the file does not exist.
 */
func (self *FileStorageSession) Fetch(element *id.Identifier) []byte {
	return self.provider.taskMaster.Submit(tasks.READ, element, func() interface{} {
		elementPath:= filepath.FromSlash(element.Path())
		filepath:= filepath.Join(self.provider.root.Name(), elementPath)
		
		bytes, err:= ioutil.ReadFile(filepath)
		if err != nil { panic(err)}
	}).Get().([]byte)
}

/**
 * Stores the given contents at the given location.
 * The file is created if it does not already exist.
 */
func (self *FileStorageSession) Store(element *id.Identifier, content []byte) {
	self.provider.taskMaster.Submit(tasks.WRITE, element, func() interface{} {
		elementPath:= filepath.FromSlash(element.Path())
		filepath:= filepath.Join(self.provider.root.Name(), elementPath)
		
		err:= ioutil.WriteFile(filepath, content, 0/*(os.FileMode(0777)*/)
		if err != nil { panic(err)}
	}).Get();
}

/**
 * Stores the given contents at the given location if the file 
 * does not already exist.  
 * If the file already exists then self method does nothing.
 * 
 * @param waitMillis
 * 		if the file already exists and parameter is greater than zero   
 * 		then wait the denoted number of milliseconds for some other 
 * 		process to delete the file.
 * 
 * @return 
 * 		true if the file was created, false if the file already exists 
 * 		and was not deleted within the wait period.
 */
func (self *FileStorageSession) Create(path *id.Identifier, content []byte, waitMillis uint64) bool {
	return self.provider.taskMaster.Submit(tasks.CREATE, element, func() interface{} {
	}).Get().(bool);
}

/**
 * Deletes the contents stored at the given locations.
 */
func (self *FileStorageSession) Delete(path *id.Identifier) {
	self.provider.taskMaster.Submit(tasks.DELETE, element, func() interface{} {
		elementPath:= filepath.FromSlash(element.Path())
		filepath:= filepath.Join(self.provider.root.Name(), elementPath)
		
		err:= os.Remove(filepath)
		if err != nil { panic(err)}
	}).Wait()
}

/**
 * Flush any pending changes made by self session to physical storage.
 */
func (self *FileStorageSession) Flush() {
	self.provider.taskMaster.Wait()
}

