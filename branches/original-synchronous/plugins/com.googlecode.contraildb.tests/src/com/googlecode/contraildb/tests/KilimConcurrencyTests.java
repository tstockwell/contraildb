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

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import junit.framework.TestCase;
import kilim.Mailbox;
import kilim.Pausable;
import kilim.Task;
import kilim.tools.WeaverClassLoader;

import com.googlecode.contraildb.core.async.ContrailAction;

/**
 * Basic Kilim tests.
 * 
 * @author Ted Stockwell
 */
public class KilimConcurrencyTests extends TestCase {

	public static class Chain extends Task {
		Mailbox<String> inBox, outBox;

		public Chain(Mailbox<String> in, Mailbox<String> out) {
			inBox = in;
			outBox = out;
		}

		public void execute() throws Pausable {
			String sb = inBox.get();
			outBox.put(sb + "x");
		}
	}

	public static class ChainTestRunner extends Task {
		public void execute() throws Pausable {
			System.out.println("main started");
			int n = 10;
			Mailbox<String> first = new Mailbox<String>();
			Mailbox<String> in = first;
			Mailbox<String> out = new Mailbox<String>();
			for (int i = 0; i < n; i++) {
				new Chain(in, out).start();
				in = out;
				out = new Mailbox<String>();
			}
			first.put("");
			String result = out.get();
			assertTrue(result.length() == n);
		}
	}
	
	public static class JoinTaskTestRunner extends Task {
		public void execute() throws Pausable {
			final ContrailAction[] tasks= new ContrailAction[5/*100*/];
			final boolean[] completed= new boolean[tasks.length];
			for (int i= 0; i < tasks.length; i++) {
				final int tasknum= i;
				completed[tasknum]= false;
				tasks[i]= new ContrailAction("Test Task "+tasknum) {
					protected void action() throws Pausable, Exception {
						// wait for preceding task to complete
						if (0 < tasknum)
							tasks[tasknum-1].getResult().join();
						System.out.println("task "+tasknum);
						completed[tasknum]= true;
					}
				};
			}
			
			// run all tasks
			for (int i= 0; i < tasks.length; i++) {
				tasks[i].submit();
			}
			
			// wait for last task to complete
			tasks[tasks.length-1].getResult().join();
			
			// make sure that every task was actually executed
			for (int i= 0; i < tasks.length; i++) {
				assertTrue("Task "+i+" was not completed", completed[i]);
			}
		}
	}	

	WeaverClassLoader _weaverClassLoader;

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
	}
	
	protected void startTask(Class<? extends Task> klass) throws Pausable, Throwable {
		Thread.currentThread().setContextClassLoader(_weaverClassLoader);
		Object testRunner = _weaverClassLoader.loadClass(klass.getName()).newInstance();
		Method startMethod= testRunner.getClass().getMethod("start", (Class<?>[])null);
		startMethod.invoke(testRunner, (Object[])null);
	}

	public void testSimpleContinuation() throws Pausable, Throwable {
		startTask(ChainTestRunner.class);
	}
	
	/**
	 *  Tests ability of a task to wait for another task to complete 
	 */
	public void testContrailTaskJoins() throws Pausable, Throwable {
		startTask(JoinTaskTestRunner.class);
	}
	
}
