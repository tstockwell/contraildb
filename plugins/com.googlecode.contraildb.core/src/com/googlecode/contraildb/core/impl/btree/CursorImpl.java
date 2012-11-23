/**
 * 
 */
package com.googlecode.contraildb.core.impl.btree;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

import kilim.Pausable;

import com.googlecode.contraildb.core.storage.StorageUtils;


@SuppressWarnings({ "unchecked", "rawtypes" })
public class CursorImpl<T extends Comparable, V> 
implements IBTreePlusCursor<T,V> {
	private final BPlusTree _tree;
	private final Direction _direction;
	private final ReentrantLock _lock = new ReentrantLock();	
	
	public CursorImpl(BPlusTree index, Direction direction) {
		_direction= direction;
		_tree= index;
	}
	Node<T> _page;
	int _index;

	@Override synchronized public V elementValue() throws Pausable {
		return (V) _page._values[_index];
	}
	
	private void initFirst() throws Pausable {
		_index= -1;
		if ((_page= _tree.getRoot()) != null) {
			while (!_page.isLeaf()) 
				_page= _page.getChildNode(0);
		}
	}
	protected void initLast() throws Pausable {
		if ((_page= _tree.getRoot()) != null) {
			while (!_page.isLeaf()) {
				_page= _page.getChildNode(_page._size-1);
			}
			_index= _page._size;
		}
	}

	@Override synchronized public T keyValue() throws Pausable {
		return _page._keys[_index];
	}
	
	protected boolean previous() throws Pausable {
		if (_page == null)
			initLast();
		if (_page == null)
			return false;
		
		if (_index <= 0) {
			if (_page._previous == null) 
				return false;
			_page = StorageUtils.syncFetch(_page.getStorage(), _page._previous);
			_index = _page._size;
		}
		
		_index--;
		return true;
	}
	
	protected boolean ge(T e) throws Pausable {
		if ((_page= _tree.getRoot()) == null)
			return false;
		
		while (!_page.isLeaf()) {
			_index= _page.indexOf(e);
			_page = _page.getChildNode(_index);
		}
		_index= _page.indexOf(e);
		
		return 0 <= _index && _index < _page._size && (BPlusTree.compare(e, _page._keys[_index]) == 0); 
	}
	
	protected boolean le(T e) throws IOException, Pausable {
		boolean isGE= ge(e);
		if (isGE) {
			T t= keyValue();
			if (BPlusTree.compare(t, e) == 0)
				return true;
			if (previous())
				return true;
			return false;
		}
		boolean isLast= last();
		return isLast;
	}
	
	@Override
	public boolean next() throws IOException, Pausable {
	/*synchronized*/_lock.lock(); try {
			
		if (Direction.REVERSE == _direction) {
			boolean isPrev= previous();
			return isPrev;
		}
		
		if (_page == null) 
			return first();
		
		if (_page._size-1 <= _index) {
			if (_page._next == null)  
				return false;
			_page = _page.getStorage().fetch(_page._next);
			_index = -1;
		}
		
		_index++;
		return true;
		
	} finally { _lock.unlock(); }
	}

	@Override
	public boolean hasNext() throws IOException, Pausable {
	/*synchronized*/_lock.lock(); try {
		
		if (Direction.REVERSE == _direction)
			return hasPrevious();
		
		if (_page == null) 
			initFirst();
		if (_page == null)
			return false;
		
		if (_page._size-1 <= _index) {
			if (_page._next == null)  
				return false;
			_page = _page.getStorage().fetch(_page._next);
			_index = -1;
		}
		return true;
		
	} finally { _lock.unlock(); }}

	public boolean hasPrevious() throws IOException, Pausable {
	/*synchronized*/_lock.lock(); try {
		
		if (_page == null)
			initLast();
		if (_page == null)
			return false;
		
		if (_index <= 0) {
			if (_page._previous == null) 
				return false;
			_page = _page.getStorage().fetch(_page._previous);
			_index = _page._size;
		}
		return true;
		
	} finally { _lock.unlock(); }}

	@Override public boolean first() throws IOException, Pausable {
	/*synchronized*/_lock.lock(); try {
		
		if (Direction.REVERSE == _direction)
			return last();
		
		initFirst();
		if (_page == null)
			return false;
		return next();
		
	} finally { _lock.unlock(); }}

	protected boolean last() throws IOException, Pausable {
		if (Direction.REVERSE == _direction)
			return first();
		
		initLast();
		if (_page == null)
			return false;
		return previous();
	}
	
	@Override public IBTreePlusCursor.Direction getDirection() {
		return _direction;
	}
	
	@Override public boolean to(T e) throws IOException, Pausable {
	/*synchronized*/_lock.lock(); try {
		
		if (Direction.REVERSE == _direction) {
			boolean isLE= le(e); 
			return isLE;
		}
		boolean isGE= ge(e); 
		return isGE;
		
	} finally { _lock.unlock(); }}
	
	/**
	 * Find the value associated with the given key.
	 * Always starts the search from the beginning 
	 */
	@Override
	public V find(T key) throws IOException, Pausable {
	/*synchronized*/_lock.lock(); try {
		
		if (Direction.REVERSE == _direction) {
			initLast();
		}
		else
			initFirst();
		
		if (!to(key))
			return null;
		T k= keyValue();
		if (BPlusTree.compare(key, k) != 0)
			return null;
		return elementValue();
		
	} finally { _lock.unlock(); }}
	
}