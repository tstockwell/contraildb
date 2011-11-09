package com.googlecode.contraildb.core.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import com.googlecode.contraildb.core.Identifier;


/**
 * A container that stores objects in a tree, in memory, according to their associated Identifier.
 * This container arranges the items in a tree of nodes.
 * The nodes can be navigated in tree-like fashion. 
 * 
 * Is thread-safe.
 * 
 * @param T  the types of Objects stored in this container
 *  
 * @author Ted Stockwell
 */
public class IdentifierIndexedStorage<T> {
	
	public static interface Visitor<X> {
		void visit(Identifier identifier, X content);
	}
	
	private static class Node<X> {
		private Identifier _identifier;
		private HashSet<Node<X>> _children= new HashSet<Node<X>>(1);
		private Node<X> _parent;
		private X _content;
	}
	
	private HashMap<Identifier, Node<T>> _contents= new HashMap<Identifier, Node<T>>(); 
	
	public IdentifierIndexedStorage() {
	}
	
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
	
	synchronized public void clear() {
		_contents.clear();
	}
	
	synchronized public void delete(Iterable<Identifier> paths) {
		for (Identifier path: paths) 
			delete(path);
	}

	synchronized public boolean exists(Identifier path) {
		return _contents.containsKey(path);
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

	synchronized public void delete(Identifier i) {
		_delete(_contents.get(i));
	}
	
	void _delete(Node<T> node) {
		if (node != null) {
			node._content= null;
			if (node._children.isEmpty()) {
				_contents.remove(node._identifier);
				if (node._parent != null) {
					Node<T> parent= node._parent; 
					parent._children.remove(node);
					if (parent._children.isEmpty() && parent._content == null)
						_delete(parent);
				}
			}
		}
	}
	

	synchronized public void store(Identifier p, T byteArray) {
		_store(p, byteArray);
	}
	private Node<T> _store(Identifier p, T byteArray) {
		
		Node<T> node= _contents.get(p);
		if (node == null) {
			node= new Node<T>();
			node._identifier= p;
			_contents.put(p, node);
		}
		node._content= byteArray;

		Identifier parentId= p.getParent();
		if (parentId != null) {  
			Node<T> parentNode= _contents.get(parentId);
			if (parentNode == null) 
				parentNode= _store(parentId, null);
			parentNode._children.add(node);
			node._parent= parentNode;
		}

		return node;
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
}
