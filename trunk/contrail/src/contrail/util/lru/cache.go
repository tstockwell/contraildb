  /*
   2  Copyright 2011 Google Inc.
   3  
   4  Licensed under the Apache License, Version 2.0 (the "License");
   5  you may not use this file except in compliance with the License.
   6  You may obtain a copy of the License at
   7  
   8       http://www.apache.org/licenses/LICENSE-2.0
   9  
  10  Unless required by applicable law or agreed to in writing, software
  11  distributed under the License is distributed on an "AS IS" BASIS,
  12  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  13  See the License for the specific language governing permissions and
  14  limitations under the License.
  15  */
  16  // originally downloaded from http://gosourcefile.appspot.com/camlistore.org/pkg/lru/cache.go
  17  package lru
  18  
  19  import (
  20  	"container/list"
  21  	"sync"
  22  )
  23  
  24  type Cache struct {
  25  	maxEntries int
  26  
  27  	lk    sync.Mutex
  28  	ll    *list.List
  29  	cache map[string]*list.Element
  30  }
  31  
  32  type entry struct {
  33  	key   string
  34  	value interface{}
  35  }
  36  
  37  func New(maxEntries int) *Cache {
  38  	return &Cache{
  39  		maxEntries: maxEntries,
  40  		ll:         list.New(),
  41  		cache:      make(map[string]*list.Element),
  42  	}
  43  }
  44  
  45  func (c *Cache) Add(key string, value interface{}) {
  46  	c.lk.Lock()
  47  	defer c.lk.Unlock()
  48  
  49  	// Already in cache?
  50  	if ee, ok := c.cache[key]; ok {
  51  		c.ll.MoveToFront(ee)
  52  		ee.Value.(*entry).value = value
  53  		return
  54  	}
  55  
  56  	// Add to cache if not present
  57  	ele := c.ll.PushFront(&entry{key, value})
  58  	c.cache[key] = ele
  59  
  60  	if c.ll.Len() > c.maxEntries {
  61  		c.removeOldest()
  62  	}
  63  }
  64  
  65  func (c *Cache) Get(key string) (value interface{}, ok bool) {
  66  	c.lk.Lock()
  67  	defer c.lk.Unlock()
  68  	if ele, hit := c.cache[key]; hit {
  69  		c.ll.MoveToFront(ele)
  70  		return ele.Value.(*entry).value, true
  71  	}
  72  	return
  73  }
  74  
  75  func (c *Cache) RemoveOldest() {
  76  	c.lk.Lock()
  77  	defer c.lk.Unlock()
  78  	c.removeOldest()
  79  }
  80  
  81  // note: must hold c.lk
  82  func (c *Cache) removeOldest() {
  83  	ele := c.ll.Back()
  84  	if ele == nil {
  85  		return
  86  	}
  87  	c.ll.Remove(ele)
  88  	delete(c.cache, ele.Value.(*entry).key)
  89  }
  90  
  91  func (c *Cache) Len() int {
  92  	c.lk.Lock()
  93  	defer c.lk.Unlock()
  94  	return c.ll.Len()
  95  }
  96  