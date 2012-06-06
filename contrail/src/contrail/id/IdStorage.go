package id

import (
	"sync"
)

/**
 * IdStorage stores objects in a tree, in memory, according to their associated Identifier.
 * This container arranges the items in a tree of nodes.
 * The nodes can be navigated in tree-like fashion. 
 * Is thread-safe.
 * 
 * This class also defines an interface for visiting nodes.
 * There are several methods named Visit* that may be used to visit nodes.
 *  
 * @author Ted Stockwell
 */
type IdStorage struct {
	contents map[string]*tNode // map to all nodes
	values map[string]*tNode   // map to nodes with values
	lock *sync.Mutex
}

type tNode struct {
	identifier *Identifier
	children map[string]*tNode
	parent *tNode
	content interface{}
}
type VisitFunction func(identifier *Identifier, content interface{})

func CreateIdStorage() *IdStorage {
	this:= new(IdStorage)
	this.contents= make(map[string]*tNode, 10)
	this.values= make(map[string]*tNode, 10)
	this.lock= new(sync.Mutex)
	return this
}

func (this *IdStorage) store(id *Identifier, content interface{}) *tNode {
	path:= id.Path()
	node:= this.contents[path]
	if node == nil {
		node= &tNode {
			identifier:id,
			content:content,
			children:make(map[string]*tNode, 0),
		}
		this.contents[path]= node
	}
	node.content= content

	parentId:= id.Parent()
	if parentId != nil {  
		parentNode:= this.contents[parentId.Path()]
		if parentNode == nil { 
			parentNode= this.store(parentId, nil)
		}
		parentNode.children[path]= node
		node.parent= parentNode
	}

	return node
}

func (this *IdStorage) Store(id *Identifier, content interface{}) {
	this.lock.Lock()
	defer this.lock.Unlock()
	
	node:= this.store(id, content)
	this.values[id.Path()]= node
}

func (this *IdStorage) delete(node *tNode) {
	if node != nil {
		node.content= nil

		// if the node has no children then we can delete the node also 		
		if len(node.children) <= 0 {
			path:= node.identifier.Path()
			delete(this.contents, path)
			delete(this.values, path)
			if node.parent != nil {
				parent:= node.parent
				delete(parent.children, path);
				if len(parent.children) <= 0 && parent.content == nil {
					this.delete(parent)
				}
			}
		}
	}
}

func (this *IdStorage) Delete(id *Identifier) {
	this.lock.Lock()
	defer this.lock.Unlock()
	
	this.delete(this.contents[id.Path()])
}

func (this *IdStorage) Clear() {
	this.lock.Lock()
	defer this.lock.Unlock()

	// clear the map
	this.contents= make(map[string]*tNode, 10)
	this.values= make(map[string]*tNode, 10)
}

func (this *IdStorage) DeleteAll(paths []*Identifier) {
	this.lock.Lock()
	defer this.lock.Unlock()

	for _,id:= range paths { 
		this.delete(this.contents[id.Path()])
	}
}

func (this *IdStorage) Exists(id *Identifier) bool {
	this.lock.Lock()
	defer this.lock.Unlock()
	
	return this.values[id.Path()] != nil
}

func (this *IdStorage) Fetch(id *Identifier) interface{} {
	this.lock.Lock()
	defer this.lock.Unlock()

	node:= this.contents[id.Path()]
	if node == nil {
		return nil
	}
	return node.content
}

/**
 * Returns an IdStorage that contains all the found results for the given 
 * identifiers
 */  
func (this *IdStorage) FetchAll(ids []*Identifier) *IdStorage {
	this.lock.Lock()
	defer this.lock.Unlock()

	items:= CreateIdStorage()
	for _, id:= range ids {
		node:= this.contents[id.Path()]
		if node != nil && node.content != nil {
			items.Store(id, node.content)
		}
	}
	return items
}

/**
 * Returns an IdStorage that contains all the found results for the given 
 * identifier
 */  
func (this *IdStorage) FetchChildren(id *Identifier) *IdStorage {
	this.lock.Lock()
	defer this.lock.Unlock()

	items:= CreateIdStorage()
	n:= this.contents[id.Path()]
	if n != nil && n.children != nil && 0 < len(n.children) {
		for _, node:= range n.children {
			if node.content != nil {
				items.Store(node.identifier, node.content)
			}
		}
	}
	return items
}

