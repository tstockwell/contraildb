package com.googlecode.contraildb.core.utils;

import com.googlecode.contraildb.core.IResult;

/**
 * A convenience handler that invokes the onNull method if the incoming result returns null.
 * @author ted.stockwell
 */
@SuppressWarnings({"rawtypes","unchecked"})
abstract public class NullHandler<I> extends Handler {

	public NullHandler(IResult<I> task) {
		super(task);
	}
	public NullHandler(I value) {
		this(TaskUtils.asResult(value));
	}
	
	protected IResult<I> onSuccess() throws Exception {
		IResult incoming= incoming();
		if (incoming.getResult() == null)
			return onNull();
		return incoming;
	}
	
	abstract protected IResult<I> onNull() throws Exception;
	
}
