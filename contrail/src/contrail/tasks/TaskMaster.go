package tasks

/**
 * A convenient task scheduler for Contrail that coordinates tasks according 
 * to an associated Identifier and operation type (READ, WRITE, DELETE, LIST, 
 * or CREATE).
 * The purpose of this class is to manage the order in which asynchronous 
 * requests are executed so that operations are performed in a way that 
 * preserves sequential serialization (in other words, the operations appear to 
 * have all been executed sequentially, in the order that they arrived) while 
 * also allowing for as much parallelization as possible in order to maximize 
 * performance.
 *  
 * Clients of this class call the Submit method with an Identifier, an 
 * associated task type, and a function to execute.
 * The function will be invoked at a future time when this class determines 
 * that the operation is 'safe' to execute.
 * The Submit method returns a Future that can be used to track the 
 * progress of the submitted task.
 *
 * Clients can also use the Join method to wait for all submitted tasks to 
 * complete.
 * 
 * This class contains a list of identifiers.
 * An Identifier indicates an object's location in Contrails hierarchy.
 * The task manager coordinates operations on objects according to their place in 
 * the hierarchy as well as the type of operation being performed.
 * 
 * Here are the rules for scheduling tasks...
 * 
 * A DELETE operation on an object may not proceed until all pending operations 
 * on the associated object or any of its descendants have completed.
 * A DELETE operation blocks any subsequent operations on an object and all its 
 * descendants until the delete has completed.
 * 
 * A READ operation on an object may not proceed until all pending WRITE and 
 * CREATE operations on that object have completed.  
 * 
 * A LIST operation on an object may not proceed until all pending WRITE and 
 * CREATE operations of any children have completed.  
 * 
 * A WRITE operation on an object may not proceed until all pending READ, WRITE, 
 * and CREATE operations on that object have completed.  
 * 
 * A CREATE operation on an object may not proceed until all pending operations 
 * on that object have completed.     
 * 
 * @author Ted Stockwell
 */
 
 import (
 	. "contrail/id"
 	"sync"
 	"contrail/util/errors"
 )

type tOperation int
const READ 		tOperation= 1 
const WRITE 	tOperation= 2 
const DELETE 	tOperation= 3 
const LIST 		tOperation= 4 
const CREATE 	tOperation= 5 

type tTaskSet map[*tTask]*tTask
func newTaskSet() tTaskSet { return make(tTaskSet) }
func (self tTaskSet) add(task *tTask) { self[task]= task }
func (self tTaskSet) remove(task *tTask) { delete(self,task) }
func (self tTaskSet) contains(task *tTask) bool { return self[task] != nil }



// info for managing/scheduling a single task 
type tTask struct {
	op tOperation
	id *Identifier
	pendingTasks tTaskSet
	result *Future
	call func() interface{}
}

type TaskMaster struct {
	taskStorage *IdStorage
	lock *sync.Mutex
}

func CreateTaskMaster() *TaskMaster {
	return &TaskMaster{
		taskStorage: CreateIdStorage(),
		lock: new (sync.Mutex),
	}
}

func (self *TaskMaster) addTask(task *tTask) {

	// add task to internal list
	tasks, _:= self.taskStorage.Fetch(task.id).(tTaskSet)
	if tasks == nil {
		tasks= newTaskSet()
		self.taskStorage.Store(task.id, tasks)
	}
	tasks.add(task)
	
	// if there are no pending tasks then just run the given task
	if task.pendingTasks == nil || len(task.pendingTasks) <= 0 {
		self.runTask(task)
		return
	}
	
	// add listeners to pending tasks and fire up this task when they're all done
	for pendingTask,_:= range task.pendingTasks {
		pt:= pendingTask
		pendingTask.result.OnComplete(func (future *Future) {
			delete(task.pendingTasks, pt)
			if  len(task.pendingTasks) <= 0 {
				self.lock.Lock()
				defer self.lock.Unlock()
				
				self.runTask(task)
			}
		})
	}
}

