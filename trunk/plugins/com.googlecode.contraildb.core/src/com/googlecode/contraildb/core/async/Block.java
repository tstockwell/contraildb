package com.googlecode.contraildb.core.async;

import com.googlecode.contraildb.core.IResult;

/**
 * A block is a Handler that is not associated with a task when it is constructed.
 * This enables Handlers to be constructed without being hard-wired to some incoming task.
 * This enables the wiring of handlers to be delegated to some other object.
 * 
 * @see Series, Parallel
 *  
 * @author ted.stockwell
 */
public class Block<I, O> extends Handler<I, O> {
	public Block() { }
	
	/**
	 * If a block represents a 'top-level' task, and has no incoming result, 
	 * then the block can be executed using this method. 
	 */
	public IResult<O> run() {
		handleResult(TaskUtils.DONE);
		return outgoing();
	}
}
