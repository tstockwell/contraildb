package com.googlecode.contraildb.core.impl;

import java.io.IOException;
import java.io.Serializable;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.impl.btree.BTree;
import com.googlecode.contraildb.core.impl.btree.IForwardCursor;
import com.googlecode.contraildb.core.impl.btree.IOrderedSetCursor;
import com.googlecode.contraildb.core.impl.btree.IOrderedSetCursor.Direction;
import com.googlecode.contraildb.core.impl.btree.KeyValueSet;
import com.googlecode.contraildb.core.storage.IEntity;
import com.googlecode.contraildb.core.utils.Handler;
import com.googlecode.contraildb.core.utils.Immediate;
import com.googlecode.contraildb.core.utils.InvocationHandler;
import com.googlecode.contraildb.core.utils.TaskUtils;


/**
 * Contrail creates an index for every unique property name. 
 * The index maps property values to the identifiers of the entities that contain that property. 
 * If there is more that one entity for a given property value then
 * instead the index instead contains the id of another index that lists all the entities.
 * 
 * When iterating through an index, identifiers associated with the same
 * property value are always returned in order (the order defined by the
 * Identifier class itself, which is the lexicographical order of the Identifier
 * as a String).
 * 
 * @author ted stockwell
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class PropertyIndex<K extends Comparable<K> & Serializable>
{
	
	KeyValueSet<K, Identifier> _btree;
	
	static final Identifier __indexRoot= Identifier.create("net/sf/contrail/core/indexes/sets");
	
	@Immediate public PropertyIndex(KeyValueSet<K, Identifier> btree) throws IOException {
		_btree= btree;
	}


	synchronized public IResult<Void> insert(final K key, final Identifier document) throws IOException {
		return new InvocationHandler<Identifier>(_btree.cursor(Direction.FORWARD).find(key)) {
			protected IResult onSuccess(Identifier found) throws Exception {
				final Identifier value= found;
				if (value == null) { 
					// the key has no value currently associated with it, just store the identifier
					spawn(_btree.insert(key, document));
					return TaskUtils.DONE;
				} 
				
				if (__indexRoot.isAncestorOf(value)) {
					// the key has more than one value currently associated with it, 
					// add the identifier to the list
					return new InvocationHandler<IEntity>(_btree.getStorage().fetch(value)) {
						protected IResult onSuccess(IEntity item) throws Exception {
							BTree<Identifier> set= (BTree<Identifier>) item;
							spawn(set.insert(document));
							return TaskUtils.DONE;
						}
					};
				}

				// the key has a single value currently associated with it
				// create a set index and add the two values
				final Identifier setId= Identifier.create(__indexRoot);
				return new Handler(KeyValueSet.create(_btree.getStorage(), setId)) {
					protected IResult onSuccess() throws Exception {
						KeyValueSet set= (KeyValueSet) incoming().getResult();
						spawn(set.insert(value));
						spawn(set.insert(document));
						spawn(_btree.insert(key, setId));
						return TaskUtils.DONE;
					}
				};
			}
		};
	}


	synchronized public IPropertyCursor<K> cursor(Direction direction) 
	{
		return new PropertyCursorImpl(_btree, direction) {
			private  IResult<IForwardCursor<Identifier>> toIterable(Identifier id)  {
				if (id == null)
					return TaskUtils.asResult(new IOrderedSetCursor.EmptyForwardCursor<Identifier>());
				if (__indexRoot.isAncestorOf(id)) {
					return new Handler(_btree.getStorage().fetch(id)) {
						protected IResult onSuccess() throws Exception {
							BTree<Identifier> tree= (BTree<Identifier>) incoming().getResult();
							return asResult(tree.forwardCursor());
						}
					};
				}
				return TaskUtils.asResult(new IOrderedSetCursor.SingleValueCursor<Identifier>(id));
			}
			@Override
			public IResult<IForwardCursor<Identifier>> elementValue() {
				return new Handler(super.elementValue()) {
					protected IResult onSuccess() throws Exception {
						return toIterable((Identifier)incoming().getResult());
					}
				};
			}
		};
	}


	synchronized public IResult<Void> remove(final K key, final Identifier document) {
		return new Handler(_btree.cursor(Direction.FORWARD).find(key)) {
			protected IResult onSuccess() throws Exception {
				Identifier value= (Identifier) incoming().getResult();
				if (value == null)
					return TaskUtils.DONE;
				if (__indexRoot.isAncestorOf(value)) {
					return new Handler(_btree.getStorage().fetch(value)) {
						protected IResult onSuccess() throws Exception {
							final BTree<Identifier> set= (BTree<Identifier>) incoming().getResult();
							return new Handler(set.remove(document)) {
								protected IResult onSuccess() throws Exception {
									if (set.isEmpty()) {
										spawn(_btree.remove(key));
										spawn(set.delete());
									}
									return TaskUtils.DONE;
								}
							};
						}
					};
				}
				
				if (value.equals(document)) 
					spawn(_btree.remove(key));
				return TaskUtils.DONE;
			}
		};
	}
}
