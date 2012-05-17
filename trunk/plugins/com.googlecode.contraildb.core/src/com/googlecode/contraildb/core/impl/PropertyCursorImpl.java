package com.googlecode.contraildb.core.impl;

import java.io.Serializable;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.impl.btree.KeyValueSet;
import com.googlecode.contraildb.core.impl.btree.CursorImpl;
import com.googlecode.contraildb.core.impl.btree.IOrderedSetCursor;
import com.googlecode.contraildb.core.impl.btree.IForwardCursor;

/**
 * A cursor for navigating a PropertyIndex.
 * @author ted.stockwell
 */
public class PropertyCursorImpl<K extends Comparable<K> & Serializable> 
extends CursorImpl<K, IForwardCursor<Identifier>>
implements IPropertyCursor<K>
{

	public PropertyCursorImpl(KeyValueSet<K, IForwardCursor<Identifier>> index, IOrderedSetCursor.Direction direction) {
		super(index, direction);
	}

}
