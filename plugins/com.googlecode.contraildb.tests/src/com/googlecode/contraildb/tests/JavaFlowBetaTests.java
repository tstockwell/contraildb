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

import junit.framework.TestCase;

import org.apache.commons.javaflow.Continuation;
import org.apache.commons.javaflow.ContinuationClassLoader;


/**
 * Test the Contrail storage system.
 * 
 * @author Ted Stockwell
 */
public class JavaFlowBetaTests extends TestCase {
	
	MyRunnable _runnable;
	
	@Override
	protected void setUp() throws Exception {
		ClassLoader parent= JavaFlowBetaTests.class.getClassLoader();
		ClassLoader cl = new ContinuationClassLoader( new URL[]{}, parent );
		_runnable= (MyRunnable) cl.loadClass(MyRunnable.class.getCanonicalName()).newInstance();
	}

	public void testSimpleContinuation() {
		System.out.println("main started");
		Continuation c = Continuation.startWith(new MyRunnable());
		System.out.println("in main after continuation return");
		while (c != null) {
			c = Continuation.continueWith(c);
			System.out.println("in main");
		}
	}
}