func (self *TaskMaster) runTask(task *tTask) {
	go func() {
		// if the task function panics then this function 
		// complete the associated future with an error
		defer func() {
		        if err := recover(); err != nil {
		        	task.result.SetError(errors.CreateError(err))
		        }
		    }()		
		    
		// run the associated function
		// if no error occurs then successfully complete the associated future
		// with the returned value 
		value:= task.call()
		task.result.SetSuccess(value)
	}()
}
	
func (self *TaskMaster) findPendingTasks(op tOperation, id *Identifier) tTaskSet {

	pendingTasks:= newTaskSet()

	visitor:= func (nodeId *Identifier, content interface{}) {
		tasksInProgress, _:= content.(tTaskSet) 
		if tasksInProgress != nil {
			for taskInProgress,_:= range tasksInProgress {
				if !taskInProgress.result.Done() && IsDependentTask(op, taskInProgress.op) {
					pendingTasks.add(taskInProgress)
				}
			}
		}
	}
	self.taskStorage.VisitNode(id, visitor)
	
	/*
	 * optimization
	 * incoming write tasks can cancel pending write tasks as long as there 
	 * are no pending READS
	 */
	if (op == WRITE) {
		noReads:= true
		for pendingTask,_:= range pendingTasks {
			if pendingTask.op == READ {
				noReads= false
			}
		}
		if noReads {
			for pendingTask,_:= range pendingTasks {
				if pendingTask.op == WRITE {
					pendingTask.result.SetCancel()
				}
			}
		}
	}

	// look down the tree for operations on any children
	if op == LIST || op == DELETE { 
		self.taskStorage.VisitDescendents(id, visitor)
	}

	// look up the tree for deletes
	self.taskStorage.VisitParents(id, visitor)

	return pendingTasks
}

/**
 * Executes the given function at an appropriate time in the future.
 * The returned Future is bound to the given function.
 */		
func (self *TaskMaster) Submit(op tOperation, id *Identifier, task func() interface{}) *Future {
	self.lock.Lock()
	defer self.lock.Unlock()

	pendingTasks:= 	self.findPendingTasks(op, id)
	ttsk:= tTask {
		op: 			op,
		id: 			id,
		pendingTasks: 	pendingTasks,
		result: 		CreateFuture(),
		call:			task,
	}
	self.addTask(&ttsk)
	return ttsk.result
}
 

func IsDependentTask (incomingOp tOperation, previousOp tOperation) bool {
	switch incomingOp {
		case READ: 
			switch (previousOp) { 
				case DELETE: 	return true
				case WRITE: 	return true 
			}
		case WRITE: 
			switch (previousOp) { 
				case READ: 		return true
				case DELETE: 	return true
				case WRITE: 	return true
				case CREATE: 	return true 
			}
		case DELETE: 
			return true
		case LIST: 
			switch (previousOp) { 
				case DELETE: 	return true
				case WRITE: 	return true
				case CREATE: 	return true
			}
		case CREATE:   
			return true
	}
	return false;
}

func (self *TaskMaster) Close() {
	self.Join()
	
	self.lock.Lock()
	defer self.lock.Unlock()
	self.taskStorage= nil
}

func (self *TaskMaster) Wait() {
	WaitAll(self.getFutures());
}

func (self *TaskMaster) Join() {
	JoinAll(self.getFutures());
}

func (self *TaskMaster) getFutures() []*Future {
	self.lock.Lock()
	defer self.lock.Unlock()

	futures:= make([]*Future, 0, self.taskStorage.Size())	
	for _,id:= range self.taskStorage.ListAll() {
		tasksInProgress, _:= self.taskStorage.Fetch(id).(tTaskSet)
		if tasksInProgress != nil {
			for taskInProgress,_:= range tasksInProgress {
				futures= append(futures, taskInProgress.result)
			}
		}
	}
	return futures;
}
