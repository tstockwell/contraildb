package com.googlecode.contraildb.core.storage;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.utils.IResult;


public class StorageUtils {
	
	public static final <T extends IEntity> List<T> syncFetch(IEntityStorage.Session storage, Iterable<Identifier> ids)
	{
		ArrayList<IResult<T>> fetched= new ArrayList<IResult<T>>();
		for (Identifier id:ids) {
			IResult<T> result= storage.fetch(id);
			fetched.add(result);
		}
		ArrayList<T> results= new ArrayList<T>();
		for (IResult<T> result: fetched)
			results.add(result.get());
		return results;
	}

	public static <T extends IEntity> T syncFetch(IEntityStorage.Session storageSession, Identifier indexId) {
		IResult<T> result= storageSession.fetch(indexId);
		return result.get();
	}
}
