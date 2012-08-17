package com.googlecode.contraildb.core;

import java.io.IOException;
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
	public <T extends Item> IPreparedQuery<T> prepare(ContrailQuery query) throws IOException;

	/**
	 * Commits all changes and closes the session
	 * A client *MUST* call this method or the close method when finished with a session
	 * No other methods may be invoked after invoking this method (except the 
	 * close method, but calling the close method is not necessary after calling this method).
	 * 
	 * @throws ConflictingCommitException when a potentially conficting change has been comitted to the database by another process.
	 * When this exception is throw the caller should open a new session and retry the transaction. 
	 */
	public void commit() throws IOException, ConflictingCommitException;

	/**
	 * Abandons any uncommitted changes and closes the session
	 * A client *MUST* call this method or the commit method when finished with a transaction
	 * No other methods may be invoked after invoking this method.
	 * 
	 * @throws ConflictingCommitException when a potentially conficting change has been comitted to the database by another process.
	 * When this exception is throw the caller should open a new session and retry the transaction. 
	 */
	public void close() throws IOException;

	public boolean isActive();
	
	
	/**
	 * Return the under raw storage
	 */
	public IStorageProvider getStorageProvider();
	
	
	
	public <E extends Item> boolean create(E entity) throws IOException;
	public <E extends Item> void store(E... entities) throws IOException;
	public <E extends Item> void store(Iterable<E> entities) throws IOException;
	public <E extends Item> void update(E... entities) throws IOException;
	public <E extends Item> void update(Iterable<E> entities) throws IOException;

	public void delete(Identifier... paths) throws IOException;
	public void delete(Collection<Identifier> paths) throws IOException;
	public <E extends Item> void delete(E... entities) throws IOException;
	public <E extends Item> void delete(Iterable<E> entities) throws IOException;

	public <E extends Item> E fetch(Identifier path) throws IOException;
	public <E extends Item> Collection<E> fetch(Identifier... paths) throws IOException;
	public <E extends Item> Collection<E> fetch(Iterable<Identifier> paths) throws IOException;

	public Collection<Identifier> listChildren(Identifier path) throws IOException;
	public Map<Identifier, Collection<Identifier>> listChildren(Identifier... paths) throws IOException;
	public Map<Identifier, Collection<Identifier>> listChildren(Collection<Identifier> paths) throws IOException;
	public <E extends Item> Collection<Identifier> listChildren(E path) throws IOException;
	public <E extends Item> Map<Identifier, Collection<Identifier>> listChildren(E... paths) throws IOException;
	public <E extends Item> Map<Identifier, Collection<Identifier>> listChildren(Iterable<E> entities) throws IOException;

	public <E extends Item> Collection<E> fetchChildren(Identifier path) throws IOException;
	public <E extends Item> Map<Identifier, Collection<E>> fetchChildren(Identifier... paths) throws IOException; 
	public <E extends Item>  Map<Identifier, Collection<E>> fetchChildren(Collection<Identifier> paths) throws IOException; 
	public <E extends Item, C extends Item> Map<Identifier, Collection<C>> fetchChildren(E... paths) throws IOException; 
	public <E extends Item, C extends Item> Map<Identifier, Collection<C>> fetchChildren(Iterable<E> entities) throws IOException; 
	public <E extends Item, C extends Item> Collection<C> fetchChildren(E entity) throws IOException; 

	public void deleteAllChildren(Identifier... paths) throws IOException;
	public void deleteAllChildren(Collection<Identifier> paths) throws IOException;
	public <E extends Item> void deleteAllChildren(E... paths) throws IOException;
	public <E extends Item> void deleteAllChildren(Iterable<E> entities) throws IOException;
	


}
