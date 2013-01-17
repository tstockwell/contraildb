package com.googlecode.contraildb.core.async;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.utils.Logging;


/**
 * A base task class for Contrail related tasks.
 * 
 * All concurrency in the Contrail database is implemented via the Task facility.
 * In order for the ContrailTask scheduler to appropriately schedule the execution of tasks 
 * it is required that ContrailTasks expose the relationships between the tasks, essentially 
 * denoting the parallelism in the application.  This approach is similar to that of the 
 * <a href="http://en.wikipedia.org/wiki/Cilk">Cilk language</a> except that ContrailTasks 
 * denote more information about parallel relationships (an identifier and an operation) 
 * and can therefore achieve greater parallelism. 
 * Contrail's approach to controlling concurrency is based on the concept of 
 * <a href="http://en.wikipedia.org/wiki/Serializability">Serializability</a>.
 * An set of operations executed concurrently is serializable if the result of executing 
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
 * 		Operation.FLUSH
 *
 * All tasks are associated with a 'domain' that controls the execution of tasks in that domain.
 * To execute a task a ContrailTask must be submitted to a TaskDomain for execution.
 * The ContrailTask.submit may be used to execute a ContrailTask without specifying a TaskDomain.
 * In this case the task will be associated with the domain associated with the current thread.
 * If the current thread is not executing a ContrailTask (thus there is no associated domain) then 
 * an error will occur.
 * 
 * This class uses a fixed pool of threads to run tasks.
 * 
 * Subclasses need to implement the run method.
 *
 * @param <T> The result type returned by the <tt>getResult</tt> method
 * 
 * @author Ted Stockwell
 * @see SerializableScheduler
 */
abstract public class ContrailTask<T> {
	
	private static Logger __logger= Logging.getLogger();
	
	/**
	 * Kilim task type used to execute ContrailTasks.
	 * Kilim Tasks that has this type can be recognized as being associated 
	 * with a ContrailTask.  
	 */
	private static class InternalTask extends ContrailTask {
		ContrailTask _contrailTask;
		public InternalTask(ContrailTask contrailTask) {
			_contrailTask= contrailTask;
		}
	}
	
	
	/**
	 * Returns true if the current thread is running a ContrailTask.  
	 */
	public static final boolean isContrailTask() {
		Thread currentThread= Thread.currentThread();
		if (!(currentThread instanceof WorkerThread))
			return false;
		WorkerThread workerThread= (WorkerThread)currentThread;
		ContrailTask currentTask= workerThread.getCurrentTask();
		if (currentTask instanceof InternalTask)
			return true;
		return false;
	}
	/**
	 * Returns the current ContrailTask, if any.  
	 */
	public static final <T> ContrailTask<T> getContrailTask() {
		Thread currentThread= Thread.currentThread();
		if (!(currentThread instanceof WorkerThread))
			return null;
		WorkerThread workerThread= (WorkerThread)currentThread;
		ContrailTask currentTask= workerThread.getCurrentTask();
		if (currentTask instanceof InternalTask)
			return ((InternalTask)currentTask)._contrailTask;
		return null;
	}
	
	/**
	 * Returns true if the current thread is running a ContrailTask and that 
	 * task has been canceled.  
	 */
	public static final boolean isTaskCanceled() {
		ContrailTask contrailTask= getContrailTask();
		if (contrailTask == null) 
			return false;

		IResult result= contrailTask.getResult();
		if (result.isDone())
			return result.isCancelled();
		
		return false;
	}
	
	Identifier _id;
	Operation _operation;
	String _name;
	private volatile boolean _done= false;
	private volatile boolean _submitted= false;
	private volatile boolean _running= false;
	private final Result<T> _result= new Result<T>();
	private TaskTracker _tracker= null;
	
	public ContrailTask(Identifier id, Operation operation, String name) {
		if ((_id= id) == null)
			_id= Identifier.create();
		
		if ((_operation= operation) == null)
			_operation= Operation.READ;
		_name= name;
	}
	public ContrailTask(Identifier id, Operation operation) {
		this(id, operation, null);
	}
	
	public ContrailTask() {
		this(Identifier.create(), Operation.READ, null);
	}
	
	/**
	 * Adds the given result to an internal list of list 'child' tasks.
	 * Contrail will insure that all child tasks complete before this task 
	 * is allowed to complete. 
	 */
	protected void subtask(IResult result) {
		if (_tracker == null)
			_tracker= new TaskTracker();
		_tracker.track(result);
	}
	
	protected void awaitAll() throws Pausable {
		_tracker.await();
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
			if (!_done) {
				if (cancelled) {
					try { stop(); } catch (Throwable t) { Logging.warning("Error while trying to stop a task", t); } 
				}
				_done= true;
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
			final Throwable[] err= new Throwable[] { null };
			final Mailbox<Boolean> outBox= new Mailbox<Boolean>();
			new InternalTask(this) {
				@Override
				public void execute() throws Pausable, Exception {
					try {
						result[0]= ContrailTask.this.run();
						if (_tracker != null)
							_tracker.await();
					}
					catch (Throwable x) {
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
			_submitted= true;
			new ContrailTask() {
				public void execute() throws Pausable, Exception {
					runTask();
				}
			}.start();
		}
		return getResult();
	}
	synchronized public boolean isSubmitted() {
		return _submitted;
	}
	
	
	synchronized public boolean isDone() {
		return _done;
	}
	public static void sleep(long waitMillis_) throws Pausable {
		ContrailTask.sleep(waitMillis_);
	}
}
