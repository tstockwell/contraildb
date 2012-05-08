package com.googlecode.contraildb.core;



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
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     *
     * @return the computed result
     * @throws An unchecked exception if an error occurred while producing the result
     */
    public V get();

    /**
     * Add a callback to be invoked when the result is available.
     * 
     * The returned IResult instance may be used to handle errors
     * that occur when the handler is invoked.  That is, if the 
     * handler throws an error when it is invoked then the returned
     * result will be completed with an error.  
     * It is convenient to handle errors that occur in the handler 
     * by adding an onError handler to the returned IResult. 
     * 
     */
	public IResult<Void> onComplete(IResultHandler<V> handler);
	
    /**
     * Add a callback to be invoked this result is completed successfully.
     * 
     * The returned IResult instance may be used to handle errors
     * that occur when the handler is invoked.  That is, if the 
     * handler throws an error when it is invoked then the returned
     * result will be completed with an error.  
     * It is convenient to handle errors that occur in the handler 
     * by adding an onError handler to the returned IResult. 
     */
	public IResult<Void> onSuccess(IResultHandler<V> handler);


    /**
     * Add a callback to be invoked this result is completed with an error.
     */
	public void onError(IResultHandler<V> handler);
}
