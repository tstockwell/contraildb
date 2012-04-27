package com.googlecode.contraildb.core.utils;


/**
 * An interface for handling results produced by asynchronous operations
 * A completion handler may produce some result. 
 * @param I in the input type
 * @param O in the output type
 */
public interface Completion<I,O> {
	public Receipt<O> complete(Receipt<I> receipt);
}
