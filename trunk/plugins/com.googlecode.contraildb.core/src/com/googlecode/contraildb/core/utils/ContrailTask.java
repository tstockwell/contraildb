package com.googlecode.contraildb.core.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.IResultHandler;
import com.googlecode.contraildb.core.Identifier;


/**
 * A base task class for Contrail related tasks.
 * In Contrail a task is considered to be some operation on a resource.
 * The operations are considered to be one of:
 * 	 	Operation.READ,
 * 		Operation.WRITE,
 * 		Operation.DELETE,
 * 		Operation.LIST, or
 * 		Operation.CREATE
 * 
 * Subclasses need to implement the run method.
 * 
 * This class uses a fixed pool of threads to run tasks.
 *
 * @param <T> The result type returned by the <tt>getResult</tt> method
 * 
 * @author Ted Stockwell
 * @see ContrailTaskTracker
 */
@SuppressWarnings({"unchecked", "rawtypes"})
abstract public class ContrailTask<T> {



	/**
	 * Tasks waiting to be executed
	 */
	private static HashedLinkedList<ContrailTask> __tasks= new ConcurrentHashedLinkedList<ContrailTask>();
	private static Object __done= new Object(); // used to wait/notify when tasks are completed
	private static Object __arrive= new Object(); // used to wait/notify when new tasks arrive
	private static ArrayList<ContrailTask> __deferred= new ArrayList<ContrailTask>();
	private static ArrayList<Thread> __yielded= new ArrayList<Thread>();
	private static int __THREAD_COUNT= 0;
	
	private static Logger __logger= Logging.getLogger();
	
	
	static class ContrailThread extends Thread {
		ContrailTask _currentTask= null;
		
		public ContrailThread() {
			super("Contrail Thread "+(++__THREAD_COUNT));
			setDaemon(true);
		}
		
		public void run() {
			ContrailTask task= null;
			while (true) {
				synchronized (__arrive) {
					try {
						if ((task= __tasks.removeFirst()) == null)
							__arrive.wait();
					}
					catch (InterruptedException x) {
					}
				}
				if (task != null) { 
					_currentTask= task;
					task.runTask();
					_currentTask= null;
				}
			}
		}
	}
	
	static {
		for (int count= Runtime.getRuntime().availableProcessors() * 10; 0 < count--;) { 
			new ContrailThread().start();
		}
	}
	
	
	
	
	
	public static enum Operation {
		READ,
		WRITE,
		DELETE,
		LIST,
		CREATE
	}
	
	
	/**
	 * Returns true if the current thread is running a ContrailTask and that 
	 * task has been canceled.  
	 */
	public static final boolean isTaskCanceled() {
		Thread thread= Thread.currentThread();
		if (thread instanceof ContrailThread) {
			ContrailTask t= ((ContrailThread)thread)._currentTask;
			if (t != null) {
				IResult result= t.getResult();
				if (result.isDone())
					return result.isCancelled();
			}
		}
		return false;
	}
	/**
	 * Returns true if the current thread is running a ContrailTask that has yielded.  
	 */
	public static final boolean isTaskYielded() {
		Thread thread= Thread.currentThread();
		synchronized (__yielded) {
			if (__yielded.contains(thread))
				return true;
		}
		return false;
	}
	
	
	
	Identifier _id;
	Operation _operation;
	private volatile boolean _done= false;
	private volatile boolean _submitted= false;
	private volatile List<ContrailTask<?>> _pendingTasks;
	private final Result<T> _result= new Result<T>(); 
	
	
	public ContrailTask(Identifier id, Operation operation) {
		if ((_id= id) == null)
			_id= Identifier.create();
		
		if ((_operation= operation) == null)
			_operation= Operation.READ;
	}
	
	public ContrailTask() {
		this(Identifier.create(), Operation.READ);
	}
	
	public void cancel() {
		_result.cancel();
		done(true);
	}
	
	/**
	 * This method is invoked when a task is cancelled.
	 * Subclasses should override this method and do whatever is 
	 * required in order to make the task stop its operation.  
	 */
	protected void stop() {
		// do nothing
	}
	
	
	
	@Override
	public String toString() {
		return "{_id="+_id+", _operation="+_operation+"}";
	}
	
	public Identifier getId() {
		return _id;
	}
	
	public Operation getOperation() {
		return _operation;
	}
	
	protected abstract T run() throws Exception;
	
	protected void error(Throwable throwable) {
		_result.error(throwable);
		done(false);
	}
	
	protected void success(T result) {
		_result.success(result);
		done(true);
	}
	protected void setResult(IResult<T> result) {
		result.onComplete(new IResultHandler<T>() {
			@Override
			public void complete(IResult<T> result) {
				success(result.getResult());
			}
		});
	}
	
	private void done(boolean cancelled) {
		if (!_done) {
			
			synchronized (__done) {
				if (!_done) {
					if (cancelled) {
						try { stop(); } catch (Throwable t) { Logging.warning("Error while trying to stop a task", t); } 
					}
					_done= true;
					
					// check for deferred tasks that can be run now
					for (int i= __deferred.size(); 0 < i--;) {
						ContrailTask deferredTask= __deferred.get(i);
						for (int p= deferredTask._pendingTasks.size(); 0 < p--;) { 
							ContrailTask pending= (ContrailTask) deferredTask._pendingTasks.get(p);
							if (pending._done) 
								deferredTask._pendingTasks.remove(p);
						}
						if (deferredTask._pendingTasks.isEmpty()) 
							if (__deferred.remove(deferredTask)) 
								deferredTask.submit();
					}
					
					__done.notifyAll();
				}
			}
		}
	}
	
