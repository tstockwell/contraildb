package com.googlecode.contraildb.core.storage.remote;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import com.googlecode.contraildb.core.storage.provider.IStorageProvider;


/**
 * An interface to remote storage providers.
 * @author Ted Stockwell
 *
 */
public interface IRemoteSimpleStorage extends Remote, IStorageProvider {
	
	@Override
	Session connect() throws IOException, RemoteException;

}
