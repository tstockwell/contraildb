package com.googlecode.contraildb.common;

import java.util.WeakHashMap;



/**
 * A simple, fast read/write lock.
 * 
 * @author Ted Stockwell
 */
public class ReadWriteLock {
	
	private int _writeCount= 0;
	private Thread _writerThread= null; 
	private WeakHashMap<Thread, int[]> _readCounts= new WeakHashMap<Thread, int[]>();  

	public synchronized void aquireReadLock() {
		Thread thread= Thread.currentThread();
		while (_writerThread != null && thread != _writerThread) {
			try {
				wait();
			}
			catch (InterruptedException x) {
			}
		}
		int[] count= _readCounts.get(thread);
		if (count == null) {
			_readCounts.put(thread, new int[] { 1 });
		}
		else 
			count[0]++;
	}
	
	public synchronized void releaseReadLock() {
		Thread thread= Thread.currentThread();
		int[] count= _readCounts.get(thread);
		if (count != null && --count[0] <= 0) 
				_readCounts.remove(thread);
		notifyAll();
	}
	
	public synchronized void acquireWriteLock() {
		Thread thread= Thread.currentThread();
		while (true) {
			if (_writerThread == thread)
				break;
			if (_readCounts.isEmpty())
				break;
			if (_readCounts.size() == 1 && _readCounts.containsKey(thread))
				break;
			try {
				wait();
			}
			catch (InterruptedException x) {
			}
		}
		if (_writerThread == null) {
			_writerThread= thread;
			_writeCount= 1;
		}
		else
			_writeCount++;
	}
	
	
	public synchronized void releaseWriteLock() {
		if (Thread.currentThread() == _writerThread) {
			if (--_writeCount <= 0) {
				_writerThread= null;
				notifyAll();
			}
		}
	}
	

}

