package com.googlecode.contraildb.core.async;

import com.googlecode.contraildb.core.IResult;

@SuppressWarnings({"rawtypes","unchecked"})
abstract public class ResultHandler<I> extends Handler {
	public ResultHandler(IResult<I> results) {
		super(results);
	}
	
	final protected IResult onSuccess() throws Exception {
		onSuccess((I)incoming().getResult());
		return TaskUtils.DONE;
	}
	abstract protected IResult onSuccess(I results) throws Exception;
}
