package com.googlecode.contraildb.core;

import java.util.Collection;
import java.util.Map;

import com.googlecode.contraildb.core.storage.provider.IStorageProvider;




/**
 * A ContrailSession is used to query an existing revision of the database or 
 * to create and commit a new revision.
 * 
 * @author Ted Stockwell
 */
public interface IContrailSession
{
	
	/**
	 * Get the database revision associated with this session. 
	 */
	public long getRevisionNumber();
	
	/**
	 * Query for items.
	 */
	public <T extends Item> IResult<IPreparedQuery<T>> prepare(ContrailQuery query);

	/**
	 * Commits all changes and closes the session
	 * A client *MUST* call this method or the close method when finished with a session
	 * No other methods may be invoked after invoking this method (except the 
	 * close method, but calling the close method is not necessary after calling this method).
	 * 
	 * @throws ConflictingCommitException when a potentially conflicting change has been committed to the database by another process.
	 * When this exception is throw the caller should open a new session and retry the transaction. 
	 */
	public IResult<Void> commit();

	/**
	 * Abandons any uncommitted changes and closes the session
	 * A client *MUST* call this method or the commit method when finished with a transaction
	 * No other methods may be invoked after invoking this method.
	 * 
	 * @throws ConflictingCommitException when a potentially conflicting change has been committed to the database by another process.
	 * When this exception is throw the caller should open a new session and retry the transaction. 
	 */
	public IResult<Void> close();

	public boolean isActive();
	
	
	/**
	 * Return the under raw storage
	 */
	public IStorageProvider getStorageProvider();
	
	
	
	public <E extends Item> IResult<Boolean> create(E entity);
	public <E extends Item> IResult<Void> store(E... entities);
	public <E extends Item> IResult<Void> store(Iterable<E> entities);
	public <E extends Item> IResult<Void> update(E... entities);
	public <E extends Item> IResult<Void> update(Iterable<E> entities);

	public IResult<Void> delete(Identifier... paths);
	public IResult<Void> delete(Collection<Identifier> paths);
	public <E extends Item> IResult<Void> delete(E... entities);
	public <E extends Item> IResult<Void> delete(Iterable<E> entities);

	public <E extends Item> IResult<E> fetch(Identifier path);
	public <E extends Item> IResult<Collection<E>> fetch(Identifier... paths);
	public <E extends Item> IResult<Collection<E>> fetch(Iterable<Identifier> paths);
	public <E extends Item> IResult<E> fetch(IResult<Identifier> path);

	public IResult<Collection<Identifier>> listChildren(Identifier path);
	public IResult<Map<Identifier, Collection<Identifier>>> listChildren(Identifier... paths);
	public IResult<Map<Identifier, Collection<Identifier>>> listChildren(Collection<Identifier> paths);
	public <E extends Item> IResult<Collection<Identifier>> listChildren(E path);
	public <E extends Item> IResult<Map<Identifier, Collection<Identifier>>> listChildren(E... paths);
	public <E extends Item> IResult<Map<Identifier, Collection<Identifier>>> listChildren(Iterable<E> entities);

	public <E extends Item> IResult<Collection<E>> fetchChildren(Identifier path);
	public <E extends Item> IResult<Map<Identifier, Collection<E>>> fetchChildren(Identifier... paths); 
	public <E extends Item>  IResult<Map<Identifier, Collection<E>>> fetchChildren(Collection<Identifier> paths); 
	public <E extends Item, C extends Item> IResult<Map<Identifier, Collection<C>>> fetchChildren(E... paths); 
	public <E extends Item, C extends Item> IResult<Map<Identifier, Collection<C>>> fetchChildren(Iterable<E> entities); 
	public <E extends Item, C extends Item> IResult<Collection<C>> fetchChildren(E entity); 

	public IResult<Void> deleteAllChildren(Identifier... paths);
	public IResult<Void> deleteAllChildren(Collection<Identifier> paths);
	public <E extends Item> IResult<Void> deleteAllChildren(E... paths);
	public <E extends Item> IResult<Void> deleteAllChildren(Iterable<E> entities);
	


}
