package com.googlecode.contraildb.core.utils;

import java.util.Collection;

@SuppressWarnings("unchecked")
public class TaskUtils {
	
	
	
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
	public static final <T extends ContrailTask<?>> Collection<T> joinAll(Collection<T> tasks) {
		Throwable t= null;
		for (ContrailTask<?> task: tasks) {
			Throwable t2= task.getThrowable();
			if (t == null) 
				t= t2;
		}
		if (t != null) 
			throwSomething(t);
		return tasks;
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
	
	public static <X, Y extends X> IResult<X> asResult( final Y bs) {
		return new IResult<X>() {
			public X get() {
				return bs;
			}
			public boolean isDone() {
				return true;
			}
		};
	}
}
