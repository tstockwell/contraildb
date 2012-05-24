package com.googlecode.contraildb.core.async;




/**
 * Executes handlers concurrently (or maybe just in parallel, depending on 
 * if the underlying system is single-threaded or not).
 * 
 * If an error occurs in any of the handlers the onError method is invoked.
 * The onError method is meant to be overwritten by subclasses.
 * 
 * @author ted.stockwell
 *
 */
@SuppressWarnings({"rawtypes","unchecked"})
public class Parallel extends Handler {
	public Parallel() {
	}
}
