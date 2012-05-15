package com.googlecode.contraildb.core.utils;

import com.googlecode.contraildb.core.IResult;

@SuppressWarnings({"rawtypes","unchecked"})
abstract public class InvocationAction<I> extends Handler<I, Void> {
	public InvocationAction(IResult<I> results) {
		super(results);
	}
	
	final protected IResult onSuccess() throws Exception {
		onSuccess((I)incoming().getResult());
		return TaskUtils.DONE;
	}
	abstract protected void onSuccess(I results) throws Exception;
}
