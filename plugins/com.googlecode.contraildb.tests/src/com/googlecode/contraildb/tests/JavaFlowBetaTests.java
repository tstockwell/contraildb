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

import com.googlecode.contraildb.core.async.ContrailAction;
import com.googlecode.contraildb.core.async.ContrailTask;
import com.googlecode.contraildb.core.async.IResult;
import com.googlecode.contraildb.core.async.TaskUtils;


/**
 * Test the Contrail storage system.
 * 
 * @author Ted Stockwell
 */
@SuppressWarnings("rawtypes")
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
	
	
	public static class SimpleTaskTestRunner implements Runnable {
		public void run() {
			final ContrailAction[] tasks= new ContrailAction[100]; 
			final IResult[] results= new IResult[100]; 
			for (int i= 0; i < tasks.length; i++) {
				final int tasknum= i;
				tasks[i]= new ContrailAction("Test Task "+tasknum) {
					protected void action() throws Exception {
						System.out.println("task "+tasknum);
					}
				};
			}
			
			// run all tasks
			for (int i= 0; i < tasks.length; i++) {
				results[i]= tasks[i].submit();
			}
			
			// wait tasks to complete
			TaskUtils.combineResults(tasks);
			
		}
	}
	/**
	 * Simple test that runs several tasks at once
	 */
	public void testSimpleContrailTask() throws Throwable {
		Thread.currentThread().setContextClassLoader(_javaflowClassLoader);
		Runnable testRunner= (Runnable)_javaflowClassLoader.loadClass(SimpleTaskTestRunner.class.getName()).newInstance();
		testRunner.run();
	}
	
	
	public static class JoinTaskTestRunner implements Runnable {
		public void run() {
			final ContrailAction[] tasks= new ContrailAction[5/*100*/];
			final boolean[] completed= new boolean[tasks.length];
			for (int i= 0; i < tasks.length; i++) {
				final int tasknum= i;
				completed[tasknum]= false;
				tasks[i]= new ContrailAction("Test Task "+tasknum) {
					protected void action() throws Exception {
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
	/**
	 *  Tests ability of a task to wait for another task to complete 
	 */
	public void testContrailTaskJoins() throws Throwable {
		Thread.currentThread().setContextClassLoader(_javaflowClassLoader);
		Runnable testRunner= (Runnable)_javaflowClassLoader.loadClass(JoinTaskTestRunner.class.getName()).newInstance();
		testRunner.run();
	}
}
