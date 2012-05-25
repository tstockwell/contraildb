package com.googlecode.contraildb.core.impl.btree;

/**
 * A marker interface for indicating when a cursor is always in the FORWARD direction
 */
public interface IForwardCursor<K> extends IBTreeCursor<K> { }