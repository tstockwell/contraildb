package com.googlecode.contraildb.common;



public interface IResultHandler<V> {
	public void onComplete(IResult<V> result) throws Exception;
}
