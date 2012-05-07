package com.googlecode.contraildb.core.utils;

import java.util.ArrayList;
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
	private ArrayList<IResultHandler> _handlers= null;

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
		if (_handlers != null) {
			for (final IResultHandler handler:_handlers) {
				new ContrailAction() {
					@Override protected void action() throws Exception {
						handler.complete(Result.this);
					}
				}.submit();
			}
			_handlers= null;
		}
		notify();
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
	synchronized public void onComplete(IResultHandler<V> handler) {
		if (_done) {
			handler.complete(this);
			return;
		}
		if (_handlers == null)
			_handlers= new ArrayList<IResultHandler>();
		_handlers.add(handler);
	}

	@Override
	public boolean isCancelled() {
		return _cancelled;
	}
}
