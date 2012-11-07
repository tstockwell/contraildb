package com.googlecode.contraildb.core.async;

import kilim.Pausable;



/**
 * An <tt>IResult</tt> represents the result of an asynchronous
 * computation.  
 * 
 * @param <V> The result type returned by this result's <tt>get</tt> method
 */
public interface IResult<V> {
	
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
     * Returns <tt>true</tt> if this task completed successfully.
     */
    public boolean isSuccess();
	
    /**
     * Returns <tt>true</tt> if this task was cancelled.
     */
    public boolean isCancelled();
	
    /**
     * If this task failed then get the error.
     */
    public Throwable getError();
	
    /**
     * If this task completed successfully then get the result.
     * @throws IllegalStateException if the associated task is not complete.
     */
    public V getResult();
    
    /**
     * Add a callback to be invoked when the result is available.
     */
	public void addHandler(IResultHandler<V> handler);

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     * 
     * @return the computed result
     * @throws An unchecked exception if an error occurred while producing the result
     */
    public V get() throws Pausable;
    
    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     * Note, this call potentially blocks the current thread, unlike #get() that 
     * pauses the current fiber but doesn't block the thread.
     * Use this method in situations where Pausable cannot be thrown, like 
     * a JUnit test method.
     * 
     * @return the computed result
     * @throws An unchecked exception if an error occurred while producing the result
     */
    public V getb();
    

    /**
     * Waits if necessary for the computation to complete.
     * 
     * Does NOT throw an exception if an error occurred in the associated task.
     * 
     */
    public void join() throws Pausable;
    
    /**
     * Waits if necessary for the computation to complete.
     * Calling this method can block the current thread therefore it's
     * use should generally be avoided.
     * 
     * Does NOT throw an exception if an error occurred in the associated task.
     * 
     */
    public void joinb();
    
}