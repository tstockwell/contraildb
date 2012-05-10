package com.googlecode.contraildb.core.utils;

import java.util.ArrayList;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.IResultHandler;

@SuppressWarnings({"rawtypes","unchecked"})
public class Handler<I,O> implements IResultHandler<I> {

	IResult _incoming;
	Result _outgoing= new Result();
	ArrayList<IResult> _pending= new ArrayList<IResult>(); 
	
	public Handler(IResult<I> task) {
		task.onComplete(this);
	}
	public Handler(IResult... tasks) {
		this((IResult<I>)TaskUtils.combineResults(tasks));
	}
	public Handler(I value) {
		this(TaskUtils.asResult(value));
	}

	@Override
	final public void onComplete(IResult<I> result) throws Exception {
		_incoming= result;
		try {
			onComplete();
		}
		catch (Throwable t) {
			Logging.warning("Error while handling complete", t);
		}
		if (result.isSuccess()) {
			try {
				final IResult<O> retval= onSuccess();
				IResult finalResult= TaskUtils.combineResults( retval, TaskUtils.combineResults(_pending));
				finalResult.onComplete(new Handler() {
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
	
	protected void onComplete() throws Exception { }
	protected IResult<O> onSuccess() throws Exception { return TaskUtils.NULL; }
	protected void onError() { }
	protected void onCancelled() { }
	
	protected void error(Throwable t) {
		_outgoing.error(t);
	}

	/**
	 * A subtask this is a logical part of this handler.
	 * Monitor the given result and if it completes with an error then 
	 * complete this handler's result with an error.  
	 * Same for cancellation.
	 */
	protected void spawnChild(IResult task) {
		_pending.add(task);
	}
	protected void spawnChild(Handler handler) {
		spawnChild(handler.toResult());
	}
	/**
	 * A subtask this is a logical part of this handler.
	 * Monitor the given result and if it completes with an error then 
	 * complete this handler's result with an error.  
	 * Same for cancellation.
	 */
	protected void spawnChild(IResultHandler task) {
		_pending.add(task.toResult());
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

}
