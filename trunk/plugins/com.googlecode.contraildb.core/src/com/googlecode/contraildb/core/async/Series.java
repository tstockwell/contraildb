package com.googlecode.contraildb.core.async;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.googlecode.contraildb.core.IResult;



/**
 * This class uses reflection to look at its methods, which perform 
 * some task and return Results, and then creates handlers which 
 * execute the methods sequentially.
 * 
 * The order of the execution is determined with the help of the @seq annotation.
 * The @seq annotation specifies the name of the method to be executed 
 * *before* the annotated method.
 * An example...
 * 
 *    			Handler hander= new Series(inputResult) {
 *    				@init("doFetch") Item item;
 *    
 *    				IResult doFetch() {
 *    					return fetch(path);
 *    				}
 *    				@seq("doFetch") IResult doDelete() {
 *    					return new Parallel() {
 *    						IResult delete() {
 *    							gigitty();
 *    							_storageSession.delete(path);
 *    						}
 *    						IResult clearCache() {
 *    							_cache.delete(path);
 *    						}
 *    					};
 *    				}
 *    
 *    				// specifies an access modifier, so not wrapped by a handler
 *    				private void gigitty() {
 *    					System.out.println("gigitty");
 *    				}
 *    			};
 *    
 * The above handler will run fetch, and doDelete in that order.
 * The @init annotation indicates that the result of the doFetch 
 * method should be saved in the item field. 
 * 
 * A method may return a Result and that result will become the input 
 * to the handler created for the next method.
 * A method may return void, in which case a Result<Void> will be the 
 * input the next handler.
 * 
 * All methods that do not specify an access modifier are assumed to 
 * be handler methods.
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

	private static class HandlerInfo {
		Method method;
		Field initField;
	}
	private static HashMap<Class, List<HandlerInfo>> _methodsByClass= 
			new HashMap<Class, List<HandlerInfo>>();
	
	List<Handler> _handlers;
	
	// the input to the first handler in series
	Result _result= new Result();

	public Series() {
		createHandlers();
	}
	public Series(IResult result) {
		super(result);
		createHandlers();
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
	public void handleResult(IResult result) {
		super.handleResult(result);
	}
	
	private void createHandlers() {
		List<HandlerInfo> handlerMethods= getHandlerMethods();
		_handlers= createHandlers(handlerMethods);
		wireHandlers();
		
	}
	private List<Handler> createHandlers(List<HandlerInfo> handlerMethods) {
		ArrayList<Handler> handlers= new ArrayList<Handler>();
		for (HandlerInfo info:handlerMethods) {
			handlers.add(createHandler(info));
		}
		return handlers;
	}
	
	private Handler createHandler(final HandlerInfo info) {
		return new Handler() {
			protected IResult onSuccess() throws Exception {
				
				final Object result= info.method.invoke(Series.this, (Object[])null);
				if (info.initField == null) {
					if (result == null)
						return TaskUtils.DONE;
					if (result instanceof IResult)
						return (IResult) result;
					return TaskUtils.asResult(result);
				}

				// there is an init field that needs to be initialized
				if (result == null) {
					info.initField.set(Series.this, null);
					return TaskUtils.DONE;
				}
				if (!(result instanceof IResult)) {
					info.initField.set(Series.this, result);
					return TaskUtils.DONE;
				}
				return new Handler(result) {
					protected IResult onSuccess() throws Exception {
						Object value= ((IResult)result).getResult();
						info.initField.set(Series.this, value);
						return TaskUtils.DONE;
					}
				};
			}
		};
	}
	
	private void wireHandlers() {

		// connect all handlers but the first one in series
		for (int i= 1; i < _handlers.size(); i++) {
			Handler handler= _handlers.get(i);
			handler.handleResult(_handlers.get(i-1));
		}

		// connect first handler to internal result which will completed
		// when this handler is completed, thus firing off the first handler.
		if (0 < _handlers.size()) {
			_handlers.get(0).handleResult(_result);
		}
	}
	
	synchronized private List<HandlerInfo> getHandlerMethods() {
		Class thisClass= this.getClass();
		List<HandlerInfo> handlerMethods= _methodsByClass.get(thisClass);
		if (handlerMethods != null)
			return handlerMethods;
		handlerMethods= new ArrayList<HandlerInfo>();
		
		// find fields to be initialized.
		Field[] declaredFields= thisClass.getDeclaredFields();
		HashMap<String, Field> initFields= new HashMap<String, Field>();
		for (Field field:declaredFields) {
			init init= field.getAnnotation(init.class);
			if (init != null) {
				initFields.put(init.value().toLowerCase(), field);
				field.setAccessible(true);
			}
		}
		
		// find handler methods
		Method[] declaredMethods= thisClass.getDeclaredMethods();
		for (Method method:declaredMethods) {
			int modifers= method.getModifiers();
			if (!Modifier.isPublic(modifers) && !Modifier.isProtected(modifers) && !Modifier.isPrivate(modifers)) {
				// no access modifier specified - assume its a handler method
				method.setAccessible(true);
				HandlerInfo info= new HandlerInfo();
				info.method= method;
				info.initField= initFields.get(method.getName().toLowerCase());
				handlerMethods.add(info);
			}
		}
		
		// sort methods
		if (1 < handlerMethods.size()) {
			while (1 < handlerMethods.size()) {
				boolean noChange= true;
				for (int i= 1; i < handlerMethods.size(); i++) {
					HandlerInfo iinfo= handlerMethods.get(i);
					Method imethod= iinfo.method;
					String iname= imethod.getName().toLowerCase();
					
					if (iname.startsWith("start")) {
						handlerMethods.remove(i);
						handlerMethods.add(0, iinfo);
						noChange= false;
						continue;
					}
					
					seq iseq= imethod.getAnnotation(seq.class);
					String isuffix= iseq != null ? iseq.value().toLowerCase() : "";
					for (int j= 0; j < i; j++) {
						HandlerInfo jinfo= handlerMethods.get(j);
						Method jmethod= jinfo.method;
						String jname= jmethod.getName().toLowerCase();
						seq jseq= jmethod.getAnnotation(seq.class);
						String jsuffix= jseq != null ? jseq.value().toLowerCase() : "";;
						if (0 < jsuffix.length() && iname.startsWith(jsuffix) && j+1 < i) {
							handlerMethods.remove(i);
							handlerMethods.add(j+1, iinfo);
							noChange= false;
							break;
						}
						if (0 < isuffix.length() && jname.startsWith(isuffix) && j+1 < i) {
							handlerMethods.remove(j);
							handlerMethods.add(i, jinfo);
							noChange= false;
							break;
						}
					}
				}
				if (noChange)
					break;
			}
		}
		
		_methodsByClass.put(thisClass, handlerMethods);
		return handlerMethods;
	}
	
	
	@Override
	protected IResult onSuccess() throws Exception {
		if (0 < _handlers.size()) {
			// fire the first handler in series
			_result.success(null);
			
			// return result from last handler
			return _handlers.get(_handlers.size()-1);
		}
		
		// no handlers in series, we're done.
		_outgoing.success(null);
		return TaskUtils.DONE;
	}
	
	protected void run(IResult handler) {
		
	}
}
