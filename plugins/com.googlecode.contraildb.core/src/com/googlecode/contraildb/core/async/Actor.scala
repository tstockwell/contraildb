package com.googlecode.contraildb.core.async

/**
 * Contrail's implementation of Actor concept.
 * A player has an internal TaskMaster that manages the execution of tasks.
 * 
 * Unlike other actor implementations that handle messages in a central 'reactor' function, 
 * subclasses implement methods in the usual OO fashion but the methods always execute 
 * code by passing a closure to the sync method, the sync method will execute 
 * received functions in the order in which they are received.
 * 
 * This approach eliminates the duplicate effort of defining an API on top of the conventional 
 * actor API.
 */
trait Actor {
	protected var _scheduler:TaskScheduler= _;
	
	protected def sync[V](taskMethod:  => V):Result[V]= {
	  val task= Task.toTask(taskMethod);
	  _scheduler.submit(task);
	  return task;
	}
	
	protected def scheduler(scheduler:TaskScheduler) {
	  _scheduler= scheduler;
	}
}