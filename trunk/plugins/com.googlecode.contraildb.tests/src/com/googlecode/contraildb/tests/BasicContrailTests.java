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
package com.googlecode.contraildb.tests;

import static com.googlecode.contraildb.core.ContrailQuery.all;
import static com.googlecode.contraildb.core.ContrailQuery.and;
import static com.googlecode.contraildb.core.ContrailQuery.any;
import static com.googlecode.contraildb.core.ContrailQuery.eq;
import static com.googlecode.contraildb.core.ContrailQuery.gt;
import static com.googlecode.contraildb.core.ContrailQuery.lt;
import static com.googlecode.contraildb.core.ContrailQuery.ne;
import static com.googlecode.contraildb.core.ContrailQuery.or;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import com.google.appengine.tools.development.ApiProxyLocalFactory;
import com.google.apphosting.api.ApiProxy;
import com.googlecode.contraildb.core.ContrailQuery;
import com.googlecode.contraildb.core.ContrailQuery.SortDirection;
import com.googlecode.contraildb.core.ContrailServiceFactory;
import com.googlecode.contraildb.core.IContrailService;
import com.googlecode.contraildb.core.IContrailService.Mode;
import com.googlecode.contraildb.core.IContrailSession;
import com.googlecode.contraildb.core.IPreparedQuery;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.Item;
import com.googlecode.contraildb.core.storage.provider.IStorageProvider;
import com.googlecode.contraildb.core.storage.provider.RamStorageProvider;


