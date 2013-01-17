package com.googlecode.contraildb.core.async;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;


import com.googlecode.contraildb.core.utils.Logging;

object TaskUtils {
	
	
	/**
	 * Returns a Result that is completed when all the given tasks are complete.
	 * If any task completes with an error then the returned Result also completes with an error.
	 */
	public static final <T extends IResult<?>> IResult<Void> combineResults(Collection<T> tasks) {
		if (tasks == null || tasks.isEmpty())
			return DONE;
		final Result<Void> result= new Result<Void>();
		final int[] count= new int[] { tasks.size() };
		final IResult[] error= new IResult[] { null };
		IResultHandler handler= new IResultHandler() {
			public void onComplete(IResult r) throws Exception {
				if (!r.isSuccess()) {
					error[0]= r;
				}
				if (--count[0] <= 0) {
					if (error[0] != null) {
						result.error(error[0].getError());
					}
					else
						result.success(null);
				}
			}
		};
		for (IResult<?> task: tasks) {
			task.addHandler(handler);
		}
		return result;
	} 
	public static final <T extends IResult<?>> IResult<Void> combineResults(T task, T... moreTasks) {
		if (moreTasks.length <= 0)
			return (IResult<Void>) task;
		ArrayList<T> list= new ArrayList<T>(Arrays.asList(moreTasks));
		list.add(task);
		return combineResults(list);
	} 
	public static final <T extends IResult<?>> IResult<Void> combineResults(T[] tasks) {
		if (tasks == null || tasks.length <= 0)
			return DONE;
		final Result<Void> result= new Result<Void>();
		final int[] count= new int[] { tasks.length };
		final IResult[] error= new IResult[] { null };
		IResultHandler handler= new IResultHandler() {
			public void onComplete(IResult r) throws Exception {
				if (!r.isSuccess()) {
					error[0]= r;
				}
				if (--count[0] <= 0) {
					if (error[0] != null) {
						result.error(error[0].getError());
					}
					else
						result.success(null);
				}
			}
		};
		for (IResult<?> task: tasks) {
			task.addHandler(handler);
		}
		return result;
	} 
	public static final <T extends ContrailTask> IResult<Void> combineTasks(Collection<T> tasks) {
		if (tasks == null || tasks.size() <= 0)
			return DONE;
		final Result<Void> result= new Result<Void>();
		final int[] count= new int[] { tasks.size() };
		final IResult[] error= new IResult[] { null };
		IResultHandler handler= new IResultHandler() {
			public void onComplete(IResult r) throws Exception {
				if (!r.isSuccess()) {
					error[0]= r;
				}
				if (--count[0] <= 0) {
					if (error[0] != null) {
						result.error(error[0].getError());
					}
					else
						result.success(null);
				}
			}
		};
		for (ContrailTask<?> task: tasks) {
			task.getResult().addHandler(handler);
		}
		return result;
	}
	public static final <T extends ContrailTask> IResult<Void> combineResults(T[] tasks) {
		if (tasks == null || tasks.length <= 0)
			return DONE;
		final Result<Void> result= new Result<Void>();
		final int[] count= new int[] { tasks.length };
		final IResult[] error= new IResult[] { null };
		IResultHandler handler= new IResultHandler() {
			public void onComplete(IResult r) throws Exception {
				if (!r.isSuccess()) {
					error[0]= r;
				}
				if (--count[0] <= 0) {
					if (error[0] != null) {
						result.error(error[0].getError());
					}
					else
						result.success(null);
				}
			}
		};
		for (ContrailTask<?> task: tasks) {
			task.getResult().addHandler(handler);
		}
		return result;
	} 
	
	public static final <T extends Throwable> void throwSomething(Throwable t, Class<T> type) throws T {
		if (t == null)
			return;
		if (type.isAssignableFrom(t.getClass()))
				throw (T)t;
		throwSomething(t);
	}
	
	static final <T extends Throwable> Throwable throwIfMatchingInstance(Throwable t, Class<T> type) throws T {
		if (t == null)
			return null;
		if (type.isAssignableFrom(t.getClass()))
				throw (T)t;
		return t;
	}
	public static final void throwSomething(Throwable t) {
		if (t == null)
			return;
		if (t instanceof RuntimeException)
			throw (RuntimeException)t;
		if (t instanceof Error)
			throw (Error)t;
		throw new RuntimeException(t);
	}
	
	
	public static <T extends ContrailTask<?>> Collection<T> submitAll(Collection<T> tasks) {
		for (ContrailTask<?> task: tasks)
			task.submit();
		return tasks;
	}

	
}
