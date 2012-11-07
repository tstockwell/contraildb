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

import junit.framework.TestCase;
import kilim.Pausable;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.async.TaskUtils;
import com.googlecode.contraildb.core.storage.provider.IStorageProvider;
import com.googlecode.contraildb.core.storage.provider.RamStorageProvider;


/**
 * Test the Contrail storage provider system.
 * 
 * @author Ted Stockwell
 */
public class ContrailStorageProviderTests extends TestCase {
	
	IStorageProvider _rawStorage;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		_rawStorage= new RamStorageProvider();
		//_rawStorage= new FileStorageProvider(new File("/temp/test/contrail"), true);
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testProviderStorage() throws Pausable, Exception {
		IStorageProvider.Session storage= _rawStorage.connect().get();

		Identifier id= Identifier.create("person-0.1");
		String object_0_1= "person-0.1";
		storage.store(id, TaskUtils.asResult(object_0_1.getBytes()));
		storage.flush();
		String fetched= new String(storage.fetch(id).get());
		assertEquals(object_0_1, fetched);
	}
	
	
}