/**
 * Basic ContrailService API tests.
 * 
 * @author Ted Stockwell
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BasicContrailTests extends TestCase {
	
	// properties
	private static final String TYPE = "type";
	private static final String PROFESSION = "profession";
	private static final String SALARY = "salary";
	private static final String MARRIED = "married";
	private static final String MEMBERS = "members";
	private static final String CHILDREN = "children";
	
	// types
	private static final String PERSON = "Person";
	private static final String POSSE = "Posse";
	private static final String DEITY = "Deity";

	IStorageProvider _storageProvider;
	IContrailService _datastore;
	HashMap<Identifier, Item> _fixturesByKey= new HashMap<Identifier, Item>();
	HashMap<String, List<Item>> _fixturesByType= new HashMap<String, List<Item>>();
	HashMap<String, List<Identifier>> _fixtureKeysByType= new HashMap<String, List<Identifier>>();
	
	@Override
	protected void tearDown() throws Exception {
		_datastore.close();
	}
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		// setup Google App Engine environment
		ApiProxy.setEnvironmentForCurrentThread(new TestEnvironment());
		ApiProxyLocalFactory factory= new ApiProxyLocalFactory();
		factory.setApplicationDirectory(new File("."));
		ApiProxy.setDelegate(factory.create());

		
		_storageProvider= new RamStorageProvider();
//		_storageProvider= new FileStorageProvider(new File("/temp/test/contrail"), true);
//		_storageProvider= new ErrorDetectingStorageSPI(
//				new RamStorageSPI(), 
//				new SimpleFileStorageSPI(new File("/temp/test/contrail"), true));
		_datastore = 
			ContrailServiceFactory.getContrailService(_storageProvider);
		
		// put data
		Item fixture= new Item("Good Guys");
		fixture.setProperty(TYPE, POSSE);
		HashSet<Identifier> goodGuys= new HashSet<Identifier>();
		fixture.setProperty(MEMBERS, goodGuys);
		addFixture(fixture);
		
		HashSet<Identifier> badGuys= new HashSet<Identifier>();
		fixture= new Item("Bad Guys");
		fixture.setProperty(TYPE, POSSE);
		fixture.setProperty(MEMBERS, badGuys);
		addFixture(fixture);
		
		fixture= new Item("Politicians");
		fixture.setProperty(TYPE, POSSE);
		HashSet<Identifier> politicians= new HashSet<Identifier>();
		fixture.setProperty(MEMBERS, politicians);
		addFixture(fixture);
		
		
		fixture= new Item("Gandhi");
		fixture.setProperty(TYPE, PERSON);
		fixture.setProperty(PROFESSION, "lawyer");
		fixture.setProperty(SALARY, 20000.00);
		fixture.setProperty(MARRIED, Boolean.TRUE);
		goodGuys.add(fixture.getId());
		addFixture(fixture);
		
		fixture= new Item("Hitler");
		fixture.setProperty(TYPE, PERSON);
		fixture.setProperty(PROFESSION, "politician");
		fixture.setProperty(SALARY, 100000.00);
		fixture.setProperty(MARRIED, Boolean.FALSE);
		badGuys.add(fixture.getId());
		politicians.add(fixture.getId());
		addFixture(fixture);
		
		fixture= new Item("Churchill");
		fixture.setProperty(TYPE, PERSON);
		fixture.setProperty(PROFESSION, "politician");
		fixture.setProperty(SALARY, 20000.50);
		fixture.setProperty(MARRIED, Boolean.TRUE);
		goodGuys.add(fixture.getId());
		politicians.add(fixture.getId());
		addFixture(fixture);
		
		fixture= new Item("Jesus");
		fixture.setProperty(TYPE, PERSON);
		fixture.setProperty(PROFESSION, "carpenter");
		fixture.setProperty(SALARY, 0.00);
		fixture.setProperty(MARRIED, Boolean.FALSE);
		goodGuys.add(fixture.getId());
		addFixture(fixture);
		Item jesus= fixture;
		
		fixture= new Item("God");
		fixture.setProperty(TYPE, DEITY);
		fixture.setProperty(CHILDREN, jesus.getId());
		goodGuys.add(fixture.getId());
		addFixture(fixture);
		
		fixture= new Item("Einstein");
		fixture.setProperty(TYPE, PERSON);
		fixture.setProperty(PROFESSION, "physicist");
		fixture.setProperty(SALARY, 99000.50);
		fixture.setProperty(MARRIED, Boolean.TRUE);
		goodGuys.add(fixture.getId());
		addFixture(fixture);
		
		// test put
		IContrailSession transaction= _datastore.beginSession(Mode.READWRITE);
		for (String kind : _fixturesByType.keySet()) {
			List<Item> list= _fixturesByType.get(kind);
			transaction.store(list);
		}
		transaction.commit();
	}
	
	private void addFixture(Item fixture) {
		String fixtureType= fixture.getProperty(TYPE);
		List<Item> list= _fixturesByType.get(fixtureType);
		if (list == null) {
			list= new ArrayList<Item>();
			_fixturesByType.put(fixtureType, list);
		}
		list.add(fixture);
		
		List<Identifier> keys= _fixtureKeysByType.get(fixtureType);
		if (keys == null) {
			keys= new ArrayList<Identifier>();
			_fixtureKeysByType.put(fixtureType, keys);
		}
		keys.add(fixture.getId());
		_fixturesByKey.put(fixture.getId(), fixture);
	}
	
	public void testBasics() throws Exception {
		Identifier id= Identifier.create("1");
		String name= "$$$$$$$$$$$$$$$$$$";
		ContrailQuery query = new ContrailQuery().where(eq("NAME", name));
		IContrailSession transaction= _datastore.beginSession(Mode.READWRITE);
		{
			Item item= new Item(id);
			item.setProperty("NAME", name);
			transaction.store(item);
			
			Item f= transaction.fetch(id);
			assertNotNull(f);
			assertEquals(id, f.getId());
			assertNotNull(transaction.prepare(query).item());
		}
		transaction.commit();
		
		transaction= _datastore.beginSession(Mode.READWRITE);
		{
			Item f= transaction.fetch(id);
			assertNotNull(f);
			assertEquals(id, f.getId());
			assertNotNull(transaction.prepare(query).item());
		}
		transaction.commit();
	}

	public void testBootstrap() throws Exception {
		
		IContrailSession transaction= _datastore.beginSession(Mode.READWRITE);

		for (String kind : _fixturesByType.keySet()) {
			List<Identifier> keys= _fixtureKeysByType.get(kind);
			
			// fetch all keys and make sure we get them all 
			Collection<Item> allEntities= transaction.fetch(keys);
			assertEquals(keys.size(), allEntities.size());

			// fetch all entities and check all fetched properties
			for (Identifier key : keys) {
				Item fixture= _fixturesByKey.get(key);
				Item fetched= transaction.fetch(key);
				assertNotNull(fetched);
				assertEquals(key, fetched.getId());
				assertEquals(kind, fetched.getProperty(TYPE));
				
				Map<String, Object> fixtureProperties= fixture.getAllProperties();
				for (String property: fixtureProperties.keySet()) {
					Object fixtureProperty= fixture.getProperty(property);
					Object fetchedProperty= fetched.getProperty(property);
					if (!fixtureProperty.equals(fetchedProperty))
						assertEquals("property values do not match:"+property, fixtureProperty, fetchedProperty);
				}
			}
			
			// delete a Record and make sure its deleted
			transaction.delete(keys.get(keys.size()/2));
			ContrailQuery query = new ContrailQuery().where(eq(TYPE, kind));
			assertEquals(keys.size()-1, transaction.prepare(query).count());
		}

		transaction.close();
	}
	
	public void testBasicQueries() throws Exception {
		IContrailSession transaction= _datastore.beginSession(Mode.READONLY);

			int personCount= _fixturesByType.get(PERSON).size();

			// test query
			ContrailQuery query = new ContrailQuery().where(eq(TYPE, PERSON));
			IPreparedQuery results= transaction.prepare(query);
			assertEquals(personCount, results.count());

			// test filters
			query = new ContrailQuery().where(eq(TYPE, PERSON));
			query.addFilter(PROFESSION, ContrailQuery.FilterOperator.EQUAL, "lawyer");
			results= transaction.prepare(query);
			assertEquals(1, results.count());
			assertEquals(Identifier.create("Gandhi"), results.item().getId());

			query = new ContrailQuery().where(eq(TYPE, PERSON));
			query.addFilter(PROFESSION, ContrailQuery.FilterOperator.EQUAL, "politician");
			results= transaction.prepare(query);
			assertEquals(2, results.count());

			query = new ContrailQuery().where(eq(TYPE, PERSON));
			query.addFilter(PROFESSION, ContrailQuery.FilterOperator.GREATER_THAN, "lawyer");
			results= transaction.prepare(query);
			assertEquals(3, results.count());

			query = new ContrailQuery();
			query.addFilter(PROFESSION, ContrailQuery.FilterOperator.LESS_THAN, "lawyer");
			results= transaction.prepare(query);
			assertEquals(1, results.count());

			query = new ContrailQuery().where(eq(TYPE, PERSON));
			query.addFilter(PROFESSION, ContrailQuery.FilterOperator.LESS_THAN, "lawyer");
			results= transaction.prepare(query);
			assertEquals(1, results.count());

			query = new ContrailQuery();
			query.addFilter(PROFESSION, ContrailQuery.FilterOperator.GREATER_THAN, "carpenter");
			query.addFilter(PROFESSION, ContrailQuery.FilterOperator.LESS_THAN, "politician");
			results= transaction.prepare(query);
			assertEquals(2, results.count());

			query = new ContrailQuery().where(eq(TYPE, PERSON));
			query.addFilter(PROFESSION, ContrailQuery.FilterOperator.GREATER_THAN, "carpenter");
			query.addFilter(PROFESSION, ContrailQuery.FilterOperator.LESS_THAN, "politician");
			results= transaction.prepare(query);
			assertEquals(2, results.count());
			
			query = new ContrailQuery().where(eq(PROFESSION, "politician"));
			results= transaction.prepare(query);
			assertEquals(2, results.count());
			
			query = new ContrailQuery().where(ne(PROFESSION, "politician"));
			results= transaction.prepare(query);
			assertEquals(3, results.count());
			
			query = new ContrailQuery().where(or( eq(PROFESSION, "carpenter"), eq(PROFESSION, "politician")));
			results= transaction.prepare(query);
			assertEquals(3, results.count());
			
			query = new ContrailQuery().where(gt(PROFESSION, "carpenter"));
			results= transaction.prepare(query);
			assertEquals(4, results.count());
			
			query = new ContrailQuery().where(lt(PROFESSION, "politician"));
			results= transaction.prepare(query);
			assertEquals(3, results.count());
			
			query = new ContrailQuery().where(and(gt(PROFESSION, "carpenter"), lt(PROFESSION, "politician")));
			results= transaction.prepare(query);
			assertEquals(2, results.count());

			query = new ContrailQuery().where(and(eq(TYPE, PERSON), gt(PROFESSION, "carpenter"), lt(PROFESSION, "politician")));
			results= transaction.prepare(query);
			assertEquals(2, results.count());
			
			query = new ContrailQuery().where(
					or( and(gt(PROFESSION, "carpenter"), lt(PROFESSION, "politician")), 
						eq(PROFESSION, "carpenter")));
			results= transaction.prepare(query);
			assertEquals(3, results.count());
			
			query = new ContrailQuery().where(and(eq(TYPE, PERSON), 
					or( and(gt(PROFESSION, "carpenter"), lt(PROFESSION, "politician")), 
						eq(PROFESSION, "carpenter"))));
			results= transaction.prepare(query);
			assertEquals(3, results.count());
			
			// test in collection
			Identifier keyHitler= Identifier.create("Hitler");
			Identifier keyChurchill= Identifier.create("Churchill");
			query = new ContrailQuery().where(eq(TYPE, POSSE)); 
			query.addFilter(MEMBERS, ContrailQuery.FilterOperator.EQUAL, keyHitler);
			results= transaction.prepare(query); // find POSSES that contain Hitler as a member
			assertEquals(2, results.count());
			query.addFilter(MEMBERS, ContrailQuery.FilterOperator.EQUAL, keyChurchill);
			results= transaction.prepare(query); // find POSSES that contain Hitler and Churchill as members
			assertEquals(1, results.count());
			
			query = new ContrailQuery().where(and(eq(TYPE, POSSE), eq(MEMBERS, keyHitler)));
			results= transaction.prepare(query); // find POSSES that contain Hitler as a member
			assertEquals(2, results.count());
			
			query = new ContrailQuery().where(and(eq(TYPE, POSSE), any(MEMBERS, keyHitler, keyChurchill)));
			results= transaction.prepare(query); // find POSSES that either Hitler or Churchill as members
			assertEquals(3, results.count());
			
			query = new ContrailQuery().where(and(eq(TYPE, POSSE), all(MEMBERS, keyHitler, keyChurchill)));
			results= transaction.prepare(query); // find POSSES that contain Hitler and Churchill as members
			assertEquals(1, results.count());
			assertEquals(Identifier.create("Politicians"), results.item().getId());
			
			query = new ContrailQuery().where(eq(TYPE, PERSON));
			results= transaction.prepare(query); // find POSSES that contain Hitler and Churchill as members
			assertNotNull(results.identifiers());
			
//			query = new ContrailQuery().where(and(eq(TYPE, POSSE), none(MEMBERS, keyHitler)));
//			results= transaction.prepare(query); // find POSSES that do not contain Hitler 
//			assertEquals(1, results.countEntities());
//			assertEquals(Identifier.create("Good Guys"), results.asSingleRecord().getId());
//			
//			query = new ContrailQuery().where(and(eq(TYPE, POSSE), or(none(MEMBERS, keyHitler), all(MEMBERS, keyChurchill))));
//			results= transaction.prepare(query); // find POSSES that do not contain Hitler or contain Churchill 
//			assertEquals(2, results.countEntities());
//			
//			query = new ContrailQuery().where(and(eq(TYPE, POSSE), none(MEMBERS, keyHitler, keyChurchill)));
//			results= transaction.prepare(query); // find POSSES that do not contain Hitler and do not contain Churchill 
//			assertEquals(0, results.countEntities());
//			
//			query = new ContrailQuery().where(id(Identifier.create("Jesus")));
//			results= transaction.prepare(query);
//			assertEquals(2, results.countEntities());
			
			//
			// joins
			//
			
			// find items that have a child whose profession is carpenter
			query = new ContrailQuery();
			query.join(CHILDREN).where(eq(PROFESSION, "carpenter"));
			results= transaction.prepare(query);
			assertEquals(1, results.count());
			
			
	// support subqueries....		
//			// find orders for all customers named smith
//			query= new Query("Orders").where(in("customer", new Query("Customer").where(eq("name", "Smith")).setStringsOnly()));

		transaction.close();
	}
	
	public void testSorting() throws Exception {
		IContrailSession transaction= _datastore.beginSession(Mode.READONLY);

			for (SortDirection direction : SortDirection.values()) {
				for (String property : Arrays.asList(PROFESSION, SALARY, MARRIED)) {
					ContrailQuery query = new ContrailQuery().where(eq(TYPE, PERSON));
					query.addSort(property, direction);
					IPreparedQuery<Item> results= transaction.prepare(query);
					Comparable last= null;
					List<Item> items= results.list();
					for (Item e : items) {
						Object value= e.getProperty(property);
						if (last != null) {
							String errMsg= "Incorrect sort order, property="+property+", order="+direction+", previous value="+last+", current value="+value;
							if (direction == SortDirection.ASCENDING) {
								assertTrue(errMsg, last.compareTo(value) <= 0);
							}
							else
								assertTrue(errMsg, last.compareTo(value) >= 0);
						}
						last= (Comparable) value;
					}
				}
			}

		transaction.close();
	}
	
	
	public void testBasicTransaction() throws Exception {
			IContrailSession session= _datastore.beginSession(Mode.READWRITE);
			Item object_0_1= new Item("person-0.1");
			session.store(object_0_1);
			session.commit();
			
			IContrailSession T1= _datastore.beginSession(Mode.READWRITE);
			IContrailSession T2= _datastore.beginSession(Mode.READONLY);
			
			// TEST PUT ISOLATION
			// put object-1.1 in transaction 1
			Item object_1_1= new Item("person-1.1");
			T1.store(object_1_1);
			// object-1.1 should be visible in T1 
			// This is different than the Google Datastore but similar to how most databases work 
			// (and I think the Google way is not useful).
			assertNotNull("transaction isolation violated", T1.fetch(object_1_1.getId()));
			// object-1.1 should not be visible outside of T1 since it is not yet committed 
			assertNull("transaction isolation violated", T2.fetch(object_1_1.getId()));
			IContrailSession T3= _datastore.beginSession(Mode.READONLY);
			assertNull("transaction isolation violated", T3.fetch(object_1_1.getId()));
			T3.close();
			IContrailSession T4= _datastore.beginSession(Mode.READWRITE);
			assertNull("transaction isolation violated", T4.fetch(object_1_1.getId()));
			T4.close();
			
			// TEST DELETE ISOLATION
			T1.delete(object_0_1.getId());
			assertNull("transaction isolation violated", T1.fetch(object_0_1.getId()));
			// object-0.1 should still be visible outside of T1 since it is not yet committed 
			assertNotNull("transaction isolation violated", T2.fetch(object_0_1.getId()));
			T3= _datastore.beginSession(Mode.READONLY);
			assertNotNull("transaction isolation violated", T3.fetch(object_0_1.getId()));
			T3.close();
			T4= _datastore.beginSession(Mode.READWRITE);
			assertNotNull("transaction isolation violated", T4.fetch(object_0_1.getId()));
			T4.close();
			
			// TEST COMMIT AND ACTIVE STATE
			assertTrue(T1.isActive());
			T1.commit();
			assertFalse(T1.isActive());

			// TEST PUT ISOLATION
			// object-1.1 should now be visible to new transactions
			T3= _datastore.beginSession(Mode.READONLY);
			assertNotNull(T3.fetch(object_1_1.getId()));
			T3.close();
			T4= _datastore.beginSession(Mode.READWRITE);
			assertNotNull(T4.fetch(object_1_1.getId()));
			T4.close();
			// object-1.1 should still not be visible to T2 since T2 was started before object-1.1 was committed
			assertNull("transaction isolation violated", T2.fetch(object_1_1.getId()));
			
			// TEST DELETE ISOLATION
			// object-0.1 should not be visible to new transactions 
			T3= _datastore.beginSession(Mode.READONLY);
			assertNull(T3.fetch(object_0_1.getId()));
			T3.close();
			T4= _datastore.beginSession(Mode.READWRITE);
			assertNull(T4.fetch(object_0_1.getId()));
			T4.close();
			// object-0.1 should still be visible to T2 
			assertNotNull(T2.fetch(object_0_1.getId()));
	}
	
	/**
	 * Recreates a bug encountered while running Pole Position.
	 * The session.store failed in the second round because there was a problem with the KEY_KIND index. 
	 */
	public void testRecreateMultipleIndexValues() throws Exception {
		Item i1= new Item(Identifier.create("1")).setProperty(Item.KEY_KIND, "TestItem");
		Item i2= new Item(Identifier.create("2")).setProperty(Item.KEY_KIND, "TestItem");
		IContrailSession session= _datastore.beginSession(Mode.READWRITE);
		session.store(i1);
		session.store(i2);
		session.commit();
		session.close();
		
		session= _datastore.beginSession(Mode.READWRITE);
		session.delete(i1);
		session.delete(i2);
		session.commit();
		session.close();
		
		session= _datastore.beginSession(Mode.READWRITE);
		session.store(i1);
		session.store(i2);
		session.commit();
		session.close();
	}
	
	public void testRollback() throws Exception {
		IContrailSession session= _datastore.beginSession(Mode.READWRITE);
		Item object_0_1= new Item("person-0.1");
		session.store(object_0_1);
		session.commit();
		session.close();
		
		IContrailSession T1= _datastore.beginSession(Mode.READWRITE);
		
		// put object-1.1 in transaction 1
		Item object_1_1= new Item("person-1.1");
		T1.store(object_1_1);
		T1.delete(object_0_1.getId());
		
		assertTrue(T1.isActive());
		T1.close(); // abondon any changes
		assertFalse(T1.isActive());

		// object-1.1 should not be visible 
		// object-0.1 should still be visible 
		IContrailSession T3= _datastore.beginSession(Mode.READONLY);
		assertNull("transaction isolation violated", T3.fetch(object_1_1.getId()));
		assertNotNull("transaction isolation violated", T3.fetch(object_0_1.getId()));
		T3.close();
		IContrailSession T4= _datastore.beginSession(Mode.READWRITE);
		assertNull("transaction isolation violated", T4.fetch(object_1_1.getId()));
		assertNotNull("transaction isolation violated", T4.fetch(object_0_1.getId()));
		T4.close();
	}
	
	
}
