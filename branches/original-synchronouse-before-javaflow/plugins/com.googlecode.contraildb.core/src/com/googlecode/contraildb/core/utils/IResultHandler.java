package com.googlecode.contraildb.core.utils;



public interface IResultHandler<V> {
	public void onComplete(IResult<V> result) throws Exception;
}
