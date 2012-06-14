package id

import (
	"contrail/util/lru"
)


/**
 * Just like TreeStorage except that the least recently used items are  
 * removed in order to conserve memory.
 * If the itemCount passed to the constructor is <= 0 then items will never expire.   
 *   
 * @author Ted Stockwell
 */
type LRUIdStorage struct {
	storage TreeStorage
	maxEntries int
	cache *lru.Cache
}

func CreateLRUTreeStorage(maxEntries int) TreeStorage {
	self:= new(LRUIdStorage)
	self.storage= CreateTreeStorage()
	self.maxEntries= maxEntries
	self.cache= lru.New(maxEntries)
	self.cache.OnDelete(func(c *lru.Cache, key string, value interface{}) {
		self.storage.Delete(value.(*Identifier))
	})
	return TreeStorage(self)
}

func (self *LRUIdStorage) touch(identifier *Identifier) {
	self.cache.Add(identifier.Path(), identifier)
}

func (self *LRUIdStorage) touchAll(ids []*Identifier) {
	for _,i:= range ids {
		self.cache.Add(i.Path(), i)
	}
}

func (self *LRUIdStorage) Store(id *Identifier, content interface{}) {
	self.storage.Store(id, content)
	self.touch(id)		
}

func (self *LRUIdStorage) Delete(id *Identifier) {
	self.storage.Delete(id)
}

func (self *LRUIdStorage) Clear() {
	self.storage.Clear()
}

func (self *LRUIdStorage) DeleteAll(paths []*Identifier) {
	self.storage.DeleteAll(paths)
}

func (self *LRUIdStorage) Exists(id *Identifier) bool {
	self.touch(id)		
	return self.storage.Exists(id)
}

func (self *LRUIdStorage) Fetch(id *Identifier) interface{} {
	self.touch(id)		
	return self.storage.Fetch(id)
}

func (self *LRUIdStorage) FetchAll(ids []*Identifier) TreeStorage {
	self.touchAll(ids)		
	return self.storage.FetchAll(ids)
}

func (self *LRUIdStorage) FetchChildren(id *Identifier) TreeStorage {
	self.touch(id)		
	children:= self.storage.FetchChildren(id)
	self.touchAll(children.ListAll())
	return children
}

func (self *LRUIdStorage) FetchChildrenAll(ids []*Identifier) TreeStorage {
	self.touchAll(ids)		
	allChildren:= self.storage.FetchChildrenAll(ids)
	for _,children:= range allChildren.Values() {
		self.touchAll(children.(*IdStorage).ListAll())
	}
	return allChildren
}

func (self *LRUIdStorage) ListChildren(id *Identifier) []*Identifier {
	self.touch(id)		
	children:= self.storage.ListChildren(id)
	self.touchAll(children)
	return children
}

func (self *LRUIdStorage) ListChildrenAll(ids []*Identifier) TreeStorage {
	self.touchAll(ids)		
	allChildren:= self.storage.ListChildrenAll(ids)
	for _,children:= range allChildren.Values() {
		self.touchAll(children.([]*Identifier))
	}
	return allChildren
}

func (self *LRUIdStorage) ListAll() []*Identifier {
	all:= self.storage.ListAll()
	self.touchAll(all)		
	return all
}

func (self *LRUIdStorage) Values() []interface{} {
	return self.storage.Values()
}

func (self *LRUIdStorage) Size() int {
	return self.storage.Size()
}

func (self *LRUIdStorage) FetchDescendents(id *Identifier) TreeStorage {
	self.touch(id)		
	allChildren:= self.storage.FetchDescendents(id)
	for _,children:= range allChildren.Values() {
		self.touchAll(children.(*IdStorage).ListAll())
	}
	return allChildren
}

func (self *LRUIdStorage) VisitNode(id *Identifier, visit VisitFunction) {
	self.storage.VisitNode(id, visit)
}

func (self *LRUIdStorage) VisitParents(id *Identifier, visit VisitFunction) {
	self.storage.VisitParents(id, visit)
}

func (self *LRUIdStorage) VisitDescendents(id *Identifier, visit VisitFunction) {
	self.storage.VisitDescendents(id, visit)
}

func (self *LRUIdStorage) VisitChildren(id *Identifier, visit VisitFunction) {
	self.storage.VisitChildren(id, visit)
}
