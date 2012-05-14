package com.googlecode.contraildb.core.impl.btree;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.PrintStream;
import java.util.NoSuchElementException;

import com.googlecode.contraildb.core.ContrailException;
import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.impl.btree.IBTreeCursor.Direction;
import com.googlecode.contraildb.core.storage.Entity;
import com.googlecode.contraildb.core.storage.IEntityStorage;
import com.googlecode.contraildb.core.utils.ConditionalHandler;
import com.googlecode.contraildb.core.utils.ExternalizationManager;
import com.googlecode.contraildb.core.utils.ExternalizationManager.Serializer;
import com.googlecode.contraildb.core.utils.Handler;
import com.googlecode.contraildb.core.utils.Immediate;
import com.googlecode.contraildb.core.utils.ResultIterator;
import com.googlecode.contraildb.core.utils.TaskUtils;


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
public class BPlusTree<T extends Comparable, V> 
extends Entity
//implements Iterable<T>
{
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
	private static final <K extends Comparable, V> IResult<BPlusTree<K,V>> createInstance(
	IEntityStorage.Session storageSession, Identifier id, int pageSize, boolean hasLeafValues) 
	throws IOException 
	{
		final BPlusTree<K,V> btree= new BPlusTree<K,V>(id, pageSize, hasLeafValues);
		return new Handler(storageSession.store(btree)) {
			protected IResult onSuccess() throws Exception {
				return asResult(btree);
			}
		};
	}
	public static <K extends Comparable, V> IResult<BPlusTree<K,V>> createInstance(
	IEntityStorage.Session storageSession, int pageSize) 
	throws IOException 
	{
		return createInstance(storageSession, Identifier.create(), pageSize, true);
	}
	public static <K extends Comparable, V> IResult<BPlusTree<K,V>> createBPlusTree(
	IEntityStorage.Session storageSession, Identifier identifier) 
	throws IOException 
	{
		return createInstance(storageSession, identifier, DEFAULT_SIZE, true);
	}
	public static <K extends Comparable, V> IResult<BPlusTree<K,V>> createInstance(
	IEntityStorage.Session storageSession) 
	throws IOException 
	{
		return createInstance(storageSession, Identifier.create(), DEFAULT_SIZE, true);
	}

	@Immediate protected BPlusTree(Identifier identifier, int pageSize, boolean hasLeafValues) throws IOException {
		super(identifier);
		if ((pageSize & 1) != 0)
			throw new IllegalArgumentException( "Page size' must be even");
		_pageSize = pageSize;
		_hasLeafValues= hasLeafValues;
	}
	
	protected BPlusTree() { }

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
		}.toResult();
	}

	/**
	 * Insert an entry in the BTree.
	 */
	public synchronized IResult<Void> insert(T key) throws IOException {
		return insert(key, (V) NO_VALUE);
	}
	
	/**
	 * Insert an entry in the BTree.
	 */
	public synchronized IResult<Void> insert(final T key, final V value) throws IOException {
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

				final InnerNode<T> newRoot= new InnerNode<T>(BPlusTree.this);
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

	/**
	 * Remove an entry with the given key from the BTree.
	 */
	public synchronized IResult<Void> remove(final T key) throws IOException {
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

	public ResultIterator<T> iterator() {
		final IBTreeCursor<T> navigator= cursor(Direction.FORWARD);
		return new ResultIterator<T>() {
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

	@Immediate public boolean isEmpty() throws IOException {
		if (_root == null)
			return true;
		return _root.isEmpty();
	}
	
	public IBTreePlusCursor<T, V> cursor(Direction direction) {
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


	public static final Serializer<BPlusTree> SERIALIZER= new Serializer<BPlusTree>() {
		private final int typeCode= BPlusTree.class.getName().hashCode();
		public BPlusTree readExternal(java.io.DataInput in) 
		throws IOException {
			BPlusTree journal= new BPlusTree();
			readExternal(in, journal);
			return journal;
		};
		public void writeExternal(java.io.DataOutput out, BPlusTree journal) 
		throws IOException {
			Entity.SERIALIZER.writeExternal(out, journal);
			ExternalizationManager.writeExternal(out, journal._rootId, Identifier.SERIALIZER);
			out.writeInt(journal._pageSize);
		};
		public void readExternal(DataInput in, BPlusTree journal)
		throws IOException {
			Entity.SERIALIZER.readExternal(in, journal);
			journal._rootId= ExternalizationManager.readExternal(in, Identifier.SERIALIZER);
			journal._pageSize= in.readInt();
		}
		public int typeCode() {
			return typeCode;
		}
	};
	
}
