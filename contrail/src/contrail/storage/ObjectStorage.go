package storage

import (
	"bytes"
	"time"
	"encoding/gob"
	"contrail/id"
	"contrail/tasks"
)

/**
 * An API for storing objects to a raw storage instance.
 * This storage API...
 * 
 * 	...handles the serialization of objects to byte streams.
 * 
 * 	...introduces some JPA-like lifecycle management.
 * 		If a stored object implements the Lifecycle interface then 
 * 		the Lifecycle methods will be invoked at appropriate points.
 * 
 * 	...caches objects in order to avoid as much serialization and 
 * 		deserialization as possible.
 * 
 * 	...can be used by multiple clients in multiple threads.
 *		Each client should call the ObjectStorage.Connect method to create its 
 *		own session.  
 * 		It is safe to use an instance of ObjectStorage from multiple threads. 
 * 		It is also safe to use an instance of ObjectStorage.Session from 
 * 		multiple threads. 
 * 
 * 	...is parallel.  Each session manages the order in which its internal tasks 
 *		are executed so that operations are performed in a way that allows for 
 *		as much parallelization as possible in order to maximize performance.  
 *   
 * @author Ted Stockwell
 */
type ObjectStorage struct {
	storageProvider StorageProvider
}

type ObjectStorageSession struct {
	owner *ObjectStorage
	storageSession StorageSession
	cache id.TreeStorage
	taskMaster *tasks.TaskMaster
}

func CreateObjectStorage (storageProvider StorageProvider) *ObjectStorage {
	return &ObjectStorage { storageProvider: storageProvider }
}

func (self *ObjectStorage) Connect() *ObjectStorageSession {
	return &ObjectStorageSession {
		owner: self,
		storageSession: self.storageProvider.Connect(),
		cache:  id.CreateLRUTreeStorage(cacheSize),
		taskMaster: tasks.CreateTaskMaster(),
	}
}

func (self *ObjectStorageSession) Store(identifier *id.Identifier, item interface{}) {

	serializeTask:= tasks.GoResult(func() interface{} { return SerializeObject(item) })
	
	lifecycle:= item.(Lifecycle)
	if lifecycle != nil {
		lifecycle.SetStorage(self)
	}

	self.taskMaster.Submit(tasks.WRITE, identifier, func() interface{} {
		self.cache.Store(identifier, item);
		if (lifecycle != nil) {
			lifecycle.OnInsert(identifier)
		}
		self.storageSession.Store(identifier, serializeTask.Get().([]byte))
		return nil
	})
}

func (self *ObjectStorageSession) Delete(path *id.Identifier) {
	self.taskMaster.Submit(tasks.DELETE, path, func() interface{} {
		item:= self.Fetch(path)
		self.cache.Delete(path)
		self.storageSession.Delete(path)
		lifecycle:= item.(Lifecycle)
		if lifecycle != nil {
			lifecycle.OnDelete()
		}
		return nil
	})
}

func (self *ObjectStorageSession) Fetch(path *id.Identifier) interface{} {
	return self.taskMaster.Submit(tasks.READ, path, func() interface{} {
		storable:= self.cache.Fetch(path)
		if storable == nil {
			bytes:= self.storageSession.Fetch(path)
			if bytes != nil { 
				storable= self.readStorable(path, bytes)
			}
		}
		return storable
	}).Get()
}

func (self *ObjectStorageSession) readStorable(path *id.Identifier, contents []byte) interface{} {
	if contents == nil { return nil }

	storable:= DeserializeObject(contents)
	if storable == nil {  return nil }
	
	lifecycle:= storable.(Lifecycle)
	if lifecycle != nil { lifecycle.SetStorage(self) }
	
	self.cache.Store(path, storable)
	
	if lifecycle != nil { lifecycle.OnLoad(path) }
	return storable
}

func (self *ObjectStorageSession) FetchChildren(path *id.Identifier) id.TreeStorage {
	return self.taskMaster.Submit(tasks.LIST, path, func() interface{} {
		results:= id.CreateTreeStorage()
		children:= self.storageSession.ListChildren(path)
		todo:= make([]*tasks.Future, len(children))
		for i,childId:= range children {
			todo[i]= tasks.Go(func() {
				bytes:= self.storageSession.Fetch(childId)
				object:= self.readStorable(childId, bytes)
				results.Store(childId, object)
			})
		}
		tasks.WaitAll(todo)
		return results
	}).Get().(id.TreeStorage)
}


/*
 * Implementation Note:
 * Caching is not problematic, even though there can be multiple processes writing 
 * to storage, since the higer level Contrail APIs never changes a stored object, 
 * it only writes new versions.  
 * However, while caching stored objects is not a problem, caching lists of children is,
 * since obviously other processes are adding children. 
 */
func (self *ObjectStorageSession) ListChildren(path *id.Identifier) []*id.Identifier {
	return self.taskMaster.Submit(tasks.LIST, path, func() interface{} {
		return self.storageSession.ListChildren(path)
	}).Get().([]*id.Identifier)
}

func (self *ObjectStorageSession) Flush() {
	self.taskMaster.Join()
	self.storageSession.Flush()
}

func (self *ObjectStorageSession) Close() {
	self.Flush()
	self.taskMaster.Close()
	self.storageSession.Close()
	
	self.storageSession= nil
	self.taskMaster= nil
	self.cache= nil
}

func (self *ObjectStorageSession) Create(path *id.Identifier, item interface{}, wait time.Duration) bool {
	serializeTask:= tasks.GoResult(func() interface{} { return SerializeObject(item) })
	return self.taskMaster.Submit(tasks.LIST, path, func() interface{} {
		created:= false;
		if self.storageSession.Create(path, serializeTask.Get().([]byte), wait) {
			lifecycle:= item.(Lifecycle)
			if lifecycle != nil {
				lifecycle.SetStorage(self)
			}
			self.cache.Store(path, item)
			if lifecycle != nil {
				lifecycle.OnInsert(path)
			}
			created= true;
		}
		return created
	}).Get().(bool)
}


func (self *ObjectStorageSession) DeleteAll(paths []*id.Identifier) {
	todo:= make([]*tasks.Future, len(paths))
	for i,path:= range paths {
		todo[i]= tasks.Go(func() { self.Delete(path) })
	}
	tasks.WaitAll(todo)
}

func (self *ObjectStorageSession) DeleteAllChildren(path *id.Identifier) {
	children:= self.ListChildren(path);
	todo:= make([]*tasks.Future, len(children))
	for i,path:= range children {
		todo[i]= tasks.Go(func(){ self.Delete(path) })
	}
	tasks.WaitAll(todo)
}

func SerializeObject(item interface{}) []byte {
    buffer:= new(bytes.Buffer)
    enc := gob.NewEncoder(buffer) 
    err := enc.Encode(item)
    if err != nil {
        panic(err)
    }
    return buffer.Bytes()
}


func DeserializeObject(data []byte) interface{} {
    buffer:= bytes.NewBuffer(data)
    dec := gob.NewDecoder(buffer) 
    var item interface{}
    err:= dec.Decode(item)
    if err != nil {
        panic(err)
    }
    return item	
}