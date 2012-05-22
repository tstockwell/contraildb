package com.googlecode.contraildb.core.async;

import com.googlecode.contraildb.core.IResult;

/**
 * A block is a Handler that is not necessarily associated with a task when it is constructed.
 * This enables Handlers to be constructed without being hard-wired to some incoming task.
 * This enables the wiring of handlers to be delegated to some other object.
 * 
 * 'top-level' blocks may not be hooked up to an incoming result and their 
 * execution will need to be kicked off using the start method.  
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
		handleResult(TaskUtils.DONE); // this causes the block's onSuccess method to be invoked
		return outgoing();
	}
}
