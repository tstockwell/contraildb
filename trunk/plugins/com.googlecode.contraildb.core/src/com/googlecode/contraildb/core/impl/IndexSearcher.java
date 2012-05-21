package com.googlecode.contraildb.core.impl;

import static com.googlecode.contraildb.core.ContrailQuery.FilterOperator.EQUAL;
import static com.googlecode.contraildb.core.ContrailQuery.FilterOperator.GREATER_THAN;
import static com.googlecode.contraildb.core.ContrailQuery.FilterOperator.GREATER_THAN_OR_EQUAL;
import static com.googlecode.contraildb.core.ContrailQuery.FilterOperator.LESS_THAN;
import static com.googlecode.contraildb.core.ContrailQuery.FilterOperator.LESS_THAN_OR_EQUAL;
import static com.googlecode.contraildb.core.ContrailQuery.FilterOperator.NOT_EQUAL;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.googlecode.contraildb.core.ContrailException;
import com.googlecode.contraildb.core.ContrailQuery;
import com.googlecode.contraildb.core.ContrailQuery.FilterOperator;
import com.googlecode.contraildb.core.ContrailQuery.FilterPredicate;
import com.googlecode.contraildb.core.ContrailQuery.QuantifiedValues;
import com.googlecode.contraildb.core.ContrailQuery.Quantifier;
import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.Item;
import com.googlecode.contraildb.core.impl.btree.IForwardCursor;
import com.googlecode.contraildb.core.impl.btree.IKeyValueCursor;
import com.googlecode.contraildb.core.impl.btree.IOrderedSetCursor;
import com.googlecode.contraildb.core.impl.btree.IOrderedSetCursor.Direction;
import com.googlecode.contraildb.core.impl.btree.KeyValueSet;
import com.googlecode.contraildb.core.storage.StorageSession;
import com.googlecode.contraildb.core.utils.Handler;
import com.googlecode.contraildb.core.utils.IAsyncerator;
import com.googlecode.contraildb.core.utils.InvocationAction;
import com.googlecode.contraildb.core.utils.InvocationHandler;
import com.googlecode.contraildb.core.utils.NullHandler;
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
	public <T extends Item> IResult<Void> index(final Iterable<T> entities) {
		class shared {
			Map<String, Collection<T>> entitiesByProperty= new HashMap<String, Collection<T>>();
		}
		final shared shared= new shared();
		
		IResult<Void> getEntitiesByProperty= new InvocationAction<KeyValueSet<Identifier,Identifier>>(getIdIndex()) {
			protected void onSuccess(KeyValueSet<Identifier,Identifier> idIndex) throws Exception {
				for (T t: entities) {
					for (String property: t.getIndexedProperties().keySet()) {
						Collection<T> c= shared.entitiesByProperty.get(property);
						if (c == null) {
							c= new ArrayList<T>();
							shared.entitiesByProperty.put(property, c);
						}
						c.add(t);
					}
					spawn(idIndex.insert(t.getId()));
				}
			};
		}.toResult();
		
		return new Handler(getEntitiesByProperty) {
			protected IResult onSuccess() throws Exception {
				for (final Map.Entry<String, Collection<T>> e: shared.entitiesByProperty.entrySet()) {
					final String propertyName= e.getKey();
					
					final IResult<PropertyIndex> getPropertyIndex= 
						new NullHandler<PropertyIndex>(getPropertyIndex(propertyName)) {
							protected IResult onNull() throws Exception {
								return createPropertyIndex(propertyName);}};
								
					spawn(new Handler(getPropertyIndex) {
						protected IResult onSuccess() throws Exception {
							final PropertyIndex propertyIndex= getPropertyIndex.getResult();
							for (final T t: e.getValue()) {
								Object propertyValue= t.getProperty(propertyName);
								if (propertyValue instanceof Comparable || propertyValue == null) {
									spawn(propertyIndex.insert((Comparable<?>) propertyValue, t.getId()));
								}
								else if (propertyValue instanceof Collection) {
									Collection<Comparable> collection= (Collection)propertyValue;
									for (Comparable comparable: collection) 
										spawn(propertyIndex.insert(comparable, t.getId()));
								}
								else
									throw new ContrailException("Cannot store property value. A value must be one of the basic types supported by Contrail or a collection supported types:"+propertyValue);
							}
							return TaskUtils.DONE;
						}
					});
				}
				return TaskUtils.DONE;
			}
		};
	}
	
	private IResult<KeyValueSet<Identifier,Identifier>> getIdIndex() {
		final Identifier indexId= Identifier.create("net/sf/contrail/core/indexes/"+Item.KEY_ID);
		final IResult<KeyValueSet> fetch= _storageSession.fetch(indexId);
		return new Handler(fetch) {
			protected IResult onSuccess() throws Exception {
				KeyValueSet tree= fetch.getResult();
				if (tree == null)
					return KeyValueSet.create(_storageSession, indexId);
				return asResult(tree);
			}
		};
	}
	
	
	private IResult<PropertyIndex> getPropertyIndex(String propertyName) {
		Identifier indexId= Identifier.create("net/sf/contrail/core/indexes/"+propertyName);
		final IResult<KeyValueSet> fetch= _storageSession.fetch(indexId);
		return new Handler(fetch) {
			protected IResult onSuccess() throws Exception {
				KeyValueSet tree= fetch.getResult();
				if (tree == null)
					return TaskUtils.NULL;
				return asResult(new PropertyIndex(tree));
			}
		};
	}

	private IResult<PropertyIndex> createPropertyIndex(String propertyName) 
	{
		Identifier indexId= Identifier.create("net/sf/contrail/core/indexes/"+propertyName);
		final IResult<KeyValueSet> fetchTree= _storageSession.fetch(indexId);
		return new Handler() {
			protected IResult onSuccess() throws Exception {
				KeyValueSet tree= fetchTree.getResult();
				return asResult(new PropertyIndex(tree));
			}
		};
	}

	public <T extends Item> IResult<Void> unindex(Iterable<T> entities)  {
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
			final String propertyName= e.getKey();
			final IResult<PropertyIndex> getPropertyIndex= getPropertyIndex(propertyName);
			tasks.add(new Handler(getPropertyIndex) {
				protected IResult onSuccess() throws Exception {
					final PropertyIndex propertyIndex= getPropertyIndex.getResult();
					if (propertyIndex == null) 
						throw new ContrailException("Missing index for property "+propertyName);
					
					ArrayList<IResult<Void>> tasks= new ArrayList<IResult<Void>>();
					for (final T t: e.getValue()) {
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
					return TaskUtils.combineResults(tasks);
				}
			});
		}
		return TaskUtils.combineResults(tasks);
	}
	

	public IResult<IAsyncerator<Identifier>> fetchIdentifiers(final ContrailQuery query) {
		final IResult<List<IForwardCursor<Identifier>>> createFilterCursors= createFilterCursors(query.getFilterPredicates());
		return new Handler(createFilterCursors) {
			protected IResult onSuccess() throws Exception {
				List<IForwardCursor<Identifier>> filterCursors= createFilterCursors.getResult();
				final IForwardCursor<Identifier> queryCursor= (filterCursors.size() == 1) ?
						filterCursors.get(0) :
							new ConjunctiveCursor<Identifier>(filterCursors);
						
				IAsyncerator<Identifier> iterator= new IAsyncerator<Identifier>() {
					public IResult<Void> remove() {
						throw new UnsupportedOperationException();
					}
					
					public IResult<Identifier> next() {
						return new InvocationHandler<Boolean>(queryCursor.next()) {
							protected IResult onSuccess(Boolean next) {
								if (!next)
									throw new IllegalStateException("no element available");
								return queryCursor.keyValue();
							}
						};
					}
					public IResult<Boolean> hasNext() {
						return queryCursor.hasNext();
					}
				};
				
				return asResult(iterator);
			}
		};
	}
	
	/**
	 * Create a cursor that implements the semantics of a given query predicates.
	 */
	private IResult<List<IForwardCursor<Identifier>>> createFilterCursors(Iterable<FilterPredicate> clauses) 
	{
		ArrayList<IResult> tasks= new ArrayList<IResult>();
		final List<IForwardCursor<Identifier>> filterCursors= new ArrayList<IForwardCursor<Identifier>>();
		for (final FilterPredicate filterPredicate: clauses) {
			IForwardCursor<Identifier> filterCursor= null;
			FilterOperator op= filterPredicate.getOperator();
			final String propertyName= filterPredicate.getPropertyName();
			List<FilterPredicate> subClauses= filterPredicate.getClauses();
			if (op == FilterOperator.AND) {
				tasks.add(new InvocationAction<List<IForwardCursor<Identifier>>>(createFilterCursors(subClauses)){
					protected void onSuccess(List<IForwardCursor<Identifier>> cursors) throws Exception {
						filterCursors.add(new ConjunctiveCursor<Identifier>(cursors));
					}
				});
			}
			else if (op == FilterOperator.OR) {
				tasks.add(new InvocationAction<List<IForwardCursor<Identifier>>>(createFilterCursors(subClauses)){
					protected void onSuccess(List<IForwardCursor<Identifier>> cursors) throws Exception {
						filterCursors.add(new DisjunctiveCursor<Identifier>(cursors));
					}
				});
			}
			else {
				final IResult<PropertyIndex> getPropertyIndex= getPropertyIndex(propertyName);
				tasks.add(new Handler(getPropertyIndex) {
					protected IResult onSuccess() throws Exception {
						PropertyIndex index= getPropertyIndex.getResult();
						if (index == null)
							throw new ContrailException("No index found for property: "+propertyName);
						return new InvocationAction<IForwardCursor<Identifier>>(createFilterCursor(index, filterPredicate)){
							protected void onSuccess(IForwardCursor<Identifier> cursor) throws Exception {
								filterCursors.add(cursor);
							}
						};
					}
				});
			}
			filterCursors.add(filterCursor);
		}
		return new Handler(tasks) {
			protected IResult onSuccess() throws Exception {
				return asResult(filterCursors);
			}
		};
	}

	/**
	 * Create a cursor that implements the semantics of an individual query predicate.
	 */
	private <K extends Comparable<K> & Serializable> IResult<IForwardCursor<Identifier>> 
	createFilterCursor(PropertyIndex index, FilterPredicate filterPredicate) 
	{
		final QuantifiedValues quantifiedValues= filterPredicate.getQuantifiedValues();
		final FilterOperator op= filterPredicate.getOperator();
		final Quantifier quantifier= quantifiedValues.getType();
		K[] values= quantifiedValues.getValues(); 
		if (values.length <= 0) 
			return TaskUtils.asResult(new IOrderedSetCursor.EmptyForwardCursor<Identifier>());
		
		if (op == LESS_THAN || op == LESS_THAN_OR_EQUAL) {
			return createLessThanCursor(index, op, values);
		}
		
		if (op == GREATER_THAN || op == GREATER_THAN_OR_EQUAL) {
			return createGreaterThanCursor(index, op, values);
		} 
		
		if (op == EQUAL) {
			return createEqualToCursor(index, quantifier, values);
		}
		
		if (op == NOT_EQUAL) {
			return createNotEqualCursor(index, quantifier, values);
		}
		
//		case IS_NULL: {

		throw new ContrailException("Unsupported operator:"+op);
	}

	private IResult<IForwardCursor<Identifier>> 
	createNotEqualCursor(PropertyIndex index, final Quantifier quantifier, Comparable<?>[] values) 
	{
		final ArrayList<IForwardCursor<Identifier>> cursors= new ArrayList<IForwardCursor<Identifier>>();
		ArrayList<IResult> tasks= new ArrayList<IResult>();
		for (final Comparable member: values) {
			
			final ArrayList<IForwardCursor<Identifier>> crsrs= new ArrayList<IForwardCursor<Identifier>>();
			ArrayList<IResult> innerTasks= new ArrayList<IResult>();
			for (Direction d: Direction.values()) {
				final IKeyValueCursor<Comparable, IForwardCursor<Identifier>> propertyCursor= index.cursor(d);
				
				// check to see if the cursor contains the value.
				// if not then we can just not bother using this cursor
				innerTasks.add(new InvocationHandler<Boolean>(propertyCursor.to(member)) {
					protected IResult onSuccess(Boolean found) throws Exception {
						if (!found)
							return TaskUtils.DONE;
						return new InvocationHandler<Comparable>(propertyCursor.keyValue()) {
							protected IResult onSuccess(Comparable keyValue) {
								if (KeyValueSet.compare(member, keyValue) != 0) 
									return TaskUtils.DONE;
								return new InvocationHandler<IForwardCursor<Identifier>>(propertyCursor.elementValue()) {
									protected IResult onSuccess(IForwardCursor<Identifier> elementValue) {
										crsrs.add(elementValue);
										return TaskUtils.DONE;
									}
								};
							}
						};
					}
				});
				
			}
			
			tasks.add(new Handler(innerTasks) {
				protected IResult onSuccess() {
					if (!crsrs.isEmpty()) 
						cursors.add((crsrs.size() == 1) ? crsrs.get(0) : new DisjunctiveCursor<Identifier>(crsrs));
					return TaskUtils.DONE;
				}
			});
		}
		
		return new Handler(tasks) {
			protected IResult onSuccess() {
				if (cursors.isEmpty())
					return TaskUtils.asResult(new IKeyValueCursor.EmptyForwardCursor<Identifier>());
				if (cursors.size() == 1)
					return TaskUtils.asResult(cursors.get(0));
				if (Quantifier.ALL == quantifier)
					return TaskUtils.asResult(new ConjunctiveCursor<Identifier>(cursors));
				return TaskUtils.asResult(new DisjunctiveCursor<Identifier>(cursors));
			}
		};
	}

	/**
	 * Create a cursor that navigates the given property index and returns the identifiers 
	 * that associated with entries that match the given values.
	 * 
	 * @param index  The property to navigate
	 * @param quantifier  If ALL then match entries that contain all the given values (remember that properties may be multi-valued) 
	 * 					If ANY then match entries that contain any of the given values 
	 * @param values the values to match
	 */
	private IResult<IForwardCursor<Identifier>> 
	createEqualToCursor(PropertyIndex index, final Quantifier quantifier, Comparable<?>[] values) 
	{
		final List<IForwardCursor<Identifier>> cursors= 
				Collections.synchronizedList(new ArrayList<IForwardCursor<Identifier>>());
		
		ArrayList<IResult> createCursors= new ArrayList<IResult>();
		for (final Comparable member: values) {
			final IKeyValueCursor<Comparable, IForwardCursor<Identifier>> propertyCursor= index.cursor(Direction.FORWARD);
			
			// an optimization
			// check to see if the cursor contains the value.
			// if not then we can just not bother using this cursor
			createCursors.add(new InvocationHandler<Boolean>(propertyCursor.to(member)) {
				protected IResult onSuccess(Boolean found) throws Exception {
					if (!found)
						return TaskUtils.DONE;
					return new InvocationHandler<Comparable>(propertyCursor.keyValue()) {
						protected IResult onSuccess(Comparable keyValue) {
							if (KeyValueSet.compare(member, keyValue) != 0) 
								return TaskUtils.DONE;
							return new InvocationHandler<IForwardCursor<Identifier>>(propertyCursor.elementValue()) {
								protected IResult onSuccess(IForwardCursor<Identifier> elementValue) {
									cursors.add(elementValue);
									return TaskUtils.DONE;
								}
							};
						}
					};
				}
			});
		}
		return new Handler(createCursors) {
			protected IResult onSuccess() {
				if (cursors.isEmpty())
					return asResult(new IKeyValueCursor.EmptyForwardCursor<Identifier>());
				if (cursors.size() == 1)
					return asResult(cursors.get(0));
				if (Quantifier.ALL == quantifier)
					return asResult(new ConjunctiveCursor<Identifier>(cursors));
				return asResult(new DisjunctiveCursor<Identifier>(cursors));
			}
		};
	}

	/**
	 * Create a cursor that navigates the given property index and returns the identifiers 
	 * associated with entries that are greater than (or greater than or equal) to the given values.
	 * 
	 * @param index  The property to navigate
	 * @param op  either GREATER_THAN or GREATER_THAN_OR_EQUAL
	 * 
	 * @param values the values to match
	 */
	private <K extends Comparable<K> & Serializable> IResult<IForwardCursor<Identifier>> 
	createGreaterThanCursor(PropertyIndex index, final FilterOperator op, K[] values) 
	{
		final K value= values[0]; // only one value is considered when evaluating gt/ge
		final IPropertyCursor<K> propertyCursor= index.cursor(Direction.FORWARD);
		
		// check to see if there are actually any results.
		// if not then return an empty cursor
		IResult noResults= new InvocationHandler<Boolean>(propertyCursor.to(value)) {
			protected IResult onSuccess(Boolean found) {
				if (!found) 
					return asResult(new IKeyValueCursor.EmptyForwardCursor<Identifier>());
				return new InvocationHandler<K>(propertyCursor.keyValue()) {
					protected IResult onSuccess(K keyValue) {
						if (op == GREATER_THAN && KeyValueSet.compare(value, keyValue) == 0) { 
							return new InvocationHandler<Boolean>(propertyCursor.next()) {
								protected IResult onSuccess(Boolean found) {
									if (!found)
										return asResult(new IKeyValueCursor.EmptyForwardCursor<Identifier>());
									return TaskUtils.NULL;
								}
							};
						}
						return TaskUtils.NULL;
					}
				};
			}
		};
		
		return new Handler(noResults) {
			protected IResult onSuccess() throws Exception {
				if (incoming().getResult() != null) 
					return incoming(); 
				return IdentifierCursorAdaptor.create(propertyCursor);
			}
		};
	}

	private <K extends Comparable<K> & Serializable> IResult<IForwardCursor<Identifier>> 
	createLessThanCursor(PropertyIndex index, final FilterOperator op, K[] values) 
	{
		final K value= values[0]; // only one value is considered when evaluating lt/le
		final IPropertyCursor<K> propertyCursor= index.cursor(Direction.REVERSE);
		
		// check to see if there are actually any results.
		// if not then return an empty cursor
		IResult noResults= new InvocationHandler<Boolean>(propertyCursor.to(value)) {
			protected IResult onSuccess(Boolean found) {
				if (!found) 
					return asResult(new IKeyValueCursor.EmptyForwardCursor<Identifier>());
				return new InvocationHandler<K>(propertyCursor.keyValue()) {
					protected IResult onSuccess(K keyValue) {
						if (op == LESS_THAN && KeyValueSet.compare(value, keyValue) == 0) { 
							return new InvocationHandler<Boolean>(propertyCursor.next()) {
								protected IResult onSuccess(Boolean found) {
									if (!found)
										return asResult(new IKeyValueCursor.EmptyForwardCursor<Identifier>());
									return TaskUtils.NULL;
								}
							};
						}
						return TaskUtils.NULL;
					}
				};
			}
		};
		
		return new Handler(noResults) {
			protected IResult onSuccess() throws Exception {
				if (incoming().getResult() != null) 
					return incoming(); 
				return IdentifierCursorAdaptor.create(propertyCursor);
			}
		};
	}
	
