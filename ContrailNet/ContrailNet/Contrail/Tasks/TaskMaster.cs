using System;

namespace Contrail.Tasks
{
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
	public class TaskMaster
	{
		
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
			if (_sessionTasks == null)
				return TaskUtils.asResult(null);
			List<ContrailTask> tasks= new ArrayList<ContrailTask>(_sessionTasks);
			List<IResult> results= new ArrayList<IResult>(_sessionTasks.size());
			for (ContrailTask task:tasks)
				results.add(task.getResult());
			return TaskUtils.combineResults(results);
		}
		
		@Override
		protected void finalize() throws Throwable {
			close();
			super.finalize();
		}
		
		synchronized public <T> IResult<T> submit(final ContrailTask<T> task) {
			if (_closed)
				throw new IllegalStateException("The session has already been closed");
			List<ContrailTask<?>> pendingTasks= findPendingTasks(task);
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
			synchronized (_tasks) {
				Set<ContrailTask> tasks= _tasks.fetch(taskId);
				if (tasks == null) {
					tasks= new HashSet<ContrailTask>();
					_tasks.store(taskId, tasks);
				}
				tasks.add(task);
			}
			_sessionTasks.add(task);
			task.getResult().addHandler(_completionListener);
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
		
		/**
		 * Returns a result that completes when all the current tasks are complete.
		 */
		public IResult<Void> complete() {
			List<ContrailTask> tasks;
			synchronized (this) {
				tasks= new ArrayList<ContrailTask>(_sessionTasks);
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

