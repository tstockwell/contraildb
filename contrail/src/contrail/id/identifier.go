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
 * Identifiers are 'symbols', only one instance of a given identifier 
 * ever exists in memory.     
 * 
 * @author Ted Stockwell
 */
type Identifier struct {
	completePath string;
	ancestors []Identifier;
	name string;
	properties map[string]interface{};
}

var cache Cache = lru.New(1000);
var empty_ancestors []Identifier = []Identifier{};

func getCached(path string) Identifer {
	lock.Lock()
	defer lock.UnLock()
	return cache[path]
}


func Create(path string) Identifier {
	path= strings.Trim(path, "/")
	var id Identifier= cache.Get(path)
	if (id != null)
		return id
	
	id= new Identifier{}
	id.completePath= path;
	
	i:= strings.LastIndex(path, "/")
	if 0 <= i {
		id.name= path[i+1:]
		var parent Identifier= Create(path[:i])
		id.ancestors= append(parent.ancestors, parent)
	}
	else { 
		id.name= path
		id.ancestors= empty_ancestors
	}
	id.properties= map[string]interface{}
	
	cache.Add(id);
	
	return id
}


/**
 * Create a unique identifier with a random UUID for the path 
*/
func Unique() Identifier {
	return Create(uuid.GenUUID())
}

/**
 * Create/Get a child identifier
 */ 
func (parent Identifier*) Child(String name) {
	if (parent == null)
		return Create(name)
	return Create(parent.completePath+"/"+name)
}

func (this Identifier*) Parent() {
	if (len(this.ancestors) <= 0)
		return null
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
//type identifier {
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
//		Identifier identifier= null;
//		synchronized (__cache) {
//			Reference<Identifier> ref= __cache.get(path);
//			if (ref != null && (identifier= ref.get()) != null)
//				return identifier;
//			identifier= new Identifier(path);
//			__cache.put(path, new IdentifierReference(identifier));
//		}
//		return identifier;
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
//	public boolean isAncestorOf(Identifier identifier) {
//		Identifier[] a= identifier._ancestors;
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
