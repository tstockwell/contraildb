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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import com.google.appengine.tools.development.ApiProxyLocalFactory;
import com.google.apphosting.api.ApiProxy;
import com.googlecode.contraildb.core.IContrailService.Mode;
import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.async.TaskUtils;
import com.googlecode.contraildb.core.storage.Entity;
import com.googlecode.contraildb.core.storage.EntityStorage;
import com.googlecode.contraildb.core.storage.IEntityStorage;
import com.googlecode.contraildb.core.storage.LockFolder;
import com.googlecode.contraildb.core.storage.ObjectStorage;
import com.googlecode.contraildb.core.storage.StorageSession;
import com.googlecode.contraildb.core.storage.StorageSystem;
import com.googlecode.contraildb.core.storage.provider.IStorageProvider;
import com.googlecode.contraildb.core.storage.provider.RamStorageProvider;
import com.googlecode.contraildb.core.utils.ContrailAction;


/**
 * Test the Contrail storage system.
 * 
 * @author Ted Stockwell
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ContrailStorageTests extends TestCase {
	
	StorageSystem _storage;
	IStorageProvider _rawStorage;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		// setup Google App Engine environment
		ApiProxy.setEnvironmentForCurrentThread(new TestEnvironment());
		ApiProxyLocalFactory factory= new ApiProxyLocalFactory();
		factory.setApplicationDirectory(new File("."));
		ApiProxy.setDelegate(factory.create());

		_rawStorage= new RamStorageProvider();
		//_rawStorage= new FileStorageProvider(new File("/temp/test/contrail"), true);
		_storage= StorageSystem.create(_rawStorage).get();
	}
	
	@Override
	protected void tearDown() throws Exception {
		_storage.close();
		super.tearDown();
	}
	
	/**
	 * Make sure that the Java mkdirs methods works the way we expect...
	 * it should only return true if and only if the folder is created.
	 */
	public void testMkdirsSemantics() {
		while (true) {
			File folder= new File("/"+Identifier.create().getName());
			if (!folder.exists()) {
				assertTrue(folder.mkdirs());
				
				// the folder exists now so mkdirs should return false since the folder is not created
				assertFalse(folder.mkdirs()); 
				break;
			}
		}
	}
	
	
	public void testSimpleCreate() throws Throwable {
		final IResult<byte[]> content= TaskUtils.asResult("hello".getBytes());
		
		for (int f= 0; f < 10; f++) {			
			Identifier identifier= Identifier.create("file-"+f);
			IStorageProvider.Session session= _rawStorage.connect().get();
			assertNotNull(session);
			IResult<Boolean> result= session.create(identifier, content, Integer.MAX_VALUE);
			assertNotNull(result);
			assertTrue(result.get());
			session.delete(identifier);
			session.close().get();
		}
		
	}
	
	public void testObjectStorage() throws Exception {
		for (int i= 0; i < 100; i++) {
			IEntityStorage.Session storage= 
					new EntityStorage(_storage.getStorageProvider()).connect().get();

			Entity object= new Entity("person-"+i);
			storage.store(object);
			assertNotNull(storage.fetch(object.getId()));
			storage.close();
		}
	}
	
	public void testFetchObjectStore() throws Exception {
		
		for (int i= 1; i <= 100; i++) {
			ObjectStorage.Session objectStorage= new ObjectStorage(_rawStorage).connect().get();
			Identifier entity= Identifier.create("person-"+i);
			objectStorage.store(entity, entity);
			if (objectStorage.fetch(entity) == null) 
				fail("Entity not found : "+entity);
			objectStorage.flush();
			objectStorage.close();
		}
	}
	
	public void testEntityStore() throws Exception {
		
		for (int i= 1; i <= 100; i++) {
			IEntityStorage.Session objectStorage= new EntityStorage(_rawStorage).connect().get();
			Entity object= new Entity("person-"+i);
			objectStorage.store(object);
			if (objectStorage.fetch(object.getId()) == null) 
				fail("Entity not found : "+object);
			objectStorage.flush();
			objectStorage.close();
		}
	}
	
	/**
	 * Verify that creates/deletes work across threads. 
	 * @throws IOException
	 */
	public void testCreate() throws Throwable {
		final IResult<byte[]> content= TaskUtils.asResult("hello".getBytes());
		
		// test that only thread can create the same file at a time
		for (int f= 0; f < 10; f++) {			
			final Identifier identifier= Identifier.create("file-"+f);
			final IStorageProvider.Session session= _rawStorage.connect().get();
			assertNotNull(session);
			final List count= Collections.synchronizedList(new ArrayList());
			ExecutorService executorService= Executors.newFixedThreadPool(20);
			final Throwable[] error= new Throwable[] { null }; 
			for (int t= 0; t < 20; t++) {
				executorService.execute(new Runnable() {
					public void run() {
						try {
							IResult<Boolean> result= session.create(identifier, content, Integer.MAX_VALUE);
							assertNotNull(result);
							assertTrue(result.get());
							count.add(true);
							assertEquals("More than one lock was granted", 1, count.size());
							try { Thread.sleep(10); } catch (InterruptedException x) { }
							count.remove(0);
							session.delete(identifier);
						}
						catch (Throwable t) {
							synchronized (error) {
								if (error[0] == null) {
									error[0]= t;
									t.printStackTrace();
								}
							}
						}
					}
				});
			}
			executorService.shutdown();
			executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
			if (error[0] != null)
				throw error[0];
			
			session.close().get();
		}
		
	}
	
	/**
	 * Verify that storage-based locks work across threads. 
	 * @throws IOException
	 */
	public void testLocks() throws Throwable {
		
		// test that only one lock is ever granted at a time
		IEntityStorage.Session storage= new EntityStorage(_rawStorage).connect().get();
		for (int f= 0; f < 10; f++) {			
			Entity rootFolder= new Entity(Identifier.create());
			storage.store(rootFolder);
			final LockFolder lockFolder= new LockFolder(rootFolder);
			storage.store(lockFolder);
			storage.flush();
			
			final List count= Collections.synchronizedList(new ArrayList());
			ExecutorService executorService= Executors.newFixedThreadPool(20);
			final Throwable[] error= new Throwable[1]; 
			for (int t= 0; t < 20; t++) {
				executorService.execute(new Runnable() {
					public void run() {
						try {
							String processId= Identifier.create().toString();
							count.add(lockFolder.lock(processId, true));
							assertEquals("More than one lock was granted", 1, count.size());
							Thread.sleep(10);
							count.remove(0);
							lockFolder.unlock(processId);
						} catch (Throwable t) {
							synchronized (error) {
								if (error[0] == null) {
									error[0]= t;
									t.printStackTrace();
								}
							}
						}
					}
				});
			}
			executorService.shutdown();
			executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
			if (error[0] != null)
				throw error[0];
		}
	}
	
	/**
	 * Verify that the create method works correctly when invoked by many 
	 * threads at once.
	 */
	public void testConcurrentFileCreation() throws Exception {
		final IStorageProvider.Session storage= _rawStorage.connect().get();
		for (int f= 0; f < 10; f++) {			
			final Identifier folderId= Identifier.create(Integer.toString(f));
			ArrayList<IResult<Boolean>> results= new ArrayList<IResult<Boolean>>();
			for (int t= 0; t < 20; t++) 
				results.add(storage.create(folderId, TaskUtils.asResult(new byte[] { ' ' }), 0));
			int count= 0;
			for (IResult<Boolean> result: results)
				if (result.get())
					count++;
			assertEquals(1, count);
		}
		
	}
	
	public void testBasicTransaction() throws Exception {
			StorageSession session= _storage.beginSession(Mode.READWRITE).get();
			assertEquals(1, session.getRevisionNumber());
			Entity object_0_1= new Entity("person-0.1");
			session.store(object_0_1);
			assertNotNull(session.fetch(object_0_1.getId()).get());
			session.commit();
			
			StorageSession T1= _storage.beginSession(Mode.READWRITE).get();
			assertEquals(1, T1.getStartingCommitNumber());
			assertEquals(2, T1.getRevisionNumber());
			assertNotNull(T1.fetch(object_0_1.getId()).get());
			StorageSession T2= _storage.beginSession(Mode.READONLY).get();
			assertEquals(1, T2.getRevisionNumber());
			
			// TEST PUT ISOLATION
			// put object-1.1 in transaction 1
			Entity object_1_1= new Entity("person-1.1");
			T1.store(object_1_1);
			assertNotNull(T1.fetch(object_1_1.getId()).get());
			// object-1.1 should not be visible outside of T1 since it is not yet committed 
			assertNull("transaction isolation violated", T2.fetch(object_1_1.getId()).get());
			StorageSession T3= _storage.beginSession(Mode.READONLY).get();
			assertEquals(1, T3.getRevisionNumber());
			assertNull("transaction isolation violated", T3.fetch(object_1_1.getId()).get());
			T3.close();
			StorageSession T4= _storage.beginSession(Mode.READWRITE).get();
			assertEquals(3, T4.getRevisionNumber());
			assertNull("transaction isolation violated", T4.fetch(object_1_1.getId()).get());
			T4.close();
			
			// TEST DELETE ISOLATION
			T1.delete(object_0_1.getId());
			// object_0_1 should not be visible in T1 
			assertNull("transaction isolation violated", T1.fetch(object_0_1.getId()).get());
			// object-0.1 should still be visible outside of T1 since it is not yet committed 
			assertNotNull("transaction isolation violated", T2.fetch(object_0_1.getId()).get());
			T3= _storage.beginSession(Mode.READONLY).get();
			assertNotNull("transaction isolation violated", T3.fetch(object_0_1.getId()).get());
			T3.close();
			T4= _storage.beginSession(Mode.READWRITE).get();
			assertEquals(4, T4.getRevisionNumber());
			assertNotNull("transaction isolation violated", T4.fetch(object_0_1.getId()).get());
			T4.close();
			
			// TEST COMMIT AND ACTIVE STATE
			assertTrue(T1.isActive());
			T1.commit();
			assertFalse(T1.isActive());

			// TEST PUT ISOLATION
			// object-1.1 should now be visible to new transactions
			T3= _storage.beginSession(Mode.READONLY).get();
			assertEquals(2, T3.getRevisionNumber());
			assertNotNull(T3.fetch(object_1_1.getId()).get());
			T3.close();
			T4= _storage.beginSession(Mode.READWRITE).get();
			assertNotNull(T4.fetch(object_1_1.getId()).get());
			T4.close();
			// object-1.1 should still not be visible to T2 since T2 was started before object-1.1 was committed
			assertNull("transaction isolation violated", T2.fetch(object_1_1.getId()).get());
			
			// TEST DELETE ISOLATION
			// object-0.1 should not be visible to new transactions 
			T3= _storage.beginSession(Mode.READONLY).get();
			assertNull(T3.fetch(object_0_1.getId()).get());
			T3.close();
			T4= _storage.beginSession(Mode.READWRITE).get();
			assertNull(T4.fetch(object_0_1.getId()).get());
			T4.close();
			// object-0.1 should still be visible to T2 
			assertNotNull(T2.fetch(object_0_1.getId()).get());
	}
	
	public void testRollback() throws Exception {
		StorageSession session= _storage.beginSession(Mode.READWRITE).get();
		assertEquals(1, session.getRevisionNumber());
		Entity object_0_1= new Entity("person-0.1");
		session.store(object_0_1);
		session.commit();
		session.close();
		
		StorageSession T1= _storage.beginSession(Mode.READWRITE).get();
		
		// put object-1.1 in transaction 1
		Entity object_1_1= new Entity("person-1.1");
		T1.store(object_1_1);
		assertNotNull(T1.fetch(object_1_1.getId()).get());
		assertNotNull(T1.fetch(object_0_1.getId()).get());
		T1.delete(object_0_1.getId());
		assertNull(T1.fetch(object_0_1.getId()).get());
		
		assertTrue(T1.isActive());
		T1.close(); // abandon any changes
		assertFalse(T1.isActive());

		// object-1.1 should not be visible 
		// object-0.1 should still be visible 
		StorageSession T3= _storage.beginSession(Mode.READONLY).get();
		assertNull("transaction isolation violated", T3.fetch(object_1_1.getId()).get());
		assertNotNull("transaction isolation violated", T3.fetch(object_0_1.getId()).get());
		T3.close();
		StorageSession T4= _storage.beginSession(Mode.READWRITE).get();
		assertNull("transaction isolation violated", T4.fetch(object_1_1.getId()).get());
		assertNotNull("transaction isolation violated", T4.fetch(object_0_1.getId()).get());
		T4.close();
	}
	
	
	public void testFetchMultipleSessions() throws Exception {
		
		for (int i= 1; i <= 100; i++) {
			StorageSession session= _storage.beginSession(Mode.READWRITE).get();
			Entity entity= new Entity("person-"+i);
			session.store(entity);
			if (session.fetch(entity.getId()) == null) 
				fail("Entity not found : "+entity.getId());
			session.commit();
		}
	}
	
	public void testCleanup() throws Exception {
		for (int i= 1; i <= 100; i++) {
			StorageSession session= _storage.beginSession(Mode.READWRITE).get();
			Entity entity= new Entity("person-"+i);
			session.store(entity);
			if (session.fetch(entity.getId()) == null) 
				fail("Entity not found : "+entity.getId());
			session.commit();
		}
		
		_storage.cleanup();
		
		assertEquals("Failed to clean up all unused revisions", 1, _storage.getAvailableRevisions().get().size());
	}
	
	/**
	 * Reproduces a bug where a stored object was not returned from the listChildren method.
	 */
	public void testConcurrentObjectStoreListChildren() throws IOException {
		//final ObjectStorage storage= new ObjectStorage(_rawStorage);
		final ObjectStorage.Session storage= new ObjectStorage(_rawStorage).connect().get();
		ArrayList<IResult<Void>> tasks= new ArrayList<IResult<Void>>();
		for (int f= 0; f < 10; f++) {			
			final Identifier folderId= Identifier.create(Integer.toString(f));
			for (int t= 0; t < 10; t++) {
				final int task= t; 
				tasks.add(new ContrailAction() {
					protected void action() throws Exception {
						for (int i= 0; i < 10; i++) {
							
							// store an object in a folder
							String s= Integer.toString(task)+"-"+i;
							Identifier id= Identifier.create(folderId, s);
							storage.store(id, s);
							
							// now, list the folder's children and make sure our object is listed
							Collection<Identifier> children= storage.listChildren(folderId).get();
							assertTrue(children.contains(id));
						}
						
					}
				}.submit());
			}
		}
		TaskUtils.combineResults(tasks).get();
	}
	/**
	 * Reproduces a bug where a stored object was not returned from the listChildren method.
	 */
	public void testConcurrentStoreAPIListChildren() throws IOException {
		//final ObjectStorage storage= new ObjectStorage(_rawStorage);
		final IStorageProvider.Session storage= _rawStorage.connect().get();
		ArrayList<IResult<Void>> tasks= new ArrayList<IResult<Void>>();
		for (int f= 0; f < 10; f++) {			
			final Identifier folderId= Identifier.create(Integer.toString(f));
			for (int t= 0; t < 10; t++) {
				final int task= t; 
				tasks.add(new ContrailAction() {
					protected void action() throws Exception {
						for (int i= 0; i < 10; i++) {
							
							// store an object in a folder
							String s= Integer.toString(task)+"-"+i;
							Identifier id= Identifier.create(folderId, s);
							storage.store(id, TaskUtils.asResult(s.getBytes()));
							
							// now, list the folder's children and make sure our object is listed
							Collection<Identifier> children= storage.listChildren(folderId).get();
							if (!children.contains(id)) {
								String msg= "Problem in listChildren.\nFolder does not contain "+id;
								msg+= "\nFolder contains "+children;
								fail(msg);
							}
						}
					}
				}.submit());
			}
		}
		TaskUtils.combineResults(tasks).get();
		
	}
	
}
