package com.googlecode.contraildb.core;

public interface IResultHandler<V> {
	public void complete(IResult<V> result);
}
