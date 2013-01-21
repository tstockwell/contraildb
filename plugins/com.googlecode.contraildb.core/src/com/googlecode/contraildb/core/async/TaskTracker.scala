package com.googlecode.contraildb.core.async;

import java.util.ArrayList;
import scala.collection.mutable.ArrayBuffer


/** 
 * A simple utility for running tasks and waiting until they're all done.
 * This task scheduler executes all received tasks in parallel
 * 
 * @author ted.stockwell
 */
class TaskTracker extends TaskScheduler {
	val _tasks= new ArrayBuffer[Result[_ <: Any]]();
	
	def submit[V](taskMethod: => V):Result[V]= {
	  this.synchronized {
		  val task:Task[V]= Task.toTask(taskMethod);
		  submit(task);
		  return task;
	  }
	}
	
	def track[T](result:Result[T]):Result[T]= {
	  this.synchronized {
		_tasks+= result;
		return result;
	  }
	}

	/**
	 * Executes a function when all the tasks currently being tracked have completed.
	 */
	def onDone (todo: => Any)= {
	  this.synchronized {
	    TaskUtils.combineResults(_tasks) | todo;
	  }
	}
	
	def |(handler: => Any) { onDone(handler) }
	
}
