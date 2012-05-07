package com.googlecode.contraildb.core.impl;

import static com.googlecode.contraildb.core.ContrailQuery.FilterOperator.EQUAL;
import static com.googlecode.contraildb.core.ContrailQuery.FilterOperator.GREATER_THAN;
import static com.googlecode.contraildb.core.ContrailQuery.FilterOperator.GREATER_THAN_OR_EQUAL;
import static com.googlecode.contraildb.core.ContrailQuery.FilterOperator.LESS_THAN;
import static com.googlecode.contraildb.core.ContrailQuery.FilterOperator.LESS_THAN_OR_EQUAL;
import static com.googlecode.contraildb.core.ContrailQuery.FilterOperator.NOT_EQUAL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.googlecode.contraildb.core.ContrailException;
import com.googlecode.contraildb.core.ContrailQuery;
import com.googlecode.contraildb.core.ContrailQuery.FilterOperator;
import com.googlecode.contraildb.core.ContrailQuery.FilterPredicate;
import com.googlecode.contraildb.core.ContrailQuery.QuantifiedValues;
import com.googlecode.contraildb.core.ContrailQuery.Quantifier;
import com.googlecode.contraildb.core.IProcessor;
import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.Item;
import com.googlecode.contraildb.core.impl.btree.BPlusTree;
import com.googlecode.contraildb.core.impl.btree.BTree;
import com.googlecode.contraildb.core.impl.btree.IBTreeCursor;
import com.googlecode.contraildb.core.impl.btree.IBTreeCursor.Direction;
import com.googlecode.contraildb.core.impl.btree.IBTreePlusCursor;
import com.googlecode.contraildb.core.impl.btree.IForwardCursor;
import com.googlecode.contraildb.core.storage.StorageSession;
import com.googlecode.contraildb.core.storage.StorageUtils;
import com.googlecode.contraildb.core.utils.ContrailAction;
import com.googlecode.contraildb.core.utils.TaskUtils;



