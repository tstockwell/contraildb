/**
 * 
 */
package com.googlecode.contraildb.core.storage;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.DataInput;
import java.io.IOException;
import java.util.Collection;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.async.IResult;
import com.googlecode.contraildb.core.utils.ExternalizationManager.Serializer;


/**
 * Convenient base case for items stored in IEntityStorage.
 *  
 * @author ted stockwell
 */
public class Entity implements IEntity, ILifecycle {
	private static final long serialVersionUID = 1L;



	public static Serializer<Entity> SERIALIZER= new Serializer<Entity>() {
		private final String typeCode= Entity.class.getName();
		public Entity readExternal(java.io.DataInput in) 
		throws IOException {
			Entity entity= new Entity();
			entity.id= Identifier.SERIALIZER.readExternal(in);
			return entity;
		};
		public void writeExternal(java.io.DataOutput out, Entity object) 
		throws IOException {
			Identifier.SERIALIZER.writeExternal(out, object.id);
		};
		public void readExternal(DataInput in, Entity object)
		throws IOException {
			object.id= Identifier.SERIALIZER.readExternal(in);
		}
		public String typeCode() {
			return typeCode;
		}
	};
	
	

	protected Identifier id;
	transient protected IEntityStorage.Session storage;
	transient private PropertyChangeSupport _changeSupport= new PropertyChangeSupport(this);
	
	public Entity(Identifier identifier) {
		this.id= identifier;
	}
	public Entity(String identifier) {
		this.id= Identifier.create(identifier);
	}
	protected Entity() { }
	
	public Identifier getId() {
		return id;
	}

	public IEntityStorage.Session getStorage() {
		return storage;
	}
	

	public IResult<Collection<Identifier>> listChildren() throws IOException {
		return storage.listChildren(id);
	}
	
	public IResult<Collection<Entity>> getChildren() throws IOException {
		return storage.fetchChildren(id);
	}
	
	public void delete() throws IOException {
		storage.delete(getId());
	}
	
	public void deleteAllChildren() throws IOException {
		storage.deleteAllChildren(id);
	}
	
	public void update() throws IOException {
		storage.store(this).getb();
	}
	
	
	@Override
	public void setStorage(IEntityStorage.Session storage) {
		this.storage= storage;
	}
	
	@Override
	public void onDelete()
	throws IOException 
	{
		// do nothing
	}
	@Override
	public void onInsert(Identifier identifier)
	throws IOException {
		// do nothing
	}
	@Override
	public void onLoad(Identifier identifier)
	throws IOException 
	{
		// do nothing
	}



	
	
	
	
	
	
    @Override
    public void addPropertyChangeListener( PropertyChangeListener listener) {
    	_changeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public PropertyChangeListener[] getPropertyChangeListeners() {
    	return _changeSupport.getPropertyChangeListeners();
    }

    @Override
    public void addPropertyChangeListener( String propertyName, PropertyChangeListener listener) {
    	_changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    @Override
    public void removePropertyChangeListener( String propertyName, PropertyChangeListener listener) {
    	_changeSupport.removePropertyChangeListener(propertyName, listener);
    }

    @Override
    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
    	return _changeSupport.getPropertyChangeListeners(propertyName);
    }

    public void firePropertyChange(String propertyName,  Object oldValue, Object newValue) {
    	_changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    public void firePropertyChange(String propertyName, int oldValue, int newValue) {
    	_changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    protected void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
    	_changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    protected void firePropertyChange(PropertyChangeEvent evt) {
    	_changeSupport.firePropertyChange(evt);
    }

    
    protected void fireIndexedPropertyChange(String propertyName, int index, Object oldValue, Object newValue) {
    	_changeSupport.fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
    }

    protected void fireIndexedPropertyChange(String propertyName, int index, int oldValue, int newValue) {
    	_changeSupport.fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
    }

    protected void fireIndexedPropertyChange(String propertyName, int index, boolean oldValue, boolean newValue) {
    	_changeSupport.fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
    }

    @Override
    public synchronized boolean hasListeners(String propertyName) {
    	return _changeSupport.hasListeners(propertyName);
    }
	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		_changeSupport.removePropertyChangeListener(listener);
		
	}
	
	
}