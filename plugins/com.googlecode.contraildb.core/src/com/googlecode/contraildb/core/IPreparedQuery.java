package com.googlecode.contraildb.core;

import java.util.List;

import com.googlecode.contraildb.core.utils.IAsyncerator;

public interface IPreparedQuery<T extends Item>  {
	
	/**
	 * @return The session that created this query 
	 */
	public IContrailSession getSession();

	/**
	 * An asynchronous method for getting identifiers.
	 * This method is the fastest way to retrieve some results 
	 * since you dont have to wait for all the results. 
	 */
	public IResult<IAsyncerator<Identifier>> identifiers(FetchOptions fetchOptions);
	/**
	 * An asynchronous method for getting values.
	 * This method is the fastest way to retrieve some results 
	 * since you dont have to wait for all the results. 
	 */
	public IResult<IAsyncerator<T>> iterate(FetchOptions fetchOptions);

	/**
	 * A convenience method for getting results.
	 * Returns all the results at once.  
	 * This methods uses the iterate method
	 * and just collects all the results before returning.
	 */
	public IResult<List<T>> list();

	/**
	 * A convenience method for getting results.
	 * Returns only the first result, if any.  
	 */
	public IResult<T> item();

	/**
	 * A convenience method for just getting the number of results 
	 * that would be returned by a query.  
	 */
	public IResult<Integer> count();

	/**
	 * A convenience method for just getting identifiers returned by a query.  
	 */
	public IResult<Iterable<Identifier>> identifiers();
}
