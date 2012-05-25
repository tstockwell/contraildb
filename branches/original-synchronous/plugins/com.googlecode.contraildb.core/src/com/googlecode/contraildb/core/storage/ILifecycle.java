package com.googlecode.contraildb.core.storage;

import java.io.IOException;

import com.googlecode.contraildb.core.Identifier;


/**
 * Provides callbacks from this storage session to objects
 */
public interface ILifecycle {
	void setStorage(IEntityStorage.Session storage);
	void onInsert(Identifier identifier) throws IOException;
	void onLoad(Identifier identifier) throws IOException;
	void onDelete() throws IOException;
}