/**
 * Encapsulates Contrail query functionality .
 * 
 * @author ted stockwell
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class IndexSearcher {
	
	StorageSession  _storageSession;
	
	public IndexSearcher(StorageSession storageSession) {
		_storageSession= storageSession;
	}
	
	/**
	 * Index the given entities.
	 */
	public <T extends Item> void index(Iterable<T> entities) throws IOException {
		Map<String, Collection<T>> entitiesByProperty= new HashMap<String, Collection<T>>();
		BTree<Identifier> idIndex= getIdIndex();
		for (T t: entities) {
			for (String property: t.getIndexedProperties().keySet()) {
				Collection<T> c= entitiesByProperty.get(property);
				if (c == null) {
					c= new ArrayList<T>();
					entitiesByProperty.put(property, c);
				}
				c.add(t);
			}
			idIndex.insert(t.getId());
		}
		
		ArrayList<IResult<Void>> tasks= new ArrayList<IResult<Void>>();
		for (final Map.Entry<String, Collection<T>> e: entitiesByProperty.entrySet()) {
			tasks.add(new ContrailAction() {
				protected void action() throws IOException {
					final String propertyName= e.getKey();
					PropertyIndex var= getPropertyIndex(propertyName);
					if (var == null)
						var= createPropertyIndex(propertyName);
					final PropertyIndex propertyIndex= var;
					
					ArrayList<IResult<Void>> tasks= new ArrayList<IResult<Void>>();
					for (final T t: e.getValue()) {
						tasks.add(new ContrailAction() {
							protected void action() throws IOException {
								Object propertyValue= t.getProperty(propertyName);
								if (propertyValue instanceof Comparable || propertyValue == null) {
									propertyIndex.insert((Comparable<?>) propertyValue, t.getId());
								}
								else if (propertyValue instanceof Collection) {
									Collection<Comparable> collection= (Collection)propertyValue;
									for (Comparable comparable: collection) 
										propertyIndex.insert(comparable, t.getId());
								}
								else
									throw new ContrailException("Cannot store property value. A value must be one of the basic types supported by Contrail or a collection supported types:"+propertyValue);
							}
						}.submit());
					}
					try {
						TaskUtils.combineResults(tasks).get();
					}
					catch (Throwable t) {
						TaskUtils.throwSomething(t, IOException.class);
					}
				}
			}.submit());
		}
		try {
			TaskUtils.combineResults(tasks).get();
		}
		catch (Throwable t) {
			TaskUtils.throwSomething(t, IOException.class);
		}
	}
	
	private BTree<Identifier> getIdIndex() throws IOException {
		Identifier indexId= Identifier.create("net/sf/contrail/core/indexes/"+Item.KEY_ID);
		BTree tree= StorageUtils.syncFetch(_storageSession, indexId);
		if (tree == null)
			tree= BTree.createInstance(_storageSession, indexId);
		return tree;
	}
	
	
	private PropertyIndex getPropertyIndex(String propertyName) throws IOException {
		Identifier indexId= Identifier.create("net/sf/contrail/core/indexes/"+propertyName);
		BPlusTree tree= StorageUtils.syncFetch(_storageSession, indexId);
		if (tree == null)
			return null;
		return new PropertyIndex(tree);
	}

	private PropertyIndex createPropertyIndex(String propertyName) 
	throws IOException 
	{
		Identifier indexId= Identifier.create("net/sf/contrail/core/indexes/"+propertyName);
		BPlusTree tree= BPlusTree.createBPlusTree(_storageSession, indexId);
		PropertyIndex propertyIndex= new PropertyIndex(tree);
		return propertyIndex;
	}

	public <T extends Item> void unindex(Iterable<T> entities) throws IOException {
		Map<String, Collection<T>> entitiesByProperty= new HashMap<String, Collection<T>>();
		for (T t: entities) {
			for (String property: t.getIndexedProperties().keySet()) {
				Collection<T> c= entitiesByProperty.get(property);
				if (c == null) {
					c= new ArrayList<T>();
					entitiesByProperty.put(property, c);
				}
				c.add(t);
			}
		}
		
		ArrayList<IResult<Void>> tasks= new ArrayList<IResult<Void>>();
		for (final Map.Entry<String, Collection<T>> e: entitiesByProperty.entrySet()) {
			tasks.add(new ContrailAction() {
				protected void action() throws IOException {
					final String propertyName= e.getKey();
					final PropertyIndex propertyIndex= getPropertyIndex(propertyName);
					if (propertyIndex == null) 
						throw new ContrailException("Missing index for property "+propertyName);
					
					ArrayList<IResult<Void>> tasks= new ArrayList<IResult<Void>>();
					for (final T t: e.getValue()) {
						tasks.add(new ContrailAction() {
							protected void action() throws IOException {
								Object propertyValue= t.getProperty(propertyName);
								if (propertyValue instanceof Comparable || propertyValue == null) {
									propertyIndex.remove((Comparable<?>) propertyValue, t.getId());
								}
								else if (propertyValue instanceof Collection) {
									Collection<Comparable> collection= (Collection)propertyValue;
									for (Comparable comparable: collection) 
										propertyIndex.remove(comparable, t.getId());
								}
								else
									throw new ContrailException("Cannot store property value. A value must be one of the basic types supported by Contrail or a collection supported types:"+propertyValue);
							}
						}.submit());
					}
					try {
						TaskUtils.combineResults(tasks).get();
					}
					catch (Throwable t) {
						TaskUtils.throwSomething(t, IOException.class);
					}
				}
			}.submit());
		}
		try {
			TaskUtils.combineResults(tasks).get();
		}
		catch (Throwable t) {
			TaskUtils.throwSomething(t, IOException.class);
		}
	}
	

	public void fetchIdentifiers(final ContrailQuery query, final IProcessor processor) {
		new ContrailAction() {
			protected void action() throws Exception {
				try {
					List<IForwardCursor<Identifier>> filterCursors= createFilterCursors(query.getFilterPredicates());
					final IForwardCursor<Identifier> queryCursor= (filterCursors.size() == 1) ?
						filterCursors.get(0) :
						new ConjunctiveCursor<Identifier>(filterCursors);
					while (queryCursor.next()) {
						if (!processor.result(queryCursor.keyValue()))
							break;
					}
					processor.complete(null);
				}
				catch (IOException x) {
					processor.complete(x);
				}
			}
		}.submit();
	}
	
	private List<IForwardCursor<Identifier>> createFilterCursors(Iterable<FilterPredicate> clauses) 
	throws IOException 
	{
		List<IForwardCursor<Identifier>> filterCursors= new ArrayList<IForwardCursor<Identifier>>();
		for (FilterPredicate filterPredicate: clauses) {
			IForwardCursor<Identifier> filterCursor= null;
			FilterOperator op= filterPredicate.getOperator();
			String propertyName= filterPredicate.getPropertyName();
			List<FilterPredicate> subClauses= filterPredicate.getClauses();
			if (op == FilterOperator.AND) {
				filterCursor= new ConjunctiveCursor<Identifier>(createFilterCursors(subClauses));
			}
			else if (op == FilterOperator.OR) {
				filterCursor= new DisjunctiveCursor<Identifier>(createFilterCursors(subClauses));
			}
			else {
				PropertyIndex index= getPropertyIndex(propertyName);
				if (index == null)
					throw new ContrailException("No index found for property: "+propertyName);
				filterCursor= createFilterCursor(index, filterPredicate);
			}
			filterCursors.add(filterCursor);
		}
		return filterCursors;
	}

	private IForwardCursor<Identifier> createFilterCursor(PropertyIndex index, FilterPredicate filterPredicate) 
	throws IOException 
	{
		final QuantifiedValues quantifiedValues= filterPredicate.getQuantifiedValues();
		final FilterOperator op= filterPredicate.getOperator();
		final Quantifier quantifier= quantifiedValues.getType();
		Comparable<?>[] values= quantifiedValues.getValues(); 
		if (values.length <= 0) 
			return new IBTreeCursor.EmptyForwardCursor<Identifier>();
		
		if (op == LESS_THAN || op == LESS_THAN_OR_EQUAL) {
			Comparable<?> value= values[0]; 
			IBTreePlusCursor<Comparable, IForwardCursor<Identifier>> propertyCursor= index.cursor(Direction.REVERSE);
			if (!propertyCursor.to(value)) 
				return new IBTreeCursor.EmptyForwardCursor<Identifier>();
			ArrayList<IForwardCursor<Identifier>> cursors= new ArrayList<IForwardCursor<Identifier>>();
			if (op == LESS_THAN && BPlusTree.compare(value, propertyCursor.keyValue()) == 0)
				if (!propertyCursor.next())
					return new IBTreeCursor.EmptyForwardCursor<Identifier>();
			cursors.add(propertyCursor.elementValue());
			/**
			 * Note - this loop is actually reading all results into memory.
			 * This logic needs to be changed so that results are not fetched until the cursor is accessed. 
			 */
			while (propertyCursor.next())
				cursors.add(0, propertyCursor.elementValue());
			return new DisjunctiveCursor(cursors);
		}
		
		if (op == GREATER_THAN || op == GREATER_THAN_OR_EQUAL) {
			Comparable<?> value= values[0]; 
			IBTreePlusCursor<Comparable, IForwardCursor<Identifier>> propertyCursor= index.cursor(Direction.FORWARD);
			if (!propertyCursor.to(value)) 
				return new IBTreeCursor.EmptyForwardCursor<Identifier>();
			ArrayList<IForwardCursor<Identifier>> cursors= new ArrayList<IForwardCursor<Identifier>>();
			if (op == GREATER_THAN && BPlusTree.compare(value, propertyCursor.keyValue()) == 0)
				if (!propertyCursor.next())
					return new IBTreeCursor.EmptyForwardCursor<Identifier>();
			cursors.add(propertyCursor.elementValue());
			while (propertyCursor.next())
				cursors.add(propertyCursor.elementValue());
			return new DisjunctiveCursor(cursors);
		} 
		
		if (op == EQUAL) {
			ArrayList<IForwardCursor<Identifier>> cursors= new ArrayList<IForwardCursor<Identifier>>();
			for (Comparable member: values) {
				IBTreePlusCursor<Comparable, IForwardCursor<Identifier>> propertyCursor= index.cursor(Direction.FORWARD);
				if (propertyCursor.to(member) && BPlusTree.compare(member, propertyCursor.keyValue()) == 0) {
					cursors.add(propertyCursor.elementValue());
				}
			}
			if (cursors.isEmpty())
				return new IBTreeCursor.EmptyForwardCursor<Identifier>();
			if (cursors.size() == 1)
				return cursors.get(0);
			if (Quantifier.ALL == quantifier)
				return new ConjunctiveCursor<Identifier>(cursors);
			return new DisjunctiveCursor<Identifier>(cursors);
		}
		
		if (op == NOT_EQUAL) {
			
////////////			
			ArrayList<IForwardCursor<Identifier>> cursors= new ArrayList<IForwardCursor<Identifier>>();
			for (Comparable member: values) {
				ArrayList<IForwardCursor<Identifier>> crsrs= new ArrayList<IForwardCursor<Identifier>>();
				for (Direction d: Direction.values()) {
					IBTreePlusCursor<Comparable, IForwardCursor<Identifier>> propertyCursor= index.cursor(d);
					if (propertyCursor.to(member)) {
						if (BPlusTree.compare(propertyCursor.keyValue(), member) != 0 || propertyCursor.next()) {
							do {
								crsrs.add(propertyCursor.elementValue());
							} while (propertyCursor.next());
						}
					}
				}
				if (!crsrs.isEmpty()) 
					cursors.add((crsrs.size() == 1) ? crsrs.get(0) : new DisjunctiveCursor<Identifier>(crsrs));
			}
			if (cursors.isEmpty())
				return new IBTreeCursor.EmptyForwardCursor<Identifier>();
			if (cursors.size() == 1)
				return cursors.get(0);
			if (Quantifier.ALL == quantifier)
				return new ConjunctiveCursor<Identifier>(cursors);
			return new DisjunctiveCursor<Identifier>(cursors);
		}
		
//		case IS_NULL: {

		throw new ContrailException("Unsupported operator:"+op);
	}
}
