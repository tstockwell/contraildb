package com.googlecode.contraildb.core.utils;

import kilim.Mailbox;
import kilim.Pausable;
import kilim.Task;

public class TestTask extends Task {
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
		if (result.length() != n)
			throw new RuntimeException("wrong");
	}

}
