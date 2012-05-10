package com.googlecode.contraildb.core.impl.btree;

import java.io.DataInput;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.storage.StorageUtils;
import com.googlecode.contraildb.core.utils.Handler;
import com.googlecode.contraildb.core.utils.TaskUtils;
import com.googlecode.contraildb.core.utils.ExternalizationManager.Serializer;




/**
 * An inner node in a BTree
 */
@SuppressWarnings({"rawtypes","unchecked"})
public class InnerNode<K extends Comparable>
extends Node<K>
{
	final static long serialVersionUID = 1L;
	
	public InnerNode(BPlusTree<K,?> btree) {
		super(btree);
	}
	
	protected InnerNode() { }

	@Override boolean isLeaf() { return false; }
	@Override Node<K> clone(Node<K> node) { return  new InnerNode<K>(node._index); }
	@Override K getLookupKey() throws IOException { return getLargestKey(); }		
	@Override int indexOf(K key) { 
		int left = 0;
		int right = _size - 1;

		while (left <= right) {
			int middle = (left + right) / 2;
			int i= BPlusTree.compare(_keys[middle], key);
			if (i == 0) {
				left= middle+1;
				break;
			}
			if (i < 0) {
				left = middle + 1;
			} else {
				right = middle - 1;
			}
		}
		if (_size <= left)
			return _size-1;
		assert 0 <= left;
		return left;
	}

	/**
	 * Insert the given key and value.
	 * @return new Node if the key was inserted and provoked an overflow.
	 */
	@Override public Node<K> insert(K key, Object value) throws IOException {
		int index = indexOf(key);
		
		Node<K> child = getChildNode(index);
		Node<K> newSibling= child.insert(key, value);
		if (newSibling == null)  // no overflow means we're done with insertion
			return null;

		// there was an overflow, we need to insert the overflow page
		Node<K> overflow = null;
		Identifier newSiblingId = newSibling.getId();
		K keyForChildNode= child.getLookupKey();
		K keyForSiblingNode= newSibling.getLookupKey();
		Identifier childId = child.getId();
		if (!isFull()) {
			setEntry(index, keyForChildNode, childId);
			insertEntry(index+1, keyForSiblingNode, newSiblingId);
		}
		else {
			// page is full, we must divide the page
			overflow = split();
			if (index < _size) { 
				setEntry(index, keyForChildNode, childId);
				insertEntry(index+1, keyForSiblingNode, newSiblingId);
			}
			else {
				overflow.setEntry(index-_size, keyForChildNode, childId);
				overflow.insertEntry(index-_size+1, keyForSiblingNode, newSiblingId);
			}
			getStorage().store(overflow);
		}
		update();
		return overflow;
	}
	
	@Override boolean remove(K key) throws IOException {

		int index = indexOf(key);
		
		Node<K> child = getChildNode(index);
		boolean underflow= child.remove(key);
		if (!underflow)
			return false;

		if (index < _size - 1) {
			Node<K> rightSibling = getChildNode(index + 1);
			if (rightSibling._size+child._size <= _index._pageSize) {
				child.merge(rightSibling);
				removeEntry(index);
				_values[index]= child.getId();
				update();
			}
		}
		if (0 < index) {
			Node<K> leftSibling = getChildNode(index - 1);
			if (leftSibling._size+child._size <= _index._pageSize) {
				leftSibling.merge(child);
				removeEntry(index-1);
				_values[index-1]= leftSibling.getId();
				update();
			}
		}
		if (index == 0 && child.isEmpty()) {
			removeEntry(0);
			update();
		}

		return _size < _index._pageSize / 2;
	}
	
	
	/**
	 * Deletes this node and all children
	 */
	public IResult<Void> delete() {
		ArrayList<IResult> tasks= new ArrayList<IResult>();
		for (int i= _size; 0 < i--;) {
			final IResult fetch = getStorage().fetch((Identifier)_values[i]);
			tasks.add(new Handler(fetch) {
				protected IResult onSuccess() throws Exception {
					Node<?> childBPage = (Node<?>) fetch.getResult();
					return childBPage.delete();
				}
			}.toResult());
		}
		return new Handler(TaskUtils.combineResults(tasks)) {
			protected IResult onSuccess() throws Exception {
				return InnerNode.super.delete();
			}
		}.toResult();
	}

	/**
	 * Dump this node and all children. 
	 */
	void dump(PrintStream out, int depth) throws IOException {
		super.dump(out, depth);
		for (int i = 0; i < _size; i++) {
			Node<K> child = getChildNode(i);
			child.dump(out, depth+1);
		}
	}
	
	

	public static final Serializer<InnerNode> SERIALIZER= new Serializer<InnerNode>() {
		private final int typeCode= InnerNode.class.getName().hashCode();
		public InnerNode readExternal(java.io.DataInput in) 
		throws IOException {
			InnerNode journal= new InnerNode();
			readExternal(in, journal);
			return journal;
		};
		public void writeExternal(java.io.DataOutput out, InnerNode journal) 
		throws IOException {
			Node.SERIALIZER.writeExternal(out, journal);
		};
		public void readExternal(DataInput in, InnerNode journal)
		throws IOException {
			Node.SERIALIZER.readExternal(in, journal);
		}
		public int typeCode() {
			return typeCode;
		}
	};
	
}
