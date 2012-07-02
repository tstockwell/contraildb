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
	
	// Makes a copy of the Item
	// @param newId the id the new Item 
	Copy(newId *id.Identifier) Item 
	
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
	RemoveProperty(propertyName string) interface{}
	
	SetIndexedProperty(propertyName string, value interface{})
	SetUnindexedProperty(propertyName string, value interface{})
	SetIndexedProperties(map[string]interface{})
	SetUnindexedProperties(map[string]interface{})

	
}