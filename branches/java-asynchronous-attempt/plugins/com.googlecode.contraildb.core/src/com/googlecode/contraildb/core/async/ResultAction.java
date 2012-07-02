package com.googlecode.contraildb.core.async;

import com.googlecode.contraildb.core.IResult;

@SuppressWarnings({"rawtypes","unchecked"})
abstract public class ResultAction<I> extends Handler<I, Void> {
	public ResultAction(IResult<I> results) {
		super(results);
	}
	
	final protected IResult onSuccess() throws Exception {
		onSuccess((I)incoming().getResult());
		return TaskUtils.DONE;
	}
	abstract protected void onSuccess(I results) throws Exception;
}
