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
import com.googlecode.contraildb.core.ContrailServiceFactory;
import com.googlecode.contraildb.core.IContrailService;
import com.googlecode.contraildb.core.IContrailSession;
import com.googlecode.contraildb.core.IPreparedQuery;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.Item;
import com.googlecode.contraildb.core.ContrailQuery.SortDirection;
import com.googlecode.contraildb.core.IContrailService.Mode;
import com.googlecode.contraildb.core.storage.provider.FileStorageProvider;
import com.googlecode.contraildb.core.storage.provider.IStorageProvider;
import com.googlecode.contraildb.core.storage.provider.RamStorageProvider;


/**
 * task concurrency tests.
 * 
 * @author Ted Stockwell
 */
@SuppressWarnings("unchecked")
public class ContrailTasksTests extends TestCase {
	
	
	public void testDeadlock() throws Exception {
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
