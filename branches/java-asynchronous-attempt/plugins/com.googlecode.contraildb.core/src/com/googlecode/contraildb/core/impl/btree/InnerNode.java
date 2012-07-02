package com.googlecode.contraildb.core.impl.btree;

import java.io.DataInput;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.async.ConditionalHandler;
import com.googlecode.contraildb.core.async.Handler;
import com.googlecode.contraildb.core.async.Immediate;
import com.googlecode.contraildb.core.async.ResultHandler;
import com.googlecode.contraildb.core.async.TaskUtils;
import com.googlecode.contraildb.core.utils.ExternalizationManager.Serializer;




/**
 * An inner node in a BTree
 */
@SuppressWarnings({"rawtypes","unchecked"})
public class InnerNode<K extends Comparable>
extends Node<K>
{
	final static long serialVersionUID = 1L;
	
	public InnerNode(KeyValueSet<K,?> btree) {
		super(btree);
	}
	
	protected InnerNode() { }

	@Immediate @Override boolean isLeaf() { return false; }
	@Immediate @Override Node<K> clone(Node<K> node) { return  new InnerNode<K>(node._index); }
	@Override IResult<K> getLookupKey() { return TaskUtils.asResult(getLargestKey()); }		
	@Immediate @Override int indexOf(K key) { 
		int left = 0;
		int right = _size - 1;

		while (left <= right) {
			int middle = (left + right) / 2;
			int i= KeyValueSet.compare(_keys[middle], key);
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
	@Override public IResult<Node<K>> insert(final K key, final Object value) {
		final int index = indexOf(key);
		
		return new ResultHandler<Node<K>>(getChildNode(index)) {
			protected IResult onSuccess(final Node<K> child) throws Exception {
				
				return new ResultHandler<Node<K>>(child.insert(key, value)) {
					protected IResult onSuccess(final Node<K> newSibling) throws Exception {
						if (newSibling == null)  // no overflow means we're done with insertion
							return TaskUtils.NULL;
						
						// there was an overflow, we need to insert the overflow page
						final IResult<K> getKeyForChild= child.getLookupKey();
						final IResult<K> getKeyForSibling= newSibling.getLookupKey();
						return new Handler(getKeyForChild, getKeyForSibling) {
							protected IResult onSuccess() throws Exception {
								final K keyForChildNode= getKeyForChild.getResult();
								final K keyForSiblingNode= getKeyForSibling.getResult();
								
								final Identifier newSiblingId = newSibling.getId();
								
								final Identifier childId = child.getId();
								
								IResult<Node<K>> overflow= new ConditionalHandler(isFull()) {
									protected IResult onFalse() throws Exception {
										setEntry(index, keyForChildNode, childId);
										insertEntry(index+1, keyForSiblingNode, newSiblingId);
										return TaskUtils.NULL;
									}
									protected IResult onTrue() throws Exception {
										// page is full, we must divide the page
										return new ResultHandler<Node<K>>(split()) {
											protected IResult onSuccess(Node<K> overflow) throws Exception {
												if (index < _size) { 
													setEntry(index, keyForChildNode, childId);
													insertEntry(index+1, keyForSiblingNode, newSiblingId);
												}
												else {
													overflow.setEntry(index-_size, keyForChildNode, childId);
													overflow.insertEntry(index-_size+1, keyForSiblingNode, newSiblingId);
												}
												getStorage().store(overflow);
												return asResult(overflow);
											}
										};
									}
								};
								return overflow;
							}
							protected IResult lastly() throws Exception {
								return update();
							}
						};
						
					}
				};
			}
		};
	}
	
	@Override IResult<Boolean> remove(final K key) {

		final int index = indexOf(key);
		return new ResultHandler<Node<K>>(getChildNode(index)) {
			protected IResult onSuccess(final Node<K> child) throws Exception {
				return new ResultHandler<Boolean>(child.remove(key)) {
					@Override
					protected IResult onSuccess(Boolean underflow) throws Exception {
						if (!underflow)
							return TaskUtils.FALSE;
						
						IResult doRemove= TaskUtils.DONE;
						if (index < _size - 1) {
							doRemove= new ResultHandler<Node<K>>(getChildNode(index + 1)) {
								protected IResult onSuccess(Node<K> rightSibling) throws Exception {
									if (rightSibling._size+child._size <= _index._pageSize) {
										return new Handler(child.merge(rightSibling)) {
											protected IResult onSuccess() throws Exception {
												removeEntry(index);
												_values[index]= child.getId();
												return update();
											}
										};
									}
									return TaskUtils.DONE;
								}
							};
						}
						if (0 < index) {
							doRemove= new ResultHandler<Node<K>>(getChildNode(index - 1)) {
								protected IResult onSuccess(final Node<K> leftSibling) throws Exception {
									if (leftSibling._size+child._size <= _index._pageSize) {
										return new Handler(leftSibling.merge(child)) {
											protected IResult onSuccess() throws Exception {
												removeEntry(index-1);
												_values[index-1]= leftSibling.getId();
												return update();
											}
										};
									}
									return TaskUtils.DONE;
								}
							};
						}
						if (index == 0 && child.isEmpty()) {
							removeEntry(0);
							doRemove= update();
						}
						
						return new Handler(doRemove) {
							protected IResult onSuccess() throws Exception {
								return asResult(_size < _index._pageSize / 2);
							}
						};
					}
				};
			}
		};
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
			});
		}
		return new Handler(TaskUtils.combineResults(tasks)) {
			protected IResult onSuccess() throws Exception {
				return InnerNode.super.delete();
			}
		};
	}

	/**
	 * Dump this node and all children. 
	 */
	void dump(PrintStream out, int depth) throws IOException {
		super.dump(out, depth);
		for (int i = 0; i < _size; i++) {
			Node<K> child = getChildNode(i).get();
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
