package com.googlecode.contraildb.core;

import java.io.IOException;
import java.util.List;

public interface IPreparedQuery<T extends Item>  {
	
	/**
	 * @return The session that created this query 
	 */
	public IContrailSession getSession();

	public List<T> list(FetchOptions fetchOptions) throws IOException;

	public List<T> list() throws IOException;

	public Iterable<T> iterate(FetchOptions fetchOptions) throws IOException;

	public Iterable<T> iterate() throws IOException;

	public T item() throws IOException;

	public int count() throws IOException;

	public Iterable<Identifier> identifiers() throws IOException;
}
