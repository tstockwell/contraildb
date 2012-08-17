package com.googlecode.contraildb.provider.s3;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import net.sf.storehaus.s3filesystem.S3Bucket;
import net.sf.storehaus.s3filesystem.S3Object;
import net.sf.storehaus.s3filesystem.S3Utils;

import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;

public class AmazonS3Object implements S3Object {
	
	String _name;
	AmazonS3Bucket _bucket;
	org.jets3t.service.model.S3Object _object;
	RestS3Service _restS3Service;
	
	protected org.jets3t.service.model.S3Object getS3Object() 
	throws S3ServiceException 
	{
			if (_object == null) {
				_object= _restS3Service.getObject(_bucket.getName(), _name, null, null, null, null, null, null);
			}
			return _object;
	}
	

	public AmazonS3Object(AmazonS3Bucket bucket, String key) {
		_name= key;
		_bucket= bucket;
		_restS3Service= bucket._restS3Service;		
	}

	public S3Bucket getBucket() {
		return _bucket;
	}

	public String getName() {
		return _name;
	}
	
	public int length() {
		if (_object != null) {
			return (int)_object.getContentLength();
		}
		return -1;
	}
	
	
	synchronized public void putContents(int offset, ByteBuffer buffer) 
	throws IOException 
	{
		try {
			org.jets3t.service.model.S3Object object= getS3Object();
			object.setDataInputStream(new ByteBufferInputStream(buffer));
		} catch (S3ServiceException e) {
			e.printStackTrace();
			throw new IOException(e.getMessage(), e);
		}
	}

	public ByteBuffer getContents() throws IOException {
		try {
			org.jets3t.service.model.S3Object object= getS3Object();
			BufferedInputStream in= new BufferedInputStream(object.getDataInputStream());
			try {
				int length= (int) object.getContentLength();
				byte[] bytes= new byte[length];
				while (in.read(bytes) != -1) {
				};
				return ByteBuffer.wrap(bytes);
			} 
			finally {
				S3Utils.close(in);
			}
		} catch (S3ServiceException e) {
			e.printStackTrace();
			throw new IOException(e.getMessage(), e);
		}
	}

	public String getCanonicalName() throws IOException {
		String bucket= _bucket.getCanonicalName();
		return bucket+"/"+_name;
	}

	public void sync() throws IOException {
		// do nothing
		
	}

	public boolean create() throws IOException {
		try {
			boolean created= false;
			if (exists() == false) {
				org.jets3t.service.model.S3Object object= new org.jets3t.service.model.S3Object(_name);

				_object= _restS3Service.putObject(_bucket.getName(), object);
				created= true;
			}
			return created;
		} 
		catch (S3ServiceException e) {
			e.printStackTrace();
			throw new IOException(e.getMessage(), e);
		}
	}

	public boolean delete() throws IOException {
		boolean deleted= false;
		try {
			_restS3Service.deleteObject(_bucket.getName(), _name);
			deleted= true;
		} catch (S3ServiceException e) {
		}
		return deleted;
	}

	public boolean exists() throws IOException {
		try {
			return getS3Object() != null;
			
		} catch (S3ServiceException e) {
		}
		return false;
	}

}
