package com.googlecode.contraildb.core.impl.btree;

import java.io.DataInput;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.locks.ReentrantLock;

import kilim.Pausable;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.async.ContrailAction;
import com.googlecode.contraildb.core.async.ContrailTask;
import com.googlecode.contraildb.core.async.IResult;
import com.googlecode.contraildb.core.async.TaskTracker;
import com.googlecode.contraildb.core.async.TaskUtils;
import com.googlecode.contraildb.core.utils.ExternalizationManager.Serializer;




/**
 * An inner node in a BTree
 */
@SuppressWarnings("rawtypes")
public class InnerNode<K extends Comparable>
extends Node<K>
{
	final static long serialVersionUID = 1L;
	
	private final ReentrantLock _lock = new ReentrantLock();	
	
	public InnerNode(BPlusTree<K,?> btree) {
		super(btree);
	}
	
	protected InnerNode() { }

	@Override boolean isLeaf() { return false; }
	@Override Node<K> clone(Node<K> node) { return  new InnerNode<K>(node._index); }
	@Override K getLookupKey() throws Pausable { return getLargestKey(); }		
	@Override int indexOf(K key) {
	/*synchronized*/_lock.lock(); try {
	
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
	} finally { _lock.unlock(); }}

	/**
	 * Insert the given key and value.
	 * @return new Node if the key was inserted and provoked an overflow.
	 */
	@Override public IResult<Node<K>> insertA(final K key, final Object value) {
		return new ContrailTask<Node<K>>() {
			@Override
			protected Node<K> run() throws Pausable, Exception {
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
		}.submit();
	}
	
	@Override IResult<Boolean> removeA(final K key) {
		return new ContrailTask<Boolean>() {
			@Override
			protected Boolean run() throws Pausable, Exception {
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
		}.submit();
	}
	
	
	/**
	 * Deletes this node and all children
	 * @throws IOException, Pausable 
	 */
	@Override public void delete() throws IOException, Pausable {
		try {
			TaskTracker deletes= new TaskTracker();
			for (int i= _size; 0 < i--;) {
				deletes.track(new ContrailAction() {
					@Override protected void action() throws Pausable, IOException {
						Node<?> childBPage = getStorage().fetch(id);
						childBPage.delete();
						
				}}.submit());
			}
			deletes.await();
			
			InnerNode.super.delete();
			
		} 
		catch (Exception e) {
			TaskUtils.throwSomething(e, IOException.class);
		}
	}

	/**
	 * Dump this node and all children. 
	 */
	void dump(PrintStream out, int depth) throws Pausable {
		super.dump(out, depth);
		for (int i = 0; i < _size; i++) {
			Node<K> child = getChildNode(i);
			child.dump(out, depth+1);
		}
	}
	
	

	public static final Serializer<InnerNode> SERIALIZER= new Serializer<InnerNode>() {
		private final String typeCode= InnerNode.class.getName();
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
		public String typeCode() {
			return typeCode;
		}
	};
	
}
