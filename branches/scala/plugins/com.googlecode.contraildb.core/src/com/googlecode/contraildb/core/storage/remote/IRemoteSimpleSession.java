package com.googlecode.contraildb.core.storage.remote;

import java.rmi.Remote;

import com.googlecode.contraildb.core.storage.provider.IStorageProvider;


public interface IRemoteSimpleSession extends Remote, IStorageProvider.Session{

}
