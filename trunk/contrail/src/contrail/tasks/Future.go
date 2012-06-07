package tasks

import (
	"sync"
)

/*
  func GoList(id *Identifier, func() []*Identifer) Future 
  // An example of how to spawn a concurrent task
  children:= tasks.GoList(path, func() { return storageSession.listChildren(path) })

  // IdentifierList.Get() panics if error in the func passed to tasks.DoList  
  for i,c:= range []*Identifer(children.Get())) { 
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
	err error
	completeHandlers []func(*Future)
	lock *sync.Mutex
	doneNotify chan[bool] 
}

func *Future CreateFuture() {
	return &Future {
		lock: sync.Mutex{}
		doneNotify: make(chan[bool], 1)
	}
}

/**
 * Called when the associated function has been cancelled
 */	
func (self *Future) Cancel() {
	self.lock.Lock()
	defer self.lock.Unlock()
	
	if !self.done {
		self.cancelled= true
		self.complete(false, null, null);
	}
}

/**
 * Called when the associated function has completed successfully
 */	
func (self *Future) Success(result interface{}) {
	self.lock.Lock()
	defer self.lock.Unlock()
	
	if !self.done {
		self.cancelled= true
		self.complete(true, result, null);
	}
}

/**
 * Called when the associated function fails
 */	
func (self *Future) Error(err error) {
	self.lock.Lock()
	defer self.lock.Unlock()
	
	if !self.done {
		self.cancelled= true
		self.complete(false, result, err);
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
	return self.success	
}

/**
 * If this task failed then get the error.
 */
func (self *Future) Error() error {
	self.lock.Lock()
	defer self.lock.Unlock()
	return self.error	
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
func (self *Future) OnComplete(func handler(future Future)) {
	self.lock.Lock()
	defer self.lock.Unlock()

	if self.done {
		go handler(self)
		return
	} 

	if self.completedHandlers == nil {
		self.completedHandlers= make([]func(*Future), 1)
	}
	append(self.completedHandlers, future)
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
	<-self.doneNotify // wait for completion
	self.lock.Lock()
	self.doneNotify= nil
}

func complete(success bool, result interface{}, err error) {
	self.lock.Lock()
	defer self.lock.Unlock()
	
	if self.done { return }
	self.done= true
	self.success= success
	self.error= err
	self.result= result
	
	if self.completedHandlers != nil {
		for _,handler:= range self.ccompletedHandlers {
			go handler(self)
		}
		self.completedHandlers= nil
	}
	
	
	self.lock.Unlock()
	self.doneNotify<-true // notify the Join() method that results are available
	self.lock.Lock()
}