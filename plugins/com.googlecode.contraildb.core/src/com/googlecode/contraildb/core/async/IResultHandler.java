package com.googlecode.contraildb.core.async;



public interface IResultHandler<V> {
	public void onComplete(IResult<V> result) throws Exception;
}
