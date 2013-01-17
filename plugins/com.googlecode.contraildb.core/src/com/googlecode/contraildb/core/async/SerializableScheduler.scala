package com.googlecode.contraildb.core.async;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.utils.IdentifierIndexedStorage;
import com.sun.org.apache.xalan.internal.xsltc.compiler.sym;

object Operation extends Enumeration {
    type Operation = Value
    val READ, WRITE, DELETE, LIST, CREATE, FLUSH = Value
}
/**
 * A task scheduler for Contrail that coordinates tasks according 
 * to an associated Identifier and operation type (READ, WRITE, DELETE, LIST, 
 * or CREATE).
 * The purpose of this class is to manage the order in which asynchronous 
 * requests are executed so that operations are performed in a way that 
 * preserves sequential serialization (in other words, the operations appear to 
 * have all been executed sequentially, in the order that they arrived) while 
 * also allowing for as much parallelization as possible in order to maximize 
 * performance.  
 * 
 * Contrail arranges all objects into a nested hierarchy.  
 * An Identifier indicates an object's location in the hierarchy.
 * The task manager coordinates operations on objects according to their place in 
 * the hierarchy as well as the type of operation being performed.
 * 
 * Here are the rules...
 * 
 * A FLUSH operation may not proceed until all pending operations known to the 
 * scheduler have completed.
 * A FLUSH operation blocks all subsequent operations until the flush has completed.
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
 * the IStorageProvider.create method is to coordinate asynchronous CREATE 
 * requests, CREATE requests do not have to wait for other CREATE requests.   
 * 
 * @author Ted Stockwell
 */
class SerializableScheduler extends TaskScheduler {
	
	IdentifierIndexedStorage<Set<ContrailTask>> _tasks= 
		new IdentifierIndexedStorage<Set<ContrailTask>>();
	ArrayList<ContrailTask> _flushTasks= new ArrayList<ContrailTask>();
	LinkedList<ContrailTask> _tasksToBeRemoved= new LinkedList<ContrailTask>();
	ContrailAction _taskRemoval= null;
	
	public TaskDomain() {
	}
	
	public Session beginSession() {
		return new Session();
	}

	public boolean contains(ContrailAction action) {
		Set<ContrailTask> tasks= _tasks.fetch(action.getId());
		if (tasks == null)
			return false;
		return tasks.contains(action);
	}
	
	private List<ContrailTask> findPendingTasks(ContrailTask task) {

		Identifier taskId= task.getId();
		final Operation op= task.getOperation();
		final ArrayList<ContrailTask> pendingTasks= new ArrayList<ContrailTask>();
		pendingTasks.addAll(_flushTasks);
		
		if (op == Operation.FLUSH) {
			ArrayList<ContrailTask> alltasks= new ArrayList<ContrailTask>();
			for (Set<ContrailTask> tasks2:_tasks.values()) {
				alltasks.addAll(tasks2);
			}
			alltasks.addAll(_flushTasks);
			return alltasks;
		}

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
				return true; 
			}
			case FLUSH:  { 
				return true; 
			}
		}
		return false;
	}

	public class Session {
		
		/**
		 * This result handler is added to every task.
		 * When a task completes this handler removes the task from the domain.
		 */
		IResultHandler _completionListener= new Handler() {
			@Override public void onComplete() {
				IResult incoming= incoming();
				for (ContrailTask task:_sessionTasks) {
					if (task.getResult() == incoming) {
						removeTask(task);
						return;
					}
				}
				throw new RuntimeException("Internal Error: should have found result");
			}
		};
		
		List<ContrailTask> _sessionTasks= Collections.synchronizedList(new ArrayList<ContrailTask>());
		private boolean _closed=false;
		
		synchronized public IResult<Void> close() {
			if (_closed)
				return TaskUtils.asResult(null);
			_closed= true;

			ArrayList<ContrailTask> alltasks= new ArrayList<ContrailTask>();
			synchronized (_tasks) {
				for (Set<ContrailTask> tasks2:_tasks.values()) {
					alltasks.addAll(tasks2);
				}
				alltasks.addAll(_flushTasks);
			}
			return TaskUtils.combineTasks(alltasks);
		}
		
		@Override
		protected void finalize() throws Throwable {
			close();
			super.finalize();
		}
		
		synchronized public <T> IResult<T> submit(final ContrailTask<T> task) {
			if (_closed)
				throw new IllegalStateException("The session has already been closed");
			List<ContrailTask> pendingTasks= findPendingTasks(task);
			addTask(task);
			if (pendingTasks.isEmpty())
				return task.submit();

			// submit task for execution *after* pending tasks have completed
			final Result<T> result= new Result<T>();
			ArrayList<IResult> pendingResults= new ArrayList<IResult>(pendingTasks.size());
			for (ContrailTask t:pendingTasks) {
				pendingResults.add(t.getResult());
			}
			TaskUtils.combineResults(pendingResults).addHandler(new Handler() {
				protected void onComplete() throws Exception {
					result.complete(task.submit());
				}
			});
			return result;
		}
		
		
		private void addTask(ContrailTask<?> task) {
			Identifier taskId= task.getId();
			Operation operation= task.getOperation();
			synchronized (_tasks) {
				if (operation == Operation.FLUSH) {
					_flushTasks.add(task);
				}
				else {
					Set<ContrailTask> tasks= _tasks.fetch(taskId);
					if (tasks == null) {
						tasks= new HashSet<ContrailTask>();
						_tasks.store(taskId, tasks);
					}
					tasks.add(task);
				}
			}
			_sessionTasks.add(task);
			task.getResult().addHandler(_completionListener);
		}
		
		private void removeTask(ContrailTask<?> task) {
			// remove the task from internal lists
			Identifier ti= task.getId();
			Operation operation= task.getOperation();
			synchronized (_tasks) {
				if (operation == Operation.FLUSH) {
					_flushTasks.remove(task);
				}
				else {
					Set<ContrailTask> list= _tasks.fetch(ti);
					if (list != null) {
						list.remove(task);
						if (list.isEmpty())
							_tasks.delete(ti);
					}
				}
			}
			_sessionTasks.remove(task);
		}
		
		/**
		 * Returns a result that completes when all the current tasks are complete.
		 */
		public IResult<Void> complete() {
			List<ContrailTask> tasks;
			synchronized (this) {
				tasks= new ArrayList<ContrailTask>(_sessionTasks);
			}
			synchronized (_tasks) {
				tasks.addAll(_flushTasks);
			}
			ArrayList<IResult> results= new ArrayList<IResult>(tasks.size());
			for (ContrailTask task:tasks)
				results.add(task.getResult());
			return TaskUtils.combineResults(results);
		}

//		/**
//		 * @throws An unchecked exception if an error occurred while producing the result
//		 */
//		public void join() {
//			List<ContrailTask> tasks;
//			synchronized (this) {
//				tasks= new ArrayList<ContrailTask>(_sessionTasks);
//			}
//			for (ContrailTask task:tasks)
//				task.getResult().get();
//		}
	}
	
}
