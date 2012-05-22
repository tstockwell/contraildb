package com.googlecode.contraildb.core.async;

import com.googlecode.contraildb.core.IResult;

/**
 * Calls the onSuccess method if and only if the value passed in the 
 * constructor is true. 
 * 
 * @author ted.stockwell
 *
 */
abstract public class If<T> extends Handler<Boolean,T> {
	
	public If(IResult<Boolean> result) {
		super(result);
	}
	public If(boolean value) {
		this(TaskUtils.asResult(value));
	}
	
	@Override
	protected void _onComplete(IResult<Boolean> result) {
		if (!result.isSuccess() || result.getResult()) 
			super._onComplete(result);
	}
}
