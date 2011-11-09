package com.googlecode.contraildb.core.utils;

import java.util.Map;

import com.googlecode.contraildb.core.Identifier;



/**
 * Just like IdentifierIndexedStorage except that the least recently used items are  
 * removed in order to conserve memory.
 * If the itemCount passed to the constructor is <= 0 then items will never expire.   
 *   
 * @author Ted Stockwell
 *
 * @param T  the types of Objects stored in this container
 */
public class LRUIdentifierIndexedStorage<T> 
extends IdentifierIndexedStorage<T> 
{
	
	ConcurrentHashedLRUList<Identifier> _lruList= new ConcurrentHashedLRUList<Identifier>();
	
	public LRUIdentifierIndexedStorage() {
		this(ConcurrentHashedLRUList.DEFAULT_ITEM_COUNT);
	}
	

	/**
	 * @param itemCount
	 * 		if less than 0 then items never expire
	 */
	public LRUIdentifierIndexedStorage(int itemCount) {
		_lruList= new ConcurrentHashedLRUList<Identifier>(itemCount);
	}
	
	private void touch(Identifier identifier) {
		_lruList.put(identifier);
		Identifier i;
		while ((i= _lruList.poll()) != null)
			delete(i);
	}
	
	private void touch(Iterable<Identifier> identifiers) {
		for (Identifier identifier:identifiers)
			_lruList.put(identifier);
		Identifier i;
		while ((i= _lruList.poll()) != null)
			delete(i);
	}


	@Override
	public void clear() {
		super.clear();
		_lruList.clear();
	}

	@Override
	public void delete(Identifier identifier) {
		_lruList.remove(identifier);
		super.delete(identifier);		
	}

	@Override
	public void delete(Iterable<Identifier> paths) {
		for (Identifier identifier:paths) 
			_lruList.remove(identifier);

		super.delete(paths);
	}

	@Override
	public T fetch(Identifier path) {
		T t= super.fetch(path);
		if (t != null)
			touch(path);
		return t;
	}

	@Override
	public Map<Identifier, T> fetch(Iterable<Identifier> paths) {
		Map<Identifier, T>  items= super.fetch(paths);
		touch(items.keySet());
		return items;
	}

	@Override
	public Map<Identifier, T> fetchChildren(Identifier path) {
		Map<Identifier, T>  items= super.fetchChildren(path);
		touch(items.keySet());
		return items;
	}

	@Override
	public Map<Identifier, Map<Identifier, T>> fetchChildren(Iterable<Identifier> paths) {
		Map<Identifier, Map<Identifier, T>> items= super.fetchChildren(paths);
		for (Map<Identifier, T> m:items.values())
			touch(m.keySet());
		return items;
	}

	@Override
	public void store(Identifier p, T byteArray) {
		super.store(p, byteArray);
		touch(p);
	}

	@Override
	public void store(Map<Identifier, T> records) {
		super.store(records);
		touch(records.keySet());
	}

}
