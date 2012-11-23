package com.googlecode.contraildb.core.impl.btree;

import java.io.IOException;

import kilim.Pausable;

/**
 * Adds method for getting values associated with key elements.
 * 
 * @author Ted Stockwell
 */
public interface IBTreePlusCursor<K, V> extends IBTreeCursor<K> {
	
	/**
	 * Returns the value associated with the current key value, if any.
	 */
	V elementValue() throws Pausable;

	V find(K key) throws IOException, Pausable;
	
}
