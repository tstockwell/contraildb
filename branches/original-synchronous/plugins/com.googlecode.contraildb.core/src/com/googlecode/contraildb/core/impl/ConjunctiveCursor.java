package com.googlecode.contraildb.core.impl;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import kilim.Pausable;

import com.googlecode.contraildb.core.impl.btree.BPlusTree;
import com.googlecode.contraildb.core.impl.btree.IForwardCursor;


/**
 * Performs logical conjunction on a set of cursors.
 * That is, this cursor returns the intersection of all elements in a set of cursors.
 *  
 * @author Ted Stockwell
 * 
 */
@SuppressWarnings("unchecked")
public class ConjunctiveCursor<T extends Comparable<T>> implements IForwardCursor<T> {
	
	final IForwardCursor<T>[] _cursors;
	
	public ConjunctiveCursor(List<IForwardCursor<T>> filterCursors) {
		_cursors= new IForwardCursor[filterCursors.size()];
		filterCursors.toArray(_cursors);
	}

	@Override
	public T keyValue() throws Pausable {
		if (_cursors.length <= 0)
			throw new NoSuchElementException();
		return _cursors[0].keyValue();
	}

	@Override
	public boolean first() throws Pausable, IOException {
		if (!_cursors[0].first())
			return false;
		return to(_cursors[0].keyValue());
	}

	@Override
	public boolean to(T e) throws Pausable, IOException {
		if (!_cursors[0].to(e))
			return false;

		T value= _cursors[0].keyValue();
		while (true) {

			T ge= value;
			for (int i= 1; i < _cursors.length; i++) {
				IForwardCursor<T> cursor= _cursors[i];
				if (!cursor.to(value))
					return false;
				T t= cursor.keyValue();
				if (BPlusTree.compare(value, t) < 0)
					ge= t;
			}
			if (BPlusTree.compare(value, ge) == 0) 
				return true;

			if (!_cursors[0].to(ge))
				return false;
			value= _cursors[0].keyValue();
		}
	}

	@Override public boolean hasNext() throws Pausable {
		throw new UnsupportedOperationException();
	}

	@Override public boolean next() throws Pausable, IOException {
		
		while (_cursors[0].next()) {
			if (to(_cursors[0].keyValue())) 
				return true;
		}
		return false;
	}
	
	@Override
	public Direction getDirection() {
		return Direction.FORWARD;
	}
}