/**
 * Returns an IdStorage that contains all the found results for the given 
 * identifiers.
 * The returned IdStorage has values with type IdStorage
 */  
func (this *IdStorage) FetchChildrenAll(ids []*Identifier) *IdStorage {
	this.lock.Lock()
	defer this.lock.Unlock()

	results:= CreateIdStorage()
	for _, id:= range ids {
		results.Store(id, this.FetchChildren(id))
	}
	return results
}

func (this *IdStorage) ListChildren(id *Identifier) []*Identifier {
	this.lock.Lock()
	defer this.lock.Unlock()

	n:= this.contents[id.Path()]
	if n != nil && 0 < len(n.children) {
		list:= make([]*Identifier, 0, len(n.children))
		for _, node:= range n.children {
			list= append(list, node.identifier)
		}
		return list
	}
	return make([]*Identifier, 0)
}

/**
 * Returns an IdStorage that contains all the found results for the given 
 * identifiers.
 * The returned IdStorage has values with type []Identifier
 */  
func (this *IdStorage) ListChildrenAll(ids []*Identifier) *IdStorage {
	this.lock.Lock()
	defer this.lock.Unlock()

	results:= CreateIdStorage()
	for _,id:= range ids { 
		results.Store(id, this.ListChildren(id))
	}
	return results
}

func (this *IdStorage) ListAll() []*Identifier {
	this.lock.Lock()
	defer this.lock.Unlock()

	list:= make([]*Identifier, 0, len(this.contents))
	for _,node:= range this.values { 
		list= append(list, node.identifier)
	}
	return list
}

func (this *IdStorage) Values() []interface{} {
	this.lock.Lock()
	defer this.lock.Unlock()
	
	values:= make([]interface{}, 0, len(this.contents))
	for _, node:= range this.values {
		values= append(values, node.content)
	}
	return values
}

func (this *IdStorage) Size() int {
	this.lock.Lock()
	defer this.lock.Unlock()
	
	return len(this.values)
}


/**
 * Returns an IdStorage that contains all the found results for the given 
 * identifiers.
 */  
func (this *IdStorage) FetchDescendents(id *Identifier) *IdStorage {
	this.lock.Lock()
	defer this.lock.Unlock()

	n:= this.contents[id.Path()]
	items:= CreateIdStorage()
	if n != nil && n.children != nil && 0 < len(n.children) {
		todo:= make([]*tNode, 0, 10)
		todo= append(todo, n)
		for ;0 < len(todo); {
			n= todo[0]
			todo= todo[1:]
			for _,node:= range n.children {
				if (node.content != nil) {
					items.Store(node.identifier, node.content)
				}
				todo=append(todo, node)
			}
		}
	}
	return items
}

func (this *IdStorage) VisitNode(id *Identifier, visit VisitFunction) {
	this.lock.Lock()
	defer this.lock.Unlock()

	node:= this.contents[id.Path()]
	if node != nil { 
		visit(node.identifier, node.content)
	}
}

func (this *IdStorage) VisitParents(id *Identifier, visit VisitFunction) {
	this.lock.Lock()
	defer this.lock.Unlock()

	node:= this.contents[id.Path()]
	if node != nil {
		for n:= node.parent; n != nil; n= n.parent {
			visit(n.identifier, n.content)
		}
	}
}

func (this *IdStorage) VisitDescendents(id *Identifier, visit VisitFunction) {
	this.lock.Lock()
	defer this.lock.Unlock()

	n:= this.contents[id.Path()]
	if n != nil { 
		todo:= make([]*tNode, 0, 10)
		todo= append(todo, n)
		for ;0 < len(todo); {
			n= todo[0]
			todo= todo[1:]
			for _, node:= range n.children {
				todo= append(todo, node)
				visit(node.identifier, node.content)
			}
		}
	}
}

func (this *IdStorage) VisitChildren(id *Identifier, visit VisitFunction) {
	this.lock.Lock()
	defer this.lock.Unlock()

	n:= this.contents[id.Path()]
	if n != nil { 
		for _,node:= range n.children {
			visit(node.identifier, node.content)
		}
	}
}

