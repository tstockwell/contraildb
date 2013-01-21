package com.googlecode.contraildb.core.async

object Task {
  	def toTask[V](taskMethod:  => V):Task[V]= {
	  return new Task[V]() {
		  def run():V= {
		    val v= taskMethod;
		    return v;
		  };
	  }
  	}
}

/**
 * A tasks.
 * 
 * @author Ted Stockwell
 * @see TaskScheduler
 */
trait Task[+V] extends Result[V] with Actor with Runnable {
	private var _name:String= _;
	
	def name()= _name;
	
	override def cancel() {
	  sync {
		_cancelled= true;
		done(true);
	  }
	}
	
	/**
	 * This method is invoked when a task is cancelled.
	 * Subclasses should override this method and do whatever is 
	 * required in order to make the task stop its operation.  
	 */
	protected def stop() {
		// do nothing
	}
	
	override def toString():String= {
		var txt= "{";
		if (_name != null)
			txt+= "_name="+_name+", ";
		txt+= "hashcode="+hashCode()+", ";
		txt+= "}";
		return txt;
	}
	
	/**
	 * The method that implements the task, subclassses must implement this...
	 */
	def run():V;
	
	/**
	 * 
	 */
	protected def error(throwable:Throwable) {
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

}