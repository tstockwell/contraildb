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
 * Clients of this class call the submit method with an Identifier, an 
 * associated task type, and a function to execute.
 * The function will be invoked at a future time when this class determines 
 * that the operation is 'safe' to execute.
 *
 * Clients can also use the Join method to wait for all submitted tasks to 
 * complete.
 * 
 * Contrail arranges all objects into a nested hierarchy.  
 * An Identifier indicates an object's location in the hierarchy.
 * The task manager coordinates operations on objects according to their place in 
 * the hierarchy as well as the type of operation being performed.
 * 
 * Here are the rules...
 * 
 * A DELETE operation on an object may not proceed until all pending operations 
 * on the associated object or any of its descendants have completed.
 * However, DELETE operations on an object are NOT blocked by CREATE operations 
 * on the same object.    
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
 * A CREATE operation on an object may not proceed until all pending READ, 
 * and WRITE operations on that object have completed.  Since the purpose of 
 * the StorageProvider.Create method is to coordinate asynchronous CREATE 
 * requests CREATE requests do not have to wait for other CREATE requests.   
 * 
 * @author Ted Stockwell
 */
 
 import (
 	"sync"
 	"list"
 )

type op int
const READ 		op= 1 
const WRITE 	op= 2 
const DELETE 	op= 3 
const LIST 		op= 4 
const CREATE 	op= 5 

type tTask struct {
	operation op
	id *Identifier
	pendingTasks List
	result Future
}
type Conductor struct {
	tasks map[string][]*tTask
	lock *sync.Mutex
}

func CreateConductor() *Conductor {
	return &Conductor{
		tasks: make(map[id.Identifier]typeTask, 0, 20),
		lock: new (sync.Mutex),
	}
}

func (this *Conductor) addTask(task *tTask) {

	// add task to internal list
	path:= task.id.Path()
	tasks:= this.tasks[path]
	if tasks == nil {
		tasks= make([]*tTask, 1)
		this.tasks[path]= tasks
	}
	tasks= append(tasks, task)
	
	// if there are no pending tasks then just run the given task
	if tTask.pendingTasks == nil || len(tTask.pendingTasks) <= 0 {
		this.runTask(tTask)
		return
	}
	
	// add listeners to pending tasks and fire up this task when they're all done
	for _,t:= range tTask.pendingTasks {
		t.result.onComplete(func (future Future) {
			remove(t.pendingTasks, t)
		})
	}
}
		
func (this *Conductor) Submit(operationType op, id *Identifier, task func()) {
	this.lock.Lock()
	defer this.lock.Unlock()
	
	addTask(&tTask {
		operation: op,
		id: id,
		pendingTasks: findPendingTasks(op, id),
	})
}
 
 
public class ContrailTaskTracker {
	
	IdentifierIndexedStorage<Set<ContrailTask>> _tasks= 
		new IdentifierIndexedStorage<Set<ContrailTask>>();
	LinkedList<ContrailTask> _tasksToBeRemoved= new LinkedList<ContrailTask>();
	ContrailAction _taskRemoval= null;
	
	public Session beginSession() {
		return new Session();
	}

	public boolean contains(ContrailAction action) {
		Set<ContrailTask> tasks= _tasks.fetch(action.getId());
		if (tasks == null)
			return false;
		return tasks.contains(action);
	}
	
	private List<ContrailTask<?>> findPendingTasks(ContrailTask task) {

		Identifier taskId= task.getId();
		final Operation op= task.getOperation();
		final ArrayList<ContrailTask<?>> pendingTasks= new ArrayList<ContrailTask<?>>();

		IdentifierIndexedStorage.Visitor visitor= new IdentifierIndexedStorage.Visitor<Set<ContrailTask>>() {
			public void visit(Identifier identifier, Set<ContrailTask> list) {
				if (list != null) {
					for (ContrailTask task2: list) {
						if (!task2.isDone() && isDependentTask(op, task2.getOperation())) {
							pendingTasks.add(task2);
						}
					}
				}
			}
		};
		_tasks.visitNode(taskId, visitor);
		
		
		/*
		 * optimization
		 * incoming write tasks can cancel pending write tasks as long as there 
		 * are no pending READS
		 */
		if (task._operation == Operation.WRITE) {
			boolean noReads= true;
			for (ContrailTask t: pendingTasks) {
				if (t._operation == Operation.READ) {
					noReads= false;
					break;
				}
			}
			if (noReads) {
				for (ContrailTask t: pendingTasks) {
					if (t._operation == Operation.WRITE)
						t.cancel();
				}
			}
		}


		// look down the tree for operations on any children
		if (op == Operation.LIST || op == Operation.DELETE) 
			_tasks.visitDescendents(taskId, visitor);

		// look up the tree for deletes
		_tasks.visitParents(taskId, visitor);

		return pendingTasks;		
	}
	
	private static boolean isDependentTask(Operation incomingOp, Operation previousOp) {
		switch (incomingOp) {
			case READ: {
				switch (previousOp) { case DELETE: case WRITE: /*case CREATE:*/ return true; }
			} break;
			case WRITE: {
				switch (previousOp) { case READ: case DELETE: case WRITE: case CREATE: return true; }
			} break;
			case DELETE: {
				switch (previousOp) { case READ: case DELETE: case WRITE: case LIST: return true; }
			} break;
			case LIST: {
				switch (previousOp) { case DELETE: case WRITE: case CREATE: return true; }
			} break;
			case CREATE:  { 
				switch (previousOp) { case READ: case DELETE: case WRITE: case LIST: return true; }
			}
		}
		return false;
	}

