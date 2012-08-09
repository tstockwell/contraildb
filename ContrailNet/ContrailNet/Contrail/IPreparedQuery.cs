using System;
using System.Collections.Generic;

namespace Contrail {

public interface IPreparedQuery<T>  where T : Item
{
    // The session that created this query 
    string Session { get; set; }

	// Enumerates the identifiers of all matching items.
	// This method id the most basic way to get results and is the fastest.
	IEnumerator<Identifier> identifiers();

	// A convenience method that returns items instead of identifiers.
	IEnumerator<T> iterate(FetchOptions fetchOptions);

	// A convenience method that returns items instead of identifiers.
	IEnumerator<T> iterate();

	// A convenience method that returns a list of all matching items
	List<T> List(FetchOptions fetchOptions);

	// A convenience method that returns a list of all matching items
	List<T> List();

	// A convenience method that returns the first matching item
	T Item();

	// A convenience method that returns just the number of matching items
	int count();
}

}