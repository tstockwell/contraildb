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
	
	// the input to the first handler in series
	Result _result= new Result();

	public Series() {
		
	}
	public Series(IResult result) {
		
	}
//	public Series(Handler<?,?>... handlers) {
//		_handlers= handlers;
//		
//		// connect all handlers but the first one in series
//		for (int i= 1; i < _handlers.length; i++) {
//			_handlers[i].handleResult(_handlers[i-1]);
//		}
//		
//		// connect first handler to internal result which will completed
//		// when this handler is completed, thus firing off the first handler.
//		if (0 < _handlers.length) {
//			_handlers[0].handleResult(_result);
//		}
//	}
	
	@Override
	protected IResult onSuccess() throws Exception {
		if (0 < _handlers.length) {
			// fire the first handler in series
			_result.success(null);
			
			// return result from last handler
			return _handlers[_handlers.length-1];
		}
		
		// no handlers in series, we're done.
		_outgoing.success(null);
		return TaskUtils.DONE;
	}
	
	protected void run(IResult handler) {
		
	}
}
