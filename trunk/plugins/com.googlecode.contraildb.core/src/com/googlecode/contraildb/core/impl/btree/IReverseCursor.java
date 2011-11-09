package com.googlecode.contraildb.core.impl.btree;

/**
 * A marker interface for indicating when a cursor is always in the REVERSE direction
 */
public interface IReverseCursor<K> extends IBTreeCursor<K> { }