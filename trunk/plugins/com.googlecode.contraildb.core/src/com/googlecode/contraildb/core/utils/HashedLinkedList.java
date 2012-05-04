package com.googlecode.contraildb.core.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;



/**
 * A simple linked list that also tracks items by hash code for fast lookups 
 * and removes.
 * Not thread-safe. 
 * @see ConcurrentHashedLinkedList
 *
 * @param T  the types of Objects stored in this container
 */
public class HashedLinkedList<T> implements Iterable<T>
{
	static class Node<X> {
		Node<X> prev;
		Node<X> next;
		X item;
		
		public Node(Node<X> nextNode, X item) {
			if (nextNode == null) {
				prev= next= this;
			}
			else {
				(prev= nextNode.prev).next= this;
				(next= nextNode).prev= this;
			}
			this.item= item;
		}
		
		void prepend(Node<X> nextNode) {
			next.prev= prev;
			prev.next= next;
			
			(prev= nextNode.prev).next= this;
			(next= nextNode).prev= this;
		}

		void remove() {
			next.prev= prev;
			prev.next= next;
		}
	}
	
	protected Node<T> _root;
	protected final HashMap<T, Node<T>> _nodesByItem= new HashMap<T, Node<T>>();
	
	/**
	 * Inserts the given item at the end of the list
	 * If the item is already in the set then it is moved to the beginning of the list.  
	 * 
	 * If this list already contained an object with the same hash code then 
	 * the old item is removed from the list and returned.
	 */
	public T append(T item) {
		T old= null;

		Node<T> node= _nodesByItem.get(item);
		if (node != null) {
			if (node != _root) 
				_root.prepend(node);
			if (node.item != item) { 
				old= node.item;
				node.item= item;
			}
		}
		else {
			node=  new Node<T>(_root, item);
			_nodesByItem.put(item, node);
			if (_root == null)
				_root= node;
		}

		return old;
	}
	
	/**
	 * Inserts the given item at the end of the list 
	 * If the item is already in the set then it is moved to the end of the list.  
	 * 
	 * If this list already contained an object with the same hash code then 
	 * the old item is removed from the list and returned.
	 */
	public T prepend(T item) {
		T old= append(item);
		_root= _root.prev;
		return old;
	}
	
	public void clear() {
		_nodesByItem.clear();
		_root= null;
	}

	
	/**
	 * Returns true if the item was found and removed.
	 */
	public boolean remove(T item) {
		Node<T> node= _nodesByItem.remove(item);
		if (node != null && node.item == item) {
			if (_nodesByItem.size() <= 0) {
				_root= null;
			}
			else {
				node.remove();
				if (node == _root) 
					_root= node.next;
			}
			return true;
		}
		return false;
	}

	public int size() {
		return _nodesByItem.size();
	}

	
	/**
	 * Remove and return the last item from the list.
	 */
	public T removeLast() {
		if (_root == null)
			return null;
		
		Node<T> n;
		if (_nodesByItem.size() <= 1) {
			n= _root;
			_root= null;
		}
		else {
			n= _root.prev;
			n.remove();
		}
		_nodesByItem.remove(n.item);
		return n.item;
	}
	
	
	/**
	 * Remove and return the first item from the list.
	 */
	public T removeFirst() {
		if (_root == null)
			return null;
		
		Node<T> n;
		if (_nodesByItem.size() <= 1) {
			n= _root;
			_root= null;
		}
		else {
			n= _root;
			_root= n.next;
			n.remove();
		}
		_nodesByItem.remove(n.item);
		return n.item;
	}

	public boolean isEmpty() {
		return _root == null;
	}

	@Override
	public Iterator<T> iterator() {
		return new ArrayList<T>(_nodesByItem.keySet()).iterator();
	}

}
