package com.googlecode.contraildb.core;


/**
 * An interface for retrieving query results asynchronously.
 */
public interface IProcessor {
	/** 
	 * This method is called when a query result has been found.
	 * It is recommended that this method not do any heavy result 
	 * processing, just save the result and return as quickly as possible.
	 * 
	 * @param identifier An id of an object that satisfies the associated query. 
	 * @return true if more results should be retrieved, false if the associated query should be cancelled.
	 */
	public boolean result(Identifier identifier);
	/**
	 * This method is invoked when there are no more results available or an error has occurred	.
	 * 
	 */
	public void complete(Throwable error);
}