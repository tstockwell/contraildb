package com.googlecode.contraildb.core.async;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.googlecode.contraildb.core.utils.Logging;

@SuppressWarnings({"unchecked","rawtypes"})
public class TaskUtils {
	
	
	public static final IResult<Void> DONE= asResult(null); 
	public static final IResult<Boolean> SUCCESS= asResult(true); 
	public static final IResult<Boolean> FAIL= asResult(false); 
	public static final IResult<Boolean> TRUE= asResult(true); 
	public static final IResult<Boolean> FALSE= asResult(false); 
	public static final IResult NULL= asResult(null); 
	public static final <T> IResult<T> NULL() { return NULL; }
	
	public static final <T> IResult<T> run(Handler<?,T> handler) {
		handler.handleResult(DONE);
		return handler.outgoing();
	}
	
	
	
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
	public static final <T extends ContrailTask<?>> IResult<Void> combineResults(T[] tasks) {
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
	static final void throwSomething(Throwable t) {
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

	
	
	/**
	 * Convert a static value to a Result
	 */
	public static <X, Y extends X> IResult<X> asResult( final Y bs) {
		return new IResult<X>() {
			public X get() {
				return bs;
			}
			public void join() {
				// do nothing
			}
			public boolean isDone() {
				return true;
			}
			@Override public boolean isSuccess() {
				return true;
			}
			@Override public Throwable getError() {
				return null;
			}
			@Override public X getResult() {
				return bs;
			}
			@Override
			public boolean isCancelled() {
				return false;
			}
			@Override
			public void addHandler(IResultHandler<X> handler) {
				try {
					handler.onComplete(this);
				}
				catch (Throwable t) {
					Logging.warning("Error while handling completion", t);
				}
			}
		};
	}

	public static <X extends Throwable, T extends IResult<?>> void getAll(Collection<T> results, Class<X> errorType) throws X {
		get(combineResults(results), errorType);
	}
	public static <T extends Throwable> void get(IResult results, Class<T> errorType) throws T {
		try {
			results.join();
		}
		catch (Throwable t) {
			throwSomething(t, errorType);
		}
	}
}
