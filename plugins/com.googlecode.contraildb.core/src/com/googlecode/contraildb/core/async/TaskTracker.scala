package com.googlecode.contraildb.core.async;

import java.util.ArrayList;


/** 
 * A simple utility for running tasks and waiting until they're all done.
 * @author ted.stockwell
 */
class TaskTracker extends TaskScheduler {
	private ArrayList<IResult> _tasks= new ArrayList<IResult>();
	
	public TaskTracker() {
	}
	public TaskTracker(IResult... results) {
		trackAll(results);
	}

	public <T> IResult<T> submit(ContrailTask<T> task) {
		IResult<T> result= task.submit();
		_tasks.add(result);
		return result;
	}
	
	public <T> IResult<T> track(IResult<T> result) {
		_tasks.add(result);
		return result;
	}
	public void trackAll(IResult... results) {
		for (IResult result:results) {
			_tasks.add(result);
		}
	}
	
	/**
	 * Does not return until all tasks are complete.
	 * @throws a runtime exception if an error occurs in any of the tasks.
	 */
	public void await() throws Pausable {
		TaskUtils.combineResults(_tasks).get();
	}
	
	/**
	 * Does not return until all tasks are complete.
	 * @throws an exception of type t or a RuntimeException if an error occurs in any of the tasks.
	 */
	public <T extends Throwable> void await(Class<T> t) throws T, Pausable {
		TaskUtils.combineResults(_tasks).get();
	}
	
	/**
	 * Blocking version of get
	 */
	public void awaitb() {
		TaskUtils.combineResults(_tasks).getb();
	}
}
