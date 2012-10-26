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
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Basic Kilim tests for verifying Kilim features and runtime characteristics.
 * 
 * @author Ted Stockwell
 */
public class PluginClassLoaderTest {
	
	public static void main(String[] args) throws Throwable {
		
		URL url= new File("/eclipse-java-indigo-SR1-win32/eclipse/plugins/org.junit_3.8.2.v3_8_2_v20100427-1100/junit.jar").toURL(); 
		//file:/C:/eclipse-java-indigo-SR1-win32/eclipse/plugins/org.junit_3.8.2.v3_8_2_v20100427-1100/junit.jar		
		URL[] urls= new URL[] {
				 new URL("file:/C:/eclipse-java-indigo-SR1-win32/eclipse/plugins/org.junit_3.8.2.v3_8_2_v20100427-1100/junit.jar"), 
				 new URL("file:/C:/eclipse-java-indigo-SR1-win32/eclipse/plugins/org.junit_3.8.2.v3_8_2_v20100427-1100/junit.jar"), 
				 new URL("file:/C:/eclipse-java-indigo-SR1-win32/eclipse/plugins/org.junit_4.8.2.v4_8_2_v20110321-1705/junit.jar"), 
				 new URL("file:/C:/eclipse-java-indigo-SR1-win32/eclipse/plugins/org.hamcrest.core_1.1.0.v20090501071000.jar"), 
				 new URL("file:/C:/temp/workspace/kilim-builder-test-workspace/com.googlecode.contraildb.tests/bin/"), 
				 new URL("file:/C:/Program Files/Java/jdk1.6.0_25/jre/lib/resources.jar"), 
				 new URL("file:/C:/Program Files/Java/jdk1.6.0_25/jre/lib/rt.jar"), 
				 new URL("file:/C:/Program Files/Java/jdk1.6.0_25/jre/lib/jsse.jar"),
				 new URL("file:/C:/Program Files/Java/jdk1.6.0_25/jre/lib/jce.jar"), 
				 new URL("file:/C:/Program Files/Java/jdk1.6.0_25/jre/lib/charsets.jar") 
				};
				
				URLClassLoader classLoader= new URLClassLoader(urls);
				Class c= classLoader.loadClass("junit.framework.TestCase");
				if (c != null)
					System.out.println("SUCCESS");
	}
}
