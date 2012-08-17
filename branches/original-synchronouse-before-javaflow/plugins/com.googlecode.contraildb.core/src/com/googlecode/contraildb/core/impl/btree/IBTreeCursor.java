package com.googlecode.contraildb.core.impl.btree;

import java.io.IOException;

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
public interface IBTreeCursor<K> {


	/**
	 * Cursor navigation types
	 */
	public static enum Direction {
		FORWARD,
		REVERSE
	}

	public static class EmptyCursor<K> implements IBTreeCursor<K> {
		private final Direction _direction;
		public EmptyCursor(Direction d) { _direction= d; } 
		public boolean after(K e) { return false; }
		public boolean before(K e) { return false; }
		public K keyValue() { return null; }
		public boolean first() { return false; }
		public Direction getDirection() { return _direction; }
		public boolean hasNext() { return false; }
		public boolean last() { return false; }
		public boolean next() { return false; }
		public boolean to(K e) { return false; }
	}
	public class SingleValueCursor<T extends Comparable<T>> implements IForwardCursor<T> {
		private T _t;
		private int _state= 0;
		public SingleValueCursor(T t) { _t= t;  }
		public T keyValue() { return _t; }
		public boolean first() { if (1 < _state) return false; _state= 1; return true; }
		public Direction getDirection() { return Direction.FORWARD; }
		public boolean hasNext() { return (_state == 0); }
		public boolean next() { if (1 <= _state) return false; _state= 1; return true; }
		public boolean to(T e) {
			if (1 < _state)
				return false;
			int i= BPlusTree.compare(e, _t);
			if (i == 0) {
				_state= 1;
				return true;
			}
			else if (i < 0) {
				if (_state == 1) { 
					_state= 2;
					return false;
				}
				_state= 1;
				return true; 
			}
			return false;
		}
	}

	public static class EmptyForwardCursor<K> extends EmptyCursor<K> implements IForwardCursor<K> {
		public EmptyForwardCursor() { super(Direction.FORWARD); }
	}

	public static class EmptyReverseCursor<K> extends EmptyCursor<K> implements IReverseCursor<K> {
		public EmptyReverseCursor() { super(Direction.REVERSE); }
	}
	
	Direction getDirection();
	
	boolean hasNext() throws IOException;
	
	/**
	 * @return 
	 * 	the value of the key associated with the current cursor position.
	 */
	K keyValue() throws IOException;
	
    /**
     * Moves the cursor to the next element.
     * 
     * @return 
     * 	<code>false</code> if there is no such element.
     */
    boolean next() throws IOException;
	
    /**
     * Moves the cursor to the given element or, if the element does not exist, the next element..
     * 
     * @return 
     * 	<code>false</code> if there is no such element.
     */
    boolean to(K e) throws IOException;

    /**
     * Moves the cursor to the first element.
     * 
     * @return 
     * 	<code>false</code> if there is no such element.
     */
    boolean first() throws IOException;

}
