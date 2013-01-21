package com.googlecode.contraildb.core.async
import java.util.ArrayList
import java.util.HashMap
import java.util.concurrent._;


object TaskScheduler {
	private val executorService= Executors.newCachedThreadPool();
  
}

/**
 * Takes incoming instances of the Task class and stores them in a queue.
 * The tasks are removed from the queue at an appropriate future time and 
 * executed.
 * 
 * The default implementation merely queues up tasks and executes them 
 * sequentially.  This is useful for a basic actor implementation.
 * 
 * The SerializableScheduler class implements a version of TaskScheduler
 * that executes tasks in a way that guarantees 
 * <a href="http://en.wikipedia.org/wiki/Serializability">serializability</a>. 
 * 
 * The TaskTracker class implements a version of TaskScheduler
 * that executes tasks as soon as they are submitted, its only purpose 
 * is to provide a way of tracking the progress of a group of tasks. 
 * 
 */
class TaskScheduler {
	private var _tasks:HashMap[Integer, Task[Any]]= new HashMap();
	private var _taskOrder:ArrayList[Integer]= new ArrayList();
  
	def submit[T](task:Task[T]):Result[V] {
	  val thisScheduler= this;
	  thisScheduler.synchronized {
		  val hashCode= task.hashCode;
		  task.onDone {
		    thisScheduler.synchronized {
			  if (_tasks.remove(hashCode) != null)
				  _taskOrder.remove(0);
		    }
		  }
		  if (_tasks.isEmpty()) {
			  _taskOrder.add(hashCode);
			  _tasks.put(hashCode, task);
			  executeTask(task);
			  return
		  }

		  val lastTask= _tasks.get(_taskOrder.get(_taskOrder.size()-1));
		  _taskOrder.add(hashCode);
		  _tasks.put(hashCode, task);
		  lastTask.onDone {
			  executeTask(task);
		  }
	  }
	}
	
	def submit[V](taskMethod: => V):Result[V]= {
	  this.synchronized {
		  val task:Task[V]= Task.toTask(taskMethod);
		  submit(task);
		  return task;
	  }
	}
	
	/**
	 * Internal method for executing a task.
	 * The default implementation just uses an instance of 
	 * java.util.concurrent.ExecutorService to execute tasks
	 * concurrently.
	 * Subclassed may override this method to implement
	 * thier own scheme.  
	 */
	protected def executeTask[T] (task:Task[T]) {
		TaskScheduler.executorService.execute(task);
	}

}