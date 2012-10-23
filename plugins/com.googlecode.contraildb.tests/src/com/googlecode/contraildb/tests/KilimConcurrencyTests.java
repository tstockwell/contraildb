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

import kilim.Mailbox;
import kilim.Pausable;
import kilim.Task;

/**
 * Basic Kilim tests for verifying Kilim features and runtime characteristics.
 * 
 * @author Ted Stockwell
 */
public class KilimConcurrencyTests extends ContrailTestCase2 {

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
	
	public void testSimpleContinuation() throws Throwable {
		runTest(new Task() {
			public void execute() throws Pausable {
				System.out.println("main started");
				int n = 10;
				Mailbox<String> first = new Mailbox<String>();
				Mailbox<String> in = first;
				Mailbox<String> out= null;
				for (int i = 0; i < n; i++) {
					out = new Mailbox<String>();
					new Chain(in, out).start();
					in = out;
				}
				first.put("");
				String result = out.get();
				assertTrue(result.length() == n);
			}
		});
	}
}
