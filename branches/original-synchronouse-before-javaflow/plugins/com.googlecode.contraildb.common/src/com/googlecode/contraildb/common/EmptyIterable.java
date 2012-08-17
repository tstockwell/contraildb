package com.googlecode.contraildb.common;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class EmptyIterable<T> implements Iterable<T>, Serializable {
	private static final long serialVersionUID = 1L;

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			public boolean hasNext() {
				return false;
			}
			public T next() {
					throw new NoSuchElementException();
			}
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

}
