package com.googlecode.contraildb.core.utils;


/**
 * A <tt>Receipt</tt> (commonly known as a future) represents the result of an asynchronous operation.
 * Completion handlers may be registered with a future and they will be
 * invoked when the operation completes.    
 * 
 * @param <V> The result type returned by this future's <tt>get</tt> method
 */
public interface Receipt<T> {
	
    /**
     * Returns <tt>true</tt> if this task completed.
     *
     * Completion may be due to normal termination, an exception, or
     * cancellation -- in all of these cases, this method will return
     * <tt>true</tt>.
     *
     * @return <tt>true</tt> if this task completed
     */
    public boolean isDone();
    
    /**
     * Returns <tt>true</tt> if this task was cancelled.
     */
    public boolean isCancelled();
    
    /**
     * Returns <tt>true</tt> if this task successfully completed.
     */
    public boolean isSuccess();
    
    /**
     * Returns the result, null if the task failed, was cancelled, or has not completed.
     */
    public T getResult();
    
    /**
     * Returns the error, if any.
     * Returns null if the task successfully completed, was cancelled, or has not completed.
     */
    public Throwable getError();
    
    /**
     * Register a completion handler to be invoked when the result is ready
     */
    public void onComplete(Completion<T,?> handler);
    
}
