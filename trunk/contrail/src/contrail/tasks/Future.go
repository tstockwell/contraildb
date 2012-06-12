package tasks

import (
	"sync"
	"runtime"
)

/*
  func GoList(id *Identifier, func() []*Identifer) Future 
  // An example of how to spawn a concurrent task
  children:= tasks.GoList(path, func() { return storageSession.listChildren(path) })

  // IdentifierList.Get() panics if error in the func passed to tasks.DoList  
  for i,c:= range children.Get().([]*Identifer)) { 
  }
*/


/**
 * A <tt>Future</tt> represents the result of an asynchronous
 * computation.  
 * 
 */
type Future struct {
	done bool
	result interface{}
	success bool
	cancelled bool
	err interface{}
	completeHandlers []func(*Future)
	lock *sync.Mutex
	doneNotify chan bool 
}

func CreateFuture() *Future {
	return &Future {
		lock: &sync.Mutex{},
		doneNotify: make(chan bool, 1),
	}
}

/**
 * Called when the associated function has been cancelled
 */	
func (self *Future) SetCancel() {
	self.lock.Lock()
	defer self.lock.Unlock()
	
	if !self.done {
		self.cancelled= true
		self.complete(false, nil, nil)
	}
}

/**
 * Called when the associated function has completed successfully
 */	
func (self *Future) SetSuccess(result interface{}) {
	self.lock.Lock()
	defer self.lock.Unlock()
	
	if !self.done {
		self.cancelled= false
		self.complete(true, result, nil)
	}
}

/**
 * Called when the associated function fails
 */	
func (self *Future) SetError(err interface{}) {
	self.lock.Lock()
	defer self.lock.Unlock()
	
	if !self.done {
		self.cancelled= false
		self.complete(false, nil, err)
	}
}
	
/**
 * Returns <tt>true</tt> if this task completed.
 *
 * Completion may be due to normal termination, an exception, or
 * cancellation -- in all of these cases, this method will return
 * <tt>true</tt>.
 *
 * @return <tt>true</tt> if this task completed
 */
func (self *Future) Done() bool {
	self.lock.Lock()
	defer self.lock.Unlock()
	return self.done	
}

/**
 * Returns <tt>true</tt> if this task completed successfully.
 */
func (self *Future) Success() bool {
	self.lock.Lock()
	defer self.lock.Unlock()
	return self.success	
}

/**
 * Returns <tt>true</tt> if this task was cancelled.
 */
func (self *Future) Cancelled() bool {
	self.lock.Lock()
	defer self.lock.Unlock()
	return self.cancelled	
}

/**
 * If this task failed then get the error.
 */
func (self *Future) Error() interface{} {
	self.lock.Lock()
	defer self.lock.Unlock()
	return self.err
}

/**
 * If this task completed successfully then get the result.
 */
func (self *Future) Result() interface{} {
	self.lock.Lock()
	defer self.lock.Unlock()
	return self.result	
}

/**
 * Add a callback to be invoked when the result is available.
 */
func (self *Future) OnComplete(handler func(future *Future)) {
	self.lock.Lock()
	defer self.lock.Unlock()

	if self.done {
		go handler(self)
		return
	} 

	if self.completeHandlers == nil {
		self.completeHandlers= make([]func(*Future), 0)
	}
	self.completeHandlers= append(self.completeHandlers, handler)
}

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
func (self *Future) Get() interface{} {
	self.lock.Lock()
	defer self.lock.Unlock()
	
	if self.done {
		if !self.success && !self.cancelled {
			panic(self.err)
		}
		return self.result 
	}
	self.lock.Unlock()
	<-self.doneNotify // wait for completion
	self.lock.Lock()
	self.doneNotify= nil
	if !self.success && !self.cancelled {
		panic(self.err)
	}
	return self.result
}

/**
 * Waits if necessary for the computation to complete.
 * 
 * This method is provided as a convenience for those users that don't 
 * want to use Contrail asynchronously.  
 */
func (self *Future) Join() {
	self.lock.Lock()
	defer self.lock.Unlock()
	
	if self.done { return }
	self.lock.Unlock()
	
	runtime.Gosched()
	<-self.doneNotify // wait for completion
	
	self.lock.Lock()
	self.doneNotify= nil
}

func (self *Future) complete(success bool, result interface{}, err interface{}) {
	if self.done { return }
	self.done= true
	self.success= success
	self.err= err
	self.result= result
	
	if self.completeHandlers != nil {
		for _,handler:= range self.completeHandlers {
			go handler(self)
		}
		self.completeHandlers= nil
	}
	
	
	self.lock.Unlock()
	self.doneNotify<-true // notify the Join() method that results are available
	self.lock.Lock()
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