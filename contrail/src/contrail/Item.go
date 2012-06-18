package contrail

import (
	"math/big"
	"time"
	"contrail/id"
)

type PropertyChangeListener func(source Item, propertyName string, oldValue interface{}, newValue interface{}) 


/**
 * An interface that every object to be stored in a Contrail database is required to implement.
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
 *
 *  Item has methods for tracking changes to properties.
 *  @see ***PropertyChangeListener
 * 
 *  Items are thread-safe.
 * 
 * @author Ted Stockwell
 */
type Item interface {
	/**
	 * @return a unique identifier used to locate an IEntity in storage.
	 */
	Identifier() *id.Identifier 
	Copy() Item 
	
	GetString(propertyName string) string 
	GetIdentifier(propertyName string) *id.Identifier 
	GetRat(propertyName string) big.Rat 
	GetInt(propertyName string) big.Int 
	GetBool(propertyName string) bool 
	GetItem(propertyName string) Item 
	GetByte(propertyName string) byte 
	GetTime(propertyName string) time.Time 
	GetFloat(propertyName string) float64 
	GetLong(propertyName string) int64 
	GetSlice(propertyName string) []interface{} 
	
	AllProperties() map[string]interface{}
	IndexedProperties() map[string]interface{}
	UnindexedProperties() map[string]interface{}

	GetProperty(propertyName string) interface{}
	HasProperty(propertyName string) bool
	RemoveProperty(propertyName string) Item
	SetProperties(map[string]interface{})
	SetProperty(propertyName string, value interface{})
	SetUnindexedProperty(propertyName string, value interface{})

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
    AddPropertyChangeListener(listener PropertyChangeListener)
    
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
    RemovePropertyChangeListener(listener PropertyChangeListener)
    

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

    AddPropertyChangeListener(propertyName string, listener PropertyChangeListener)

    /**
     * Remove a PropertyChangeListener for a specific property.
     *
     * @param propertyName  The name of the property that was listened on.
     * @param listener  The PropertyChangeListener to be removed
     */
    RemovePropertyChangeListener(propertyName string, listener PropertyChangeListener)
	
}