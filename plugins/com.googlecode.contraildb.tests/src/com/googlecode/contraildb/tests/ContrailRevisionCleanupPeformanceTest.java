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

import java.io.IOException;

import com.googlecode.contraildb.core.ConflictingCommitException;
import com.googlecode.contraildb.core.IContrailService.Mode;
import com.googlecode.contraildb.core.storage.Entity;
import com.googlecode.contraildb.core.storage.StorageSession;
import com.googlecode.contraildb.core.storage.StorageSystem;
import com.googlecode.contraildb.core.storage.provider.IStorageProvider;
import com.googlecode.contraildb.core.storage.provider.RamStorageProvider;
import com.googlecode.contraildb.core.utils.Logging;



/**
 * Test the Contrail storage system.
 * 
 * @author Ted Stockwell
 */
public class ContrailRevisionCleanupPeformanceTest {
	
	public static void main(String[] args) throws IOException, ConflictingCommitException {

		IStorageProvider rawStorage= new RamStorageProvider();
		//ISimpleStorage rawStorage= new FileStorageSPI(new File("/temp/test/contrail"), true);
		StorageSystem _storage= new StorageSystem(rawStorage);
		
		for (int i= 1; i <= 100; i++) {
			StorageSession session= _storage.beginSession(Mode.READWRITE);
			Entity object_0_1= new Entity("person-"+i);
			session.store(object_0_1);
			session.commit();
		}
		
		_storage.cleanup();
		
		Logging.finer("Should only be one revision left");
	}
	
	
}
