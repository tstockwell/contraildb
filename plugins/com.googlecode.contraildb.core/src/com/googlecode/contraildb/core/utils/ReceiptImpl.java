package com.googlecode.contraildb.core.utils;

import java.util.ArrayList;


@SuppressWarnings({"unchecked", "rawtypes"})
public class ReceiptImpl<T> implements Receipt<T>{
	
	private T _value;
	private boolean _done= false;
	private boolean _cancelled= false;
	private boolean _success= false;
	private Throwable _error= null;
	private ArrayList<Completion<T,?>> _handlers= null;

	@Override
	synchronized public boolean isDone() {
		return _done;
	}
	
	synchronized public void success(T value) {
		_done= true;
		_value= value;
		_success= true;
		invokeHandlers();
	}
	synchronized public void failure(Throwable t) {
		_done= true;
		_success= false;
		invokeHandlers();
	}
	synchronized public void cancel() {
		_done= true;
		_success= false;
		_cancelled= true;
		invokeHandlers();
	}

	@Override
	synchronized public T getResult() {
		return _value;
	}

	@Override
	synchronized public void onComplete(Completion<T,?> handler) {
		if (_done) {
			handler.complete(this);
		}
		else {
			if (_handlers == null)
				_handlers= new ArrayList<Completion<T,?>>();
			_handlers.add(handler);
		}
	}
	private void invokeHandlers() {
		for (Completion<T,?> handler:_handlers) {
			handler.complete(this);
		}
		_handlers= null; // save some space
	}

	@Override
	public boolean isCancelled() {
		return _cancelled;
	}

	@Override
	public boolean isSuccess() {
		return _success;
	}

	@Override
	public Throwable getError() {
		return _error;
	}
	
	public void complete(final Receipt<T> receipt) {
		receipt.onComplete(new Completion() {
			@Override public Receipt complete(final Receipt r) {
				if (r.isSuccess()) {
					success(receipt.getResult());
				}
				else if (r.isCancelled()) {
					cancel();
				}
				else
					failure(receipt.getError());
				return null;
			}
		});
	}

}
