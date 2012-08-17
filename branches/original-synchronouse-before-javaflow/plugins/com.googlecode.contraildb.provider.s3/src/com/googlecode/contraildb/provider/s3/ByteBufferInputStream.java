package com.googlecode.contraildb.provider.s3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteBufferInputStream extends InputStream {
	
	ByteBuffer _buffer;
	
	public ByteBufferInputStream(ByteBuffer buffer) {
		_buffer= buffer.duplicate();
	}

	@Override
	public int read() throws IOException {
    	if (_buffer.remaining() <= 0)
    		return -1;

		byte b= _buffer.get();
		return b & 0xFF;
	}
	
	
    public int read(byte b[], int off, int len) 
    throws IOException {
    	int remaining= _buffer.remaining();
    	if (remaining <= 0)
    		return -1;
    	if (remaining < len)
    		len= remaining;
    	_buffer.get(b, off, len);
    	return len;
    }
    
    @Override
    public long skip(long n) throws IOException {
    	int remaining= _buffer.remaining();
    	if (Integer.MAX_VALUE < n)
    		n= Integer.MAX_VALUE;
    	if (remaining <= 0)
    		return 0;
    	if (remaining < n)
    		n= remaining;
    	_buffer.position(_buffer.position()+(int)n);
    	return n;
    }

}
