package com.googlecode.contraildb.common;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class SingleValueIterable<T> implements Iterable<T>, Serializable {
	private static final long serialVersionUID = 1L;
	final T _value;

	public SingleValueIterable(T value) {
		_value= value;
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			boolean _next= true;
			public boolean hasNext() {
				return _next;
			}
			public T next() {
				if (!_next)
					throw new NoSuchElementException();
				_next= false;
				return _value;
			}
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

}
