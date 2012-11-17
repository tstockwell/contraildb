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

import com.googlecode.contraildb.core.storage.provider.FileStorageProvider;
import com.googlecode.contraildb.core.storage.provider.IStorageProvider;


/**
 * Test the Contrail storage provider system.
 * 
 * @author Ted Stockwell
 */
public class FileStorageProviderTests extends BaseStorageProviderTests {
	
	protected IStorageProvider createStorageProvider() {
		return new FileStorageProvider(new File("/temp/test/contrail"), true);
	}
	
}
