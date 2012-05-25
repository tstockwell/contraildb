package com.googlecode.contraildb.tests;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;

import com.googlecode.contraildb.core.impl.StorageNumberCodec;

import junit.framework.TestCase;

/**
 *	Tests the number encoding scheme 
 * 	@author ted stockwell
 */
public class NumberDecoderTest extends TestCase {

	public void testNumberDecoder() throws ParseException {
		String[] tokens= new String[] {
				"-100.5", "**6899:4?",
				"-10.5", "**789:4?",
				"-3.145", "*6:854?",
				"-3.14", "*6:85?",
				"-1.01", "*8:98?",
				"-1", "*8?",
				"-0.0001233", "*9:9998766?",
				"-0.000123", "*9:999876?",
				"0", "?0*",
				"0.000123", "?0.000123*",
				"0.0001233", "?0.0001233*",
				"1", "?1*",
				"1.01", "?1.01*",
				"3.14", "?3.14*",
				"3.145", "?3.145*",
				"10.5", "??210.5*",
				"100.5", "??3100.5*"
		};

		// test the ordering of the characters used in the encoding
		// if characters are not ordered in correct manner then encoding fails
		assertTrue('*' < '.');
		assertTrue('.' < '0');
		assertTrue('0' < '1');
		assertTrue('1' < '2');
		assertTrue('2' < '3');
		assertTrue('3' < '4');
		assertTrue('4' < '5');
		assertTrue('5' < '6');
		assertTrue('6' < '7');
		assertTrue('7' < '8');
		assertTrue('8' < '9');
		assertTrue('9' < ':');
		assertTrue(':' < '?');
		
		ArrayList<String> encoded= new ArrayList<String>();

		// test that the above encoded numbers are actually in lexical order like we think they are
		for (int i= 0; i < tokens.length; i= i+2) 
			encoded.add(tokens[i+1]);
		Collections.sort(encoded);
		System.out.print(encoded);
		for (int i= 0, j= 0; i < tokens.length; i= i+2) 
			assertEquals(tokens[i+1], encoded.get(j++));
		
		// test that we decode correctly
		for (int i= 0; i < tokens.length; i= i+2) 
			assertEquals(tokens[i], StorageNumberCodec.decode(tokens[i+1]));
		
		// test that we encode correctly
		for (int i= 0; i < tokens.length; i= i+2) 
			assertEquals(tokens[i+1], StorageNumberCodec.encode(tokens[i]));
		
	}
}
