package storage

import (
	"math"
	"testing"
	"time"
	"strconv"
	"contrail/id"
	"contrail"
	"sync/atomic"
)


type ObjectStorageTester struct {
	provider StorageProvider
	storage *ObjectStorage
}

func CreateObjectStorageTester(provider StorageProvider) *ObjectStorageTester {
	return &ObjectStorageTester {
		provider: provider,
		storage: CreateObjectStorage(provider),
	}
}

func (self *ObjectStorageTester) Close() {
	self.storage.Close()	
}


func (self *ObjectStorageTester) TestBasicObjectStorage(t *testing.T) {
	session:= self.storage.Connect()
	item:= contrail.CreateItemImpl(id.CreateIdentifier("person-0.1"))
	session.Store(item.Identifier(), item)
	session.Flush()
	item2:= session.Fetch(item.Identifier()).(contrail.Item)
	if item2 == nil  { t.Errorf("item2 == nil"); return } 
	if item2.Identifier() != item.Identifier() { t.Errorf("item2.Identifier() != item.Identifier()"); return } 
}


func (self *ObjectStorageTester) TestSimpleStorage(t *testing.T) {
	session:= self.storage.Connect()
	id:= id.UniqueIdentifier()
	content:= ";lkjasdpoifjasdfkjjwerqwefpq"
	session.Store(id, []byte(content))
	session.Flush()
	fetched:= string(session.Fetch(id).([]byte))
	if (content != fetched) { t.Errorf("fetched content != stored content"); return } 
	
	child:= id.Child("huey")
	session.Store(child, []byte(content))
	session.Flush()
	fetched= string(session.Fetch(child).([]byte))
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
	fetched= string(session.Fetch(child).([]byte))
	if (content != fetched) { t.Errorf("fetched content != stored content"); return } 
}

/**
 * Test multiple simultaneous creates/deletes. 
 */
func (self *ObjectStorageTester) TestCreateMulti(t *testing.T) {
	const ( content= "hello" )
	
	identifier:= id.CreateIdentifier("file-create-test")
	session:= self.storage.Connect()
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
func (self *ObjectStorageTester) TestCreateHardcore(t *testing.T) {
	const ( content= "hello" )
	
	// test that only one goroutine can create the same file at a time
	for f:= 0; f < 10; f++ {			
		identifier:= id.CreateIdentifier("file-"+strconv.Itoa(f))
		session:= self.storage.Connect()
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
func (self *ObjectStorageTester) TestConcurrentStoreAPIListChildren(t *testing.T) {
	session:= self.storage.Connect()
	
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

	
