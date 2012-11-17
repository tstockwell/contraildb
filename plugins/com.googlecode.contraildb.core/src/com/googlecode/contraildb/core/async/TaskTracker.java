package com.googlecode.contraildb.core.async;

import java.util.ArrayList;

import kilim.Pausable;

/** 
 * A simple utility for running tasks and waiting until they're all done.
 * @author ted.stockwell
 */
@SuppressWarnings("rawtypes")
public class TaskTracker {
	private ArrayList<IResult> _tasks= new ArrayList<IResult>();

	public <T> IResult<T> submit(ContrailTask<T> task) {
		IResult<T> result= task.submit();
		_tasks.add(result);
		return result;
	}
	
	public <T> IResult<T> track(IResult<T> result) {
		_tasks.add(result);
		return result;
	}
	public void track(IResult... results) {
		for (IResult result:results) {
			_tasks.add(result);
		}
	}
	
	/**
	 * Does not return until all tasks are complete.
	 * @throws a runtime exception if an error occurs in any of the tasks.
	 */
	public void get() throws Pausable {
		TaskUtils.combineResults(_tasks).get();
	}
	/**
	 * Blocking version of get
	 */
	public void getb() {
		TaskUtils.combineResults(_tasks).getb();
	}
}
