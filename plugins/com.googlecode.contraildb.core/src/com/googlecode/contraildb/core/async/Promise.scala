package com.googlecode.contraildb.core.async;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

import com.googlecode.contraildb.core.utils.Logging;

/**
 * Implementation of {@link Result}
 * 
 * @author ted.stockwell
 */
class Promise[V] extends Result[V] {
	val _tasks= new TaskMaster();
	
	/**
	 * This method is called when the associated computation has been cancelled.
	 */
	def cancel() {
	  _tasks {
		if (!done()) {
			complete(false, true, null, null);
		}
	  }
	}
	/**
	 * This method is called when the associated computation has successfully completed.
	 */
	public synchronized void success(V result) {
		if (!_done) {
			complete(true, false, result, null);
		}
	}
	/**
	 * This method is called when the associated computation has encountered an error.
	 */
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
	
	@Override public V getb() {
		
		joinb();
		
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
			if (_done) 
				return;
		}
		
		// will not return until this result is completed
		_completeBox.get(); 
		
		// since we cannot synchronize access to _completeBox, it is possible 
		// that some other thread might also be waiting on _completeBox, so we 
		// hafta prime the pump for them 
		_completeBox.putb(true);
		
		synchronized (this) {
			if (!_done)
				throw new InternalError("Received complete notification but result is not available");
		}
	}
	
	@Override public void joinb() {
		synchronized (this) {
			if (_done) 
				return;
		}
		
		// will not return until this result is completed
		_completeBox.getb(); 
		
		// since we cannot synchronise access to _completeBox, it is possible 
		// that some other thread might also be waiting on _completeBox, so we 
		// hafta prime the pump for them 
		_completeBox.putb(true);
		
		synchronized (this) {
			if (!_done)
				throw new InternalError("Received complete notification but result is not available");
		}
	}

	synchronized public void complete(final IResult<V> result) {
		result.addHandler(new Handler() {
			public void onComplete() throws Exception {
				complete(result.isSuccess(), result.isCancelled(), result.getResult(), result.getError());
			}
		});
	}

	/**
	 * This method is called when the associated computation has completed.
	 */
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
					@Override protected void action() throws Pausable, Exception {
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
		
		_completeBox.putb(Boolean.TRUE); // send notification that this result is completed
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
		return _error;
	}

	@Override
	synchronized public V getResult() {
		if (!_done)
			throw new IllegalStateException("This method cannot be called before the associated task has completed");
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