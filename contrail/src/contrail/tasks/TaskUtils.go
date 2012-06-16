package tasks

import "contrail/util/errors"

// Runs a function that returns a result
// Returns a Future that is associated with the running function.
// When the Future is complete the Result() method will return the value 
// returned from the associated function.
// Use the Get or Join methods to wait for completion. 
// If the function panics the panic is captured and the associated 
// Future will...
//		... will return false from the Future.Success method.     
//		... will return the panic value from the Future.Error method.     
//		... will invoke the same panic if the Future.Get method is invoked
func GoResult(task func() interface{}) *Future {
	future:= CreateFuture()
	go RunResult(future, task)
	return future
}
// Runs a function
// Returns a Future that is associated with the running function.
// Use the Get or Join methods to wait for completion. 
// If the function panics the panic is captured and the associated 
// Future will...
//		... will return false from the Future.Success method.     
//		... will return the panic value from the Future.Error method.     
//		... will invoke the same panic if the Future.Get method is invoked
func Go(task func()) *Future {
	future:= CreateFuture()
	go Run(future, task)
	return future
}
// Runs the given function and asociates the results with the given Future
// When the Future is completed the Future.Result() method will return the value 
// returned from the associated function.
// Use the Get or Join methods to wait for completion. 
// If the function panics the panic is captured and the associated 
// Future will...
//		... will return false from the Future.Success method.     
//		... will return the panic value from the Future.Error method.     
//		... will invoke the same panic if the Future.Get method is invoked
func RunResult(future *Future, task func() interface{}) {
	func() {
		// if the task function panics then this function 
		// complete the associated future with an error
		defer func() {
	        if err := recover(); err != nil {
	        	future.SetError(errors.CreateError(err))
	        }
		}()		
		    
		// run the associated function
		// if no error occurs then successfully complete the associated future
		// with the returned value 
		value:= task()
		future.SetSuccess(value)
	}()
}
// Runs the given function and asociates the results with the given Future
// When the Future is completed the Future.Result() method will return the value 
// returned from the associated function.
// Use the Get or Join methods to wait for completion. 
// If the function panics the panic is captured and the associated 
// Future will...
//		... will return false from the Future.Success method.     
//		... will return the panic value from the Future.Error method.     
//		... will invoke the same panic if the Future.Get method is invoked
func Run(future *Future, task func()) {
	func() {
		// if the task function panics then this function 
		// complete the associated future with an error
		defer func() {
	        if err := recover(); err != nil {
	        	future.SetError(errors.CreateError(err))
	        }
		}()		
		    
		// run the associated function
		// if no error occurs then successfully complete the associated future
		// with the returned value 
		task()
		future.SetSuccess(true)
	}()
}



/**
 * Dont return until all the given futures are complete
 * Panic if any of the associated goroutines panic'd
 */
func WaitAll(futures []*Future) {
	for _,future:= range futures {
		future.Join()
	}
	for _,future:= range futures {
		if !future.Success() && !future.Cancelled() {
			err:= future.Error()
			if err != nil {
				panic(err)
			}
		}
	}
}

/**
 * Dont return until all the given futures are complete
 */
func JoinAll(futures []*Future) {
	for _,future:= range futures {
		future.Join()
	}
}