//	/**
//	 * @return 	An forward cursor adapter that that iterates through a property 
//	 * 			cursor and returns all the Identifiers. 
//	 */
//	IResult<IForwardCursor<Identifier>> createForwardCursorAdapter(final IPropertyCursor propertyCursor) {
//		RamStorageProvider ramStorageProvider= new RamStorageProvider();
//		EntityStorage entityStorage= new EntityStorage(ramStorageProvider);
//		return new InvocationHandler<IEntityStorage.Session>(entityStorage.connect()) {
//			protected IResult onSuccess(IEntityStorage.Session session) throws Exception {
//				final IOrderedSet set= KeyValueSet.create(session, Identifier.create()).get();
//				
//				IResult fillSet= new Handler(set.insert(propertyCursor.elementValue())) {
//					protected IResult onSuccess() throws Exception {
//						return new WhileHandler() {
//							protected IResult<Boolean> While() throws Exception {
//								return propertyCursor.next();
//							}
//							protected IResult<Void> Do() throws Exception {
//								return new InvocationHandler<IForwardCursor<Identifier>>(propertyCursor.elementValue()) {
//									protected IResult onSuccess(final IForwardCursor<Identifier> ids) {
//										return new WhileHandler() {
//											protected IResult<Boolean> While() throws Exception {
//												return ids.next();
//											}
//											protected IResult<Void> Do() throws Exception {
//												return set.insert(ids.keyValue());
//											}
//										};
//									}
//								};
//							}
//						};
//					}	
//				};
//				
//				return new Handler(fillSet) {
//					protected IResult onSuccess() throws Exception {
//						return asResult(set.forwardCursor());
//					}
//				};
//			}
//		};
//	}
}
