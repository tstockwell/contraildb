package storage

import (
	"math"
	"testing"
	"time"
	"strconv"
	"contrail/id"
	"sync/atomic"
)

type StorageProviderTester struct {
	provider StorageProvider
}

func CreateStorageProviderTester(provider StorageProvider) *StorageProviderTester {
	return &StorageProviderTester {
		provider: provider,
	}
}

func (self *StorageProviderTester) Close() {
	// do nothing
	// provider should be closed by its creator	
}


func (self *StorageProviderTester) TestSimpleStorage(t *testing.T) {
	session:= self.provider.Connect()
	id:= id.UniqueIdentifier()
	content:= ";lkjasdpoifjasdfkjjwerqwefpq"
	session.Store(id, []byte(content))
	session.Flush()
	fetched:= string(session.Fetch(id))
	if (content != fetched) { t.Errorf("fetched content != stored content"); return } 
	
	child:= id.Child("huey")
	session.Store(child, []byte(content))
	session.Flush()
	fetched= string(session.Fetch(child))
	if (content != fetched) { t.Errorf("fetched content != stored content"); return } 
	
	children:= session.ListChildren(id)
	if (len(children) != 1) { t.Errorf("children not listed correctly"); return } 
	if (children[0] != child) { t.Errorf("wrong child"); return } 
		
	session.Delete(child)		
	session.Flush()
	children= session.ListChildren(id)
	if (len(children) != 0) { t.Errorf("delete didn't work"); return } 
		
	session.Create(child, []byte(content), 0)
	session.Flush()
	fetched= string(session.Fetch(child))
	if (content != fetched) { t.Errorf("fetched content != stored content"); return } 
}

/**
 * Test multiple simultaneous creates/deletes. 
 */
func (self *StorageProviderTester) TestCreateMulti(t *testing.T) {
	const ( content= "hello" )
	
	identifier:= id.CreateIdentifier("file-create-test")
	session:= self.provider.Connect()
	count:= int32(0)
	done:= make(chan bool,20)
	for i:= 0; i < 20; i++ {
		go func() {
			defer func() { done <- true }()

			created:= session.Create(identifier, []byte(content), math.MaxInt32)
			if created { 
				totalCreates:= atomic.AddInt32(&count, 1)
				if 1 < totalCreates  { t.Errorf("More than one lock was granted"); return }
				time.Sleep(time.Millisecond*10)
				atomic.AddInt32(&count, -1)
				session.Delete(identifier)
			}
		}()
	}
	
	// wait for all routnines to end
	for i:= 0; i < 20; i++ { <-done }
	
	session.Close()
}
	
/**
 * Test multiple simultaneous creates/deletes. 
 */
func (self *StorageProviderTester) TestCreateHardcore(t *testing.T) {
	const ( content= "hello" )
	
	// test that only one goroutine can create the same file at a time
	for f:= 0; f < 10; f++ {			
		identifier:= id.CreateIdentifier("file-"+strconv.Itoa(f))
		session:= self.provider.Connect()
		count:= int32(0)
		done:= make(chan bool,20)
		for i:= 0; i < 20; i++ {
			go func() {
				defer func() {done <- true}()
			
				created:= session.Create(identifier, []byte(content), math.MaxInt32)
				if !created { t.Errorf("failed to create "+identifier.Path()); return } 
				totalCreates:= atomic.AddInt32(&count, 1)
				if 1 < totalCreates  { t.Errorf("More than one lock was granted"); return }
				time.Sleep(time.Millisecond*10)
				atomic.AddInt32(&count, -1)
				session.Delete(identifier)
			}()
		}
		
		// wait for all routnines to end
		for i:= 0; i < 20; i++ { <-done }
		
		session.Close()
	}
}
	

/**
 * Reproduces a bug where a stored object was not returned from the listChildren method.
 */
func (self *StorageProviderTester) TestConcurrentStoreAPIListChildren(t *testing.T) {
	session:= self.provider.Connect()
	
	done:= make(chan bool,100)
	for f:= 0; f < 10; f++ {			
		folderId:= id.CreateIdentifier(strconv.Itoa(f))
		for j:= 0; j < 10; j++ {
			task:= strconv.Itoa(j)
			for i:= 0; i < 10; i++ {
				childName:= task+"-"+strconv.Itoa(i)
				go func() {
					defer func() {
						done<-true
					}()
				
					// store an object in a folder
					id:= folderId.Child(childName)
					session.Store(id, []byte(childName))
					session.Flush()
					
					// now, list the folder's children and make sure our object is listed
					children:= session.ListChildren(folderId)
					
					foundId:= false
					for _,childId:= range children { if childId.Path() == id.Path() { foundId= true; break } }
					 
					if !foundId { 
						t.Errorf("Problem in listChildren.\nFolder does not contain "+id.Path())
					}
				}()
			}
		}
	}
	for f:= 0; f < 10; f++ {			
		for t:= 0; t < 10; t++ {
			<-done
		}
	}
	
}
