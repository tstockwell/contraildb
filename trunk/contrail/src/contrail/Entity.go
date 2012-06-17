package contrail

import (
	"contrail/id"
)


/**
 * An object that can be stored in a Contrail database.
 * An Item is a collection of property values indexed by property names.
 * All items have an identifier that denotes a unique path to the item's 
 * location in the database.
 * An Item is just a collection of properties.
 * Property values must be one of the following:
 * 	string,
 *  time.Time, 
 * 	int64, float64, byte, math.Int, math.Rat
 * 	bool,
 * 	contrail.id.Identifier,
 * 	contrail.Item,
 *  Go Slice, where items in slice are other valid property values
 *  Go Map, where keys are string, int64, float64, byte, or bool 
 *  	and values are other valid property values     
 *
 *  Item has methods for tracking changes to properties.
 *  @see ***PropertyChangeListener
 * 
 *  Items are thread-safe.
 * 
 * @author Ted Stockwell
 */
type Item struct {
	indexedProperties map[string]interface{} 
	unindexedProperties map[string]interface{} 
}
func CreateItem() *Item {
	self:= new(Item)
	self.indexedProperties= make(map[string]interface{})
	self.unindexedProperties= make(map[string]interface{})
	return self
}

func (self *Item) Identifier() *id.Identifier {
	return self.getProperty(KEY_ID)
}

func CreateCopy(item *Item) *Item {
	self:= CreateItem()
	for k,v:= range item.indexedProperties {
		self.indexedProperties[k]= v
	}
	for k,v:= range item.unindexedProperties {
		self.unindexedProperties[k]= v
	}
	return self
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


	
	/**
	 * @return a unique identifier used to locate an IEntity in storage.
	 */
	public Identifier getId();
	
    /**
     * Add a PropertyChangeListener to the listener list.
     * The listener is registered for all properties.
     * The same listener object may be added more than once, and will be called
     * as many times as it is added.
     * If <code>listener</code> is null, no exception is thrown and no action
     * is taken.
     *
     * @param listener  The PropertyChangeListener to be added
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);
    
    /**
     * Remove a PropertyChangeListener from the listener list.
     * This removes a PropertyChangeListener that was registered
     * for all properties.
     * If <code>listener</code> was added more than once to the same event
     * source, it will be notified one less time after being removed.
     * If <code>listener</code> is null, or was never added, no exception is
     * thrown and no action is taken.
     *
     * @param listener  The PropertyChangeListener to be removed
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);
    
    /**
     * Returns an array of all the listeners that were added to the
     * PropertyChangeSupport object with addPropertyChangeListener().
     * <p>
     * If some listeners have been added with a named property, then
     * the returned array will be a mixture of PropertyChangeListeners
     * and <code>PropertyChangeListenerProxy</code>s. If the calling
     * method is interested in distinguishing the listeners then it must
     * test each element to see if it's a
     * <code>PropertyChangeListenerProxy</code>, perform the cast, and examine
     * the parameter.
     * 
     * <pre>
     * PropertyChangeListener[] listeners = bean.getPropertyChangeListeners();
     * for (int i = 0; i < listeners.length; i++) {
     *	 if (listeners[i] instanceof PropertyChangeListenerProxy) {
     *     PropertyChangeListenerProxy proxy = 
     *                    (PropertyChangeListenerProxy)listeners[i];
     *     if (proxy.getPropertyName().equals("foo")) {
     *       // proxy is a PropertyChangeListener which was associated
     *       // with the property named "foo"
     *     }
     *   }
     * }
     *</pre>
     *
     * @see PropertyChangeListenerProxy
     * @return all of the <code>PropertyChangeListeners</code> added or an 
     *         empty array if no listeners have been added
     */
    public PropertyChangeListener[] getPropertyChangeListeners();

    /**
     * Add a PropertyChangeListener for a specific property.  The listener
     * will be invoked only when a call on firePropertyChange names that
     * specific property.
     * The same listener object may be added more than once.  For each
     * property,  the listener will be invoked the number of times it was added
     * for that property.
     * If <code>propertyName</code> or <code>listener</code> is null, no
     * exception is thrown and no action is taken.
     *
     * @param propertyName  The name of the property to listen on.
     * @param listener  The PropertyChangeListener to be added
     */

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener);

    /**
     * Remove a PropertyChangeListener for a specific property.
     * If <code>listener</code> was added more than once to the same event
     * source for the specified property, it will be notified one less time
     * after being removed.
     * If <code>propertyName</code> is null,  no exception is thrown and no
     * action is taken.
     * If <code>listener</code> is null, or was never added for the specified
     * property, no exception is thrown and no action is taken.
     *
     * @param propertyName  The name of the property that was listened on.
     * @param listener  The PropertyChangeListener to be removed
     */

    public void removePropertyChangeListener( String propertyName, PropertyChangeListener listener);

    /**
     * Check if there are any listeners for a specific property, including
     * those registered on all properties.  If <code>propertyName</code>
     * is null, only check for listeners registered on all properties.
     *
     * @param propertyName  the property name.
     * @return true if there are one or more listeners for the given property
     */
    public boolean hasListeners(String propertyName);
	
    /**
     * Returns an array of all the listeners which have been associated 
     * with the named property.
     *
     * @param propertyName  The name of the property being listened to
     * @return all of the <code>PropertyChangeListeners</code> associated with
     *         the named property.  If no such listeners have been added,
     *         or if <code>propertyName</code> is null, an empty array is
     *         returned.
     */
    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName);
