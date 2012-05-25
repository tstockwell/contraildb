package com.googlecode.contraildb.core.impl;

import java.io.IOException;
import java.util.List;

import com.googlecode.contraildb.core.ContrailQuery;
import com.googlecode.contraildb.core.FetchOptions;
import com.googlecode.contraildb.core.IContrailSession;
import com.googlecode.contraildb.core.IPreparedQuery;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.Item;
import com.googlecode.contraildb.core.utils.ConversionUtils;



public class PreparedQueryImpl<T extends Item> 
implements IPreparedQuery<T> 
{
	ContrailServiceImpl _service;
	ContrailSessionImpl _session;
	ContrailQuery _query;

	PreparedQueryImpl(ContrailServiceImpl service, ContrailSessionImpl transaction, ContrailQuery query) {
		_service= service;
		_session= transaction;
		_query= query;
	}

	@Override
	public Iterable<T> iterate(FetchOptions fetchOptions) throws IOException {
		return list(fetchOptions);
	}


	@Override
	public Iterable<T> iterate() throws IOException {
		return list(FetchOptions.withPrefetchSize(0));
	}


	@Override
	public List<T> list() throws IOException {
		return list(FetchOptions.withPrefetchSize(0));
	}

	@Override
	public List<T> list(FetchOptions fetchOptions) throws IOException {
		Iterable<T> iterable= _session.search(_query);
		return ConversionUtils.toList(iterable);
	}


	@Override
	public T item() throws IOException {
		List<T> list= list(FetchOptions.withPrefetchSize(1));
		if (list.isEmpty())
			return null;
		return list.get(0);
	}


	@Override
	public int count() throws IOException {
		return ConversionUtils.toList(identifiers()).size();
	}


	@Override
	public IContrailSession getSession() {
		return _session;
	}

	@Override
	public Iterable<Identifier> identifiers() throws IOException {
		return _session.fetchIdentifiers(_query);
	}
	
}