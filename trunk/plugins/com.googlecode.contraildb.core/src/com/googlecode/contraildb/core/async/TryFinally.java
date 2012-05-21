package com.googlecode.contraildb.core.async;

import com.googlecode.contraildb.core.IResult;

/**
 * Override the doTry and doFinally methods.
 * 
 * @author ted.stockwell
 *
 */
@SuppressWarnings({"rawtypes","unchecked"})
abstract public class TryFinally extends Block {
	
	@Override
	final protected IResult onSuccess() throws Exception {
		try {
			IResult result= doTry();
			((Result)incoming()).complete(result);
		}
		catch (Exception x) {
			
		}
			// TODO Auto-generated method stub
			return super.onSuccess();
	}
	
	abstract protected IResult doTry() throws Exception;
	abstract protected IResult doFinally() throws Exception;
}
