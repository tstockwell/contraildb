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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.googlecode.contraildb.core.Identifier;

import junit.framework.TestCase;


/**
 * Looking for faster ways to serialize objects.
 * 
 * @author Ted Stockwell
 */
public class SerializationTests extends TestCase {
//	static class Node<K extends Comparable<K>>
//	extends Entity 
//	implements Cloneable
//	{
//		final static long serialVersionUID = 1L;
//
//		protected final Identifier _indexId;
//		protected final K[] _keys;
//		protected final Object[] _values;
//		protected int _size; // the # of values in the node
//		protected Identifier _previous;
//		protected Identifier _next;
//
//		public Node() {
//			super(Identifier.create());
//		}
//	}
	
	static final int OBJECT_COUNT= 10000;
	
	public void testIdentifierSerialization() 
	throws Exception 
	{
		
		ArrayList<Identifier> list= new ArrayList<Identifier>();
		for (int i= OBJECT_COUNT; 0 < i--;)
			list.add(Identifier.create());
		
		performAllTests("Identifier Tests", list);
	}
	
	
	private void performAllTests(String string, List<?> list) 
	throws IOException, ClassNotFoundException 
	{
		System.out.println("------------ "+string );
		File file= performJavaSerialization(list);
		performJavaDeserialization(file);
		file.delete();
		
		
		file= performContrailSerialization(list);
		performContrailDeserialization(file);
		file.delete();
		
	}
	

	File performContrailSerialization(List<?> objects) 
	throws IOException 
	{
		long start= System.currentTimeMillis();
		
		File file= File.createTempFile("contrail", ".txt");
		ContrailOutputStream oout= 
			new ContrailOutputStream(
					new BufferedOutputStream(
							new FileOutputStream(file)));
		for (Object item: objects)
			oout.writeObject(item);
		oout.flush();
		oout.close();
		
		long stop= System.currentTimeMillis();
		System.out.println("CONTRAIL SERIALIZATION:");
		System.out.println(" Total time: "+(stop - start)+" milliseconds" );
		System.out.println(" Total size: "+file.length() );
		return file;
	}

	void performContrailDeserialization(File file) 
	throws IOException, ClassNotFoundException 
	{
		long start= System.currentTimeMillis();
		
		ContrailInputStream oin= 
			new ContrailInputStream(
					new BufferedInputStream(
							new FileInputStream(file)));
		for (int i= OBJECT_COUNT; 0 < i--;) {
			oin.readObject();
		}
		oin.close();
		
		long stop= System.currentTimeMillis();
		System.out.println("CONTRAIL DESERIALIZATION:");
		System.out.println(" Total time: "+(stop - start)+" milliseconds" );
	}

	
	File performJavaSerialization(List<?> objects) 
	throws IOException 
	{
		long start= System.currentTimeMillis();
		
		File file= File.createTempFile("java", ".txt");
		ObjectOutputStream oout= 
			new ObjectOutputStream(
				new BufferedOutputStream(
						new FileOutputStream(file)));
		for (Object item: objects)
			oout.writeObject(item);
		oout.flush();
		oout.close();
		
		long stop= System.currentTimeMillis();
		System.out.println("JAVA SERIALIZATION:");
		System.out.println(" Total time: "+(stop - start)+" milliseconds" );
		System.out.println(" Total size: "+file.length() );
		return file;
	}

	void performJavaDeserialization(File file) 
	throws IOException, ClassNotFoundException 
	{
		long start= System.currentTimeMillis();
		
		ObjectInputStream oin= 
			new ObjectInputStream(
				new BufferedInputStream(
						new FileInputStream(file)));
		for (int i= OBJECT_COUNT; 0 < i--;) {
			oin.readObject();
		}
		oin.close();
		
		long stop= System.currentTimeMillis();
		System.out.println("JAVA DESERIALIZATION:");
		System.out.println(" Total time: "+(stop - start)+" milliseconds" );
	}
	
}
