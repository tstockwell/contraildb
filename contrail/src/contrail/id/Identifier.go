package id

import (
	"strings"
	"sync"
	"contrail/util/uuid"
	"contrail/util/lru"
)

/**
 * Identifies an item in a hierarchy of items.
 * Identifiers may have information attached to them by clients, 
 * see the GetProperty and SetProperty methods.
 * 
 * The name of an item may not contain the '/' character.
 * Names are separated by '/' characters to form hierarchies.
 *
 * Identifiers are 'symbols', only one instance of a given Identifier 
 * ever exists in memory.     
 * 
 * @author Ted Stockwell
 */
type Identifier struct {
	completePath string;
	ancestors []*Identifier;
	name string;
	properties map[string]interface{};
}

var cache *lru.Cache = lru.New(1000);
var lock *sync.Mutex= new(sync.Mutex)
var empty_ancestors []*Identifier = []*Identifier{};

func getCached(path string) *Identifier {

	lock.Lock()
	defer lock.Unlock()
	id, ok := cache.Get(path)
	if ok {
		return id.(*Identifier)
	}
	return nil
}


func CreateIdentifier(path string) *Identifier {
	path= strings.Trim(path, "/")
	if id, ok := cache.Get(path); ok {
		return id.(*Identifier)
	}
	
	var id *Identifier= new(Identifier)
	id.completePath= path;
	
	i:= strings.LastIndex(path, "/")
	if 0 <= i {
		id.name= path[i+1:]
		var parent *Identifier= CreateIdentifier(path[:i])
		id.ancestors= append(parent.ancestors, parent)
	} else { 
		id.name= path
		id.ancestors= empty_ancestors
	}
	id.properties= map[string]interface{}{}
	
	cache.Add(path, id)
	
	return id
}


/**
 * Create a unique Identifier with a random UUID for the path 
*/
func UniqueIdentifier() *Identifier {
	uuid, err:= uuid.GenUUID()
	if err == nil {
		return CreateIdentifier(uuid)
	}
	panic(err)
}

func (this *Identifier) Path() string {
	return this.completePath;
}

func (this *Identifier) Name() string {
	return this.name;
}

/**
 * Create/Get a child Identifier
 */ 
func (parent *Identifier) Child(name string) *Identifier {
	if parent == nil {
		return CreateIdentifier(name)
	}
	return CreateIdentifier(parent.completePath+"/"+name)
}

func (this *Identifier) Parent() *Identifier {
	if len(this.ancestors) <= 0 {
		return nil
	}
	return this.ancestors[len(this.ancestors)-1]
}

///**
// * Identifies an item in a hierarchy of items.
// * 
// * The name of an item may not contain the '/' character.
// * Names are separated by '/' characters to form hierarchies. 
// * 
// * @author Ted Stockwell
// */
//type Identifier {
//	private static final long serialVersionUID = 1L;
//	
//	private static final Identifier[] EMPTY_ANCESTORS= new Identifier[0];
//	
//	transient final static private Map<String, Reference<Identifier>> __cache= new TreeMap<String, Reference<Identifier>>(); 
//	transient final static private ReferenceQueue<Identifier> __referenceQueue= new ReferenceQueue<Identifier>();
//	private static class IdentifierReference extends WeakReference<Identifier> {
//		String _path;
//		public IdentifierReference(Identifier referent) {
//			super(referent, __referenceQueue);
//			_path= referent._completePath;
//		}
//	}
//
//
//	public static final Serializer<Identifier> SERIALIZER= new Serializer<Identifier>() {
//		private final int typeCode= Identifier.class.getName().hashCode();
//		public Identifier readExternal(java.io.DataInput in) 
//		throws IOException {
//			String id= in.readUTF();
//			return Identifier.create(id);
//		};
//		public void writeExternal(java.io.DataOutput out, Identifier object) 
//		throws IOException {
//			out.writeUTF(object._completePath);
//		};
//		public void readExternal(DataInput in, Identifier object)
//		throws IOException {
//			throw new UnsupportedOperationException();
//		}
//		public int typeCode() {
//			return typeCode;
//		}
//	};
//	
//	
//	public static Identifier create(String path) {
//		Identifier Identifier= null;
//		synchronized (__cache) {
//			Reference<Identifier> ref= __cache.get(path);
//			if (ref != null && (Identifier= ref.get()) != null)
//				return Identifier;
//			Identifier= new Identifier(path);
//			__cache.put(path, new IdentifierReference(Identifier));
//		}
//		return Identifier;
//	}
//	
//	
//	private Identifier(String path) {
//		while (path.endsWith("/"))
//			path= path.substring(0, path.length()-1);
//		while (path.startsWith("/"))
//			path= path.substring(1);
//		int i= path.lastIndexOf('/');
//		if (0 <= i) {
//			_completePath= path;
//			_name= path.substring(i+1);
//			Identifier parent= create(path.substring(0, i));
//			Identifier[] ancestors= parent._ancestors;
//			_ancestors= new Identifier[ancestors.length+1];
//			System.arraycopy(ancestors, 0, _ancestors, 0, ancestors.length);
//			_ancestors[ancestors.length]= parent;
//		}
//		else { 
//			_completePath= _name= path;
//			_ancestors= EMPTY_ANCESTORS;
//		}
//		
//		// clean up expired references
//		IdentifierReference ref;
//		while ((ref= (IdentifierReference)__referenceQueue.poll()) != null) {
//			synchronized (__cache) {
//				__cache.remove(ref._path);
//			}
//		}
//	}
//	
//	public static Identifier create() {
//		return create(UUID.randomUUID().toString());
//	}
//	
//	public static Identifier create(Identifier parent) {
//		return create(parent, UUID.randomUUID().toString());
//	}
//	
//	public static Identifier create(Item parent, String name) {
//		return create(parent.getId(), name);
//	}
//	
//	private Object readResolve() {
//		return create(_completePath);
//	}
//
//	public String getName() {
//		return _name;
//	}
//	
//	public boolean isAncestorOf(Identifier Identifier) {
//		Identifier[] a= Identifier._ancestors;
//		for (int i= a.length; 0 < i--;)
//			if (a[i] == this)
//				return true;
//		return false;
//	}
//	
//	@Override
//	public String toString() {
//		return _completePath;
//	}
//	
//	public Object getProperty(String propertyName) {
//		if (_properties == null)
//			return null;
//		return _properties.get(propertyName);
//	}
//	
//	synchronized public void setProperty(String propertyName, Object value) {
//		if (_properties == null)
//			_properties= new Properties();
//		_properties.put(propertyName, value);
//	}
//	
//	
//	@Override
//	/**
//	 * @return 
//	 * 	 	a negative integer, zero, or a positive integer as this object is less 
//	 * 		than, equal to, or greater than the specified object.
//	 */
//	public int compareTo(Identifier o) {
//		if (o == this)
//			return 0;
//		Identifier[] a1= _ancestors;
//		Identifier[] a2= o._ancestors;
//		for (int i= 0; i < a1.length && i < a2.length; i++) {
//			Identifier i1= a1[i];
//			Identifier i2= a2[i];
//			if (i1 != i2) 
//				return i1._name.compareTo(i2._name);
//		}
//		if (a1.length < a2.length) 
//			return -1;
//		if (a1.length > a2.length) 
//			return 1;
//		return _name.compareTo(o._name);
//	}
//	
//}
