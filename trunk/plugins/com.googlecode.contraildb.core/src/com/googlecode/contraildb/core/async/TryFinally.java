package com.googlecode.contraildb.core.async;

import com.googlecode.contraildb.core.IResult;

/**
 * Override the doTry and doFinally methods.
 * 
 * @author ted.stockwell
 *
 */
@SuppressWarnings({"rawtypes","unchecked"})
abstract public class TryFinally<T> extends Handler<Void,T> {
	
	@Override
	final protected IResult onSuccess() throws Exception {
		try {
			return doTry();
		}
		finally {
			doFinally();
		}
	}
	
	abstract protected IResult<T> doTry() throws Exception;
	abstract protected IResult<T> doFinally() throws Exception;
}
