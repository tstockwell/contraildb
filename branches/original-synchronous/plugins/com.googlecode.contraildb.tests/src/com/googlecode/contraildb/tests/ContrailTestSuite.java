/* Copyright (c) 2006, Sriram Srinivasan
 *
 * You may distribute this software under the terms of the license 
 * specified in the file "License"
 */

package com.googlecode.contraildb.tests;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import kilim.tools.WeaverClassLoader;

/** 
 * All Contrail tests must be run via this TestSuite.
 * This is because Contrail requires a special classloader 
 * that applies Kilim instrumenting at runtime.
 *  
 * @author ted.stockwell
 */
@SuppressWarnings("unchecked")
public class ContrailTestSuite {
	private static ClassLoader __classloader;
    private static TestSuite __testSuite= new TestSuite();
	
    public static Test suite() throws Exception {
    	__classloader= createKilimClassloader();
    	Thread.currentThread().setContextClassLoader(__classloader);
    	
        addTestSuite(KilimConcurrencyTests.class);
        return __testSuite;
    }
    
    
	private static void addTestSuite(Class<? extends TestCase> originalClass)  throws Exception {
        Class<? extends TestCase> testClass= (Class<? extends TestCase>) __classloader.loadClass(originalClass.getName());
        __testSuite.addTestSuite(testClass);
	}
    
	static private ClassLoader createKilimClassloader() throws Exception {
		//
		// create classloader that applies kilim instrumentation
		//
		URLClassLoader loader = (URLClassLoader) KilimConcurrencyTests.class
				.getClassLoader();
		ClassLoader gparent = loader.getParent();

		ArrayList<URL> urls2instrument = new ArrayList<URL>();
		ArrayList<URL> urls = new ArrayList<URL>();
		for (URL url : loader.getURLs()) {
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
		return new WeaverClassLoader( 
				urls2instrument.toArray(new URL[urls2instrument.size()]), loader);
	}

}
