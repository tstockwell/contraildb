package com.googlecode.contraildb.core.async;

import com.googlecode.contraildb.core.IResult;

/**
 * Executes additional code if condition is true
 * @author ted stockwell
 *
 * @param <I>
 */
@SuppressWarnings({"rawtypes","unchecked"})
abstract public class ConditionalHandler extends Handler {
	public ConditionalHandler(IResult<Boolean> conditional) {
		super(conditional);
	}
	public ConditionalHandler(boolean conditional) {
		this(asResult(conditional));
	}
	
	final protected IResult onSuccess() throws Exception {
		IResult r;
		if ((Boolean)incoming().getResult()) {
			r= onTrue();
		}
		else 
			r= onFalse();
		return new Handler(r) {
			protected IResult onSuccess() throws Exception {
				return lastly();
			}
		};
	}
	protected IResult onTrue() throws Exception {
		return TaskUtils.DONE;
	}
	protected IResult onFalse() throws Exception {
		return TaskUtils.DONE;
	}
	protected IResult lastly() throws Exception {
		return TaskUtils.DONE;
	}
}
