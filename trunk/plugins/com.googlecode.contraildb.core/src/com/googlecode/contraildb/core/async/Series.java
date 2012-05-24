package com.googlecode.contraildb.core.async;

import java.util.HashMap;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Item;
import com.googlecode.contraildb.core.storage.ILifecycle;



/**
 * This class uses reflection to look at its methods, which create 
 * Handlers and return Results, and execute the handlers sequentially.
 * 
 * The order of the execution is determined by inspecting method names.
 * For instance, a method named start will be become the first method to 
 * execute.  Method names should end with the word 'Then' and then enough
 * of the name of the next method to execute that the next method can be 
 * determined.
 * An example...
 * 
 *    			Handler hander= new Series(inputResult) {
 *    				Item item;
 *    
 *    				IResult fetchThenSave() {
 *    					return fetch(path);
 *    				}
 *    				void saveItemThenDoDel() {
 *    					item= (Item) incoming().getResult();
 *    				}
 *    				IResult doDelete() {
 *    					return new Parallel() {
 *    						IResult delete() {
 *    							_storageSession.delete(path);
 *    						}
 *    						IResult clearCache() {
 *    							_cache.delete(path);
 *    						}
 *    					};
 *    				}
 *    			};
 *    
 * The above handler will run the handlers returned from fetchThenSave,
 * saveItemThenDoDel, and doDelete in that order.
 * Notice that each method ends with the prefix of the name of the next 
 * method to run.
 * 
 * A method may return a Result and that result will become the input 
 * to the handler created for the next method.
 * A method may return void, in which case a Result<Void> will be the 
 * input the next handler.  
 *     
 * The result of a Series handler is the result returned from the last
 * executed handler.
 * 
 * If an error occurs in any of the handlers then the the Series handler
 * will return an error result.
 * 
 * @author ted.stockwell
 * 
 * @see Parallel
 *
 */
@SuppressWarnings({"rawtypes","unchecked"})
public class Series extends Handler {
	
	private static HashMap<Class, Handler[]> _handlersByClass= 
			new HashMap<Class, Handler[]>();
	
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
	
	private void createHandlers() {
		
	}
	
	
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
