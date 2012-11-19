package com.googlecode.contraildb.core.impl;

import java.io.IOException;
import java.io.Serializable;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.impl.btree.BPlusTree;
import com.googlecode.contraildb.core.impl.btree.BTree;
import com.googlecode.contraildb.core.impl.btree.CursorImpl;
import com.googlecode.contraildb.core.impl.btree.IBTreeCursor;
import com.googlecode.contraildb.core.impl.btree.IBTreePlusCursor;
import com.googlecode.contraildb.core.impl.btree.IForwardCursor;
import com.googlecode.contraildb.core.impl.btree.IBTreeCursor.Direction;
import com.googlecode.contraildb.core.storage.StorageUtils;


/**
 * In Contrail an index is created for every unique property name. The index
 * maps property values to the identifiers of the entities that contain that
 * property. If there is more that one entity for a given property value then
 * instead the index instead contains the id of another index that lists all the
 * entities.
 * 
 * When iterating through an index, identifiers associated with the same
 * property value are always returned in order (the order defined by the
 * Identifier class itself, which is the lexicographical order of the Identifier
 * as a String).
 * 
 * @author ted stockwell
 */
@SuppressWarnings("unchecked")
public class PropertyIndex<K extends Comparable<K> & Serializable>
{
	
	BPlusTree<K, Identifier> _btree;
	
	static final Identifier __indexRoot= Identifier.create("net/sf/contrail/core/indexes/sets");
	
	public PropertyIndex(BPlusTree<K, Identifier> btree) throws IOException {
		_btree= btree;
	}


	synchronized public void insert(K key, Identifier document) throws IOException {
		Identifier value= _btree.cursor(Direction.FORWARD).find(key);
		if (value == null) { 
			// the key has no value currently associated with it, just store the identifier
			_btree.insert(key, document).get();
			return;
		} 
		
		if (__indexRoot.isAncestorOf(value)) {
			// the key has more than one value currently associated with it, 
			// add the identifier to the list
			BTree<Identifier> set= StorageUtils.syncFetch(_btree.getStorage(), value);
			set.insert(document);
			return;
		}

		// the key has a single value currently associated with it
		// create a set index and add the two values
		Identifier setId= Identifier.create(__indexRoot);
		BTree<Identifier> set= BTree.createInstance(_btree.getStorage(), setId);
		set.insert(value);
		set.insert(document);
		_btree.insert(key, setId).get();
	}


	@SuppressWarnings("rawtypes")
	synchronized public IBTreePlusCursor<K, IForwardCursor<Identifier>> cursor(Direction direction) 
	throws IOException 
	{
		return new CursorImpl(_btree, direction) {
			private  IForwardCursor<Identifier> toIterable(Identifier id) throws IOException {
				if (id == null)
					return new IBTreeCursor.EmptyForwardCursor<Identifier>();
				if (__indexRoot.isAncestorOf(id)) {
					BTree<Identifier> tree= StorageUtils.syncFetch(_btree.getStorage(), id);
					return tree.forwardCursor();
				}
				return new IBTreeCursor.SingleValueCursor<Identifier>(id);
			}
			@Override
			public IForwardCursor<Identifier> elementValue() throws IOException {
				return toIterable((Identifier)super.elementValue());
			}
		};
	}


	synchronized public void remove(K key, Identifier document) throws IOException {
		Identifier value= _btree.cursor(Direction.FORWARD).find(key);
		if (value == null)
			return;
		if (__indexRoot.isAncestorOf(value)) {
			BTree<Identifier> set= StorageUtils.syncFetch(_btree.getStorage(), value);
			set.remove(document);
			if (set.isEmpty()) {
				_btree.remove(key);
				set.delete();
			}
			return;
		}
		
		if (value.equals(document)) 
			_btree.remove(key);
	}
}
