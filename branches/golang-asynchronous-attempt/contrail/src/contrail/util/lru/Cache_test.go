package lru

import (
	"testing"
)

func TestCache(t *testing.T) {
	deletedItems:= make(map[string]interface{})
	cache:= New(1)
	callbackWorking:= false
	cache.OnDelete(func(dcache *Cache, key string, value interface{}) {
		callbackWorking= true
		if dcache != cache { t.Errorf("invalid cache reference passed to callback") }
		deletedItems[key]= value
	})
	cache.Add("one", "one-value")
	if cache.Len() != 1 { t.Errorf("cache.Len() != 1") }
	cache.Add("two", "two-value")
	if !callbackWorking { t.Errorf("notifications not working"); return }
	if deletedItems["one"] == nil { t.Errorf("old item not removed from cache"); return }
	if cache.Len() != 1 { t.Errorf("cache.Len() != 1") }
}
