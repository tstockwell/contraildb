package com.googlecode.contraildb.core.utils;

import com.googlecode.contraildb.core.Identifier;


/**
 * A base task class for Contrail related tasks.
 * In Contrail a task is considered to be some operation on a resource.
 * The operations are considered to be one of:
 * 	 	Operation.READ,
 * 		Operation.WRITE,
 * 		Operation.DELETE,
 * 		Operation.LIST, or
 * 		Operation.CREATE
 * 
 * Subclasses need to implement the run method.
 * 
 * @author Ted Stockwell
 *
 * @param <T> The result type returned by the <tt>getResult</tt> method
 */
abstract public class ContrailTask<T>
extends ReceiptImpl<T>
implements Receipt<T>, CTask<T> 
{
	public static enum Operation {
		READ,
		WRITE,
		DELETE,
		LIST,
		CREATE
	}
	
	
	Identifier _id;
	Operation _operation;
	
	public ContrailTask(Identifier id, Operation operation) {
		if ((_id= id) == null)
			_id= Identifier.create();
		
		if ((_operation= operation) == null)
			_operation= Operation.READ;
	}
	
	public ContrailTask() {
		this(Identifier.create(), Operation.READ);
	}
	
	@Override
	public synchronized void cancel() {
		super.cancel();
		stop();
	}
	
	/**
	 * This method is invoked when a task is cancelled.
	 * Subclasses should override this method and do whatever is 
	 * required in order to make the task stop its operation.  
	 */
	protected void stop() {
		// do nothing
	}
	
	
	
	@Override
	public String toString() {
		return "{_id="+_id+", _operation="+_operation+"}";
	}
	
	public Identifier getId() {
		return _id;
	}
	
	public Operation getOperation() {
		return _operation;
	}
	
	@Override abstract public Receipt<T> run();
	
}

