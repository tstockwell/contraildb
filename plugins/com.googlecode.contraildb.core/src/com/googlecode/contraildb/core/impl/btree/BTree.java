package com.googlecode.contraildb.core.impl.btree;

import java.io.DataInput;
import java.io.IOException;
import java.io.Serializable;

import kilim.Pausable;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.async.ContrailTask;
import com.googlecode.contraildb.core.async.IResult;
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
@SuppressWarnings({ "unchecked", "rawtypes" })
public class BTree<T extends Comparable> 
extends BPlusTree<T, Serializable>
{
	private static final long serialVersionUID = 1L;
	
	static class ForwardCursor<K extends Comparable> 
	extends CursorImpl<K, Object>
	implements IForwardCursor<K>
	{
		public ForwardCursor(BPlusTree index) {
			super(index, Direction.FORWARD);
		}
	}
	
	public static <K extends Comparable> IResult<BTree<K>> createInstanceA(final IEntityStorage.Session storageSession, final Identifier identifier, final int pageSize) 
	{
		return new ContrailTask<BTree<K>>() {
			@Override
			protected BTree<K> run() throws Pausable, Exception {
				BTree<K> btree= new BTree<K>(identifier, pageSize);
				storageSession.store(btree);
				return btree;
			}
		}.submit();
	}
	final public static <K extends Comparable> BTree<K> createInstance(IEntityStorage.Session storageSession, Identifier identifier, int pageSize) 
	throws Pausable
	{
		return (BTree<K>) createInstanceA(storageSession, identifier, pageSize).get();
	}
	
	final public static <K extends Comparable> BTree<K> createInstance(IEntityStorage.Session storageSession, Identifier identifier) 
	throws Pausable 
	{
		return createInstance(storageSession, identifier, DEFAULT_SIZE);
	}
	final public static <K extends Comparable> BTree<K> createInstance(IEntityStorage.Session storageSession, int pageSize) 
	throws Pausable 
	{
		return createInstance(storageSession, Identifier.create(), pageSize);
	}
	
	final public static <K extends Comparable> BTree<K> createInstance(IEntityStorage.Session storageSession) 
	throws Pausable 
	{
		return createInstance(storageSession, Identifier.create(), DEFAULT_SIZE);
	}

	private BTree(Identifier identifier, int pageSize) throws IOException {
		super(identifier, pageSize, false);
	}
	public IForwardCursor<T> forwardCursor() {
		return new ForwardCursor<T>(this);
	}
	
	protected BTree() { }
	

	public static final Serializer<BTree> SERIALIZER= new Serializer<BTree>() {
		private final String typeCode= BTree.class.getName();
		public BTree readExternal(java.io.DataInput in) 
		throws IOException {
			BTree journal= new BTree();
			readExternal(in, journal);
			return journal;
		};
		public void writeExternal(java.io.DataOutput out, BTree journal) 
		throws IOException {
			BPlusTree.SERIALIZER.writeExternal(out, journal);
		};
		public void readExternal(DataInput in, BTree journal)
		throws IOException {
			BPlusTree.SERIALIZER.readExternal(in, journal);
		}
		public String typeCode() {
			return typeCode;
		}
	};
	
}
