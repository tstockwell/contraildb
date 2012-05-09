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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Iterator;

import com.googlecode.contraildb.core.impl.btree.BPlusTree;
import com.googlecode.contraildb.core.impl.btree.IBTreePlusCursor;
import com.googlecode.contraildb.core.impl.btree.IBTreeCursor.Direction;
import com.googlecode.contraildb.core.storage.EntityStorage;
import com.googlecode.contraildb.core.storage.IEntityStorage;
import com.googlecode.contraildb.core.storage.provider.RamStorageProvider;

import junit.framework.TestCase;


/**
 * Test the Contrail Btree implementation.
 * 
 * @author Ted Stockwell
 */
public class ContrailBTreeTests extends TestCase {
	
	public void testBasics() throws Exception {
		IEntityStorage.Session storage= new EntityStorage(new RamStorageProvider()).connect().get();
		BPlusTree<String, String> tree= BPlusTree.createInstance(storage);
		IBTreePlusCursor<String, String> cursor= tree.cursor(Direction.FORWARD);
		
		tree.insert("Churchill", "Good Guys");
		assertNull(cursor.find("Ghandi"));
		tree.insert("Ghandi", "Good Guys");
		
		tree.insert("key-1", "value-1.0");
		assertEquals("value-1.0", cursor.find("key-1"));
		tree.insert("key-2", "value-2.0");
		assertEquals("value-1.0", cursor.find("key-1"));
		assertEquals("value-2.0", cursor.find("key-2"));
		tree.insert("key-1", "value-1.1");
		assertEquals("value-1.1", cursor.find("key-1"));
		assertEquals("value-2.0", cursor.find("key-2"));
		
		Iterator<String> iterator= tree.iterator();
		assertTrue(iterator.hasNext());
		assertEquals("Churchill", iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals("Ghandi", iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals("key-1", iterator.next());
		assertTrue(iterator.hasNext());
		assertEquals("key-2", iterator.next());
		assertFalse(iterator.hasNext());
		
		
		tree.remove("Churchill");
		assertNull(cursor.find("Churchill"));
		tree.remove("Ghandi");
		assertNull(cursor.find("Ghandi"));
		tree.remove("key-1");
		assertNull(cursor.find("key-1"));
		tree.remove("key-2");
		assertNull(cursor.find("key-2"));
		assertTrue(tree.isEmpty());
	}
	
	public void testIncreasingInserts() throws Exception {
		IEntityStorage.Session storage= new EntityStorage(new RamStorageProvider()).connect().get();
		BPlusTree<Integer, Integer> tree= BPlusTree.createInstance(storage, 4);
		IBTreePlusCursor<Integer, Integer> finder= tree.cursor(Direction.FORWARD);
		
		String lastDump= "";
		for (int i= 0; i < 100; i++) {
			Integer I= new Integer(i);
			tree.insert(I, I);
			
			ByteArrayOutputStream bout= new ByteArrayOutputStream();
			tree.dump(new PrintStream(bout));
			String dump= bout.toString();
			
			for (int j= 0; j <= i; j++) {
				Integer J= new Integer(j);
				if (finder.find(J) == null) {
					System.out.println("TREE at i= "+(i-1));
					System.out.println(lastDump);
					System.out.println("TREE at i= "+i);
					System.out.println(dump);
					finder.find(J);
					throw new RuntimeException("Failed to find element "+J);
				}
			}
			
			IBTreePlusCursor<Integer, Integer> cursor= tree.cursor(Direction.FORWARD);
			for (int j= 0; j <= i; j++) {
				assertTrue(cursor.next());
				assertEquals(new Integer(j), cursor.elementValue());
			}
			
			cursor= tree.cursor(Direction.REVERSE);
			for (int j= i; 0 <= j; j--) {
				assertTrue(cursor.next());
				assertEquals(new Integer(j), cursor.elementValue());
			}
			
			lastDump= dump;
		}
	}
	
	public void testDecreasingInserts() throws Exception {
		IEntityStorage.Session storage= new EntityStorage(new RamStorageProvider()).connect().get();
		BPlusTree<Integer, Integer> tree= BPlusTree.createInstance(storage, 4);
		IBTreePlusCursor<Integer, Integer> finder= tree.cursor(Direction.FORWARD);
		
		String lastDump= "";
		for (int i= 100; 0 < i--;) {
			Integer I= new Integer(i);
			tree.insert(I, I);
			
			ByteArrayOutputStream bout= new ByteArrayOutputStream();
			tree.dump(new PrintStream(bout));
			String dump= bout.toString();
			
			for (int j= 100; i < j--; ) {
				Integer J= new Integer(j);
				if (finder.find(J) == null) {
					System.out.println("TREE at i= "+(i+1));
					System.out.println(lastDump);
					System.out.println("TREE at i= "+i);
					System.out.println(dump);
					throw new RuntimeException("Failed to find element "+J);
				}
			}

			IBTreePlusCursor<Integer, Integer> cursor= tree.cursor(Direction.FORWARD);
			for (int j= i; j < 100; j++) {
				assertTrue(cursor.next());
				assertEquals(new Integer(j), cursor.elementValue());
			}
			
			cursor= tree.cursor(Direction.REVERSE);
			for (int j= 100; i < j--; ) {
				assertTrue(cursor.next());
				assertEquals(new Integer(j), cursor.elementValue());
			}
			
			lastDump= dump;
		}
	}
	
	public void testIncreasingDeletes() throws Exception {
		IEntityStorage.Session storage= new EntityStorage(new RamStorageProvider()).connect().get();
		BPlusTree<Integer, Integer> tree= BPlusTree.createInstance(storage, 4);
		IBTreePlusCursor<Integer, Integer> finder= tree.cursor(Direction.FORWARD);
		
		for (int i= 0; i < 100; i++) {
			Integer I= new Integer(i);
			tree.insert(I, I);
		}
		String lastDump= tree.toString();
		
		
		for (int i= 0; i < 100; i++) {
			Integer I= new Integer(i);
			tree.remove(I);
			assertNull(finder.find(I));
			
			String dump= tree.toString();
			
			for (int j= i+1; j < 100; j++) {
				Integer J= new Integer(j);
				if (finder.find(J) == null) {
					System.out.println("TREE at i= "+(i-1));
					System.out.println(lastDump);
					System.out.println("TREE at i= "+i);
					System.out.println(dump);
					finder.find(J);
					throw new RuntimeException("Failed to find element "+J);
				}
			}
			
			lastDump= dump;
		}
		assertTrue(tree.isEmpty());
	}
	
	
	public void testDecreasingDeletes() throws Exception {
		IEntityStorage.Session storage= new EntityStorage(new RamStorageProvider()).connect().get();
		BPlusTree<Integer, Integer> tree= BPlusTree.createInstance(storage, 4);
		IBTreePlusCursor<Integer, Integer> finder= tree.cursor(Direction.FORWARD);
		
		String lastDump= "";
		for (int i= 0; i < 100; i++) {
			Integer I= new Integer(i);
			tree.insert(I, I);
		}
		for (int i= 100; 0 < i--;) {
			Integer I= new Integer(i);
			tree.remove(I);
			assertNull(finder.find(I));
			
			ByteArrayOutputStream bout= new ByteArrayOutputStream();
			tree.dump(new PrintStream(bout));
			String dump= bout.toString();
			
			for (int j= 0; j < i; j++) {
				Integer J= new Integer(j);
				if (finder.find(J) == null) {
					System.out.println("TREE at i= "+(i-1));
					System.out.println(lastDump);
					System.out.println("TREE at i= "+i);
					System.out.println(dump);
					throw new RuntimeException("Failed to find element "+J);
				}
			}
			
			lastDump= dump;
		}
		assertTrue(tree.isEmpty());
	}
	
	
	public void testIterators() throws Exception {
		IEntityStorage.Session storage= new EntityStorage(new RamStorageProvider()).connect().get();
		BPlusTree<Integer, Integer> tree= BPlusTree.createInstance(storage, 4);
		
		for (int i= 0; i < 100; i++) {
			Integer I= new Integer(i);
			tree.insert(I, I);
		}
	}
}
