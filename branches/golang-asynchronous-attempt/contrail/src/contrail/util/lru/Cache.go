/*
   Copyright  Google Inc.

   Licensed under the Apache License, Version . (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-.

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

// originally downloaded from http://gosourcefile.appspot.com/camlistore.org/pkg/lru/cache.go
// modified to add remove listeners - Ted Stockwell  
package lru

import (
	"fmt"
	"container/list"
	"sync"
)

type RemoveListener func(cache *Cache, key string, value interface{})

type Cache struct {
	maxEntries int
	removeHandlers []RemoveListener
	lk    sync.Mutex
	ll    *list.List
	cache map[string]*list.Element
}

type entry struct {
	key   string
	value interface{}
}

func New(maxEntries int) *Cache {
	return &Cache{
		maxEntries: maxEntries,
		ll:         list.New(),
		cache:      make(map[string]*list.Element),
	}
}

func (c *Cache) Add(key string, value interface{}) {
	c.lk.Lock()
	defer c.lk.Unlock()

	// Already in cache?
	if ee, ok := c.cache[key]; ok {
		c.ll.MoveToFront(ee)
		ee.Value.(*entry).value = value
		return
	}

	// Add to cache if not present
	ele := c.ll.PushFront(&entry{key, value})
	c.cache[key] = ele

	if c.ll.Len() > c.maxEntries {
		c.removeOldest()
	}
}

func (c *Cache) Get(key string) (value interface{}, ok bool) {
	c.lk.Lock()
	defer c.lk.Unlock()
	if ele, hit := c.cache[key]; hit {
		c.ll.MoveToFront(ele)
		return ele.Value.(*entry).value, true
	}
	return
}

func (c *Cache) Clear() {
	c.lk.Lock()
	defer c.lk.Unlock()
	
	c.ll= list.New()
	c.cache= make(map[string]*list.Element)
}

func (c *Cache) RemoveOldest() {
	c.lk.Lock()
	defer c.lk.Unlock()
	c.removeOldest()
}

// note: must hold c.lk
func (c *Cache) removeOldest() {
	ele := c.ll.Back()
	if ele == nil {
		return
	}
	c.ll.Remove(ele)
	value:= ele.Value.(*entry).value
	key:= ele.Value.(*entry).key
	delete(c.cache, key)
	
	if c.removeHandlers != nil {
		for _,handler:= range c.removeHandlers {
			func() {
				// ignore any panics from handlers
				defer func() {
			        if err := recover(); err != nil {
			        	fmt.Printf("panic within hander: %v\n", err)
			        }
				}()
				handler(c, key, value)
			}()
		}
	}
	
}

func (c *Cache) Len() int {
	c.lk.Lock()
	defer c.lk.Unlock()
	return c.ll.Len()
}

/**
 * Add a callback to be invoked when an item is removed.
 */
func (c *Cache) OnDelete(listener RemoveListener) {
	c.lk.Lock()
	defer c.lk.Unlock()

	if c.removeHandlers == nil {
		c.removeHandlers = make([]RemoveListener, 0)
	}
	c.removeHandlers = append(c.removeHandlers, listener)
}
