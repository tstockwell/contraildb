using System;
using System.Collections.Generic;

namespace Contrail
{


	//
	// A ContrailSession is used to query an existing revision of the database or 
	// to create and commit a new revision.
	// 
	// <author>Ted Stockwell</author>
	//
	interface IContrailSession
	{
	
		// Get the database revision associated with this session. 
		long RevisionNumber { get; set; }
	
		// Create prepared query for items.
		IPreparedQuery<T> prepare<T> (ContrailQuery query) where T:Item;

		//
		// Commits all changes and closes the session
		// A client *MUST* call this method or the close method when finished with a session
		// No other methods may be invoked after invoking this method (except the 
		// close method, but calling the close method is not necessary after calling this method).
		// 
		// <throws>
		//	ConflictingCommitException when a potentially conficting change has been comitted to the database by another process.
		//  When this exception is throw the caller should open a new session and retry the transaction. 
		// <throws>
		//
		void commit ();

		//
		// Abandons any uncommitted changes and closes the session
		// A client *MUST* call this method or the commit method when finished with a transaction
		// No other methods may be invoked after invoking this method.
		// 
		// @throws ConflictingCommitException when a potentially conficting change has been comitted to the database by another process.
		// When this exception is throw the caller should open a new session and retry the transaction. 
		//
		void close ();

		boolean isActive ();
	
	
		// Return the raw storage
		IStorageProvider getStorageProvider ();
	
		bool create<E> (E entity) where E:Item;

		void store<E> (params E[] entities) where E:Item;

		void store<E> (IEnumerator<E> entities) where E:Item;

		void update<E> (params E[] entities) where E:Item;

		void update<E> (IEnumerator<E> entities) where E:Item;

		void delete (params Identifier[] paths);

		void delete (Collection<Identifier> paths);

		void delete<E> (params E[] entities);

		void delete<E> (IEnumerator<E> entities);

		E fetch<E> (Identifier path) where E:Item;

		ICollection<E> fetch<E> (params Identifier[] paths) where E:Item;

		ICollection<E> fetch<E> (Iterable<Identifier> paths) where E:Item;

		ICollection<Identifier> listChildren (Identifier path);

		IDictionary<Identifier, ICollection<Identifier>> listChildren (params Identifier[] paths);

		IDictionary<Identifier, ICollection<Identifier>> listChildren (ICollection<Identifier> paths);

		ICollection<Identifier> listChildren (E path);

		IDictionary<Identifier, ICollection<Identifier>> listChildren (params E[] paths);

		IDictionary<Identifier, ICollection<Identifier>> listChildren (Iterable<E> entities);

		ICollection<E> fetchChildren<E> (Identifier path) where E:Item;

		IDictionary<Identifier, ICollection<E>> fetchChildren<E> (params Identifier[] paths) where E:Item;

		IDictionary<Identifier, ICollection<E>> fetchChildren<E> (ICollection<Identifier> paths) where E:Item;

		IDictionary<Identifier, ICollection<C>> fetchChildren<E,C> (params E[] paths) where E:Item where C:Item;

		IDictionary<Identifier, ICollection<C>> fetchChildren<E,C> (IEnumerator<E> entities) where E:Item where C:Item;

		ICollection<C> fetchChildren<E,C> (E entity) where E:Item where C:Item;

		void deleteAllChildren (params Identifier[] paths);

		void deleteAllChildren (ICollection<Identifier> paths);

		void deleteAllChildren<E> (params E[] paths);

		void deleteAllChildren<E> (IEnumerator<E> entities);

	}

}