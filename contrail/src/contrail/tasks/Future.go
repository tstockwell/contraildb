package tasks

import (

)

func GoList(id *Identifier, func() []*Identifer) Future 

children:= tasks.GoList(path, func() { return storageSession.listChildren(path) })

// IdentifierList.Get() panics if error in the func passed to tasks.DoList  
for i,c:= range []*Identifer(children.Get())) { 
}


/**
 * A <tt>Future</tt> represents the result of an asynchronous
 * computation.  
 * 
 */
type Future struct {
	
    /**
     * Returns <tt>true</tt> if this task completed.
     *
     * Completion may be due to normal termination, an exception, or
     * cancellation -- in all of these cases, this method will return
     * <tt>true</tt>.
     *
     * @return <tt>true</tt> if this task completed
     */
    func Done() bool
	
    /**
     * Returns <tt>true</tt> if this task completed successfully.
     */
    func Success() bool
	
    /**
     * Returns <tt>true</tt> if this task was cancelled.
     */
    func Cancelled() bool
	
    /**
     * If this task failed then get the error.
     */
    func Error() error;
	
    /**
     * If this task completed successfully then get the result.
     */
    func Result() interface{}
    
    /**
     * Add a callback to be invoked when the result is available.
     */
	func onComplete(func handler(future Future))

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     * 
     * This method is provided as a convenience for those users that don't 
     * want to use Contrail asynchronously.  
     *
     * @return the computed result
     * @panic if an error occurred while producing the result
     */
    func Get() interface{}

    /**
     * Waits if necessary for the computation to complete.
     * 
     * This method is provided as a convenience for those users that don't 
     * want to use Contrail asynchronously.  
     */
    func Join()
}
