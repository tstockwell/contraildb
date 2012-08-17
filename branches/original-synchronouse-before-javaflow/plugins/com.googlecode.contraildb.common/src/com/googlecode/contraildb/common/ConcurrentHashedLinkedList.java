package com.googlecode.contraildb.common;

import java.util.Iterator;

public class ConcurrentHashedLinkedList<T> extends HashedLinkedList<T> {

	@Override
	synchronized public T append(T item) {
		return super.append(item);
	}

	@Override
	synchronized public void clear() {
		super.clear();
	}

	@Override
	synchronized public boolean isEmpty() {
		return super.isEmpty();
	}

	@Override
	synchronized public T prepend(T item) {
		return super.prepend(item);
	}

	@Override
	synchronized public boolean remove(T item) {
		return super.remove(item);
	}

	@Override
	synchronized public T removeFirst() {
		return super.removeFirst();
	}

	@Override
	synchronized public T removeLast() {
		return super.removeLast();
	}

	@Override
	synchronized public int size() {
		return super.size();
	}

	@Override
	synchronized public Iterator<T> iterator() {
		return super.iterator();
	}
}
