package com.googlecode.contraildb.core.utils;

import java.util.LinkedList;


/**
 * A reusable LRU list.
 * Tracks items by hash code.
 * Use the poll method to remove the least recently used items.
 * Thread-safe.
 *
 * @param T  the types of Objects stored in this container
 */
public class ConcurrentHashedLRUList<T>  
{
	public static final int DEFAULT_ITEM_COUNT= 1000;
	
	private final int _itemCount;
	private final LinkedList<T> _itemsReplacedByAnotherWithDuplicateHashCode= new LinkedList<T>();
	private final HashedLinkedList<T> _list= new HashedLinkedList<T>();
	
	public ConcurrentHashedLRUList() {
		this(DEFAULT_ITEM_COUNT);
	}

	/**
	 * @param itemCount
	 * 		if less than 0 then items never expire
	 */
	public ConcurrentHashedLRUList(int itemCount) {
		_itemCount= itemCount;
	}

	
	synchronized public void put(T item) {
		if (0 < _itemCount) {
			T old= _list.prepend(item);
			if (old != null)
				_itemsReplacedByAnotherWithDuplicateHashCode.add(old);
		}
	}
	
	/**
	 * If there are items that have expired then this method returns the least 
	 * recently used expired item.   
	 */
	synchronized public T poll() {
		if (0 < _itemsReplacedByAnotherWithDuplicateHashCode.size()) {
			return _itemsReplacedByAnotherWithDuplicateHashCode.removeFirst();
		}
		int count= _list.size();
		if (_itemCount < count) 
			return _list.removeLast();
		return null;
	}

	synchronized public void clear() {
		_list.clear();
		_itemsReplacedByAnotherWithDuplicateHashCode.clear();
	}

	synchronized public void remove(T item) {
		_list.remove(item);
		_itemsReplacedByAnotherWithDuplicateHashCode.remove(item);
	}
	

}
