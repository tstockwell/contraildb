package com.googlecode.contraildb.core.storage;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.utils.IResult;


public class StorageUtils {
	
	public static final <T extends IEntity> List<T> syncFetch(IEntityStorage.Session storage, Iterable<Identifier> ids)
	{
		ArrayList<CFuture<T>> fetched= new ArrayList<CFuture<T>>();
		for (Identifier id:ids) {
			CFuture<T> result= storage.fetch(id);
			fetched.add(result);
		}
		ArrayList<T> results= new ArrayList<T>();
		for (CFuture<T> result: fetched)
			results.add(result.get());
		return results;
	}

	public static <T extends IEntity> T syncFetch(IEntityStorage.Session storageSession, Identifier indexId) {
		CFuture<T> result= storageSession.fetch(indexId);
		return result.get();
	}
}
