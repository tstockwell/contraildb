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
import java.util.HashMap;
import java.util.Map;

import com.google.apphosting.api.ApiProxy;


public class TestEnvironment implements ApiProxy.Environment {
	Map<String, Object> _attributes= new HashMap<String, Object>();
	
	public String getAppId() {
		return "Unit Tests";
	}

	public String getVersionId() {
		return "1.0";
	}

	public void setDefaultNamespace(String s) {
	}

	public String getRequestNamespace() {
		return "gmail.com";
	}

	public String getDefaultNamespace() {
		return "";
	}

	public String getAuthDomain() {
		return "gmail.com";
	}

	public boolean isLoggedIn() {
		return false;
	}

	public String getEmail() {
		return "";
	}

	public boolean isAdmin() {
		return false;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return _attributes;
	}
	
}
