package com.googlecode.contraildb.core.impl.btree;

import java.io.DataInput;
import java.io.IOException;
import java.io.Serializable;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.async.Handler;
import com.googlecode.contraildb.core.storage.IEntityStorage;
import com.googlecode.contraildb.core.utils.ExternalizationManager.Serializer;


//TODO This class must be extended to support NULL values

/**
 * A persistent, ordered set of values.
 * Allows insertions, deletions, searches, and sequential access.
 * 
 * Uses a B-tree structure. 
 * @see http://en.wikipedia.org/wiki/B-tree
 * 
 * @author Ted Stockwell
 * 
 * @param T The type of objects stored in the BTree
 */
@SuppressWarnings({"unchecked","rawtypes" })
public class BTree<T extends Comparable> 
extends KeyValueSet<T, Serializable>
{
	private static final long serialVersionUID = 1L;
	
	private BTree(Identifier identifier, int pageSize) {
		super(identifier, pageSize, false);
	}
	protected BTree() { }
	
}