	public class Session {
		
		ContrailTask.CompletionListener _completionListener= new ContrailTask.CompletionListener() {
			public void done(ContrailTask task) {
				removeTask(task);
			}
		};
		
		List<ContrailTask> _sessionTasks= Collections.synchronizedList(new ArrayList<ContrailTask>());
		
		public void close() {
			awaitCompletion();
			_sessionTasks= null;
		}
		
		@Override
		protected void finalize() throws Throwable {
			if (_sessionTasks != null) {
				try { awaitCompletion(); } catch (Throwable t) { }
			}
			super.finalize();
		}
		
		synchronized public Session submit(ContrailTask<?> task) {
			List<ContrailTask<?>> pendingTasks= null;

			pendingTasks= findPendingTasks(task);
			addTask(task);
			task.submit(pendingTasks);
			
			
			return this;
		}
		
		public <X extends Throwable, T extends ContrailTask<?>> Session invokeAll(Iterable<T> tasks, Class<X> errorType) 
		throws X 
		{
			for (ContrailTask task : tasks) 
				submit(task);
			awaitCompletion(tasks, errorType);
			return this;
		}
		public <T extends ContrailTask<?>> Session invokeAll(Iterable<T> tasks) 
		{
			for (ContrailTask task : tasks) 
				submit(task);
			awaitCompletion(tasks);
			return this;
		}
		
		public <T extends ContrailTask<?>> Session invoke(T task) 
		{
			invokeAll(ConversionUtils.asList(task));
			return this;
		}
		
		private void addTask(ContrailTask<?> task) {
			Identifier taskId= task.getId();
			synchronized (_tasks) {
				Set<ContrailTask> tasks= _tasks.fetch(taskId);
				if (tasks == null) {
					tasks= new HashSet<ContrailTask>();
					_tasks.store(taskId, tasks);
				}
				tasks.add(task);
			}
			_sessionTasks.add(task);
			task.addCompletionListener(_completionListener);
		}
		
		private void removeTask(ContrailTask<?> task) {
			// remove the task from internal lists
			Identifier ti= task.getId();
			synchronized (_tasks) {
				Set<ContrailTask> list= _tasks.fetch(ti);
				if (list != null) {
					list.remove(task);
					if (list.isEmpty())
						_tasks.delete(ti);
				}
			}
			_sessionTasks.remove(task);
		}

		public CFuture<Void> awaitCompletion() {
			HashSet<ContrailTask> done= new HashSet<ContrailTask>();
			while (!_sessionTasks.isEmpty()) {
				ArrayList<ContrailTask> tasks= new ArrayList<ContrailTask>(_sessionTasks);
				ArrayList<ContrailTask> todo= new ArrayList<ContrailTask>();
				for (ContrailTask t: tasks) {
					if (!done.contains(t)) {
						todo.add(t);
						done.add(t);
					}
				}
				if (todo.isEmpty())
					break;
				
				Throwable t= getThrowable(new ArrayList(todo));
				if (t != null) 
					TaskUtils.throwSomething(t);
			}
		}

		public <X extends Throwable> Session awaitCompletion(Class<X> errorType) throws X {
			HashSet<ContrailTask> done= new HashSet<ContrailTask>();
			while (!_sessionTasks.isEmpty()) {
				ArrayList<ContrailTask> tasks= new ArrayList<ContrailTask>(_sessionTasks);
				ArrayList<ContrailTask> todo= new ArrayList<ContrailTask>();
				for (ContrailTask t: tasks) {
					if (!done.contains(t)) {
						todo.add(t);
						done.add(t);
					}
				}
				if (todo.isEmpty())
					break;
				Throwable t= getThrowable(new ArrayList(todo));
				if (t != null) 
					TaskUtils.throwSomething(t, errorType);
			}
			return this;
		}
		
		public <T extends ContrailTask<?>> Session awaitCompletion(Iterable<T> tasks) {
			try {
				Throwable t= getThrowable(tasks);
				if (t != null) 
					TaskUtils.throwSomething(t);
			}
			finally {
				for (T t:tasks)
					_sessionTasks.remove(t);
			}
			return this;
		}
		public <X extends Throwable, T extends ContrailTask<?>> Session awaitCompletion(Iterable<T> tasks, Class<X> errorType) throws X {
			Throwable t= getThrowable(tasks);
			if (t != null) 
				TaskUtils.throwSomething(t, errorType);
			return this;
		}

		public boolean contains(ContrailAction action) {
			return _sessionTasks.contains(action);
		}

		private <T extends ContrailTask<?>> Throwable getThrowable(Iterable<T> tasks) {
			Throwable t= null;
			for (ContrailTask task : tasks) {
				
				if (task.getOperation() == Operation.CREATE && !task.isDone())
					continue;
				
				Throwable t2= task.getThrowable();
				if (t == null) 
					t= t2;
			}
			return t;
		}
	}
	
}
