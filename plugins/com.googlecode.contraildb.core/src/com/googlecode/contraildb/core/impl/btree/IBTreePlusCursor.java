package com.googlecode.contraildb.core.impl.btree;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.utils.Immediate;

/**
 * Adds method for getting values associated with key elements.
 * 
 * @author Ted Stockwell
 */
public interface IBTreePlusCursor<K, V> extends IBTreeCursor<K> {
	
	/**
	 * Returns the value associated with the current key value, if any.
	 */
	@Immediate V elementValue();

	IResult<V> find(K key);
	
}
