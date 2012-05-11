/*******************************************************************************
 * Copyright (c) 2009 Ted Stockwell
 * 
 * This file is part of the Contrail Database System.
 * 
 * Contrail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License Version 3
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.googlecode.contraildb.core.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import com.googlecode.contraildb.core.ContrailException;
import com.googlecode.contraildb.core.IContrailService;
import com.googlecode.contraildb.core.IContrailSession;
import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.storage.StorageSystem;
import com.googlecode.contraildb.core.storage.provider.IStorageProvider;
import com.googlecode.contraildb.core.utils.Handler;
import com.googlecode.contraildb.core.utils.TaskUtils;


/**
 * Implementation of Contrail service.
 * 
 * @author Ted Stockwell
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ContrailServiceImpl implements IContrailService {
	
	private Stack<IContrailSession> _transactions= new Stack<IContrailSession>();
	
	StorageSystem _storageSystem;
	
	public static IResult<ContrailServiceImpl> create(IStorageProvider storageProvider) 
	{
		final IResult<StorageSystem> createStorageSystem= StorageSystem.create(storageProvider);
		return new Handler(createStorageSystem) {
			protected IResult onSuccess() throws Exception {
				ContrailServiceImpl impl= new ContrailServiceImpl();
				impl._storageSystem= createStorageSystem.getResult();
				return TaskUtils.asResult(impl);
			}
		}.toResult();
	}
	private ContrailServiceImpl() 
	{
	}

	@Override
	public IResult<IContrailSession> beginSession(Mode mode) {
		final IResult<ContrailSessionImpl> create= ContrailSessionImpl.create(this, mode);
		return new Handler(create) {
			protected IResult onSuccess() throws Exception {
				ContrailSessionImpl session= create.getResult();
				_transactions.push(session);
				return TaskUtils.asResult(session);
			}
		}.toResult();
	}

	@Override
	public IResult<IContrailSession> beginSession(long revisionNumber) 
	{
		final IResult<ContrailSessionImpl> create= ContrailSessionImpl.create(this, revisionNumber);
		return new Handler(create) {
			protected IResult onSuccess() throws Exception {
				ContrailSessionImpl session= create.getResult();
				_transactions.push(session);
				return TaskUtils.asResult(session);
			}
		}.toResult();
	}

	@Override
	public IResult<Collection<IContrailSession>> getActiveSessions() {
		return TaskUtils.asResult(Collections.unmodifiableCollection(_transactions));
	}

	@Override
	public IResult<List<Long>> getAvailableRevisions() 
	{
		return _storageSystem.getAvailableRevisions();
	}
	
	IResult<Void> onClose(IContrailSession session) {
		_transactions.remove(session);
		return TaskUtils.DONE;
	}

	@Override
	public IResult<Void> close() {
		return _storageSystem.close();
	}

//	@Override
//	public Transaction beginSession() {
//		try {
//			ReadWriteSessionImpl transaction= new ReadWriteSessionImpl(this);
//			_transactions.push(transaction);
//			return transaction;
//		} 
//		catch (IOException e) {
//			throw new ContrailException("Error in beginTransaction", e);
//		}
//	}
//
//	@Override
//	public void delete(final Key... keys) {
//		delete(Arrays.asList(keys));
//	}
//
//	@Override
//	public void delete(final Iterable<Key> keys) {
//		ReadWriteSessionImpl transaction= null;
//		boolean success= false;
//		try {			
//			transaction= new ReadWriteSessionImpl(this);
//			delete(transaction, keys);
//			transaction.commit();
//			success= true;
//		} 
//		catch (Throwable e) {
//			throw new ContrailException("error deleting entities", e);
//		}
//		finally {
//			if (!success) 
//				safeRollback(transaction);
//		}
//	}
//
//	@Override
//	public void delete(Transaction transaction, Key... keys) {
//		delete(transaction, Arrays.asList(keys));
//	}
//
//	@Override
//	public void delete(final Transaction transaction, final Iterable<Key> keys) {
//		((ReadWriteSessionImpl)transaction).delete(keys);
//	}
//
//	@Override
//	public Entity get(final Key key) throws EntityNotFoundException {
//			return get(null, key);
//	}
//
//	@Override
//	public Map<Key, Entity> get(Iterable<Key> keys) {
//			return get(null, keys);
//	}
//
//	@Override
//	public Entity get(Transaction transaction, Key key)
//	throws EntityNotFoundException {
//		Entity[] results= get((ReadOnlySessionImpl)transaction, new Key[] { key });
//		if (results.length <= 0)
//	    	throw new EntityNotFoundException(key);
//		if (1 < results.length)
//			throw new TooManyResultsException();
//		return results[0];
//	}
//
//	@Override
//	public Map<Key, Entity> get(Transaction transaction, Iterable<Key> arg0) {
//		ArrayList<Key> keys= new ArrayList<Key>();
//		Iterator<Key> i= arg0.iterator();
//		while (i.hasNext())
//			keys.add(i.next());
//		Entity[] results= get((ReadOnlySessionImpl)transaction, keys.toArray(new Key[keys.size()]));
//		Map<Key, Entity> map= new HashMap<Key, Entity>(results.length);
//		for (Entity e : results) 
//			map.put(e.getKey(), e);
//		return map;
//	}
//	
//	public Entity[] get(ReadOnlySessionImpl transaction, final Key[] keys) {
//		try {
//			if (transaction == null)
//				transaction= getInternalSession();
//			if (keys.length <= 0)
//				return new Entity[0];
//			org.apache.lucene.search.BooleanQuery query= new BooleanQuery();
//			for (Key key : keys) 
//				query.add(new TermQuery(
//						__keyTerm.createTerm(KeyFactory.keyToString(key))), 
//						BooleanClause.Occur.SHOULD);
//			ScoreDoc[] hits = transaction.search(query, keys.length);
//			Entity[] entities= new Entity[hits.length];
//			int i= 0;
//			for (ScoreDoc scoreDoc : hits) 
//			    entities[i++]= transaction.getEntity(scoreDoc.doc);
//			return entities;
//		} 
//		catch (DatastoreFailureException x) {
//			throw x;
//		}
//		catch (Throwable e) {
//			throw new ContrailException("Error getting entities", e);
//		}
//	}
//
//	@Override
//	public Collection<Transaction> getActiveTransactions() {
//		return Collections.unmodifiableCollection((Collection)_transactions);
//	}
//
//	@Override
//	public Transaction getCurrentTransaction() {
//		return _transactions.lastElement();
//	}
//
//	@Override
//	public Transaction getCurrentTransaction(Transaction arg0) {
//		if (_transactions.isEmpty())
//			return arg0;
//		return _transactions.lastElement();
//	}
//	@Override
//	public PreparedQuery prepare(Transaction transaction, com.google.appengine.api.datastore.Query gaeQuery) {
//		return prepare(transaction, new ContrailQuery(gaeQuery));
//	}
//	
//	@Override
//	public PreparedQuery prepare(Transaction transaction, final ContrailQuery contrailQuery) {
//		
//		BooleanQuery query= new BooleanQuery();
//		query.add(new TermQuery(
//				__kindTerm.createTerm(contrailQuery.getKind())), 
//				BooleanClause.Occur.MUST);
//		
//		List<FilterPredicate> filters= contrailQuery.getFilterPredicates();
//		for (FilterPredicate filter : filters) {
//			org.apache.lucene.search.Query filterQuery= convertToLuceneQuery(filter);
//			
//			done: {
//				
//				/**
//				 * Lucene (2.4.1) doesn't do the right thing for this query (in Lucene syntax)...
//				 * 		+kind:Group +(-members:Smith)
//				 * ...so we work around here by changing the query to this...
//				 * 		+kind:Group -members:Smith
//				 */
//				if (filterQuery instanceof BooleanQuery) {
//					BooleanClause[] clauses= ((BooleanQuery)filterQuery).getClauses();
//					if (clauses.length <= 1 && clauses[0].isProhibited()) {
//						query.add(clauses[0]);
//						break done;
//					}
//				}
//				
//				query.add(filterQuery,BooleanClause.Occur.MUST);
//			}
//			
//		}
//		
//		
//		Sort sort= new Sort();
//		List<SortPredicate> sortPredicates= contrailQuery.getSortPredicates();
//		if (0 < sortPredicates.size()) {
//			SortField[] sortFields= new SortField[sortPredicates.size()];
//			int i= 0;
//			for (SortPredicate sortPredicate : sortPredicates) {
//				boolean reverse= SortDirection.DESCENDING.equals(sortPredicate.getDirection());
//				sortFields[i++]= new SortField(sortPredicate.getPropertyName(), reverse);
//			}
//			sort.setSort(sortFields);
//		}
//		
//		return new PreparedQueryImpl(this, (ReadWriteSessionImpl)transaction, query, sort);
//
//	}
//	
//	private static org.apache.lucene.search.Query convertToLuceneQuery(FilterPredicate filter) {
//		org.apache.lucene.search.Query filterQuery= null;
//		
//		FilterOperator op= filter.getOperator();
//		String propertyName= filter.getPropertyName();
//		
//		if (FilterOperator.EQUAL == op) {
//			String value= CodecUtils.toString(filter.getValue());
//			filterQuery= new TermQuery(new Term(propertyName, value));
//		}
//		else if (FilterOperator.GREATER_THAN == op) {
//			String value= CodecUtils.toString(filter.getValue());
//			filterQuery= new TermRangeQuery(propertyName, value, null, false, true);
//		}
//		else if (FilterOperator.GREATER_THAN_OR_EQUAL == op) {
//			String value= CodecUtils.toString(filter.getValue());
//			filterQuery= new TermRangeQuery(propertyName, value, null, true, true);
//		}
//		else if (FilterOperator.LESS_THAN == op) {
//			String value= CodecUtils.toString(filter.getValue());
//			filterQuery= new TermRangeQuery(propertyName, null, value, true, false);
//		}
//		else if (FilterOperator.LESS_THAN_OR_EQUAL == op) {
//			String value= CodecUtils.toString(filter.getValue());
//			filterQuery= new TermRangeQuery(propertyName, null, value, true, true);
//		}
//		else if (FilterOperator.OR == op) {
//			BooleanQuery booleanQuery= new BooleanQuery();
//			FilterPredicate[] predicates= filter.getClauses();
//			for (FilterPredicate predicate : predicates) {
//				org.apache.lucene.search.Query query= convertToLuceneQuery(predicate); 
//				booleanQuery.add(query, BooleanClause.Occur.SHOULD);
//			}
//			filterQuery= booleanQuery;
//		}
//		else if (FilterOperator.AND == op) {
//			BooleanQuery booleanQuery= new BooleanQuery();
//			FilterPredicate[] predicates= filter.getClauses();
//			for (FilterPredicate predicate : predicates) {
//				org.apache.lucene.search.Query query= convertToLuceneQuery(predicate); 
//				booleanQuery.add(query, BooleanClause.Occur.MUST);
//			}
//			filterQuery= booleanQuery;
//		}
//		else if (FilterOperator.IN == op) {
//			Object object= filter.getValue();
//			if (object instanceof QuantifiedValues) {
//				QuantifiedValues quantifiedValues= (QuantifiedValues)object;
//				BooleanClause.Occur occur; 
//				switch (quantifiedValues.getType()) {
//					case SOME: occur= BooleanClause.Occur.SHOULD; break;
//					case ALL: occur= BooleanClause.Occur.MUST; break;
//					case NONE: occur= BooleanClause.Occur.MUST_NOT; break;
//					default: throw new RuntimeException("unsupported quantifier:"+quantifiedValues.getType());
//				}
//				BooleanQuery booleanQuery= new BooleanQuery();
//				Object[] values= quantifiedValues.getValues();
//				for (Object quantifiedValue : values) {
//					String term= CodecUtils.toString(quantifiedValue);
//					booleanQuery.add(new TermQuery(new Term(propertyName, term)), occur);
//				}
//				filterQuery= booleanQuery;
//			}
//			else if (object instanceof ContrailQuery) {
//				//TODO
//				throw new RuntimeException("unfinished");
//			}
//			else if (object instanceof Collection) {
//				//TODO
//				throw new RuntimeException("unfinished");
//			}
//			else if (object instanceof Object[]) {
//				//TODO
//				throw new RuntimeException("unfinished");
//			}
//			else {
//				String value= CodecUtils.toString(object);
//				filterQuery= new TermQuery(new Term(propertyName, value));
//			}
//		}
//		else 
//			throw new ContrailException("Unsupported operator:"+op);
//
//		return filterQuery;
//	}
//
//	public PreparedQuery prepare(com.google.appengine.api.datastore.Query gaeQuery) {
//			return prepare(new ContrailQuery(gaeQuery));
//	}
//
//	@Override
//	public Key put(final Entity entity) {
//		ArrayList<Entity> list= new ArrayList<Entity>(1);
//		list.add(entity);
//		List<Key> keys= put(list);
//		return keys.get(0);
//	}
//
//	@Override
//	public List<Key> put(final Iterable<Entity> entities) {
//		ReadWriteSessionImpl transaction= null;
//		boolean success= false;
//		try {			
//			transaction= new ReadWriteSessionImpl(this);
//			List<Key> list= put(transaction, entities);
//			transaction.commit();
//			success= true;
//			return list;
//		} 
//		catch (Throwable e) {
//			throw new ContrailException("error saving entities", e);
//		}
//		finally {
//			if (!success) 
//				safeRollback(transaction);
//		}
//	}
//
//	private void safeRollback(ReadWriteSessionImpl transaction) {
//		if (transaction != null) {
//			try { 
//				transaction.rollback(); 
//			}
//			catch (Throwable t) {
//				Logging.log(Level.WARNING, "Error rolling back transaction", t);
//			}
//		}
//	}
//
//	@Override
//	public Key put(Transaction transaction, Entity arg1) {
//		ArrayList<Entity> list= new ArrayList<Entity>(1);
//		list.add(arg1);
//		List<Key> list2= put(transaction, list);
//		return list2.get(0);
//	}
//
//	@Override
//	public List<Key> put(final Transaction transaction, final Iterable<Entity> entities) {
//		
//		try {
//			((ReadWriteSessionImpl)transaction).put(entities);
//			
//			final ArrayList<Key> keys= new ArrayList<Key>();
//			for (Entity entity: entities)
//				keys.add(entity.getKey());
//			return keys;
//		} 
//		catch (IOException e) {
//			throw new ContrailException("Error putting entries", e);
//		}
//	}
//
//	@Override
//	public void close() {
//		clearInternalTransaction();
//	}
//
//	void onCommit(ReadWriteSessionImpl contrailTransaction) {
//		_transactions.remove(contrailTransaction);
//		clearInternalTransaction();
//	}
//
//	boolean isActive(ReadWriteSessionImpl session) {
//		return _transactions.contains(session);		
//	}
//
//	void onRollback(ReadWriteSessionImpl contrailTransaction) {
//		_transactions.remove(contrailTransaction);
//	}
//	
//	public synchronized ReadOnlySessionImpl getInternalSession() throws IOException {
//		if (_internalSession == null) 
//			_internalSession= new ReadOnlySessionImpl(this);
//		return _internalSession;
//	}
//
//	private void clearInternalTransaction() {
//		ReadOnlySessionImpl t;
//		synchronized (this) {
//			t= _internalSession;
//			_internalSession= null;
//		}
//		if (t != null) {
//			try {
//				t.close();
//			}
//			catch (Throwable e) {
//				Logging.log(Level.WARNING, "Error closing internal transaction", e);
//			}
//		}
//	}
//
//	@Override
//	public PreparedQuery prepare(ContrailQuery query) {
//		return prepare(null, query);
//	}

	
}
