package com.googlecode.contraildb.core.impl.btree;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.impl.btree.IOrderedSetCursor.Direction;
import com.googlecode.contraildb.core.utils.Immediate;



/**
 * Enhances IOrderedSet to associate a value with a key.
 * 
 * @author Ted Stockwell
 */
@SuppressWarnings("rawtypes")
public interface  IKeyValueSet<T extends Comparable,V> 
extends IOrderedSet<T>
{
	public IResult<Void> insert(T key, V value) ;
	public IResult<Void> insert(IResult<T> key, IResult<V> value) ;
	@Immediate public IKeyValueCursor<T,V> cursor(Direction direction);
}
