package com.googlecode.contraildb.core.impl.btree;

import java.io.DataInput;
import java.io.IOException;
import java.io.PrintStream;
import java.util.UUID;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.storage.Entity;
import com.googlecode.contraildb.core.storage.StorageUtils;
import com.googlecode.contraildb.core.utils.ExternalizationManager;
import com.googlecode.contraildb.core.utils.ExternalizationManager.Serializer;



/**
 * Leaf nodes
 */
@SuppressWarnings("unchecked")
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
	
	Node(BPlusTree<K,?> btree)  
	{
		super(Identifier.create(btree.getId(), UUID.randomUUID().toString()));
		_index = btree;
		_indexId = btree.getId();
		_size = 0;
		_keys = (K[]) new Comparable[_index._pageSize];
		_values = new Object[_index._pageSize];
	}
	
	protected Node() { }
	
	Node<K> clone(Node<K> node) { return  new Node<K>(node._index); }
	boolean isLeaf() { return true; }  
	public BPlusTree<K,?> getIndex() { return _index; }
	Node<K> getNextSibling() throws IOException { if (_next == null) return null; return StorageUtils.syncFetch(getStorage(), _next); }
	K getLookupKey() throws IOException { if (_next == null) return getLargestKey(); return getNextSibling().getSmallestKey(); }		

	public void onLoad(Identifier identifier)
	throws IOException 
	{
		super.onLoad(identifier);
		_index= StorageUtils.syncFetch(storage, _indexId);
	}

	K getSmallestKey() { return _keys[0]; }
	K getLargestKey() { return _keys[_size-1]; }
	boolean isEmpty() { return _size <= 0; }
	boolean isFull() { return _index._pageSize <= _size; }
	
	/**
	 * Insert the given key and value.
	 * @return new right sibling node if the key was inserted and provoked an overflow.
	 */
	public Node<K> insert(K key, Object value) throws IOException {
		int index = indexOf(key);
		Node<K> overflow= null;

		if (index < _size && BPlusTree.compare(key, _keys[index]) == 0) { // key already exists
			_values[index] = value;
		}
		else if (!isFull()) {
			insertEntry(index, key, value);
		}
		else { // page is full, we must divide the page
			overflow= split();

			if (index <= _size) {
				insertEntry(index, key, value);
			}
			else
				overflow.insertEntry(index-_size, key, value);
				
			getStorage().store(overflow);
		}
		
		update();
		return overflow;
	}

	/**
	 * Split this node into two and return the new node
	 * @throws IOException 
	 */
	Node<K> split() throws IOException {
		int half = _index._pageSize >> 1;
		Node<K> overflow= clone(this);
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
		if (_next != null) {
			Node<?> next = StorageUtils.syncFetch(getStorage(), _next);
			next._previous = overflow.getId();
			next.update();
		}
		_next= overflow.getId();
		
		getStorage().store(overflow);
		
		return overflow;
	}

	/**
	 * Merge the given node into this node.
	 */
	void merge(Node<K> rightSibling) throws IOException {
		if (_index._pageSize < _size + rightSibling._size)
			throw new IllegalStateException("Combined node size exceeds index page size");

		System.arraycopy(rightSibling._keys, 0, _keys, _size, rightSibling._size);
		System.arraycopy(rightSibling._values, 0, _values, _size, rightSibling._size);
		_size+= rightSibling._size;
		
		// link newly created node
		if ((_next = rightSibling._next) != null) {
			Node<?> next = StorageUtils.syncFetch(getStorage(), _next);
			next._previous = getId();
			next.update();
		}
	}
	
	/**
	 * @return
	 * 		The index of the element associated with the first key that is >= the given key.
	 * 		For leaf nodes this is the index of the first element that is >= to the given key.
	 * 		For inner nodes this is the index of the first key that is > the given key   
	 */
	int indexOf(K key) {
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
	

	void insertEntry(int index, K key, Object value) {
		System.arraycopy(_keys, index, _keys, index+1, _size-index);
		_keys[index] = key;
		System.arraycopy(_values, index, _values, index+1, _size-index);
		_values[index] = value;
		_size++;
	}

	void removeEntry(int index) {
		_size--;
		if (index < _size) {
			System.arraycopy(_keys, index+1, _keys, index, _size-index);
			System.arraycopy(_values, index+1, _values, index, _size-index);
		}
		_keys[_size]= null;
		_values[_size]= null;
	}
	
	void setEntry(int index, K key, Object value) {
		_keys[index] = key;
		_values[index] = value;
	}
	
	/**
	 * Remove the entry associated with the given key.
	 * @return true if underflow
	 */
	boolean remove(K key) throws IOException {
		int index = indexOf(key);
		if (index < _size) {
			if (BPlusTree.compare(_keys[index], key) == 0) {
				removeEntry(index);
				update();
			}
		}
		return _size < _index._pageSize / 2;
	}

	void dump(PrintStream out, int depth) throws IOException {
		String prefix = "";
		for (int i = 0; i < depth; i++) {
			prefix += "    ";
		}
		out.println(prefix + "------------------ Node id=" + getId()+", size="+_size);
		for (int i = 0; i < _size; i++) 
				out.println(prefix + i+": [" + _keys[i] + "] " + _values[i]);
	}

	Node<K> getChildNode(int index) throws IOException {
		assert 0 < index;
		return StorageUtils.syncFetch(getStorage(), (Identifier)_values[index]);
	}


	@SuppressWarnings("rawtypes")
	public static final Serializer<Node> SERIALIZER= new Serializer<Node>() {
		private final String typeCode= Node.class.getName();
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
		public String typeCode() {
			return typeCode;
		}
	};
	
}
