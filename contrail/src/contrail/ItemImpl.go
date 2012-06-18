package contrail

import (
	"contrail/id"
	"contrail/util"
)


/**
 * Basic implementation of Item interface
 * 
 * @author Ted Stockwell
 */
type ItemImpl struct {
	indexedProperties map[string]interface{} 
	unindexedProperties map[string]interface{} 
}
func CreateItemImpl() *ItemImpl {
	self:= new(ItemImpl)
	self.indexedProperties= make(map[string]interface{})
	self.unindexedProperties= make(map[string]interface{})
	return self
}

func (self *ItemImpl) Identifier() *id.Identifier {
	return self.getProperty(KEY_ID)
}

func (self *ItemImpl) Copy() Item {
	item:= CreateItemImpl()
	for k,v:= range self.indexedProperties {
		item.indexedProperties[k]= v
	}
	for k,v:= range self.unindexedProperties {
		item.unindexedProperties[k]= v
	}
	return item
}


func (self *ItemImpl) GetString(propertyName string) string 
func (self *ItemImpl) GetIdentifier(propertyName string) *id.Identifier 
func (self *ItemImpl) GetRat(propertyName string) big.Rat 
func (self *ItemImpl) GetInt(propertyName string) big.Int 
func (self *ItemImpl) GetBool(propertyName string) bool 
func (self *ItemImpl) GetItem(propertyName string) Item 
func (self *ItemImpl) GetByte(propertyName string) byte 
func (self *ItemImpl) GetTime(propertyName string) time.Time 
func (self *ItemImpl) GetFloat(propertyName string) float64 
func (self *ItemImpl) GetLong(propertyName string) int64 
func (self *ItemImpl) GetSlice(propertyName string) []interface{} 
	
func (self *ItemImpl) AllProperties() map[string]interface{} {
	all:= make(map[string]interface{})
	for k,v:= range self.indexedProperties { all[k]= v }
	for k,v:= range self.unindexedProperties { all[k]= v }
	return all
}
func (self *ItemImpl) IndexedProperties() map[string]interface{}
func (self *ItemImpl) UnindexedProperties() map[string]interface{}

func (self *ItemImpl) GetProperty(propertyName string) interface{}
func (self *ItemImpl) HasProperty(propertyName string) bool
func (self *ItemImpl) RemoveProperty(propertyName string) Item
func (self *ItemImpl) SetProperties(map[string]interface{})
func (self *ItemImpl) SetProperty(propertyName string, value interface{})
func (self *ItemImpl) SetUnindexedProperty(propertyName string, value interface{})

func (self *ItemImpl) AddPropertyChangeListener(listener PropertyChangeListener)
    
func (self *ItemImpl) RemovePropertyChangeListener(listener PropertyChangeListener)
    
func (self *ItemImpl) AddPropertyChangeListener(propertyName string, listener PropertyChangeListener)

func (self *ItemImpl) RemovePropertyChangeListener(propertyName string, listener PropertyChangeListener)



func (self *ItemImpl) AllProperties() map[string]interface{}
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
	
	public void readExternal(ObjectInput in) throws IOException {
		int count= in.readInt();
		for (int i= count; 0 < i--;) {
			String name= ExternalizationManager.StringSerializer.readExternal(in);
			Object value= ExternalizationManager.readExternal(in);
			_indexedProperties.put(name, value);
		}
		count= in.readInt();
		for (int i= count; 0 < i--;) {
			String name= ExternalizationManager.StringSerializer.readExternal(in);
			Object value= ExternalizationManager.readExternal(in);
			_unindexedProperties.put(name, value);
		}
	}
	public void writeExternal(ObjectOutput out)
	throws IOException {
		out.writeInt(_indexedProperties.size());
		for (Map.Entry<String, Object> entry: _indexedProperties.entrySet()) {
			ExternalizationManager.StringSerializer.writeExternal(out, entry.getKey());
			ExternalizationManager.writeExternal(out, entry.getValue());
		}
		out.writeInt(_unindexedProperties.size());
		for (Map.Entry<String, Object> entry: _unindexedProperties.entrySet()) {
			ExternalizationManager.StringSerializer.writeExternal(out, entry.getKey());
			ExternalizationManager.writeExternal(out, entry.getValue());
		}
	}
}

