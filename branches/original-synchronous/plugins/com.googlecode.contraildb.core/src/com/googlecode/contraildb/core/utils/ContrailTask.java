package com.googlecode.contraildb.core.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
 * @author Ted Stockwell
 *
 * @param <T> The result type returned by the <tt>getResult</tt> method
 */
@SuppressWarnings({"unchecked", "rawtypes"})
abstract public class ContrailTask<T> implements IResult<T> {
	
	/**
	 * Tasks waiting to be executed
	 */
	private static HashedLinkedList<ContrailTask> __tasks= new ConcurrentHashedLinkedList<ContrailTask>();
	private static Object __done= new Object(); // used to wait/notify when tasks are completed
	private static Object __arrive= new Object(); // used to wait/notify when new tasks arrive
	private static ArrayList<ContrailTask> __deferred= new ArrayList<ContrailTask>();
	private static int __THREAD_COUNT= 0;
	
	private static Logger __logger= Logging.getLogger();
	
	public static interface CompletionListener {
		void done(ContrailTask task);
	}
	
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
System.out.println("");
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
			if (t != null)
				return t.isCancelled();
		}
		return false;
	}
	
	
	
	Identifier _id;
	Operation _operation;
	private Throwable _throwable;
	private T _result;
	private volatile boolean _done= false;
	private volatile boolean _cancelled= false;
	private volatile boolean _submitted= false;
	private volatile List<ContrailTask<?>> _pendingTasks;
	private volatile List<CompletionListener> _completionListeners;
	
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
			done(true);
	}
	
	public boolean isCancelled() {
		return _cancelled;
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
	
	protected abstract void run() throws Exception;
	
	protected void setError(Throwable throwable) {
		_throwable= throwable;
		done(false);
	}
	
	protected void setResult(T result) {
		_result= result;
	}
	
	protected void done() {
		done(false);
	}
	
	private void done(boolean cancelled) {
		if (!_done) {
			
			synchronized (__done) {
				if (!_done) {
					if (cancelled) {
						_cancelled= true;
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
					
					// notify completion listeners 
					if (_completionListeners != null) {
						for (CompletionListener listener: _completionListeners) {
							try {
								listener.done(this);
							}
							catch (Throwable t) {
								Logging.warning(t);
							}
						}
					}
					
					__done.notifyAll();
				}
			}
		}
	}
	
	protected T getResult() {
		return _result;
	}
	
	Throwable getThrowable() {
		quietlyJoin();
		return _throwable;
	}
	
	
	private synchronized void runTask() {
if (__logger.isLoggable(Level.FINER))
	__logger.finer("run task "+hashCode()+", id "+_id+", op "+_operation+", thread "+Thread.currentThread().getName() );		
		if (!_done) { 
			try {
				run();
			}
			catch (Throwable x) {
				setError(x);
			}
			finally {

				done(false);
			}
		}
	}
	
	synchronized public ContrailTask<T> submit() {
		if (!_submitted) {
			__tasks.append(this);
			_submitted= true;
			synchronized (__arrive) {
				__arrive.notify();
			}
		}
		return this;
	}
	
	
	public <X extends Throwable> T get(Class<X> type) throws X {
		join(type);
		return _result;
	}
	public T get() {
		join();
		return _result;
	}
	public boolean isDone() {
		return _done;
	}
	
	public synchronized void addCompletionListener(CompletionListener listener) {
		if (_completionListeners == null) {
			_completionListeners= new ArrayList<CompletionListener>(1);
		}
		_completionListeners.add(listener);
	}
	
	
	/**
	 * Submit this task for execution but don't run the task until the given tasks have completed
	 */
	public ContrailTask<T> submit(List<ContrailTask<?>> dependentTasks) {
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
		return this;
	}
	
	public ContrailTask<T> join() {
		quietlyJoin();
		TaskUtils.throwSomething(_throwable);
		return this;
	}
	
	public final <X extends Throwable> T invoke(Class<X> errorType) throws X {
		submit();
		join(errorType);
		return _result;
	}
	
	public final <X extends Throwable> ContrailTask<T> join(Class<X> errorType) throws X {
		quietlyJoin();
		TaskUtils.throwSomething(_throwable, errorType);
		return this;
	}
	
	public boolean quietlyJoin() {
		return quietlyJoin(1000L*60*60/*one hour*/);
	}
	
	/**
	 * Return false if the join timed out, true if the task is complete
	 */
	public boolean quietlyJoin(long timeoutMillis) {
		final long start= System.currentTimeMillis();
		while (true) {
			ContrailTask nextTask= null;
			synchronized (__done) {

				if (_done)
					return true;
				
				/*
				 * If this task is not done then, in order to avoid deadlock, 
				 * we have to perform some other task while we wait.  
				 * If the task we're waiting for is not done them we'll see if 
				 * we can get it off the list of waiting tasks and run it in 
				 * this thread.
				 * If the task we're waiting for is deferred then try to get 
				 * one of the tasks it is waiting for off the list and run it. 
				 */
				if (_submitted) 
					if (__tasks.remove(this)) { 
						nextTask= this;
					}
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
			
			if (nextTask == null) {
				synchronized (__done) {
					if (_done)
						return true;
					try {__done.wait(); } catch (InterruptedException x) { }
				}
			}
			else { 
				nextTask.runTask();
			}
			
			
			long millisRemaining= timeoutMillis - (System.currentTimeMillis() - start);
			if (millisRemaining <= 0) 
				return false;
		}
	}
	
}

