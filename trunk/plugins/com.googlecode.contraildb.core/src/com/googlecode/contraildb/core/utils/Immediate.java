package com.googlecode.contraildb.core.utils;

/**
 * A marker annotation used in Contrail's internal implementation.
 * It is used to identify methods that return results immediately, 
 * not asynchronously.
 * When Contrail's internal implementation was being rewritten to be mostly 
 * asynchronous this marker helped to identify those methods that were 
 * known to return results immediately, rather than a method that should 
 * be asynchronous but has not yet been rewritten to be asynchronous.
 * The complexity of Contrails's code to leave as many methods as possible 
 * as synchronous methods. 
 * 
 * One thing that's nice is that, when coding in Eclipse, the IDE displays annotations 
 * during hover, so you can if a method that you are using is immediate 
 * or not at a glance.  
 * 
 * @author ted stockwell
 *
 */
public @interface Immediate {

}
