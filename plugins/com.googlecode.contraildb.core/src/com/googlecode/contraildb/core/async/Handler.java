package com.googlecode.contraildb.core.async;

import java.util.ArrayList;
import java.util.Collection;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.IResultHandler;
import com.googlecode.contraildb.core.utils.Logging;

/**
 * Handles one and only one result.
 * @author ted.stockwell
 */
@SuppressWarnings({"rawtypes","unchecked"})
public class Handler<I,O> implements IResultHandler<I>, IResult<O> {

	IResult<I> _incoming;
	Result<O> _outgoing= new Result();
	ArrayList<IResult> _pending= new ArrayList<IResult>(); 
	
	public Handler(IResult<I> task) {
		_incoming= task;
		task.addHandler(this);
	}
	public Handler() {
		// do nothing
		// incoming task needs to be set with handleResult method
	}
	public Handler(IResult... tasks) {
		this((IResult<I>)TaskUtils.combineResults(tasks));
	}
	public Handler(Collection<IResult> tasks) {
		this((IResult<I>)TaskUtils.combineResults(tasks));
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
	
	public void handleResult(IResult result) {
		if (_incoming != null)
			throw new IllegalStateException("This handler is already associated with a result");
		_incoming= result;
		result.addHandler(this);
	}

	@Override
	final public void onComplete(IResult<I> result) throws Exception {
		_incoming= result;
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
					pending.addHandler(new Handler() {
						public void onComplete() {
							IResult result= incoming();
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
    public O get() {
    	return _outgoing.get();
    }
	public void addHandler(IResultHandler<O> handler) {
		_outgoing.addHandler(handler);
	}
}
