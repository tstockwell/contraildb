package com.googlecode.contraildb.core.storage.remote;

import java.rmi.Remote;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.storage.provider.IStorageProvider;


/**
 * An interface to remote storage providers.
 * @author Ted Stockwell
 *
 */
public interface IRemoteSimpleStorage extends Remote, IStorageProvider {
	
	@Override
	IResult<Session> connect();

}
