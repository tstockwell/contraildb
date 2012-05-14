package com.googlecode.contraildb.core.utils;

import com.googlecode.contraildb.core.IResult;

@SuppressWarnings({"rawtypes","unchecked"})
abstract public class InvocationHandler<I> extends Handler {
	public InvocationHandler(IResult<I> results) {
		super(results);
	}
	
	final protected IResult onSuccess() throws Exception {
		onSuccess((I)incoming().getResult());
		return TaskUtils.DONE;
	}
	abstract protected IResult onSuccess(I results) throws Exception;
}
