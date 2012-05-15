package com.googlecode.contraildb.core;

import com.googlecode.contraildb.core.utils.IAsyncerator;


/**
 * An interface for retrieving query results asynchronously.
 */
public interface IProcessor<T> extends IAsyncerator<T> {
	public IResult<Void> close();
}