package com.googlecode.contraildb.core.async;



/**
 * A <tt>Result</tt> represents the result of an asynchronous computation.  
 * 
 * @param [V] The result type returned by this result's <tt>get</tt> method
 */
trait Result[V] {
	type Handler = (Result[V]) => Unit;
	
	protected var _done= false;
	protected var _result:V= _;
	protected var _success= false;
	protected var _cancelled= false;
	protected var _error:Throwable= _;
	protected var _completedHandlers:List[Handler] = _;
	
    /**
     * Returns <tt>true</tt> if this task completed.
     *
     * Completion may be due to normal termination, an exception, or
     * cancellation -- in all of these cases, this method will return
     * <tt>true</tt>.
     *
     * @return <tt>true</tt> if this task completed
     */
    def done():Boolean= _done;
	
    /**
     * Returns <tt>true</tt> if this task completed successfully.
     */
    def success():Boolean= _success;
	
    /**
     * Returns <tt>true</tt> if this task was cancelled.
     */
    def cancelled():Boolean= _cancelled;
	
    /**
     * If this task failed then get the error.
     */
    def error():Throwable= _error;
	
    /**
     * If this task completed successfully then get the result.
     * @throws IllegalStateException if the associated task is not complete.
     */
    def result():V;
    
    /**
     * Add a callback to be invoked when the result is available.
     */
	def onDone(handler:Handler);

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     * 
     * @return the computed result
     * @throws An unchecked exception if an error occurred while producing the result
     */
    def get():V;
    
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
    def getb():V;
    

    /**
     * Waits if necessary for the computation to complete.
     * 
     * Does NOT throw an exception if an error occurred in the associated task.
     * 
     */
    def join();
    
    /**
     * Waits if necessary for the computation to complete.
     * Calling this method can block the current thread therefore it's
     * use should generally be avoided.
     * 
     * Does NOT throw an exception if an error occurred in the associated task.
     * 
     */
    def joinb();
    
}
