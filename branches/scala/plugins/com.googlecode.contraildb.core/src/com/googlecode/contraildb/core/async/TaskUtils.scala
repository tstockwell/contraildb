package com.googlecode.contraildb.core.async;

import java.util.ArrayList
import java.util.Arrays
import java.util.Collection
import com.googlecode.contraildb.core.utils.Logging
import scala.actors.threadpool.AtomicInteger
import scala.collection.mutable.ArrayBuffer

object TaskUtils {
	
	
	/**
	 * Returns a Result that is completed when all the given tasks are complete.
	 * If any task completes with an error then the returned Result also completes with an error.
	 */
	def combineResults[T <: Result[_ <: Any]](tasks:Iterable[T]):Result[Boolean] = {
		if (tasks == null || tasks.isEmpty)
			return Result.DONE;
		val result= new Promise[Boolean]();
		val count= new AtomicInteger(tasks.size);
		for (task <- tasks) {
			task.onDone {
				if (task.success) {
					if (count.decrementAndGet() <= 0) {
					  result.success(true);
					}
				}
				else if (task.cancelled) {
				  result.cancel();
				}
				else  if (task.error != null) {
				  result.error(task.error);
				}
			}
		}
		return result;
	} 
	
	def combineResults[T <: Result[_ <: Any]](task:T, moreTasks:T*):Result[_ <: Any] = {
		if (moreTasks.length <= 0)
			return task;
		val list= new ArrayBuffer[T]();
		list++= moreTasks;
		list+= task;
		return combineResults(list);
	}
	
}
