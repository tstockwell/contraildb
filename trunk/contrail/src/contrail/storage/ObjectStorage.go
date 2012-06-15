package storage

import (
	"time"
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
	storageProvider *StorageProvider
}

type ObjectStorageSession struct {
	owner *ObjectStorage
	storageSession *StorageSession
	cache TreeStorage
	taskMaster *TaskMaster
}

func CreateObjectStorage (storageProvider *StorageProvider) *ObjectStorage {
	return &ObjectStorage { storageProvider: storageProvider }
}

func (self *ObjectStorage) Connect() *ObjectStorageSession {
	return &ObjectStorageSession {
		owner: self
		storageSession: self.storageProvider.Connect()
		cache:  id.CreateLRUTreeStorage(),
		taskMaster: tasks.CreateTaskManager(),
	}
}


func (self *ObjectStorageSession) Store(identifier *id.Identifier, item interface{}) {

	serializeTask:= tasks.GoResult(func() { return SerializeObject(item) }
	
	lifecycle:= item.(Lifecycle)
	if lifecycle != nil {
		lifecycle.setStorage(self)
	}

	taskMaster.Submit(identifier, WRITE, func() interface{} {
		self.cache.Store(identifier, item);
		if (lifecycle != nil) {
			lifecycle.OnInsert(identifier)
		}
		self.storageSession.Store(identifier, serializeTask.Get().([]byte))
		return nil
	})
}

func (self *ObjectStorageSession) Delete(path *Identifier) {
	fetchTask:= tasks.GoResult(func() { return self.Fetch(path) }
	taskMaster.Submit(path, DELETE, func() interface{} {
		self.cache.Delete(path)
		self.storageSession.Delete(path)
		lifecycle:= item.(Lifecycle)
		if lifecycle != nil {
			lifecycle.OnDelete()
		}
		return nil
	})
}

func (self *ObjectStorageSession) Fetch(path *Identifier) interface{} 
{
	return taskMaster.Submit(path, READ, func() interface{} {
		storable:= self.cache.Fetch(path)
		if storable == nil {
			bytes:= self.storageSession.Fetch(path)
			if bytes != nil { 
				storable= readStorable(path, bytes);
			}
		}
		return storable
	}).Get()
}

func (self *ObjectStorageSession) readStorable(Identifier id, byte[] contents) interface{}
{
	if contents == nil { return nil }

	storable:= DeserializeObject(contents)
	if storable == nil {  return nil }
	
	lifecycle:= storable.(Lifecycle)
	if lifecycle != nil { lifecycle.SetStorage(self) }
	
	self.cache.Store(id, storable)
	
	if lifecycle != nil { lifecycle.OnLoad() }
	return storable
}

func (self *ObjectStorageSession) FetchChildren(Identifier path) TreeStorage
{
	return taskMaster.Submit(path, LIST, func() interface{} {
		results:= CreateTreeStorage()
		children:= self.storageSession.ListChildren(path).Get().([]*id.Identifier)
		todo:= make([]Future, 0, len(children))
		for _,childId:= range children {
			todo= append(todo, tasks.GoResult(func() interface{} {
				bytes:= self.storageSession.Fetch(childId)
				object:= self.readStorable(childId, bytes)
				results.Store(childId, object)
			})
		}
		tasks.JoinAll(todo)
		return results
	}.Get().(TreeStorage)
}


/*
 * Implementation Note:
 * Caching is not problematic, even though there can be multiple processes writing 
 * to storage, since the higer level Contrail APIs never changes a stored object, 
 * it only writes new versions.  
 * However, while caching stored objects is not a problem, caching lists of children is,
 * since obviously other processes are adding children. 
 */
func (self *ObjectStorageSession) ListChildren(path *Identifier) []*id.Identifier
{
	return taskMaster.Submit(path, LIST, func() interface{} {
		return self.storageSession.ListChildren(path)
	}.Get().([]*id.Identifier)
}

func (self *ObjectStorageSession) Flush() {
	taskMaster.Join()
	self.storageSession.Flush()
}

func (self *ObjectStorageSession) Close() {
	self.Flush()
	taskMaster.Close()
	storageSession.Close()
	
	storageSession= nil
	taskMaster= nil
	cache= nil
}

func (self *ObjectStorageSession) Create(identifier *Identifier, item interface{}, wait time.Duration) interface{} bool {
	serializeTask:= tasks.GoResult(func() { return SerializeObject(item) })
	return taskMaster.Submit(path, LIST, func() interface{} {
		created:= false;
		if self.storageSession.Create(identifier, serializeTask.Get().([]byte), wait) {
			lifecycle:= item.(Lifecycle)
			if lifecycle != nil {
				lifecycle.SetStorage(self)
			}
			self.cache.Store(identifier, item)
			if lifecycle != nil {
				lifecycle.OnInsert(identifier)
			}
			created= true;
		}
		return created
	}.Get().(bool)
}


public void delete(Identifier... paths) throws IOException {
	for (Identifier identifier:paths)
			delete(identifier);
}

public void deleteAllChildren(Identifier... paths) throws IOException {
	deleteAllChildren(Arrays.asList(paths));
}

public void deleteAllChildren(Iterable<Identifier> paths) throws IOException {
	for (Identifier identifier:paths)
		deleteAllChildren(identifier);
}

public void deleteAllChildren(Identifier path) {
	final IResult<Collection<Identifier>> children= listChildren(path);
	ContrailAction action= new ContrailAction(path, Operation.LIST) {
		protected void run() {
			for (Identifier identifier: children.get()) {
				delete(identifier);
			}
		}
	};
	_trackerSession.submit(action);
}


	}
}
