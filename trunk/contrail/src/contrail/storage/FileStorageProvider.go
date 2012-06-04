package storage

import (
	"os"
	"io/ioutil"
	"path/filepath"
	"contrail/id"
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
 * This implementation is only meant for embedded use by a single process.
 * Another class, ServerStorageProvider, implements an HTTP API on top of 
 * this class that provides multi-user, client-server access to a file store. 
 *  
 * @see ServerStorageProvider for client/server access to a file store 
 * 
 * @author Ted Stockwell
 */

type FileStorageProvider struct {
	root *os.File // path the root of database file system
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
	return &FileStorageProvider{ root:file }
}


/**
 * Start a storage session. 
 */
func (this *FileStorageProvider) Connect() StorageSession {
	return &FileStorageSession { provider:this }
} 

func (this *FileStorageProvider) Root() string {
	return this.root.Name()
} 

func (this *FileStorageProvider) Close() {
	this.root.Close() // ignore any error
	this.root= nil
} 


/**
 * MUST be called when the session is no longer needed.
 * Any pending changed are flushed before closing.
 */
func (this *FileStorageSession) Close() {
	this.Flush()
}
	
/**
 * Returns the complete paths to all the children of the given path.
 */
func (this *FileStorageSession) ListChildren(element *id.Identifier) []id.Identifier {
	elementPath:= filepath.FromSlash(element.Path())
	filepath:= filepath.Join(this.provider.root.Name(), elementPath)
	
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
	return children
}
	
/**
 * @return the contents of of the given path, or null if the file does not exist.
 */
func (this *FileStorageSession) Fetch(element *id.Identifier) []byte {
	elementPath:= filepath.FromSlash(element.Path())
	filepath:= filepath.Join(this.provider.root.Name(), elementPath)
	
	bytes, err:= ioutil.ReadFile(filepath)
	if err != nil { panic(err)}
	
	return bytes
}

/**
 * Stores the given contents at the given location.
 * The file is created if it does not already exist.
 */
func (this *FileStorageSession) Store(element *id.Identifier, content []byte) {
	elementPath:= filepath.FromSlash(element.Path())
	filepath:= filepath.Join(this.provider.root.Name(), elementPath)
	
	err:= ioutil.WriteFile(filepath, content, 0/*(os.FileMode(0777)*/)
	if err != nil { panic(err)}
}

/**
 * Stores the given contents at the given location if the file 
 * does not already exist.  
 * If the file already exists then this method does nothing.
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
func (this *FileStorageSession) Create(path *id.Identifier, content []byte, waitMillis uint64) bool {
func Open(name string) (file *File, err error)
}

/**
 * Deletes the contents stored at the given locations.
 */
func (this *FileStorageSession) Delete(path *id.Identifier) {
}

/**
 * Flush any pending changes made by this session to physical storage.
 */
func (this *FileStorageSession) Flush() {
}

