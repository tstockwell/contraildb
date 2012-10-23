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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import junit.framework.TestCase;
import kilim.Task;
import kilim.tools.WeaverClassLoader;

/**
 * Base class for Contrail tests.
 * 
 * @author Ted Stockwell
 */
public class ContrailTestCase extends TestCase {

	protected WeaverClassLoader _weaverClassLoader;

	@Override
	protected void setUp() throws Exception {
		//
		// create classloader that applies kilim instrumentation
		//
		URLClassLoader parent = (URLClassLoader) KilimConcurrencyTests.class
				.getClassLoader();
		ClassLoader gparent = parent.getParent();

		ArrayList<URL> urls2instrument = new ArrayList<URL>();
		ArrayList<URL> urls = new ArrayList<URL>();
		for (URL url : parent.getURLs()) {
			String urltxt= url.toString();
			if (urltxt.contains("kilim")) {
				urls2instrument.add(url);
			}
			else
				if (urltxt.contains("contraildb")) {
					urls2instrument.add(url);
				}
				else
					urls.add(url);
		}
		URLClassLoader loader = new URLClassLoader(
				urls.toArray(new URL[urls.size()]), gparent);
		_weaverClassLoader = new WeaverClassLoader( 
				urls2instrument.toArray(new URL[urls2instrument.size()]), loader);
		Thread.currentThread().setContextClassLoader(_weaverClassLoader);
	}

	protected void startTask(Class<? extends Task> klass) throws Throwable {
		Thread.currentThread().setContextClassLoader(_weaverClassLoader);
		Object testRunner = _weaverClassLoader.loadClass(klass.getName()).newInstance();
		
		Method startMethod= testRunner.getClass().getMethod("start", (Class<?>[])null);
		startMethod.invoke(testRunner, (Object[])null);
		
		Method joinMethod= testRunner.getClass().getMethod("joinb", (Class<?>[])null);
		Object exitMsg= joinMethod.invoke(testRunner, (Object[])null);
		
        if (exitMsg == null) {
            fail("Timed Out");
        } else {
        	Field resultField=  exitMsg.getClass().getField("result");
            Object res = resultField.get(exitMsg);
            if (res instanceof Throwable) {
                ((Throwable)res).printStackTrace();
                fail(exitMsg.toString());
            }
        }
	}

}
