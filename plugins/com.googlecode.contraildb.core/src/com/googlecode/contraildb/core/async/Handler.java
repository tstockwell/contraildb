package com.googlecode.contraildb.core.async;


import java.util.ArrayList;
import java.util.Collection;

import kilim.Pausable;

import com.googlecode.contraildb.core.utils.Logging;

/**
 * Handles one and only one result.
 * Subclasses should override the onComplete() method.
 * @author ted.stockwell
 */
@SuppressWarnings({"rawtypes","unchecked"})
public class Handler<I,O> implements IResultHandler<I>, IResult<O> {

	IResult<I> _incoming;
	Result<O> _outgoing= new Result() {
		public synchronized Object getResult() {
			checkForHandler();
			return super.getResult();
		};
		public synchronized Object get() throws Pausable {
			checkForHandler();
			return super.get();
		};
		public synchronized void join() throws Pausable {
			checkForHandler();
			super.join();
		};
		public synchronized Throwable getError() {
			checkForHandler();
			return super.getError();
		}
		public synchronized boolean isSuccess() {
			checkForHandler();
			return super.isSuccess();
		}
	};
	ArrayList<IResult> _pending= new ArrayList<IResult>(); 
	
	public Handler(IResult<I> task) {
		_incoming= task;
		task.addHandler(this);
	}
	public Handler() {
		// do nothing
		// incoming task needs to be set with handleResult method
	}
//	public Handler(IResult... tasks) {
//		this((IResult<I>)TaskUtils.combineResults(tasks));
//	}
	public Handler(Collection<IResult> tasks) {
		this((IResult<I>)TaskUtils.combineResults(tasks));
	}
	public Handler(IResult result, IResult... moreResults) {
		this((IResult<I>)TaskUtils.combineResults(result, moreResults));
	}
	public Handler(I value) {
		this(TaskUtils.asResult(value));
	}
	
	final static public <X, Y extends X> IResult<X> asResult( final Y bs) {
		return TaskUtils.asResult(bs);
	}
	public static final <T extends IResult<?>> IResult<Void> combineResults(Collection<T> tasks) {
		return TaskUtils.combineResults(tasks);
	}
	
	/**
	 * Set result to handle.
	 */
	public void handleResult(IResult result) {
		if (_incoming != null)
			throw new IllegalStateException("This handler is already associated with a result");
		_incoming= result;
		result.addHandler(this);
	}

	@Override
	final public void onComplete(IResult<I> result) throws Exception {
		_onComplete(result);
	}
	protected void _onComplete(IResult<I> result) {
		try {
			onComplete();
			
			if (result.isSuccess()) {
				try {
					final IResult<O> retval= onSuccess();
					IResult pending= TaskUtils.combineResults( retval, TaskUtils.combineResults(_pending));
					final IResult lastly= lastly();
					if (lastly != TaskUtils.DONE) // an optimization
						pending= new Handler(pending) {
							protected IResult onSuccess() throws Exception {
								return lastly;
							}
						};
					pending.addHandler(new IResultHandler() {
						public void onComplete(IResult result) {
							if (result.isSuccess()) {
								_outgoing.success(retval.getResult());
							}
							else if (result.isCancelled()) {
								_outgoing.cancel();
							}
							else
								_outgoing.error(result.getError());
						}
					});
				}
				catch (Throwable t) {
					_outgoing.error(t);
				}
			}
			else if (result.isCancelled()) {
				_outgoing.cancel();
				try {
					onCancelled();
				}
				catch (Throwable t) {
					Logging.warning("Error while handling cancel", t);
				}
			}
			else {
				_outgoing.error(result.getError());
				try {
					onError();
				}
				catch (Throwable t) {
					Logging.warning("Error while handling error", t);
				}
			}
		}
		catch (Throwable t) {
			_outgoing.error(t);
		}
	}
	
	protected void onComplete() throws Exception { }
	protected IResult<O> onSuccess() throws Exception { return TaskUtils.NULL; }
	protected void onError() { }
	protected void onCancelled() { }
	
	/**
	 * The doFinally method is invoked after everything else is completed,
	 * including the result returned from the onSuccess method.
	 * The doFinally method only invoked if all tasks executed before it are successful 
	 */
	protected IResult lastly() throws Exception { return TaskUtils.DONE; }
	
	protected void error(Throwable t) {
		_outgoing.error(t);
	}

	/**
	 * Run a subtask that is a logical part of this handler.
	 * Monitor the given result and if it completes with an error then 
	 * complete this handler's result with an error.  
	 * Same for cancellation.
	 */
	protected void spawn(IResult task) {
		_pending.add(task);
	}
	
	protected Result<O> outgoing() {
		return _outgoing;
	}
	protected IResult<I> incoming() {
		return _incoming;
	}
	
	public Result<O> toResult() {
		return _outgoing;
	}
	
	/**
	 * If this handler was not created with an incoming result 
	 * and it has been exposed to dependencies then we assume that 
	 * this handler should now be run by adding a default incoming result. 
	 * 
	 * This check was added as a convenience in order to avoid always having to 
	 * explicitly calling some kind of 'run' method on top-level handlers.
	 * It makes most code a little easier to read/write.
	 * Unfortunately it makes the process of starting top-level handlers 
	 * somewhat cryptic.
	 * 
	 */
	protected void checkForHandler() {
		if (_incoming == null && !_outgoing.isDone() && !_outgoing.hasHandlers())
			handleResult(TaskUtils.DONE);
	}
	
	/////////////////////
	//
	// IResult methods
	//
    public boolean isDone() {
    	return _outgoing.isDone();
    }
    public boolean isSuccess() {
    	return _outgoing.isSuccess();
    }
    public boolean isCancelled() {
    	return _outgoing.isCancelled();
    }
    public Throwable getError() {
    	return _outgoing.getError();
    }
    public O getResult() {
    	return _outgoing.getResult();
    }
    public O get() throws Pausable {
    	return _outgoing.get();
    }
    public void join() throws Pausable {
    	_outgoing.join();
    }
	public void addHandler(IResultHandler<O> handler) {
		_outgoing.addHandler(handler);
	}
}
