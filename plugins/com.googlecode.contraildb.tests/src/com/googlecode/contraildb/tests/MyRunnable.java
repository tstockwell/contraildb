package com.googlecode.contraildb.tests;

import org.apache.commons.javaflow.Continuation;

public class MyRunnable implements Runnable {
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