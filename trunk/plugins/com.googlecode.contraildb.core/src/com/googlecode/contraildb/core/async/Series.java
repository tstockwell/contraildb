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
public class Series extends Block {
	
	Handler[] _handlers;

	public Series(Handler<?,?>... handler) {
		_handlers= handler;
	}
	
	@Override
	final protected IResult onSuccess() throws Exception {
		if (_handlers.length <= 0)
			return TaskUtils.DONE;
		
		class RunHandler extends Handler {
			int _index;
			public RunHandler(int index) {
				super(_handlers[index]);
				_index= index;
				
			}
			protected IResult onSuccess() throws Exception {
				// this is the last handler
				if (_handlers.length-1 <= _index)
					return incoming();
				// run next handler
				return new RunHandler(_index+1);
			}
			protected void onError() {
				Series.super.onError();
			}
		}
		
		return new RunHandler(0);
	}

}
