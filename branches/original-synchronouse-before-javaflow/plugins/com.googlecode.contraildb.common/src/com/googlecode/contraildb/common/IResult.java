package com.googlecode.contraildb.common;



/**
 * A <tt>Claim</tt> represents the result of an asynchronous
 * computation.  
 * 
 * @param <V> The result type returned by this Future's <tt>get</tt> method
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
     * This method should never be used in an internal Contrail API.
     * 
     * This method is provided as a convenience for those users that don't 
     * want to use Contrail asynchronously.  
     *
     * @return the computed result
     * @throws An unchecked exception if an error occurred while producing the result
     */
    public V get();

    /**
     * Waits if necessary for the computation to complete.
     * Calling this method can block the current thread therefore it's
     * use should generally be avoided.
     * 
     * This method should never be used in an internal Contrail API.
     * 
     * This method is provided as a convenience for those users that don't 
     * want to use Contrail asynchronously.  
     */
    public void join();
}
