package com.googlecode.contraildb.core.storage.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.googlecode.contraildb.core.async.IResult;


/**
 * An interface to remote storage providers.
 * @author Ted Stockwell
 *
 */
public interface IRemoteSimpleStorage extends Remote {
	
	IResult<IRemoteSimpleSession> connect() throws RemoteException;

}
