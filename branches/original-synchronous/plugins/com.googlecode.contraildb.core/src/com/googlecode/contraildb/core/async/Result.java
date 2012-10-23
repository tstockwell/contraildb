package com.googlecode.contraildb.core.async;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

import kilim.Mailbox;
import kilim.Pausable;

import com.googlecode.contraildb.core.utils.Logging;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Result<V> implements IResult<V>{
	
	private boolean _done= false;
	private V _result= null;
	private boolean _success= false;
	private boolean _cancelled= false;
	private Throwable _error= null;
	private List<IResultHandler> _completedHandlers= null;
	private Mailbox<Boolean> _completeBox= new Mailbox<Boolean>();
	
	public Result() { 
	}
	
	@Override public synchronized boolean isDone() {
		return _done;
	}
	
	
	public synchronized void cancel() {
		if (!_done) {
			complete(false, true, null, null);
		}
	}
	public synchronized void success(V result) {
		if (!_done) {
			complete(true, false, result, null);
		}
	}
	public synchronized void error(Throwable t) {
		if (!_done) {
			complete(false, false, null, t);
		}
	}

	@Override public V get() throws Pausable {
		
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

	@Override public void join() throws Pausable {
		synchronized (this) {
			if (!_done) {
				_completeBox.get(); // will not return until this result is completed
				if (!_done)
					throw new InternalError("Received complete notification but result is not available");
			}
		}
	}

	synchronized public void complete(final IResult<V> result) {
		result.addHandler(new Handler() {
			public void onComplete() throws Exception {
				complete(result.isSuccess(), result.isCancelled(), result.getResult(), result.getError());
			}
		});
	}
	synchronized public void complete(boolean success, boolean cancelled, V result, Throwable error) {
		if (_done)
			return;
		_done= true;
		_success= success;
		_cancelled= cancelled;
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
		
		_completeBox.putnb(Boolean.TRUE); // send notification that this result is completed
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
//		if (_success)
//			throw new IllegalStateException("This method cannot be called if the associated task successfully completed");
		return _error;
	}

	@Override
	synchronized public V getResult() {
		if (!_done)
			throw new IllegalStateException("This method cannot be called before the associated task has completed");
//		if (!_success)
//			throw new IllegalStateException("This method cannot be called if the associated task has not successfully completed");
		return _result;
	}

	@Override
	synchronized public void addHandler(IResultHandler<V> handler) {
		if (_done) {
			try {
				handler.onComplete(this);
			}
			catch (Throwable t) {
				Logging.warning("Error in result handler", t);
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