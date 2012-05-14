package com.googlecode.contraildb.core.utils;

import com.googlecode.contraildb.core.IResult;

@SuppressWarnings({"rawtypes","unchecked"})
abstract public class WhileHandler extends Handler {
	
	public WhileHandler() {
		super();
	}
	
	abstract protected IResult<Boolean> While() throws Exception;
	abstract protected IResult<Void> Do() throws Exception;
	
	final protected IResult onSuccess() throws Exception {
		return new Handler(While()) {
			protected IResult onSuccess() throws Exception {
				Boolean x= (Boolean)incoming().getResult();
				if (x == null || !x)
					return TaskUtils.DONE;
				return new Handler(Do()) {
					protected IResult onSuccess() throws Exception {
						return onSuccess();
					}
				};
			}
		};
	}
}
