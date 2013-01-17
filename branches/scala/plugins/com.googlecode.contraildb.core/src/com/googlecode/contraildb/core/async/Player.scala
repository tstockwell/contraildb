package com.googlecode.contraildb.core.async

/**
 * Contrail's implementation of Actor concept.
 * A player has an internal TaskMaster that manages the execution of tasks.
 */
trait Player {
	protected var _scheduler:TaskScheduler= _;
	
	protected def sync[V](taskMethod:  => V):Result[V]= {
	  val task= Task.toTask(taskMethod);
	  _scheduler.submit(task);
	  return task;
	}
}