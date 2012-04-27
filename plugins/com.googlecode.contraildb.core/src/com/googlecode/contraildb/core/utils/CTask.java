package com.googlecode.contraildb.core.utils;

/**
 * An asynchronous task.
 */
public interface CTask<T> {
	
	/**
	 * Begin task execution, return a receipt.
	 */
	public Receipt<T> run();
	
	/**
	 * Cancels this task.
	 */
	public void cancel();
}
