package com.googlecode.contraildb.core.impl.btree;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.impl.btree.IOrderedSetCursor.Direction;
import com.googlecode.contraildb.core.utils.IAsyncerator;
import com.googlecode.contraildb.core.utils.Immediate;


/**
 * Defines an interface for manipulating an ordered set of values.
 * Allows insertions, deletions, searches, and sequential access.
 * 
 * Like amny other interfaces in Contrail, the interface to an IKeyValueSet 
 * is asynchronous.
 * 
 * @author Ted Stockwell
 * 
 * @param T The type of objects stored in the BTree
 */
@SuppressWarnings("rawtypes")
public interface  IOrderedSet<T extends Comparable> 
{
	/**
	 * Insert an entry.
	 */
	public IResult<Void> insert(T key) ;
	public IResult<Void> insert(IResult<T> key) ;
	

	/**
	 * Remove an entry
	 */
	public IResult<Void> remove(T key);
	public IResult<Void> remove(IResult<T> key);

	/**
	 * Get an iterator for iterating through the values in the index. 
	 */
	public IAsyncerator<T> iterator();


	@Immediate public boolean isEmpty();
	
	/**
	 * Get a cursor for iterating through the values in the index. 
	 * The iterator 
	 */
	@Immediate public IOrderedSetCursor<T> cursor(Direction direction);
	@Immediate public IForwardCursor<T> forwardCursor();
	
}
