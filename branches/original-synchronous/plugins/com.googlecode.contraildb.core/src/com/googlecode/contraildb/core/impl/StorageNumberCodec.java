package com.googlecode.contraildb.core.impl;

import java.math.BigDecimal;
import java.text.ParseException;

import com.googlecode.contraildb.core.ContrailException;



/**
 * Encodes numbers as strings in such a way that the lexicographic order of the 
 * encoded strings is the same as the natural order of the original numbers.
 * The length of an encoded number is only slightly larger than the length 
 * of its original number.
 * Unlike other schemes, there is no limit to the size of numbers which may be encoded.
 *    
 * @author ted stockwell
 *
 */
public class StorageNumberCodec {

	public static final String decode(String input) {
		try
		{
			if (input == null)
				return null;
			if (input.length() <= 0)
				return "";
			return new Decoder(input)._output;
		} 
		catch (ParseException e) {
			throw new ContrailException("Failed to decode number:"+input, e);
		}
	}
	public static final BigDecimal decodeAsBigDecimal(String input) {
		try
		{
			if (input == null)
				return null;
			if (input.length() <= 0)
				throw new ContrailException("Internal Error: Cannot decode an empty String");
			return new BigDecimal(new Decoder(input)._output);
		} 
		catch (ParseException e) {
			throw new ContrailException("Failed to decode number:"+input, e);
		}
	}
	

	public static final String encode(String input) {
		try
		{
			if (input == null)
				return null;
			if (input.length() <= 0)
				return "";
			return new Encoder(input)._output;
		} 
		catch (ParseException e)
		{
			throw new ContrailException("Failed to parse number:"+input, e);
		}
	}
	public static final String encode(BigDecimal decimal) {
		if (decimal == null)
			return null;
		return encode(decimal.toPlainString());
	}
	
	
	
	static public class Encoder {
		
		private String _input;
		private int _position= 0;
		private int _end;
		private String _output= "";
		private boolean _isNegative= false;
		
		private Encoder(String input) throws ParseException {
			_input= input;
			_end= _input.length();
			
			char c= _input.charAt(_position); 
			if (c == '-') {
				_input.charAt(_position++);
				_isNegative= true;
			}
			
			readNumberBeforeDecimal();
			if (readDecimalPoint()) {
				readNumber(_end - _position);
			}
			_output+= _isNegative ? '?' : '*';
		}
		
		private boolean readDecimalPoint() throws ParseException {
			if (_end <= _position)
				return false;
			char c= _input.charAt(_position++);
			if (c != '.')
				throwParseException("Expected decimal point");
			if (_end <= _position)
				return false;
			_output+= _isNegative ? ':' : '.';
			return true;
		}

		private void readNumberBeforeDecimal() throws ParseException {
			char[] buffer= new char[_input.length()];
			
			// read number until decimal point reached or end
			int i= 0;
			while(_end > _position) {
				char c= _input.charAt(_position++);
				if ('0' <= c && c <= '9') { 
					buffer[i++]= (char)(_isNegative ? '0'+('9'-c) : c);
				}
				else if (c == '.') {
					_position--;
					break;
				}
			}
			
			// now figure out needed prefixes
			String prefix= "";
			int l= i;
			String unaryPrefix= _isNegative ? "*" : "?";
			while (1 < l) {
				unaryPrefix+= _isNegative ? '*' : '?';
				String s= Integer.toString(l);
				if (_isNegative) {
					char[] cs= s.toCharArray();
					for (int j= 0; j < cs.length; j++) 
						cs[j]= (char)('0'+ '9'-cs[j]);
					s= new String(cs);
				}
				prefix= s+prefix;
				l= s.length();
			}
			
			_output+= unaryPrefix; // output unary prefix count
			_output+= prefix; // output prefixes
			_output+= new String(buffer, 0, i); // now output actual number
		}

		private void readNumber(int length) {
			if (_isNegative) {
				while (0 < length--) {
					_output+= (char)('0'+('9'-_input.charAt(_position++)));
				}
			}
			else {
				_output+= _input.substring(_position, _position+length);
				_position+= length;
			}
		}

		private void throwParseException(String message) throws ParseException {
			throw new ParseException(message, _position);		
		}
	}

	
	
	
	static public class Decoder {
		
		
		private String _input;
		private int _position= 0;
		private int _end;
		private String _output= "";
		private boolean _isNegative= false;
		
		private Decoder(String input) throws ParseException {
			_input= input;
			_end= _input.length();
			int lastChar= _input.charAt(_end-1);
			while (lastChar == '*' || lastChar== '?' || lastChar == ':' || lastChar == '.') 
				lastChar= _input.charAt((--_end)-1);
			
			char c= _input.charAt(_position);
			if (c == '*') {
				_output+= '-';
				_isNegative= true;
			}
			else if (c != '?') 
				throw new ParseException("All encoded numbers must begin with either '?' or '*'", _position);
			
			readSequence();
			if (readDecimalPoint()) {
				readNumber(_end - _position);
			}
		}
		
		private boolean readDecimalPoint() throws ParseException {
			if (_end <= _position)
				return false;
			char c= _input.charAt(_position++);
			if (c != (_isNegative ? ':' : '.'))
				throw new ParseException("Expected decimal point", _position);
			if (_end <= _position)
				return false;
			_output+= '.';
			return true;
		}

		private void readSequence() throws ParseException {
			int sequenceCount= 0;
			while(true) {
				int c= _input.charAt(_position++);
				if (c == '*' || c == '?') { 
					sequenceCount++;
				}
				else {
					_position--;
					break;
				}
			}
			readNumberSequence(sequenceCount);
		}

		private void readNumberSequence(int sequenceCount) {
			int prefixLength= 1;
			while (1 < sequenceCount--) {
				prefixLength= readPrefix(prefixLength);
			}
			readNumber(prefixLength);
		}

		private int readPrefix(int length) {
			String s;
			if (_isNegative) {
				char[] cs= new char[length];
				int i= 0;
				while (0 < length--) {
					cs[i++]= (char)('0'+('9'-_input.charAt(_position++)));
				}
				s= new String(cs);
			}
			else {
				s= _input.substring(_position, _position+length);
				_position+= length;
			}
			return Integer.parseInt(s);
		}
		
		private void readNumber(int length) {
			if (_isNegative) {
				while (0 < length--) {
					_output+= (char)('0'+('9'-_input.charAt(_position++)));
				}
			}
			else {
				_output+= _input.substring(_position, _position+length);
				_position+= length;
			}
		}
	}
}
