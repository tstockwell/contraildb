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

import kilim.Pausable;
import kilim.Task;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.async.TaskUtils;
import com.googlecode.contraildb.core.storage.provider.IStorageProvider;


/**
 * Test the Contrail storage provider system.
 * 
 * @author Ted Stockwell
 */
abstract public class BaseStorageProviderTests extends ContrailTestCase {
	
	IStorageProvider _rawStorage;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		_rawStorage= createStorageProvider();
	}
	
	abstract protected IStorageProvider createStorageProvider();
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testProviderStorage() throws Throwable {
		runTest(new Task() {
			public void execute() throws Pausable ,Exception {
				IStorageProvider.Session storage= _rawStorage.connect();

				String object_0_1= "person-0.1";
				Identifier id= Identifier.create(object_0_1);
				storage.store(id, TaskUtils.asResult(object_0_1.getBytes()));
				storage.flush();
				byte[] bs= storage.fetch(id);
				assertEquals(object_0_1, new String(bs));

				// repeat, only this time dont do the flush.
				// The underlying implementation should synchronize the store 
				// and the fetch operations anyway.
				String object_0_2= "person-0.2";
				id= Identifier.create(object_0_2);
				storage.store(id, TaskUtils.asResult(object_0_2.getBytes()));
				bs= storage.fetch(id);
				assertEquals(object_0_2, new String(bs));
			}
		});
	}
	
	
}
