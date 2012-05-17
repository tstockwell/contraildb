package com.googlecode.contraildb.core.impl.btree;

import com.googlecode.contraildb.core.IResult;

/**
 * Adds method for getting values associated with key elements.
 * 
 * @author Ted Stockwell
 */
public interface IKeyValueCursor<K, V> extends IOrderedSetCursor<K> {
	
	/**
	 * Returns the value associated with the current key value, if any.
	 */
	IResult<V> elementValue();

	IResult<V> find(K key);
	
}
