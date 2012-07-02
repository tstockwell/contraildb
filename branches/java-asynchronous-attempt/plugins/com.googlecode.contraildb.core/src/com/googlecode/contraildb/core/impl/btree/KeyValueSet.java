package com.googlecode.contraildb.core.impl.btree;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.PrintStream;
import java.util.NoSuchElementException;

import com.googlecode.contraildb.core.ContrailException;
import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.async.ConditionalHandler;
import com.googlecode.contraildb.core.async.Handler;
import com.googlecode.contraildb.core.async.IAsyncerator;
import com.googlecode.contraildb.core.async.Immediate;
import com.googlecode.contraildb.core.async.TaskUtils;
import com.googlecode.contraildb.core.impl.btree.IOrderedSetCursor.Direction;
import com.googlecode.contraildb.core.storage.Entity;
import com.googlecode.contraildb.core.storage.IEntityStorage;
import com.googlecode.contraildb.core.utils.ExternalizationManager;
import com.googlecode.contraildb.core.utils.ExternalizationManager.Serializer;


//TODO This class must be extended to support NULL values

/**
 * A persistent, map of key/value pairs, ordered by keys.
 * Allows insertions, deletions, searches, and sequential access.
 * 
 * Uses a B+tree structure. 
 * @see http://en.wikipedia.org/wiki/B+tree
 * 
 * @author Ted Stockwell
 * 
 * @param T The type of objects stored in the BTree
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class KeyValueSet<T extends Comparable, V> 
extends Entity
implements IKeyValueSet<T,V>
//implements Iterable<T>
{
	static class ForwardCursor<K extends Comparable> 
	extends CursorImpl<K, Object>
	implements IForwardCursor<K>
	{
		public ForwardCursor(KeyValueSet index) {
			super(index, Direction.FORWARD);
		}
	}


	private static final long serialVersionUID = 1L;
	private static final String NO_VALUE= "__no_value"; 

	/**
	 * Default page size (number of entries per node)
	 */
	public static final int DEFAULT_SIZE = 200;
	
	/**
	 * @return 0 if c1 == c2; 
	 * 			<0 if c1 is smallest; 
	 * 			>0 if c2 is smallest  
	 */
	public static final <T extends Comparable> int compare(T c1, T c2) {
		if (c1 == null) 
			return c2 == null ? 0 : -1;
		if (c2 == null)
			return 1;
		return c1.compareTo(c2);
	}
	
	
	/**
	 * root node id.
	 */
	private Identifier _rootId;

	/**
	 * Number of entries in each BPage.
	 */
	int _pageSize;
	
	/**
	 * If false then the tree is a B-Tree with no leaf values else it is a B+Tree
	 */
	private boolean _hasLeafValues;

	private transient Node<T> _root;

	/**
	 * Create a new persistent index.
	 */
	private static final <K extends Comparable, V> IResult<KeyValueSet<K,V>> 
	create(IEntityStorage.Session storageSession, Identifier id, int pageSize, boolean hasLeafValues) 
	{
		final KeyValueSet<K,V> btree= new KeyValueSet<K,V>(id, pageSize, hasLeafValues);
		return new Handler(storageSession.store(btree)) {
			protected IResult onSuccess() throws Exception {
				return asResult(btree);
			}
		};
	}
	public static <K extends Comparable, V> IResult<KeyValueSet<K,V>> 
	create( IEntityStorage.Session storageSession, int pageSize) 
	{
		return create(storageSession, Identifier.create(), pageSize, true);
	}
	public static <K extends Comparable, V> IResult<KeyValueSet<K,V>> 
	create(IEntityStorage.Session storageSession, Identifier identifier) 
	{
		return create(storageSession, identifier, DEFAULT_SIZE, true);
	}
	public static <K extends Comparable, V> IResult<KeyValueSet<K,V>> create(
	IEntityStorage.Session storageSession) 
	{
		return create(storageSession, Identifier.create(), DEFAULT_SIZE, true);
	}

	@Immediate protected KeyValueSet(Identifier identifier, int pageSize, boolean hasLeafValues) {
		super(identifier);
		if ((pageSize & 1) != 0)
			throw new IllegalArgumentException( "Page size' must be even");
		_pageSize = pageSize;
		_hasLeafValues= hasLeafValues;
	}
	
	protected KeyValueSet() { }

	@Override
	public IResult<Void> onLoad(Identifier identifier)
	{
		final IResult fetch= storage.fetch(_rootId);
		return new Handler(super.onLoad(identifier)) {
			protected IResult onSuccess() throws Exception {
				_root= (Node<T>) fetch.getResult();
				return TaskUtils.DONE;
			}
		}.toResult();
	}

	@Override
	public IResult<Void> onInsert(Identifier identifier)
	{
		final IResult store= _root != null ? storage.store(_root) : TaskUtils.DONE;
		return new Handler(super.onInsert(identifier)) {
			protected IResult onSuccess() throws Exception {
				return store;
			}
		}.toResult();
	}

	@Override
	public IResult<Void> onDelete()
	{
		final IResult delete= _root != null ? _root.delete() : TaskUtils.DONE;
		return new Handler(super.onDelete()) {
			protected IResult onSuccess() throws Exception {
				return delete;
			}
		};
	}

	/**
	 * Insert an entry in the BTree.
	 */
	public synchronized IResult<Void> insert(T key) {
		return insert(key, (V) NO_VALUE);
	}
	public synchronized IResult<Void> insert(final IResult<T> key) {
		return new Handler(key) {
			protected IResult onSuccess() throws Exception {
				return insert(key.getResult());
			}
		};
	}
	
	/**
	 * Insert an entry in the BTree.
	 */
	public synchronized IResult<Void> insert(final T key, final V value) {
		if (key == null) 
			throw new IllegalArgumentException("Argument 'key' is null");

		// BTree is currently empty, create a new root BPage
		if (_root == null) {
			_root = new Node<T>(this);
			_rootId = _root.getId();
			return new Handler(getStorage().store(_root)) {
				protected IResult onSuccess() throws Exception {
					_root.insert(key, value);
					update();
					return TaskUtils.DONE;
				}
			};
		}

		final IResult<Node<T>> insert= _root.insert(key, value);
		return new Handler(insert) {
			protected IResult onSuccess() throws Exception {
				final Node<T> overflow= insert.getResult();
				if (overflow == null) 
					return TaskUtils.DONE;

				final InnerNode<T> newRoot= new InnerNode<T>(KeyValueSet.this);
				return new Handler(getStorage().store(newRoot)) {
					protected IResult onSuccess() throws Exception {
						if (_root.isLeaf()) {
							newRoot.insertEntry(0, overflow.getSmallestKey(), _root.getId());
						}
						else {
							newRoot.insertEntry(0, _root.getLargestKey(), _root.getId());
						}
						newRoot.insertEntry(1, overflow.getLargestKey(), overflow.getId());
						_root= newRoot;
						_rootId = newRoot.getId();
						return update();
					}
				};
			}
		};
	}
	public synchronized IResult<Void> insert(final IResult<T> key, final IResult<V> value) {
		return new Handler(key, value) {
			protected IResult onSuccess() throws Exception {
				return insert(key.getResult(), value.getResult());
			}
		};
	}

	/**
	 * Remove an entry with the given key from the BTree.
	 */
	public synchronized IResult<Void> remove(final T key) {
		return new Handler() {
			protected IResult onSuccess() throws Exception {
				if (key == null) 
					throw new IllegalArgumentException("Argument 'key' is null");

				if (_root == null)
					return TaskUtils.DONE;

				return new ConditionalHandler(_root.remove(key)) {
					// underflow
					protected IResult onTrue() throws Exception {
						return new ConditionalHandler(_root.isEmpty()) {
							protected IResult onTrue() throws Exception {
								IResult delete= _root.delete();
								_root = null;
								return delete;
							}
						};
					}

					protected IResult lastly() throws Exception {
						return update();
					}
				};
			}
		};
	}
	public IResult<Void> remove(final IResult<T> key) {
		return new Handler(key) {
			protected IResult onSuccess() throws Exception {
				return remove(key.getResult());
			}
		};
	}

	public IAsyncerator<T> iterator() {
		final IOrderedSetCursor<T> navigator= cursor(Direction.FORWARD);
		return new IAsyncerator<T>() {
			public IResult<Boolean> hasNext() {
				return new Handler(navigator.hasNext()) {
					protected void onComplete() throws Exception {
						if (incoming().getError() != null)
							throw new ContrailException("Error navigating index", incoming().getError());
					}
					protected IResult onSuccess() throws Exception {
						return incoming();
					}
				};
			}
			public IResult<T> next() {
				return new Handler(navigator.next()) {
					protected void onComplete() throws Exception {
						if (incoming().getError() != null)
							throw new ContrailException("Error navigating index", incoming().getError());
					}
					protected IResult onSuccess() throws Exception {
						if (!(Boolean)incoming().getResult())
							throw new NoSuchElementException();
						return asResult(navigator.keyValue());
					}
				};
			}
			@Override
			public IResult<Void> remove() {
				return new Handler() {
					protected IResult onSuccess() throws Exception {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	/**
	 * Deletes all BPages in this BTree, then deletes the tree from the record
	 * manager
	 */
	public synchronized IResult<Void> delete() {
		return new Handler(super.onDelete()) {
			protected IResult onSuccess() throws Exception {
				return new ConditionalHandler(_root != null) {
					protected IResult onTrue() throws Exception {
						return _root.delete();
					}
				};
			}
			protected IResult lastly() throws Exception {
				_root= null;
				return TaskUtils.DONE;
			}
		};
	}

	/**
	 * Return the size of a node in the btree.
	 */
	@Immediate public int getPageSize() {
		return _pageSize;
	}

	@Immediate protected Node<T> getRoot() {
		return _root;
	}

	@Immediate public void dump(PrintStream out) throws IOException {
		Node<T> root = getRoot();
		if (root != null) 
			root.dump(out, 0);
	}

	@Immediate public boolean isEmpty() {
		if (_root == null)
			return true;
		return _root.isEmpty();
	}
	
	public IKeyValueCursor<T, V> cursor(Direction direction) {
		return new CursorImpl(this, direction);
	}
	
	
	@Override
	public String toString() {
		try {
			ByteArrayOutputStream bout= new ByteArrayOutputStream();
			dump(new PrintStream(bout));
			return bout.toString();
		}
		catch (IOException x) {
			// not gonna happen
		}
		return super.toString();
	}


	public IForwardCursor<T> forwardCursor() {
		return new ForwardCursor<T>(this);
	}


	public static final Serializer<KeyValueSet> SERIALIZER= new Serializer<KeyValueSet>() {
		private final int typeCode= KeyValueSet.class.getName().hashCode();
		@Override public KeyValueSet readExternal(java.io.DataInput in) 
		throws IOException {
			KeyValueSet journal= new KeyValueSet();
			readExternal(in, journal);
			return journal;
		};
		@Override public void writeExternal(java.io.DataOutput out, KeyValueSet journal) 
		throws IOException {
			Entity.SERIALIZER.writeExternal(out, journal);
			ExternalizationManager.writeExternal(out, journal._rootId, Identifier.SERIALIZER);
			out.writeInt(journal._pageSize);
		};
		@Override public void readExternal(DataInput in, KeyValueSet journal)
		throws IOException {
			Entity.SERIALIZER.readExternal(in, journal);
			journal._rootId= ExternalizationManager.readExternal(in, Identifier.SERIALIZER);
			journal._pageSize= in.readInt();
		}
		@Override public int typeCode() {
			return typeCode;
		}
	};
	
}
