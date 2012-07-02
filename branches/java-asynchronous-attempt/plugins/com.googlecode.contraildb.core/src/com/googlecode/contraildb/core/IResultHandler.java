package com.googlecode.contraildb.core;

public interface IResultHandler<V> {
	public void onComplete(IResult<V> result) throws Exception;
}
