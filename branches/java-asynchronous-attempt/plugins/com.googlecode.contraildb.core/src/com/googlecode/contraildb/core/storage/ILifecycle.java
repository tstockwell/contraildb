package com.googlecode.contraildb.core.storage;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;


/**
 * Provides callbacks from this storage session to objects
 */
public interface ILifecycle {
	void setStorage(IEntityStorage.Session storage);
	IResult<Void> onInsert(Identifier identifier);
	IResult<Void> onLoad(Identifier identifier);
	IResult<Void> onDelete();
}