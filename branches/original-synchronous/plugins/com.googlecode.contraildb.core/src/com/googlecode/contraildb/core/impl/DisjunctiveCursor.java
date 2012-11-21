package com.googlecode.contraildb.core.impl;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import kilim.Pausable;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.impl.btree.BPlusTree;
import com.googlecode.contraildb.core.impl.btree.IForwardCursor;


/**
 * Performs logical disjunction on a set of cursors.
 * That is, this cursor returns the union of all elements in a set of cursors.
 *  
 * @author Ted Stockwell
 * 
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class DisjunctiveCursor<T extends Comparable> implements IForwardCursor<T> {
	
	private static Comparator<Comparable> __comparator= new Comparator<Comparable>() {
		@Override public int compare(Comparable o1,Comparable o2) {
			return BPlusTree.compare(o1, o2);
		}
	};
	
	final IForwardCursor<T>[] _cursors;
	T _value;
	TreeSet<T> _queue= new TreeSet<T>(__comparator);

	public DisjunctiveCursor(List<IForwardCursor<Identifier>> cursors) {
		_cursors= new IForwardCursor[cursors.size()];
		cursors.toArray(_cursors);
	}

	@Override public T keyValue() throws Pausable {
		if (_queue.isEmpty())
			return null;
		return _queue.first();
	}

	@Override public boolean first() throws Pausable {
		_queue.clear();
		for (int i= 0; i < _cursors.length; i++) {
			IForwardCursor<T> cursor= _cursors[i];
			if (cursor.first()) 
				_queue.add(cursor.keyValue());
		}
		
		return !_queue.isEmpty();
	}

	@Override public boolean to(T e) throws Pausable {
		
		for (int i= 0; i < _cursors.length; i++) {
			IForwardCursor<T> cursor= _cursors[i];
			if (cursor.to(e)) 
				_queue.add(cursor.keyValue());
		}
		
		T t= _queue.ceiling(e);
		if (BPlusTree.compare(e, t) <= 0) {
			T f;
			while (!_queue.isEmpty() && BPlusTree.compare(f= _queue.first(), t) < 0)
				_queue.remove(f);
			return true;
		}
		return false;
	}

	@Override public boolean hasNext() throws Pausable {
		if (!_queue.isEmpty())
			return true;
		for (int i= 0; i < _cursors.length; i++) {
			IForwardCursor<T> cursor= _cursors[i];
			if (cursor.next()) 
				_queue.add(cursor.keyValue());
		}
		return !_queue.isEmpty();
	}


	@Override public boolean next() throws Pausable {
		if (!_queue.isEmpty()) {
			_queue.remove(_queue.first());
			if (!_queue.isEmpty()) 
				return true;
		}
		for (int i= 0; i < _cursors.length; i++) {
			IForwardCursor<T> cursor= _cursors[i];
			if (cursor.next()) 
				_queue.add(cursor.keyValue());
		}
		return !_queue.isEmpty();
	}

	@Override
	public Direction getDirection() {
		return Direction.FORWARD;
	}

}
