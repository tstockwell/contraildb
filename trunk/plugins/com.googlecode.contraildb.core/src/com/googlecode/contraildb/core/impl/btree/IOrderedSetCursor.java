package com.googlecode.contraildb.core.impl.btree;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.utils.Immediate;
import com.googlecode.contraildb.core.utils.InvocationHandler;
import com.googlecode.contraildb.core.utils.TaskUtils;

/**
 * Simple API for navigating through the elements in a BTree.
 * A cursor can only navigate in one direction and cannot go back to a previous element.
 * Cursors are NOT threadsafe.
 * 
 * @author Ted Stockwell
 *
 * @param <K> The types of elements in the tree, the keys.
 * @param <V> The types of values stored in the leaves of the tree
 */
@SuppressWarnings("unchecked")
public interface IOrderedSetCursor<K> {


	/**
	 * Cursor navigation types
	 */
	public static enum Direction {
		FORWARD,
		REVERSE
	}

	public static class EmptyCursor<K> implements IOrderedSetCursor<K> {
		private final Direction _direction;
		public EmptyCursor(Direction d) { _direction= d; } 
		public boolean after(K e) { return false; }
		public boolean before(K e) { return false; }
		public IResult<K> keyValue() { return TaskUtils.NULL; }
		public IResult<Boolean> first() { return TaskUtils.FALSE; }
		public Direction getDirection() { return _direction; }
		public IResult<Boolean> hasNext() { return TaskUtils.FALSE; }
		public boolean last() { return false; }
		public IResult<Boolean> next() { return TaskUtils.FALSE; }
		public IResult<Boolean> to(K e) { return TaskUtils.FALSE; }
		public IResult<Boolean> to(IResult<K> e) { return TaskUtils.FALSE; }
	}
	public class SingleValueCursor<T extends Comparable<T>> implements IForwardCursor<T> {
		private T _t;
		private int _state= 0; // 0 before value; 1 on value; 2 after value
		public SingleValueCursor(T t) { _t= t;  }
		public IResult<T> keyValue() { if (_state == 1) return TaskUtils.asResult(_t); return TaskUtils.NULL; }
		public IResult<Boolean> first() { if (1 < _state) return TaskUtils.FALSE; _state= 1; return TaskUtils.TRUE; }
		public Direction getDirection() { return Direction.FORWARD; }
		public IResult<Boolean> hasNext() { return TaskUtils.asResult(_state == 0); }
		public IResult<Boolean> next() { if (1 <= _state) return TaskUtils.FALSE; _state= 1; return TaskUtils.TRUE; }
		public IResult<Boolean> to(T e) {
			if (1 < _state)
				return TaskUtils.FALSE;
			int i= KeyValueSet.compare(e, _t);
			if (i == 0) {
				_state= 1;
				return TaskUtils.TRUE;
			}
			else if (i < 0) {
				if (_state == 1) { 
					_state= 2;
					return TaskUtils.FALSE;
				}
				_state= 1;
				return TaskUtils.TRUE; 
			}
			return TaskUtils.FALSE;
		}
		public IResult<Boolean> to(IResult<T> result) {
			return new InvocationHandler<T>(result) {
				protected IResult<Boolean> onSuccess(T t) {
					return to(t);
				}
			};
		}
	}

	public static class EmptyForwardCursor<K> extends EmptyCursor<K> implements IForwardCursor<K> {
		public EmptyForwardCursor() { super(Direction.FORWARD); }
	}

	public static class EmptyReverseCursor<K> extends EmptyCursor<K> implements IReverseCursor<K> {
		public EmptyReverseCursor() { super(Direction.REVERSE); }
	}
	
	@Immediate Direction getDirection();
	
	IResult<Boolean> hasNext();
	
	/**
	 * @return 
	 * 	the value of the key associated with the current cursor position.
	 */
	 IResult<K> keyValue();
	
    /**
     * Moves the cursor to the next element.
     * 
     * @return 
     * 	<code>false</code> if there is no such element.
     */
    IResult<Boolean> next();
	
    /**
     * Moves the cursor to the given element or, if the element does not exist, the next element..
     * 
     * @return 
     * 	<code>false</code> if there is no such element.
     */
    IResult<Boolean> to(K e);
    IResult<Boolean> to(IResult<K> e);

    /**
     * Moves the cursor to the first element.
     * 
     * @return 
     * 	<code>false</code> if there is no such element.
     */
    IResult<Boolean> first();

}
