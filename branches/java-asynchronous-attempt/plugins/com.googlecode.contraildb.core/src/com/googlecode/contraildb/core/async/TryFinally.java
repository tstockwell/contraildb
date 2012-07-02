package com.googlecode.contraildb.core.async;

import java.lang.reflect.Method;
import java.util.Collection;

import com.googlecode.contraildb.core.IResult;

/**
 * Implements asynchronous try/finally behavior.
 * This class uses reflection to discover the methods 
 * that represent the try and finally clauses.
 * This technique makes it possible to write methods 
 * that may or may not return results, thus making it 
 * possible to create code that is as simple as possible.
 * 
 * When creating an instance of TryFinally create methods named 
 * doTry and doFinally.
 * 
 * Example...
 * 
 * 		TryFinally handler= new TryFinally() {
 * 			String result= "";
 * 			void doTry() {
 * 				result+= "try";
 * 				throw new RuntimeException("some error");
 * 			}
 * 			String doFinally() {
 * 				return result+= "-finally";
 * 			}
 * 		};
 * 		handler.join();
 * 		assertEquals("try-finally", handler.getResult());
 *
 * The result from a TryFinally will be the last result returned from either 
 * the try method or the finally method.
 * 
 * 
 * @author ted.stockwell
 *
 */
@SuppressWarnings({"rawtypes","unchecked"})
abstract public class TryFinally<T> extends Handler<Void,T> {
	private Method _tryMethod;
	private Method _finallyMethod;
	
	
	public TryFinally() {
		super();
		initMethods();
	}

	public TryFinally(Collection<IResult> tasks) {
		super(tasks);
		initMethods();
	}

	public TryFinally(IResult result, IResult... moreResults) {
		super(result, moreResults);
		initMethods();
	}

	public TryFinally(IResult<Void> task) {
		super(task);
		initMethods();
	}

	private void initMethods() {
		Method[] de
		
	}
	
	
	@Override
	final protected IResult onSuccess() throws Exception {
		try {
			return doTry();
		}
		finally {
			doFinally();
		}
	}
	
	abstract protected IResult<T> doTry() throws Exception;
	abstract protected IResult<T> doFinally() throws Exception;
}