	public IResult<T> getResult() {
		return _result;
	}
	
	private synchronized void runTask() {
if (__logger.isLoggable(Level.FINER))
	__logger.finer("run task "+hashCode()+", id "+_id+", op "+_operation+", thread "+Thread.currentThread().getName() );		
		if (!_done) { 
			try {
				T result= run();
				if (!_result.isCancelled())
					success(result); 
			}
			catch (Throwable x) {
				error(x);
			}
		}
	}
	
	synchronized public IResult<T> submit() {
		if (!_submitted) {
			__tasks.append(this);
			_submitted= true;
			synchronized (__arrive) {
				__arrive.notify();
			}
		}
		return _result;
	}
	synchronized public boolean isSubmitted() {
		return _submitted;
	}
	
	
	public boolean isDone() {
		return _done;
	}
	
	/**
	 * Submit this task for execution but don't run the task until the given tasks have completed
	 */
	public IResult<T> submit(List<ContrailTask<?>> dependentTasks) {
		if (dependentTasks != null)  {
			dependentTasks= new ArrayList<ContrailTask<?>>(dependentTasks); 
			synchronized (__done) {
				for (int i= dependentTasks.size(); 0 < i--;) {
					ContrailTask task= dependentTasks.get(i);
					if (task._done) 
						dependentTasks.remove(i);
				}
				if (!dependentTasks.isEmpty()) {
					_pendingTasks= dependentTasks;
					__deferred.add(this);
				}
				else
					dependentTasks= null;
			}
		}
		if (dependentTasks == null) 
			submit();
		return _result;
	}
	
	public T get() {
		return _result.get();
	}
	
	/**
	 * Run other tasks in this thread while waiting for other things to happen
	 * @return true if another task was run 
	 */
	protected boolean yield() {
		if (isTaskYielded())
			return false; // no nested yields for now
		
		if (yieldToDependent())
			return true;

		/*
		 * If this task has no dependencies then choose a random task to run.  
		 * DONT mess with a task that has any dependencies, choose something nice and simple.
		 */

		ContrailTask nextTask= null;
		for (Iterator<ContrailTask> i= __tasks.iterator(); i.hasNext();) {
			ContrailTask t= i.next();
			if (t._pendingTasks == null || t._pendingTasks.isEmpty()) {
				if (__tasks.remove(t)) {
					nextTask= t;
					break;
				}
			}
		}

		if (nextTask != null) {
			nextTask.runTask();
			return true;
		}
		
		return false;
	}
	
	/**
	 * Run other dependency tasks in this thread while waiting for other things to happen
	 * @return true if a task was run
	 */
	protected boolean yieldToDependent() {
		if (isTaskYielded())
			return false; // no nested yields for now
		
		ContrailTask nextTask= null;
		synchronized (__done) {

			if (_done)
				return false;

			/*
			 * If this task is not done then perform some other task while we wait.
			 * This task may be waiting on some other task to complete, if so 
			 * we will run one of those dependent tasks first.  
			 * 
			 * If the task we're waiting for is not done them we'll see if 
			 * we can get it off the list of waiting tasks and run it in 
			 * this thread.
			 * If the task we're waiting for is deferred then try to get 
			 * one of the tasks it is waiting for off the list and run it. 
			 * 
			 * If this task has no dependencies then choose a random task
			 * to run.  DONT choose a task that's dependent on this one.
			 */
			if (_submitted) 
				if (__tasks.remove(this)) { 
					nextTask= this; // this task has not been assigned to a thread, run it now.
				}

			// find a dependent task to run
			if (!_submitted && nextTask == null && _pendingTasks != null) {
				HashSet<Identifier> done= new HashSet<Identifier>();
				LinkedList<ContrailTask> todo= new LinkedList<ContrailTask>();
				todo.add(this);
				while (!_done && !todo.isEmpty() && nextTask == null) {
					ContrailTask t= todo.removeFirst();
					Identifier taskID= t.getId();
					if (done.contains(taskID))
						continue;
					done.add(taskID);
					if (!t._done) {
						if (t._submitted) {
							if (__tasks.remove(t)) { 
								nextTask= t;
							}
						}
						else if (t._pendingTasks != null) {
							List<ContrailTask> l= t._pendingTasks;
							for (int i= l.size(); 0 < i--;) {
								ContrailTask p= l.get(i);
								if (p._done) {
									t._pendingTasks.remove(i);
								}
								else
									todo.add(p);
							}
						}
					}
				}
			}
		}

		if (nextTask != null) {
			return yieldToTask(nextTask);
		}
		
		return false;
	}
	
	/**
	 * Yield to the given task
	 * @return true if the task was run
	 */
	protected boolean yieldToTask(ContrailTask task) {
		Thread thread= Thread.currentThread();
		synchronized (__yielded) {
			if (isTaskYielded())
				return false; // no nested yields for now
			
			__yielded.add(thread);
		}
		try {
			task.runTask();
			return true;
		}
		finally {
			synchronized (__yielded) {
				__yielded.remove(thread);
			}
		}
	}
}

