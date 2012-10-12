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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.apache.commons.javaflow.Continuation;
import org.apache.commons.javaflow.ContinuationClassLoader;


/**
 * Test the Contrail storage system.
 * 
 * @author Ted Stockwell
 */
public class JavaFlowBetaTests extends TestCase {
	
	public static class MyRunnable implements Runnable {
		public void run() {
			System.out.println("run started!");
			for( int i=0; i < 10; i++ ) {
				echo(i);
			}
		}

		private void echo(int x) {
			System.out.println("echo " + x);
			Continuation.suspend();
		}
	}	
	public static class SimpleTestRunner implements Runnable {
		public void run() {
			System.out.println("main started");
			Continuation c = Continuation.startWith(new MyRunnable());
			System.out.println("in main after continuation return");
			while (c != null) {
				c = Continuation.continueWith(c);
				System.out.println("in main");
			}
		}
	}	
	
	ContinuationClassLoader _javaflowClassLoader;
	
	@Override
	protected void setUp() throws Exception {
		URLClassLoader parent= (URLClassLoader) JavaFlowBetaTests.class.getClassLoader();
		ClassLoader gparent= parent.getParent();
		
		URL runnableLocation= MyRunnable.class.getProtectionDomain().getCodeSource().getLocation();
		ArrayList<URL> urls= new ArrayList<URL>();
		for (URL url:parent.getURLs()) {
			if (!url.equals(runnableLocation))
				urls.add(url);
		}
		URLClassLoader loader= new URLClassLoader(urls.toArray(new URL[urls.size()]), gparent);
		_javaflowClassLoader = new ContinuationClassLoader( new URL[]{runnableLocation}, loader);
	}

	public void testSimpleContinuation() throws Throwable {
		Thread.currentThread().setContextClassLoader(_javaflowClassLoader);
		Runnable testRunner= (Runnable)_javaflowClassLoader.loadClass(SimpleTestRunner.class.getName()).newInstance();
		testRunner.run();
	}
}
