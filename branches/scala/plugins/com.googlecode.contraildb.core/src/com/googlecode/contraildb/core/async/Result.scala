package com.googlecode.contraildb.core.async;
import scala.collection.mutable.ArrayBuffer
import com.googlecode.contraildb.core.utils.Logging



/**
 * A <tt>Result</tt> represents the result of an asynchronous computation.  
 * 
 * @param [V] The result type returned by this result's <tt>get</tt> method
 */
trait Result[V] extends Player {
	type Handler = (Result[V]) => Unit;
	
	protected var _done= false;
	protected var _result:V= _;
	protected var _success= false;
	protected var _cancelled= false;
	protected var _error:Throwable= _;
	protected var _completedHandlers:ArrayBuffer[(Result[V])=>Any]= null;
	
    /**
     * Returns <tt>true</tt> if this task completed.
     *
     * Completion may be due to normal termination, an exception, or
     * cancellation -- in all of these cases, this method will return
     * <tt>true</tt>.
     *
     * @return <tt>true</tt> if this task completed
     */
    def done:Boolean= _done;
	
    /**
     * Returns <tt>true</tt> if this task completed successfully.
     */
    def success:Boolean= _success;
	
    /**
     * Returns <tt>true</tt> if this task was cancelled.
     */
    def cancelled:Boolean= _cancelled;
	
    /**
     * If this task failed then get the error.
     */
    def error:Throwable= _error;
	
    /**
     * Add a callback to be invoked when the associated task is completed, regardless of 
     * whether the task completed successfully, was canceled, or threw an error.
     */
	def onDone (todo:(Result[V])=> Any) {
	  addHandler(todo);
	}
	def onDone (todo: => Any)

	def |(handler: => Any) {
	  sync {
	    if (done) {
	        handler;
	    }
	    else {
	      addHandler((result)=> { handler; })
	    }
	  }
	}

	/**
	 * Immediately returns the value associated with the result, if any.
	 */
	def value:V= _result;
	
    /**
     * Add a callback to be invoked when the associated task completes successfully.
     */
	def onSuccess(handler: => Any) {
	  sync {
	    if (done) {
	      if (success) {
	        handler;
	      }
	    }
	    else {
	      addHandler((result)=> { if (success) { handler; } })
	    }
	  }
	}
	
    /**
     * Add a callback to be invoked when the associated task is cancelled.
     */
	def onCancel(handler: => Any) {
	  sync {
	    if (done) {
	      if (cancelled) {
	        handler;
	      }
	    }
	    else {
	      addHandler((result)=> { if (cancelled) { handler; } })
	    }
	  }
	}
    /**
     * Add a callback to be invoked when the associated task throws an error.
     */
	def onError(handler: => Any) {
	  sync {
	    if (done) {
	      val t:Throwable= error;
	      if (t != null) {
	        handler;
	      }
	    }
	    else {
	      addHandler((result)=> {
		    	val t:Throwable= error;
		    	if (t != null) {
		    		handler;
		    	}
	      })
	    }
	  }
	}

	def addHandler(handler:(Result[V])=>Any) {
	  sync {
		if (_done) {
			try {
				handler(this);
			}
			catch {
				case t: Throwable => Logging.warning("Error in result handler", t);
			}
			return;
		}
		if (_completedHandlers == null)
			_completedHandlers= new ArrayBuffer[(Result[V])=>Any]();
		_completedHandlers.append(handler);
	  }
	}

}


object Result {
	def asResult[X](bs:X):Result[X]= {
		return new Result[X]() {
			_done= true;
			_success= true;
			_result= bs;
		}; 
	}
	val DONE= new Result[Boolean]() {
		_done= true;
		_success= true;
		_result= true;
	}; 
	val SUCCESS= new Result[Boolean]() {
		_done= true;
		_success= true;
		_result= true;
	}; 
	val FAIL= new Result[Boolean]() {
		_done= true;
		_success= false;
		_result= false;
	}; 
	val TRUE= new Result[Boolean]() {
		_done= true;
		_success= true;
		_result= true;
	}; 
	val FALSE= new Result[Boolean]() {
		_done= true;
		_success= true;
		_result= false;
	}; 
	val NULL= new Result[Unit]() {
		_done= true;
		_success= true;
		_result= null;
	}; 
  
}