package com.googlecode.contraildb.core.impl.btree;

import java.io.DataInput;
import java.io.IOException;
import java.io.PrintStream;
import java.util.UUID;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.storage.Entity;
import com.googlecode.contraildb.core.storage.IEntity;
import com.googlecode.contraildb.core.utils.ConditionalHandler;
import com.googlecode.contraildb.core.utils.ExternalizationManager;
import com.googlecode.contraildb.core.utils.ExternalizationManager.Serializer;
import com.googlecode.contraildb.core.utils.Handler;
import com.googlecode.contraildb.core.utils.Immediate;
import com.googlecode.contraildb.core.utils.InvocationAction;
import com.googlecode.contraildb.core.utils.InvocationHandler;
import com.googlecode.contraildb.core.utils.TaskUtils;



/**
 * Leaf nodes
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class Node<K extends Comparable<?>>
extends Entity 
implements Cloneable
{
	final static long serialVersionUID = 1L;

	transient BPlusTree<K,?> _index;
	protected Identifier _indexId;
	protected K[] _keys;
	protected Object[] _values;
	protected int _size; // the # of values in the node
	protected Identifier _previous;
	protected Identifier _next;
	
	@Immediate Node(BPlusTree<K,?> btree)  
	{
		super(Identifier.create(btree.getId(), UUID.randomUUID().toString()));
		_index = btree;
		_indexId = btree.getId();
		_size = 0;
		_keys = (K[]) new Comparable[_index._pageSize];
		_values = new Object[_index._pageSize];
	}
	
	protected Node() { }
	
	@Immediate Node<K> clone(Node<K> node) { return  new Node<K>(node._index); }
	@Immediate boolean isLeaf() { return true; }  
	@Immediate public BPlusTree<K,?> getIndex() { return _index; }
	
	IResult<Node<K>> getNextSibling() { 
		if (_next == null) 
			return null; 
		return getStorage().fetch(_next); 
	}
	
	IResult<K> getLookupKey() { 
		if (_next == null) 
			return TaskUtils.asResult(getLargestKey()); 
		return new InvocationHandler<Node<K>>(getNextSibling()) {
			protected IResult onSuccess(Node<K> node) throws Exception {
				return asResult(node.getSmallestKey());
			}
		}; 
	}		

	public IResult<Void> onLoad(Identifier identifier)
	{
		return new InvocationAction<IEntity>(storage.fetch(_indexId)) {
			protected void onSuccess(IEntity index) throws Exception {
				_index= (BPlusTree<K, ?>) index;
			}
		};
	}

	@Immediate K getSmallestKey() { return _keys[0]; }
	@Immediate K getLargestKey() { return _keys[_size-1]; }
	@Immediate boolean isEmpty() { return _size <= 0; }
	@Immediate boolean isFull() { return _index._pageSize <= _size; }
	
	/**
	 * Insert the given key and value.
	 * @return new right sibling node if the key was inserted and provoked an overflow.
	 */
	public IResult<Node<K>> insert(final K key, final Object value) throws IOException {
		return new Handler() {
			protected IResult onSuccess() throws Exception {
				final int index = indexOf(key);
				IResult<Node<K>> overflow= TaskUtils.NULL;

				if (index < _size && BPlusTree.compare(key, _keys[index]) == 0) { // key already exists
					_values[index] = value;
				}
				else if (!isFull()) {
					insertEntry(index, key, value);
				}
				else { // page is full, we must divide the page
					overflow= new InvocationHandler<Node<K>>(split()) {
						protected IResult onSuccess(Node<K> overflow) throws Exception {
							if (index <= _size) {
								insertEntry(index, key, value);
							}
							else
								overflow.insertEntry(index-_size, key, value);
								
							spawn(getStorage().store(overflow));
							return asResult(overflow);
						}
					};
				}
				
				return overflow;
			}
			protected IResult lastly() throws Exception {
				return update();
			}
		};
	}

	/**
	 * Split this node into two and return the new node
	 * @throws IOException 
	 */
	IResult<Node<K>> split() throws IOException {
		return new Handler() {
			protected IResult onSuccess() throws Exception {
				int half = _index._pageSize >> 1;
				final Node<K> overflow= new Node<K>(_index); // clone this node
				overflow._size= _size-half;
				System.arraycopy(_keys, half, overflow._keys, 0, overflow._size);
				System.arraycopy(_values, half, overflow._values, 0, overflow._size);
				_size= half;
				
				for (int i= _keys.length; _size < i--;) {
					_keys[i]= null;
					_values[i]= null;
				}
				
				// link newly created node
				overflow._next = _next;
				overflow._previous = getId();
				IResult checkNext= new ConditionalHandler(_next != null) {
					protected IResult onTrue() throws Exception {
						return new InvocationHandler<IEntity>(getStorage().fetch(_next)) {
							protected IResult onSuccess(IEntity results) throws Exception {
								Node<?> next = (Node<?>) results;
								next._previous = overflow.getId();
								return next.update();
							}
						};
					}
				};
				return new Handler(checkNext) {
					protected IResult onSuccess() throws Exception {
						_next= overflow.getId();
						
						spawn(getStorage().store(overflow));
						
						return asResult(overflow);
					}
				};
			}
		};
	}

	/**
	 * Merge the given node into this node.
	 */
	IResult<Void> merge(Node<K> rightSibling) {
		if (_index._pageSize < _size + rightSibling._size)
			throw new IllegalStateException("Combined node size exceeds index page size");

		System.arraycopy(rightSibling._keys, 0, _keys, _size, rightSibling._size);
		System.arraycopy(rightSibling._values, 0, _values, _size, rightSibling._size);
		_size+= rightSibling._size;
		
		// link newly created node
		if ((_next = rightSibling._next) != null) {
			return new InvocationHandler<IEntity>(getStorage().fetch(_next)) {
				protected IResult onSuccess(IEntity entity) throws Exception {
					Node<?> next = (Node<?>) entity;
					next._previous = getId();
					return next.update();
				}
			}; 
		}
		return TaskUtils.DONE;
	}
	
	/**
	 * @return
	 * 		The index of the element associated with the first key that is >= the given key.
	 * 		For leaf nodes this is the index of the first element that is >= to the given key.
	 * 		For inner nodes this is the index of the first key that is > the given key   
	 */
	@Immediate int indexOf(K key) {
		int left = 0;
		int right = _size - 1;

		// binary search
		while (left <= right) {
			int middle = (left + right) / 2;
			int i= BPlusTree.compare(_keys[middle], key);
			if (i == 0)
				return middle;
			if (i < 0) {
				left = middle + 1;
			} else {
				right = middle - 1;
			}
		}
		return left;
	}
	
	@Immediate void insertEntry(int index, K key, Object value) {
		System.arraycopy(_keys, index, _keys, index+1, _size-index);
		_keys[index] = key;
		System.arraycopy(_values, index, _values, index+1, _size-index);
		_values[index] = value;
		_size++;
	}

	@Immediate void removeEntry(int index) {
		_size--;
		if (index < _size) {
			System.arraycopy(_keys, index+1, _keys, index, _size-index);
			System.arraycopy(_values, index+1, _values, index, _size-index);
		}
		_keys[_size]= null;
		_values[_size]= null;
	}
	
	@Immediate void setEntry(int index, K key, Object value) {
		_keys[index] = key;
		_values[index] = value;
	}
	
	/**
	 * Remove the entry associated with the given key.
	 * @return true if underflow
	 */
	IResult<Boolean> remove(final K key) {
		IResult remove= new Handler() {
			protected IResult onSuccess() throws Exception {
				int index = indexOf(key);
				if (index < _size) {
					if (BPlusTree.compare(_keys[index], key) == 0) {
						removeEntry(index);
						return update();
					}
				}
				return TaskUtils.DONE;
			}
		};
		return new Handler(remove) {
			protected IResult onSuccess() throws Exception {
				return asResult(_size < _index._pageSize / 2);
			}
		};
	}

	@Immediate void dump(PrintStream out, int depth) throws IOException {
		String prefix = "";
		for (int i = 0; i < depth; i++) {
			prefix += "    ";
		}
		out.println(prefix + "------------------ Node id=" + getId()+", size="+_size);
		for (int i = 0; i < _size; i++) 
				out.println(prefix + i+": [" + _keys[i] + "] " + _values[i]);
	}

	IResult<Node<K>> getChildNode(int index) {
		assert 0 < index;
		return getStorage().fetch((Identifier)_values[index]);
	}


	public static final Serializer<Node> SERIALIZER= new Serializer<Node>() {
		private final int typeCode= Node.class.getName().hashCode();
		public Node<?> readExternal(java.io.DataInput in) 
		throws IOException {
			Node<?> journal= new Node();
			readExternal(in, journal);
			return journal;
		};
		public void writeExternal(java.io.DataOutput out, Node node) 
		throws IOException {
			Entity.SERIALIZER.writeExternal(out, node);
			
			ExternalizationManager.writeExternal(out, node._indexId, Identifier.SERIALIZER);
			ExternalizationManager.writeExternal(out, node._previous, Identifier.SERIALIZER);
			ExternalizationManager.writeExternal(out, node._next, Identifier.SERIALIZER);
			out.writeInt(node._size);
			out.writeInt(node._keys.length);
			for (int i= 0; i < node._size; i++) {
				ExternalizationManager.writeExternal(out, node._keys[i]);
				ExternalizationManager.writeExternal(out, node._values[i]);
			}
		};
		public void readExternal(DataInput in, Node node)
		throws IOException {
			Entity.SERIALIZER.readExternal(in, node);
			
			node._indexId= ExternalizationManager.readExternal(in, Identifier.SERIALIZER);
			node._previous= ExternalizationManager.readExternal(in, Identifier.SERIALIZER);
			node._next= ExternalizationManager.readExternal(in, Identifier.SERIALIZER);
			node._size= in.readInt();
			int pageSize= in.readInt();
			node._keys = new Comparable[pageSize];
			node._values = new Object[pageSize];
			for (int i= 0; i < node._size; i++) {
				node._keys[i]= ExternalizationManager.readExternal(in);
				node._values[i]= ExternalizationManager.readExternal(in);
			}
		}
		public int typeCode() {
			return typeCode;
		}
	};
	
}
