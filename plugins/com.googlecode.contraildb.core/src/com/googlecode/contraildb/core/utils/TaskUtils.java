package com.googlecode.contraildb.core.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SuppressWarnings("unchecked")
public class TaskUtils {
	
	
	/**
	 * Wraps a value as a future
	 */
	public static <T> Receipt<T> toFuture(final T value) {
		return new Receipt<T>() {
			@Override
			public boolean isDone() {
				return true;
			}

			@Override
			public T getResult() {
				return value;
			}

			@Override
			public void onComplete(Completion<T> handler) {
				handler.complete(this);
			}

			@Override
			public boolean isCancelled() {
				return false;
			}

			@Override
			public boolean isSuccess() {
				return true;
			}

			@Override
			public Throwable getError() {
				return null;
			}
		};
	}
	
	
	public static <T> Receipt<T> sequence(Receipt<?> start, CTask<?>... tasks) {
	}
	public static <T> Receipt<T> sequence(CTask<?>... tasks) {
		return new Receipt<T>() {

			@Override
			public boolean isDone() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public T get() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void onComplete(Completion<T> handler) {
				// TODO Auto-generated method stub
				
			}
		};
	}
	
	
	public static final <X extends Throwable, T extends ContrailTask<?>> Collection<T> invokeAll(Collection<T> tasks, Class<X> errorType) throws X {
		submitAll(tasks);
		return joinAll(tasks, errorType);
	} 
	public static final <T extends ContrailTask<?>> Collection<T> invokeAll(Collection<T> tasks) {
		submitAll(tasks);
		return joinAll(tasks);
	} 
	public static final <X extends Throwable, T extends ContrailTask<?>> Collection<T> joinAll(Collection<T> tasks, Class<X> errorType) throws X {
		Throwable t= null;
		for (ContrailTask<?> task: tasks) {
			Throwable t2= task.getThrowable();
			if (t == null) 
				t= t2;
		}
		if (t != null) 
			throwSomething(t, errorType);
		return tasks;
	} 
	/**
	 * Creates a future that completes when all the given tasks (represented by futures) have completed.
	 * The returned future contains all the results returned from all the tasks.
	 * If any task fails then the returned future is failed.
	 */
	@SuppressWarnings("rawtypes")
	public static final Receipt<Map<Receipt, Object>> joinAll(final List<Receipt> tasks) {
		final ReceiptImpl<Map<Receipt, Object>> future= new ReceiptImpl<Map<Receipt, Object>>();
		final HashMap<Receipt, Object> results= new HashMap<Receipt, Object>(tasks.size());
		final int[] count= new int[] { 0 };
		final Throwable[] fail= new Throwable[] { null };
		for (final Receipt task:tasks) {
			task.onComplete(new Completion() {
				@Override synchronized public Receipt complete(Receipt result) {
					results.put(task, result);
					complete();
					return null;
				}
				
				void complete() {
					if (tasks.size() <= ++count[0]) {
						if (fail[0] != null) {
							future.failure(fail[0]);
						}
						else
							future.success(results);
					}
				}
			});
		}
		return future;
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
	public static Throwable getThrowable(final Collection<ContrailTask<?>> tasks) 
	{
		try {
			for (ContrailTask<?> task: tasks)
				task.join();
		} 
		catch (Throwable e) {
			while (e instanceof RuntimeException) {
				Throwable cause= e.getCause();
				if (cause == null)
					break;
				e= cause;
			}
			return e;
		} 
		return null;
	} 
	public static Throwable getThrowable(ContrailTask<?>... tasks) 
	{
		try {
			for (ContrailTask<?> task: tasks)
				task.join();
		} 
		catch (Throwable e) {
			while (e instanceof RuntimeException) {
				Throwable cause= e.getCause();
				if (cause == null)
					break;
				e= cause;
			}
			return e;
		} 
		return null;
	}
}
