package com.googlecode.contraildb.core.impl;

import java.io.Serializable;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.impl.btree.IBTreePlusCursor;
import com.googlecode.contraildb.core.impl.btree.IForwardCursor;

/**
 * A cursor for navigating a property index.
 * @author ted.stockwell
 */
public interface IPropertyCursor<K extends Comparable<K> & Serializable>
extends IBTreePlusCursor<K,IForwardCursor<Identifier>>
{

}
