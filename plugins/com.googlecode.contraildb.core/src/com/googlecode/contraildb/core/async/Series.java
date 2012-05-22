package com.googlecode.contraildb.core.async;

import com.googlecode.contraildb.core.IResult;


/**
 * Executes handlers sequentially.
 * The result returned from the Handler.onSuccess method will be the result 
 * returned from the last handler.
 * 
 * If an error occurs in any of the handlers the onError method is invoked.
 * The onError method is meant to be overwritten by subclasses.
 * 
 * @author ted.stockwell
 *
 */
@SuppressWarnings({"rawtypes","unchecked"})
public class Series extends Handler {
	
	Handler[] _handlers;

	public Series(Handler<?,?>... handlers) {
		_handlers= handlers;
		for (int i= 1; i < _handlers.length; i++) {
			_handlers[i].handleResult(_handlers[i-1]);
		}
		if (0 < _handlers.length) {
			handleResult(_handlers[_handlers.length-1]);
		}
		else
			outgoing().complete(true, null, null);
	}
	
	@Override
	public IResult run() {
		if (0 < _handlers.length)
			_handlers[0].handleResult(TaskUtils.DONE);
		return outgoing();
	}
}
