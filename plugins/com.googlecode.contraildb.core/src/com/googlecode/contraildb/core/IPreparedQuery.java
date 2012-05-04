package com.googlecode.contraildb.core;

import java.io.IOException;
import java.util.List;

public interface IPreparedQuery<T extends Item>  {
	
	/**
	 * @return The session that created this query 
	 */
	public IContrailSession getSession();

	/**
	 * An asynchronous method for getting results.
	 * This method is the fastest way to retrieve some results 
	 * since you dont have to wait for all the results. 
	 */
	public void process(IProcessor processor) throws IOException;

	/**
	 * A convenience method for getting results.
	 * Returns all the results at once.  
	 * This methods uses the iterate method
	 * and just collects all the results before returning.
	 */
	public List<T> list(FetchOptions fetchOptions) throws IOException;

	/**
	 * A convenience method for getting results.
	 * Returns all the results at once.  
	 * This methods uses the iterate method
	 * and just collects all the results before returning.
	 */
	public List<T> list() throws IOException;

	/**
	 * A convenience method for getting results.
	 * Returns only the first result, if any.  
	 */
	public T item() throws IOException;

	/**
	 * A convenience method for just getting the number of results 
	 * that would be returned by a query.  
	 */
	public int count() throws IOException;

	/**
	 * A convenience method for just getting identifiers returned by a query.  
	 */
	public Iterable<Identifier> identifiers() throws IOException;
}
