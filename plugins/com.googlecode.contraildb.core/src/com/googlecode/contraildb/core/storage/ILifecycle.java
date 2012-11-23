package com.googlecode.contraildb.core.storage;

import java.io.IOException;

import kilim.Pausable;

import com.googlecode.contraildb.core.Identifier;


/**
 * Provides callbacks from this storage session to objects
 */
public interface ILifecycle {
	
	void setStorage(IEntityStorage.Session storage) throws IOException, Pausable;
	void onInsert(Identifier identifier) throws IOException, Pausable;
	void onLoad(Identifier identifier) throws IOException, Pausable;
	void onDelete() throws IOException, Pausable;
}