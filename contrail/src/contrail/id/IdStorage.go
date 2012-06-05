package id

import {
	"sync"
}

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
	contents map[String]tNode
	lock sync.Mutex
}
type Visitor interface {
	Visit(identifier Identifier, content interface{});
}

type tNode struct {
	identifier Identifier
	children map[String]tNode
	parent tNode
	content interface{}
}

func CreateIdStorage *IdStorage {
	this:= IdStorage{
		contents:make(map[String]tNode, 0, 10)
		lock:sync.Mutex= sync.Mutex {}
	}
	return &this;
}

func (this *IdStorage) Store(id Identifier, content interface{}) {
		store(id, content);
}
func (this *IdStorage) store(id Identifier p, content interface{}) tNode {
	this.lock.Lock()
	defer this.lock.Unlock()
		
	path:= p.Path()
	node:= this.contents[path]
	if node == nill {
		node= tNode {
			identifier:p,
			content:content
		}
		this.contents[path]= node;
	}
	node.content= content;

	parentId *Identifier = p.Parent();
	if parentId != nil) {  
		tNode parentNode= this.contents[parentId.Path()]
		if parentNode == nil { 
			parentNode= store(parentId, nil)
		}
		parentNode.children[path]= node
		node.parent= parentNode
	}

	return node;
}

func (this *IdStorage) Delete(id Identifier) {
	delete(_contents.get(i));
}

func (this *IdStorage) delete(node tNode) {
	this.lock.Lock()
	defer this.lock.Unlock()
	
	if node != nil {
		node.content= nil

		// if the node has no children then we can delete the node also 		
		if len(node.children) <= 0 {
			path:= node.identifier.Path();
			delete(this.contents, path);
			if node.parent != nil {
				parent:= node.parent; 
				delete(parent.children, path);
				if len(parent.children) <= 0 && parent.content == nil)
					delete(parent);
			}
		}
	}
}

func (this *IdStorage) Clear() {
	this.lock.Lock()
	defer this.lock.Unlock()

	this.contents.clear();
}

func (this *IdStorage) DeleteAll(paths []Identifier) {
	this.lock.Lock()
	defer this.lock.Unlock()

	for i:= len(paths)-1; 0 <= i; i-- { 
			this.delete(paths[i]);
	}
}

func (this *IdStorage) Exists(id Identifier) bool {
	this.lock.Lock()
	defer this.lock.Unlock()
	
	return this.contents[id.Path()] != nill
}

func (this *IdStorage) Fetch(id Identifier) interface{} {
	this.lock.Lock()
	defer this.lock.Unlock()

	var data tNode= this.contents[path]
	if data == nil {
		return nil
	}
	return data.content;
}

/**
 * Returns an IdStorage that contains all the found results for the given 
 * identifiers
 */  
func (this *IdStorage) FetchAll(ids []Identifier) *IdStorage {
	this.lock.Lock()
	defer this.lock.Unlock()

	items:= CreateIdStorage()
	for i, id:= range ids {
		node:= this.contents[id.Path()]
		if node != nil && node.content != nil
			items.Store(id, node.content)
	}
	return items
}

/**
 * Returns an IdStorage that contains all the found results for the given 
 * identifier
 */  
func (this *IdStorage) FetchChildren(id Identifier) *IdStorage {
	this.lock.Lock()
	defer this.lock.Unlock()

	items:= CreateIdStorage()
	n tNode= this.contents[id.Path()]
	if n != nil && n.children != nil && 0 < len(n.children) {
		for (path, node:= range n.children {
			if (node.content != nil) {
				items.Store(node.identifier, node.content)
			}
		}
		return set
	}
	return items
}

/**
 * Returns an IdStorage that contains all the found results for the given 
 * identifiers.
 * The returned IdStorage has values with type IdStorage
 */  
func (this *IdStorage) FetchChildrenAll(ids []Identifier) *IdStorage {
	this.lock.Lock()
	defer this.lock.Unlock()

	results:= CreateIdStorage()
	for (_, id:= range ids {
		results.Store(id, this.FetchChildren(id))
	}
	return results;
}

func (this *IdStorage) ListChildren(id Identifier) []Identifier {
	this.lock.Lock()
	defer this.lock.Unlock()

	n tNode:= this.contents[id]
	if n != nil && 0 < len(n.children) {
		list= make([]Identifier, 0, len(n.children))
		for i, node:= range n.children {
			list[i]= node.identifier
		}
		return list;
	}
	return make([]Identifier, 0)
}

/**
 * Returns an IdStorage that contains all the found results for the given 
 * identifiers.
 * The returned IdStorage has values with type []Identifier
 */  
func (this *IdStorage) ListChildrenAll(ids []Identifier) *IdStorage {
	this.lock.Lock()
	defer this.lock.Unlock()

	results:= CreateIdStorage()
	for i,id:= range ids { 
		results.Store(id, ListChildren(id))
	}
	return results;
}

func (this *IdStorage) Values() []interface{} {
	this.lock.Lock()
	defer this.lock.Unlock()
	
	values:= make([]interface{}, 0, len(this.contents))
	for path, node:= range this.contents {
		if (node.content != nil)
			append(values, node.content)
	}
	return values;
}


/**
 * Returns an IdStorage that contains all the found results for the given 
 * identifiers.
 */  
func (this *IdStorage) FetchDescendents(id Identifier) *IdStorage {
	this.lock.Lock()
	defer this.lock.Unlock()

	n:= this.contents[id.Path()];		
	items:= CreateIdStorage()
	if n != null && n.children != nil && 0 < len(n._children) {
		todo:= make([]tNode, 0, 10)
		append(todo, n);
		for ;0 < len(todo); {
			n= remove(todo, 0)
			for i,node:= range n.children {
				if (node.content != nill)
					items.Store(node.identifier, node.content)
				todo.add(c);
			}
		}
	}
	return items;
}

func (this *IdStorage) VisitNode(path Identifier, visitor Visitor) {
	n tNode:= this.contents[path]
	if n != nil { 
		visitor.visit(n.identifier, n.content);
	}
}

func (this *IdStorage) VisitParents(path Identifier, visitor Visitor) {
	Node<T> data= _contents.get(path);
	if (data != null) {
		for (Node<T> n= data._parent; n != null; n= n._parent) {
			visitor.visit(n._identifier, n._content);
		}
	}
}

func (this *IdStorage) VisitDescendents(path Identifier, visitor Visitor) {
	n tNode:= this.contents[path]
	if n != nil { 
		LinkedList<Node<T>> todo= new LinkedList<Node<T>>();
		todo.add(n);
		while (!todo.isEmpty()) {
			n= todo.removeFirst();
			for (Node<T> c:n._children) {
				visitor.visit(c._identifier, c._content);
				todo.add(c);
			}
		}
	}
}

func (this *IdStorage) VisitChildren(path Identifier, visitor Visitor) {
	n tNode:= this.contents[path]
	if n != nil { 
		for i,node:= range n.children) {
			visitor.visit(node.identifier, node.content);
		}
	}
}

