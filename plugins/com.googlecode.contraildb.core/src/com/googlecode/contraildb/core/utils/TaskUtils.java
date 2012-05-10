package com.googlecode.contraildb.core.utils;

import java.util.Arrays;
import java.util.Collection;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.IResultHandler;

@SuppressWarnings({"unchecked", "rawtypes"})
public class TaskUtils {
	
	public static final IResult<Void> DONE= asResult(null); 
	public static final IResult<Boolean> SUCCESS= asResult(true); 
	public static final IResult<Boolean> FAIL= asResult(false); 
	public static final IResult<Boolean> TRUE= asResult(true); 
	public static final IResult<Boolean> FALSE= asResult(false); 
	public static final IResult NULL= asResult(null); 
	
	
	
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
		IResultHandler handler= new Handler() {
			synchronized public void onComplete() {
				IResult r= incoming();
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
			task.onComplete(handler);
		}
		return result;
	} 
	public static final <T extends IResult<?>> IResult<Void> combineResults(T... tasks) {
		if (tasks == null || tasks.length <= 0)
			return DONE;
		if (tasks.length <= 1)
			return (IResult<Void>) tasks[0];
		return combineResults(Arrays.asList(tasks));
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
			throw new RuntimeException("unknown error");
		if (t instanceof RuntimeException)
			throw (RuntimeException)t;
		if (t instanceof Error)
			throw (Error)t;
		throw new RuntimeException(t);
	}
	
	
//	public static Throwable getThrowable(final Collection<ContrailTask<?>> tasks) 
//	{
//		try {
//			for (ContrailTask<?> task: tasks)
//				task.join();
//		} 
//		catch (Throwable e) {
//			while (e instanceof RuntimeException) {
//				Throwable cause= e.getCause();
//				if (cause == null)
//					break;
//				e= cause;
//			}
//			return e;
//		} 
//		return null;
//	} 
//	public static Throwable getThrowable(ContrailTask<?>... tasks) 
//	{
//		try {
//			for (ContrailTask<?> task: tasks)
//				task.join();
//		} 
//		catch (Throwable e) {
//			while (e instanceof RuntimeException) {
//				Throwable cause= e.getCause();
//				if (cause == null)
//					break;
//				e= cause;
//			}
//			return e;
//		} 
//		return null;
//	}
	
	/**
	 * Convert a static value to a Result
	 */
	public static <X, Y extends X> IResult<X> asResult( final Y bs) {
		return new IResult<X>() {
			public X get() {
				return bs;
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
			public void onComplete(IResultHandler<X> handler) {
				try {
					handler.onComplete(this);
				}
				catch (Throwable t) {
					Logging.warning("Error while handling completion", t);
				}
			}
			@Override
			public void onSuccess(IResultHandler<X> handler) {
				onComplete(handler);
			}
			@Override
			public void onError(IResultHandler<X> handler) {
				// do nothing
			}
		};
	}
}
