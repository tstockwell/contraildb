package com.googlecode.contraildb.core.async;

import com.googlecode.contraildb.core.IResult;

/**
 * A handler that returns no results.
 *  
 * @author ted.stockwell
 */
abstract public class Activity<I> extends Handler<I, Void> {
	public Activity() { }

	@Override
	protected IResult<Void> onSuccess() throws Exception {
		doActivity();
		return TaskUtils.DONE;
	}
	abstract protected void doActivity();
}
