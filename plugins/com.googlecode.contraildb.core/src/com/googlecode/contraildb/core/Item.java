package com.googlecode.contraildb.core;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.googlecode.contraildb.core.storage.Entity;
import com.googlecode.contraildb.core.utils.ConversionUtils;
import com.googlecode.contraildb.core.utils.ExternalizationManager;
import com.googlecode.contraildb.core.utils.ExternalizationManager.Serializer;


/**
 * An object that can be stored in a Contrail database.
 * All items have an identifier that denotes a unique path to the item's 
 * location in the database.
 * An Item is just a collection of properties.
 * Property values must be one of the following:
 * 	String,
 *  Date, 
 * 	Integer, Byte, Long, Float, Double, BigInteger, or BigDecimal
 * 	Boolean,
 * 	Identifier,
 * 	Item,
 *  List or Set, where items in collections are valid property values
 *  Map, where keys are valid scalar property values 
 *  	(that is, keys may NOT be Item, List, Set, or Map) 
 *  	and values are valid property values     
 * 
 * @author Ted Stockwell
 */
public class Item 
extends Entity
implements Cloneable
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * A reserved property name used to refer to the ID of an item. 
	 */
	public static final String KEY_ID= "__ID__";
	/**
	 * A reserved property name used to refer to the ID of an item. 
	 */
	public static final String KEY_KIND = "__KIND__";
	
	private TreeMap<String, Object> _indexedProperties= new TreeMap<String, Object>(); 
	private TreeMap<String, Object> _unindexedProperties= new TreeMap<String, Object>(); 
	
	
	public Item(Identifier path) {
		super(path);
		setProperty(KEY_KIND, getClass().getName());
	}
	
	public Item(String path) {
		this(Identifier.create(path));
	}
	protected Item() { }
	
	public Item(Item item) {
		super(item.getId());
		for (Map.Entry<String, Object> entry:item._indexedProperties.entrySet()) 
			_indexedProperties.put(entry.getKey(), entry.getValue());
		for (Map.Entry<String, Object> entry:item._unindexedProperties.entrySet()) 
			_unindexedProperties.put(entry.getKey(), entry.getValue());
	}
	public Item(Identifier parent, String child) {
		this(Identifier.create(parent, child));
	}
	
	public BigDecimal getBigDecimal(String propertyName) {
		return ConversionUtils.toBigDecimal(getProperty(propertyName));
	}

	public BigInteger getBigInteger(String propertyName) {
		return ConversionUtils.toBigInteger(getProperty(propertyName));
	}

	public boolean getBoolean(String propertyName) {
		return ConversionUtils.toBoolean(getProperty(propertyName));
	}

	public Item getItem(String propertyName) {
		return (Item)_indexedProperties.get(propertyName);
	}

	public byte getByte(String propertyName) {
		return ConversionUtils.toByte(getProperty(propertyName));
	}

	public Date getDate(String propertyName) {
		return ConversionUtils.toDate(getProperty(propertyName));
	}

	public double getDouble(String propertyName) {
		return ConversionUtils.toDouble(getProperty(propertyName));
	}

	public float getFloat(String propertyName) {
		return ConversionUtils.toFloat(getProperty(propertyName));
	}

	public int getInt(String propertyName) {
		return ConversionUtils.toInteger(getProperty(propertyName));
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> getList(String propertyName) {
		return (List<T>)getProperty(propertyName);
	}

	public long getLong(String propertyName) {
		return ConversionUtils.toLong(getProperty(propertyName));
	}

	@SuppressWarnings("unchecked")
	public <K, V> Map<K, V> getMap(String propertyName) {
		return (Map<K, V>)getProperty(propertyName);
	}

	public Map<String, Object> getAllProperties() {
		TreeMap<String, Object> map= new TreeMap<String, Object>(_indexedProperties);
		map.putAll(_unindexedProperties);
		return map;
	}

	public Map<String, Object> getUnindexedProperties() {
		return Collections.unmodifiableMap(_unindexedProperties);
	}

	public Map<String, Object> getIndexedProperties() {
		return Collections.unmodifiableMap(_indexedProperties);
	}

	@SuppressWarnings("unchecked")
	public <T> T getProperty(String propertyName) {
		T t= (T) _indexedProperties.get(propertyName);
		if (t == null)
			t= (T) _unindexedProperties.get(propertyName);
		return t;
	}

	@SuppressWarnings("unchecked")
	public <T> Set<T> getSet(String propertyName) {
		return (Set<T>)getProperty(propertyName);
	}

	public String getString(String propertyName) {
		return ConversionUtils.toString(getProperty(propertyName));
	}

	public boolean hasProperty(String propertyName) {
		if (_indexedProperties.containsKey(propertyName))
			return true;
		return _unindexedProperties.containsKey(propertyName);
	}

	public Item removeProperty(String propertyName) {
		_indexedProperties.remove(propertyName);
		_unindexedProperties.remove(propertyName);
		return this;
	}

	public Item setPropertiesFrom(Map<String, Object> src) {
		for (Map.Entry<String, Object> entry:src.entrySet()) {
			setProperty(entry.getKey(), entry.getValue());
		}
		return this;
	}
	
	public Item setProperty(String propertyName, Object value) {
		_indexedProperties.put(propertyName, value);
		_unindexedProperties.remove(propertyName);
		return this;
	}
	
	public Item setUnindexedProperty(String propertyName, Object value) {
		_unindexedProperties.put(propertyName, value);
		_indexedProperties.remove(propertyName);
		return this;
	}

	@Override
	public Item clone()  {
		return new Item(this);
	}
	
	
	public static final Serializer<Item> SERIALIZER= new Serializer<Item>() {
		private final int typeCode= Item.class.getName().hashCode();
		public Item readExternal(DataInput in) throws IOException {
			Item item= new Item();
			readExternal(in, item);
			return item;
		}
		public void readExternal(DataInput in, Item item) throws IOException {
			Entity.SERIALIZER.readExternal(in, item);
			int count= in.readInt();
			for (int i= count; 0 < i--;) {
				String name= ExternalizationManager.StringSerializer.readExternal(in);
				Object value= ExternalizationManager.readExternal(in);
				item._indexedProperties.put(name, value);
			}
			count= in.readInt();
			for (int i= count; 0 < i--;) {
				String name= ExternalizationManager.StringSerializer.readExternal(in);
				Object value= ExternalizationManager.readExternal(in);
				item._unindexedProperties.put(name, value);
			}
		}
		public int typeCode() {
			return typeCode;
		}
		public void writeExternal(DataOutput out, Item item)
		throws IOException {
			Entity.SERIALIZER.writeExternal(out, item);
			out.writeInt(item._indexedProperties.size());
			for (Map.Entry<String, Object> entry: item._indexedProperties.entrySet()) {
				ExternalizationManager.StringSerializer.writeExternal(out, entry.getKey());
				ExternalizationManager.writeExternal(out, entry.getValue());
			}
			out.writeInt(item._unindexedProperties.size());
			for (Map.Entry<String, Object> entry: item._unindexedProperties.entrySet()) {
				ExternalizationManager.StringSerializer.writeExternal(out, entry.getKey());
				ExternalizationManager.writeExternal(out, entry.getValue());
			}
		}
	};
}
