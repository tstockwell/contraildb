package com.googlecode.contraildb.core.async;

import java.util.logging.Level;
import java.util.logging.Logger;

import kilim.Mailbox;
import kilim.Pausable;
import kilim.Task;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.utils.ConcurrentHashedLinkedList;
import com.googlecode.contraildb.core.utils.HashedLinkedList;
import com.googlecode.contraildb.core.utils.Logging;


/**
 * A base task class for Contrail related tasks.
 * 
 * All concurrency in the Contrail database is implemented via the ContrailTask facility.
 * In order for the ContrailTask scheduler to appropriately schedule the execution of tasks 
 * it is required that ContrailTasks expose the relationships between the tasks, essentially 
 * denoting the parallelism in the application.  This approach is similar to that of the 
 * <a href="http://en.wikipedia.org/wiki/Cilk">Cilk language</a> except that ContrailTasks 
 * denote more information about parallel relationships (an identifier and an operation) 
 * and can therefore achieve greater parallelism. 
 * Contrails approach to controlling concurrency is based on the concept of 
 * <a href="http://en.wikipedia.org/wiki/Serializability">Serializability</a>.
 * An set of operation s executed concurrently is serializable if the result of executing 
 * those operations is the same as executing the operation sequentially.
 * Contrail allows as much concurrency as possible while preserving the serializability 
 * of operations. 
 *  
 * In Contrail a task is considered to be some operation on a resource.
 * A resources have a unique identifier.
 * The operations are considered to be one of:
 * 	 	Operation.READ,
 * 		Operation.WRITE,
 * 		Operation.DELETE,
 * 		Operation.LIST, or
 * 		Operation.CREATE
 * 
 * Subclasses need to implement the run method.
 *
 * All tasks are associated with a 'domain' that controls the execution of tasks in that domain.
 * To execute a task a ContrailTask must be submitted to a TaskDomain for execution.
 * The ContrailTask.submit may be used to execute a ContrailTask without specifying a TaskDomain.
 * In this case the task will be associated with the domain associated with the current thread.
 * If the current thread is not executing a ContrailTask (thus there is no associated domain) then 
 * an error will occur.
 * 
 * Contrail's concurrency facility employs a tactic known as 'work-stealing'.
 * That is, when a task needs to wait for some other task to complete the thread 
 * running the waiting task will execute some other tasks while waiting.  
 * Work-stealing ensures that the system will not become deadlocked because all 
 * available threads need to wait, thus making it impossible to execute any other 
 * tasks and make progress towards a computational goal.  
 * 
 * This class uses a fixed pool of threads to run tasks.
 *
 * @param <T> The result type returned by the <tt>getResult</tt> method
 * 
 * @author Ted Stockwell
 * @see TaskDomain
 */
@SuppressWarnings({"unchecked", "rawtypes"})
abstract public class ContrailTask<T> {
	
	public static final String CONTRAIL_THREAD_COUNT= "contrail.thread.count";



	/**
	 * Tasks waiting to be executed
	 */
	private static HashedLinkedList<ContrailTask> __tasks= new ConcurrentHashedLinkedList<ContrailTask>();
	
	private static Object __done= new Object(); // used to wait/notify when tasks are completed
	private static Object __arrive= new Object(); // used to wait/notify when new tasks arrive
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
					try { task.runTask(); } catch (Pausable p) { p.printStackTrace(); }
					_currentTask= null;
				}
			}
		}
	}
	
	static {
		// fire up threads for running tasks
		int count= Runtime.getRuntime().availableProcessors() * 2;  
		String value= System.getProperty(CONTRAIL_THREAD_COUNT);
		if (value != null) {
			try {
				int i= Integer.parseInt(value);
				if (0 < i) {
					count= i;
				}
				else {
					Logging.warning("Invalid value for "+CONTRAIL_THREAD_COUNT+":"+value);
				}
			}
			catch (Throwable t) {
				Logging.warning("Invalid value for "+CONTRAIL_THREAD_COUNT+":"+value, t);
			}
		}
		for (; 0 < count--;) { 
			new ContrailThread().start();
		}
	}
	
	
	
	
	
	/**
	 * Returns true if the current thread is running a ContrailTask.  
	 */
	public static final boolean isContrailTask() {
		return Thread.currentThread() instanceof ContrailThread;
	}
	/**
	 * Returns the current ContrailTask, if any.  
	 */
	public static final <T> ContrailTask<T> getContrailTask() {
		Thread thread= Thread.currentThread();
		if (thread instanceof ContrailThread) {
			return ((ContrailThread)thread)._currentTask;
		}
		return null;
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
	
	Identifier _id;
	Operation _operation;
	String _name;
	private volatile boolean _done= false;
	private volatile boolean _submitted= false;
	private volatile boolean _running= false;
	private final Result<T> _result= new Result<T>(); 
	
	public ContrailTask(Identifier id, Operation operation, String name) {
		if ((_id= id) == null)
			_id= Identifier.create();
		
		if ((_operation= operation) == null)
			_operation= Operation.READ;
		_name= name;
	}
	
	public ContrailTask() {
		this(Identifier.create(), Operation.READ, null);
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
		String txt= "{";
		if (_name != null)
			txt+= "_name="+_name+", ";
		txt+= "_id="+_id+", _operation="+_operation+"}";
		return txt;
	}
	
	public Identifier getId() {
		return _id;
	}
	
	public Operation getOperation() {
		return _operation;
	}
	
	protected abstract T run() throws Pausable, Exception;
	
	protected void error(Throwable throwable) {
		_result.error(throwable);
		done(false);
	}
	
	protected void success(T result) {
		_result.success(result);
		done(true);
	}
	protected void setResult(IResult<T> result) {
		result.addHandler(new Handler() {
			public void onComplete() {
				success((T)incoming().getResult());
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
					__done.notifyAll();
				}
			}
		}
	}
	
	public IResult<T> getResult() {
		return _result;
	}
	
	private boolean runTask() throws Pausable {
		synchronized (this) {
			if (_done) 
				return false;
			if (_running)
				return false;
			_running= true;
		}
if (__logger.isLoggable(Level.FINER))
	__logger.finer("run task "+hashCode()+", id "+_id+", op "+_operation+", thread "+Thread.currentThread().getName() );		
		try {
			final Object[] result= new Object[] { null };
			final Exception[] err= new Exception[] { null };
			final Mailbox<Boolean> outBox= new Mailbox<Boolean>();
			new Task() {
				@Override
				public void execute() throws Pausable, Exception {
					try {
						result[0]= ContrailTask.this.run();
					}
					catch (Exception x) {
						err[0]= x;
					}
					finally {
						outBox.put(Boolean.TRUE);
					}
				}
			}.start();
			
			// does not return until task above has completed
			outBox.get(); 
			//System.out.println("Task "+this.toString()+" is completed");
			
			if (err[0] != null) {
				error(err[0]);
			}
			else if (!_result.isCancelled())
				success((T)result[0]); 
		}
		catch (Throwable x) {
			error(x);
		}
		return true;
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
}
