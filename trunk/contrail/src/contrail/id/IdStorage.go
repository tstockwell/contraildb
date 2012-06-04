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
	Visit(identifier Identifier , content interface{});
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

func (this *IdStorage) Exists(path Identifier) {
	this.lock.Lock()
	defer this.lock.Unlock()
	
	return this.contents.containsKey(path);
	}

	synchronized public T fetch(Identifier path) {
		Node<T> data= _contents.get(path);
		if (data == null)
			return null;
		return data._content;
	}

	synchronized public Map<Identifier, T> fetch(Iterable<Identifier> paths) {
		HashMap<Identifier, T> items= new HashMap<Identifier, T>();
		for (Identifier path: paths) {
			Node<T> data= _contents.get(path);
			if (data != null && data._content != null)
				items.put(path, data._content);
		}
		return items;
	}

	synchronized public Map<Identifier, Map<Identifier, T>> fetchChildren(Iterable<Identifier> paths)
	{
		HashMap<Identifier, Map<Identifier, T>> result= new HashMap<Identifier, Map<Identifier, T>>();
		for (Identifier p: paths) 
			result.put(p, fetchChildren(p));
		return result;
	}

	synchronized public void store(Map<Identifier, T> records) {
		
		for (Map.Entry<Identifier, T> entry: records.entrySet())
			_store(entry.getKey(), entry.getValue());
	}

	synchronized public Map<Identifier, Collection<Identifier>> listChildren(Iterable<Identifier> paths)
	{
		HashMap<Identifier, Collection<Identifier>> result= new HashMap<Identifier, Collection<Identifier>>();
		for (Identifier path: paths) 
			result.put(path, listChildren(path));
		return result;
	}

	synchronized public Iterable<T> values() {
		ArrayList<T> values= new ArrayList<T>();
		for (Node<T> data:_contents.values()) {
			if (data._content != null)
				values.add(data._content);
		}
		return values;
	}

	synchronized public Map<Identifier, T> fetchChildren(Identifier path) {
		Node<T> n= _contents.get(path);		
		if (n != null && !n._children.isEmpty()) {
			HashMap<Identifier, T> set= new HashMap<Identifier, T>();
			for (Node<T> c:n._children) {
				if (c._content != null)
					set.put(c._identifier, c._content);
			}
			return set;
		}
		return Collections.emptyMap();
	}

	synchronized public Map<Identifier, T> fetchDescendents(Identifier path) {
		Node<T> n= _contents.get(path);		
		if (n != null && !n._children.isEmpty()) {
			HashMap<Identifier, T> set= new HashMap<Identifier, T>();
			LinkedList<Node<T>> todo= new LinkedList<Node<T>>();
			todo.add(n);
			while (!todo.isEmpty()) {
				n= todo.removeFirst();
				for (Node<T> c:n._children) {
					if (c._content != null)
						set.put(c._identifier, c._content);
					todo.add(c);
				}
			}
			return set;
		}
		return Collections.emptyMap();
	}

	synchronized public Collection<Identifier> listChildren(Identifier path) {
		Node<T> n= _contents.get(path);
		if (n != null && !n._children.isEmpty()) {
			ArrayList<Identifier> list= new ArrayList<Identifier>(n._children.size());
			for (Node<T> node:n._children)
				if (node._content != null)
					list.add(node._identifier);
			return list;
		}
		return Collections.emptySet();
	}

public class IdentifierIndexedStorage<T> {
	
	synchronized void visitNode(Identifier path, Visitor<T> visitor) {
		Node<T> n= _contents.get(path);
		if (n != null) 
			visitor.visit(n._identifier, n._content);
	}
	
	synchronized void visitParents(Identifier path, Visitor<T> visitor) {
		Node<T> data= _contents.get(path);
		if (data != null) {
			for (Node<T> n= data._parent; n != null; n= n._parent) {
				visitor.visit(n._identifier, n._content);
			}
		}
	}
	
	synchronized void visitDescendents(Identifier path, Visitor<T> visitor) {
		Node<T> n= _contents.get(path);		
		if (n != null) {
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
	
	synchronized void visitChildren(Identifier path, Visitor<T> visitor) {
		Node<T> n= _contents.get(path);
		if (n != null) {
			for (Node<T> node:n._children) {
				visitor.visit(node._identifier, node._content);
			}
		}
	}
}
