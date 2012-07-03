package com.googlecode.contraildb.core.utils;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

import com.googlecode.contraildb.core.utils.ContrailAction;
import com.googlecode.contraildb.core.utils.ContrailTask;
import com.googlecode.contraildb.core.utils.Logging;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Result<V> implements IResult<V>{
	
	private boolean _done= false;
	private V _result= null;
	private boolean _success= false;
	private boolean _cancelled= false;
	private Throwable _error= null;
	private List<IResultHandler> _completedHandlers= null;

	@Override public synchronized boolean isDone() {
		return _done;
	}
	
	
	public synchronized void cancel() {
		if (!_done) {
			_cancelled= true;
			complete(false, null, null);
		}
	}
	public synchronized void success(V result) {
		if (!_done) {
			complete(true, result, null);
		}
	}
	public synchronized void error(Throwable t) {
		if (!_done) {
			complete(false, null, t);
		}
	}

	@Override synchronized public V get() {
		
		join();
		
		if (!_success) {
			if (_cancelled) {
				throw new CancellationException();
			}
			else
				TaskUtils.throwSomething(_error);
		}
		
		return _result;
	}

	@Override synchronized public void join() {
		/*
		 * The current thread is one of Contrail's internal threads. 
		 * The get method should never be used in an internal API.
		 */
		if (ContrailTask.isContrailTask())
			throw new RuntimeException("Contrail actions MUST be atomic!\nUse a Handler instead of calling get().");
		
		while (!_done) {
			try {
				wait();
			}
			catch (InterruptedException x) {
			}
		}
	}

	synchronized public void complete(final IResult<V> result) {
		result.addHandler(new Handler() {
			public void onComplete() throws Exception {
				complete(result.isSuccess(), result.getResult(), result.getError());
			}
		});
	}
	synchronized public void complete(boolean success, V result, Throwable error) {
		if (_done)
			return;
		_done= true;
		_success= success;
		_error= error;
		_result= result;
		
		if (_completedHandlers != null) {
			for (final IResultHandler handler:_completedHandlers) {
				new ContrailAction() {
					@Override protected void action() throws Exception {
						try {
							handler.onComplete(Result.this);
						}
						catch (Throwable t) {
							Logging.warning("Error in completion handler", t);
						}
					}
				}.submit();
			}
			_completedHandlers= null;
		}
		
		
		notify(); // notify the get() method that results are available
	}

	@Override
	synchronized public boolean isSuccess() {
		if (!_done)
			throw new IllegalStateException("This method cannot be called before the associated task has completed");
		return _success;
	}

	@Override
	synchronized public Throwable getError() {
		if (!_done)
			throw new IllegalStateException("This method cannot be called before the associated task has completed");
		if (_success)
			throw new IllegalStateException("This method cannot be called if the associated task successfully completed");
		return _error;
	}

	@Override
	synchronized public V getResult() {
		if (!_done)
			throw new IllegalStateException("This method cannot be called before the associated task has completed");
		if (!_success)
			throw new IllegalStateException("This method cannot be called if the associated task has not successfully completed");
		return _result;
	}

	@Override
	synchronized public void addHandler(IResultHandler<V> handler) {
		Result<Void> result= new Result<Void>();
		if (_done) {
			try {
				handler.onComplete(this);
				result.success(null);
			}
			catch (Throwable t) {
				result.error(t);
			}
			return;
		}
		if (_completedHandlers == null)
			_completedHandlers= new ArrayList<IResultHandler>();
		_completedHandlers.add(handler);
	}

	@Override
	public boolean isCancelled() {
		return _cancelled;
	}
	
	public boolean hasHandlers() {
		return _completedHandlers != null && !_completedHandlers.isEmpty();
	}

}
