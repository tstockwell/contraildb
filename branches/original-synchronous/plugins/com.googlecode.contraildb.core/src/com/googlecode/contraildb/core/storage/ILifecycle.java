package com.googlecode.contraildb.core.storage;

import java.io.IOException;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.async.IResult;


/**
 * Provides callbacks from this storage session to objects
 */
public interface ILifecycle {
	
	IResult<Void> setStorageA(IEntityStorage.Session storage);
	IResult<Void> onInsertA(Identifier identifier) throws IOException;
	IResult<Void> onLoadA(Identifier identifier) throws IOException;
	IResult<Void> onDeleteA() throws IOException;
}