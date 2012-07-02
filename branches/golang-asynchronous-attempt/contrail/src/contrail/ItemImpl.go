package contrail

import (
	"math/big"
	"time"
	"sync"
	"contrail/id"
)


/**
 * Basic implementation of Item interface
 * Thread-safe
 * 
 * @author Ted Stockwell
 */
type ItemImpl struct {
	indexedProperties map[string]interface{} 
	unindexedProperties map[string]interface{}
	lock *sync.Mutex	 
}
func CreateItemImpl(identifier *id.Identifier) *ItemImpl {
	self:= new(ItemImpl)
	self.indexedProperties= make(map[string]interface{})
	self.unindexedProperties= make(map[string]interface{})
	self.lock= new (sync.Mutex)
	
	self.SetIndexedProperty(KEY_ID, identifier)
	
	return self
}

func (self *ItemImpl) Identifier() *id.Identifier {
	return self.GetProperty(KEY_ID).(*id.Identifier)
}

func (self *ItemImpl) Copy(identifier *id.Identifier) Item {
	self.lock.Lock()
	defer self.lock.Unlock()
	
	item:= CreateItemImpl(identifier)
	for k,v:= range self.indexedProperties {
		item.indexedProperties[k]= v
	}
	for k,v:= range self.unindexedProperties {
		item.unindexedProperties[k]= v
	}
	return item
}

func (self *ItemImpl) AllProperties() map[string]interface{} {
	self.lock.Lock()
	defer self.lock.Unlock()
	all:= make(map[string]interface{})
	for k,v:= range self.indexedProperties { all[k]= v }
	for k,v:= range self.unindexedProperties { all[k]= v }
	return all
}
func (self *ItemImpl) IndexedProperties() map[string]interface{} {
	self.lock.Lock()
	defer self.lock.Unlock()
	all:= make(map[string]interface{})
	for k,v:= range self.indexedProperties { all[k]= v }
	return all
}
func (self *ItemImpl) UnindexedProperties() map[string]interface{} {
	self.lock.Lock()
	defer self.lock.Unlock()
	all:= make(map[string]interface{})
	for k,v:= range self.unindexedProperties { all[k]= v }
	return all
}

func (self *ItemImpl) GetProperty(propertyName string) interface{} {
	self.lock.Lock()
	defer self.lock.Unlock()
	value:= self.indexedProperties[propertyName]
	if value == nil {
		value= self.unindexedProperties[propertyName]
	}
	return value;
}
func (self *ItemImpl) HasProperty(propertyName string) bool {
	self.lock.Lock()
	defer self.lock.Unlock()
	if self.indexedProperties[propertyName] != nil { return true }
	return self.unindexedProperties[propertyName].(bool)
}
func (self *ItemImpl) RemoveProperty(propertyName string) interface{} {
	self.lock.Lock()
	defer self.lock.Unlock()
	value:= self.indexedProperties[propertyName]
	if value != nil {
		delete(self.indexedProperties, propertyName)
		return value
	}
	value= self.unindexedProperties[propertyName]
	if value != nil {
		delete(self.unindexedProperties, propertyName)
	}
	return value
}
func (self *ItemImpl) SetIndexedProperty(propertyName string, value interface{}) {
	self.lock.Lock()
	defer self.lock.Unlock()
	self.indexedProperties[propertyName]= value
}
func (self *ItemImpl) SetUnindexedProperty(propertyName string, value interface{}) {
	self.lock.Lock()
	defer self.lock.Unlock()
	self.unindexedProperties[propertyName]= value
}
func (self *ItemImpl) SetIndexedProperties(properties map[string]interface{}) {
	self.lock.Lock()
	defer self.lock.Unlock()
	for k,v:= range properties {
		self.indexedProperties[k]= v
	}
}
func (self *ItemImpl) SetUnindexedProperties(properties map[string]interface{}) {
	self.lock.Lock()
	defer self.lock.Unlock()
	for k,v:= range properties {
		self.unindexedProperties[k]= v
	}
}


func (self *ItemImpl) GetString(propertyName string) string {
	return self.GetProperty(propertyName).(string)
} 
func (self *ItemImpl) GetIdentifier(propertyName string) *id.Identifier {
	return self.GetProperty(propertyName).(*id.Identifier)
} 
func (self *ItemImpl) GetRat(propertyName string) big.Rat {
	return self.GetProperty(propertyName).(big.Rat)
} 
func (self *ItemImpl) GetInt(propertyName string) big.Int {
	return self.GetProperty(propertyName).(big.Int)
}  
func (self *ItemImpl) GetBool(propertyName string) bool {
	return self.GetProperty(propertyName).(bool)
}  
func (self *ItemImpl) GetItem(propertyName string) Item {
	return self.GetProperty(propertyName).(Item)
}  
func (self *ItemImpl) GetByte(propertyName string) byte {
	return self.GetProperty(propertyName).(byte)
} 
func (self *ItemImpl) GetTime(propertyName string) time.Time {
	return self.GetProperty(propertyName).(time.Time)
}  
func (self *ItemImpl) GetFloat(propertyName string) float64 {
	return self.GetProperty(propertyName).(float64)
}  
func (self *ItemImpl) GetLong(propertyName string) int64 {
	return self.GetProperty(propertyName).(int64)
}  
func (self *ItemImpl) GetSlice(propertyName string) []interface{} {
	return self.GetProperty(propertyName).([]interface{})
}  
	
