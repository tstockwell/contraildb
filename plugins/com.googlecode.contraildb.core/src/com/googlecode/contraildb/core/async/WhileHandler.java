package com.googlecode.contraildb.core.async;

import com.googlecode.contraildb.core.IResult;

/**
 * Repeatedly calls the While() method, invokes the Do() method until 
 * While() returns false.
 * 
 * The Init() method is is invoked once, before the first call to While().
 *  
 * @author ted.stockwell
 */
@SuppressWarnings({"rawtypes","unchecked"})
abstract public class WhileHandler extends Handler {
	
	public WhileHandler() {
		super();
	}
	public WhileHandler(IResult result) {
		super(result);
	}
	
	protected IResult<Void> Init() throws Exception { return TaskUtils.DONE; }
	abstract protected IResult<Boolean> While() throws Exception;
	abstract protected IResult<Void> Do() throws Exception;
	
	final protected IResult onSuccess() throws Exception {
		return new Handler(Init()) {
			protected IResult onSuccess() throws Exception {
				return handleWhile();
			}
		};
	}
	final private IResult handleWhile() throws Exception {
		return new Handler(While()) {
			protected IResult onSuccess() throws Exception {
				Boolean x= (Boolean)incoming().getResult();
				if (x == null || !x)
					return TaskUtils.DONE;
				return new Handler(Do()) {
					protected IResult onSuccess() throws Exception {
						return WhileHandler.this.handleWhile();
					}
				};
			}
		};
	}
}
