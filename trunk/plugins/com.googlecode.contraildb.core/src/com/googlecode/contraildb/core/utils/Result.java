package com.googlecode.contraildb.core.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.IResultHandler;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Result<V> implements IResult<V>{
	
	private boolean _done= false;
	private V _result= null;
	private boolean _success= false;
	private boolean _cancelled= false;
	private Throwable _error= null;
	private Map<IResultHandler, Result> _completedHandlers= null;
	private Map<IResultHandler, Result> _successHandlers= null;
	private List<IResultHandler> _errorHandlers= null;

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
		while (!_done) {
			try {
				wait();
			}
			catch (InterruptedException x) {
			}
		}
		
		if (!_success) {
			if (_cancelled) {
				throw new CancellationException();
			}
			else
				TaskUtils.throwSomething(_error);
		}
		
		return _result;
	}

	synchronized public void complete(IResult<V> result) {
		if (result.isDone()) 
			complete(result.isSuccess(), result.getResult(), result.getError());
	}
	synchronized public void complete(boolean success, V result, Throwable error) {
		if (_done)
			return;
		_done= true;
		_success= success;
		_error= error;
		_result= result;
		
		if (_completedHandlers != null) {
			for (final IResultHandler handler:_completedHandlers.keySet()) {
				new ContrailAction() {
					@Override protected void action() throws Exception {
						Result<Void> handlerResult= _completedHandlers.get(handler);
						try {
							handler.complete(Result.this);
							handlerResult.success(null);
						}
						catch (Throwable t) {
							handlerResult.error(t);
						}
					}
				}.submit();
			}
			_completedHandlers= null;
		}
		
		if (_successHandlers != null && _success) {
			for (final IResultHandler handler:_successHandlers.keySet()) {
				new ContrailAction() {
					@Override protected void action() throws Exception {
						Result<Void> handlerResult= _successHandlers.get(handler);
						try {
							handler.complete(Result.this);
							handlerResult.success(null);
						}
						catch (Throwable t) {
							handlerResult.error(t);
						}
					}
				}.submit();
			}
			_successHandlers= null;
		}
		
		if (_errorHandlers != null && !_success && !_cancelled) {
			for (final IResultHandler handler:_errorHandlers) {
				new ContrailAction() {
					@Override protected void action() throws Exception {
						try {
							handler.complete(Result.this);
						}
						catch (Throwable t) {
							Logging.warning("Error in error handler", t);
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
	synchronized public IResult<Void> onComplete(IResultHandler<V> handler) {
		Result<Void> result= new Result<Void>();
		if (_done) {
			try {
				handler.complete(this);
				result.success(null);
			}
			catch (Throwable t) {
				result.error(t);
			}
			return result;
		}
		if (_completedHandlers == null)
			_completedHandlers= new HashMap<IResultHandler, Result>();
		_completedHandlers.put(handler, result);
		return result;
	}

	@Override
	public boolean isCancelled() {
		return _cancelled;
	}


	@Override
	public IResult<Void> onSuccess(IResultHandler<V> handler) {
		Result<Void> result= new Result<Void>();
		if (_done) {
			try {
				if (_success)
					handler.complete(this);
				result.success(null);
			}
			catch (Throwable t) {
				result.error(t);
			}
			return result;
		}
		if (_successHandlers == null)
			_successHandlers= new HashMap<IResultHandler, Result>();
		_successHandlers.put(handler, result);
		return result;
	}


	@Override
	public void onError(IResultHandler<V> handler) {
		if (_done) {
			if (!_success && !_cancelled) {
				try {
					handler.complete(this);
				}
				catch (Throwable t) {
					Logging.warning("Error in error handler", t);
				}
			}
			return;
		}
		if (_errorHandlers == null)
			_errorHandlers= new ArrayList<IResultHandler>();
		_errorHandlers.add(handler);
	}
}
