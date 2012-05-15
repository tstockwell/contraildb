package com.googlecode.contraildb.core.impl;

import java.io.Serializable;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.impl.btree.BPlusTree;
import com.googlecode.contraildb.core.impl.btree.CursorImpl;
import com.googlecode.contraildb.core.impl.btree.IBTreeCursor;
import com.googlecode.contraildb.core.impl.btree.IForwardCursor;

/**
 * A cursor for navigating a PropertyIndex.
 * @author ted.stockwell
 */
public class PropertyCursorImpl<K extends Comparable<K> & Serializable> 
extends CursorImpl<K, IForwardCursor<Identifier>>
implements IPropertyCursor<K>
{

	public PropertyCursorImpl(BPlusTree<K, IForwardCursor<Identifier>> index, IBTreeCursor.Direction direction) {
		super(index, direction);
	}

}
