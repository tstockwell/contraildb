package com.googlecode.contraildb.core.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * The ByteArrayOutputStream.close method does nothing.
 * In order to cancel a thread that is writing to a ByteArrayOutputStream 
 * we need the close method to cause the write methods to fail after the 
 * close method is invoked.  This implementation adds that behavior.
 *       
 * @author Ted Stockwell
 */
public class ClosableByteArrayOutputStream extends ByteArrayOutputStream {
	
	private volatile boolean _closed= false;
	
	@Override
	public synchronized byte[] toByteArray() {
		if (_closed) throw new IllegalStateException("The output stream has alread been closed");
		return super.toByteArray();
	}

	@Override
	public void close() throws IOException {
		_closed= true;
		super.close();
	}

	@Override
	public synchronized void reset() {
		if (_closed) throw new IllegalStateException("The output stream has alread been closed");
		super.reset();
	}

	@Override
	public synchronized void write(byte[] b, int off, int len) {
		if (_closed) throw new IllegalStateException("The output stream has alread been closed");
		super.write(b, off, len);
	}

	@Override
	public synchronized void write(int b) {
		if (_closed) throw new IllegalStateException("The output stream has alread been closed");
		super.write(b);
	}

	@Override
	public synchronized void writeTo(OutputStream out) throws IOException {
		if (_closed) throw new IllegalStateException("The output stream has alread been closed");
		super.writeTo(out);
	}

	@Override
	public void flush() throws IOException {
		if (_closed) throw new IllegalStateException("The output stream has alread been closed");
		super.flush();
	}

	@Override
	public void write(byte[] b) throws IOException {
		if (_closed) throw new IllegalStateException("The output stream has alread been closed");
		super.write(b);
	}
	

}
