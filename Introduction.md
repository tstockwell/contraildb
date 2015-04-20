## High Available and Horizontally scalable ##

Contrail is a schemaless, concurrent, and distributed document database designed to be highly scalable.
Contrail can be used as an embedded database on a single machine or  scale up to a cloud-based database distributed across many machines.
Contrail provides most of the features that developers expect from a traditional relational database like ACID transactions and joins.

## Pluggable Storage ##

Contrail has a pluggable storage system.
An appropriate storage system is chosen for the environment in which Contrail will be used.
Contrail has storage systems that save data in memory, to a file system, or to cloud-based storage systems like Amazon S3 and Google Storage API.
Contrail uses multiversion concurrency control (MVCC) to allow an unlimited number of clients to simultaneously access the data in its storage system.
Contrail is _strongly consistent_, any storage technology used with it must provide strong consistency for new objects.

## Data Model ##

A data object in Contrail is known as an item.
An item has one or more properties, named values of one of several data types.
Property values must be one of the following data types:
  * String,
  * Date,
  * Integer, Byte, Long, Float, Double, BigInteger, or BigDecimal
  * Boolean,
  * Identifier,
  * Item,
  * List or Set, where values in collections are valid property values
> > Not all values in a collection must be of the same data type.
  * Map, where keys are valid scalar property values and values are valid property values
> > Keys may **not** be Item, List, Set, or Map

All items have an identifier that uniquely identifies the item.
An application can fetch an item from the datastore by using its key, or by performing a query that matches the item's properties.
A query can return zero or more items, and can return the results sorted by property values.
A query can also limit the number of results returned by the datastore to conserve memory and run time.

## Transactions ##

Contrail supports ACID transactions.
Transactions in Contrail are slightly different than in other systems.
If a Contrail transaction fails due to a 'conflicting' commit then the transaction should be retried.
Contrail transactions are retried until either the transaction succeeds or the transaction fails for some reason other than a conflicting commit.