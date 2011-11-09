package com.googlecode.contraildb.provider.s3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import net.sf.storehaus.s3filesystem.S3Bucket;
import net.sf.storehaus.s3filesystem.S3Exception;
import net.sf.storehaus.s3filesystem.S3FileSystem;
import net.sf.storehaus.s3filesystem.S3Object;
import net.sf.storehaus.s3filesystem.S3QueryResults;

import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;

public class AmazonS3Bucket implements S3Bucket {
	
	String _name;
	AmazonS3FileSystem _fileSystem;
	RestS3Service _restS3Service;
	org.jets3t.service.model.S3Bucket _bucket;

	public AmazonS3Bucket(AmazonS3FileSystem fileSystem, String bucketName) 
	throws S3Exception 
	{
		_name= bucketName;
		_fileSystem= fileSystem;
		_restS3Service= _fileSystem._restS3Service;
		_bucket= new org.jets3t.service.model.S3Bucket(_name);
	}

	public Collection<S3Object> getAllObjects() throws IOException {
		try {
			org.jets3t.service.model.S3Object[] jet3tObjects= 
				_restS3Service.listObjects(_bucket);
			
			ArrayList<S3Object> objects= new ArrayList<S3Object>();
			for (int i= 0; i < jet3tObjects.length; i++) {
				
				objects.add(new AmazonS3Object(this, jet3tObjects[i].getKey()));
			}
			return objects;
		} catch (S3ServiceException e) {
			e.printStackTrace();
			throw new IOException(e.getMessage(), e);
		}
	}

	public String getName() {
		return _name;
	}

	public S3Object getObject(String objectName) {
			return new AmazonS3Object(this, objectName);
	}

	public S3FileSystem getFileSystem() {
		return _fileSystem;
	}

	public String getCanonicalName() throws IOException {
		String url= _fileSystem.getCanonicalName();
		String protocol= "";
		String hostname= url;
		int i= url.indexOf("//");
		if (0 <= i) {
			protocol= url.substring(0, i+2);
			hostname= url.substring(i+2);
		}
		hostname= _name+"."+hostname;
		return protocol+hostname;
	}

	public boolean create() throws IOException {
		try {
			if (_restS3Service.isBucketAccessible(_name)) 
				return false;
			_restS3Service.createBucket(_name);
			return true;
		} catch (S3ServiceException e) {
			e.printStackTrace();
			throw new IOException(e.getMessage(), e);
		}
	}

	public boolean delete() throws IOException {
		try {
			if (_restS3Service.isBucketAccessible(_name) == false) 
				return false;
			_restS3Service.deleteBucket(_name);
			return true;
		} catch (S3ServiceException e) {
			e.printStackTrace();
			throw new IOException(e.getMessage(), e);
		}
	}

	public boolean exists() throws IOException {
		try {
			return _restS3Service.isBucketAccessible(_name); 
		} catch (S3ServiceException e) {
			e.printStackTrace();
			throw new IOException(e.getMessage(), e);
		}
	}

	public S3QueryResults queryObjects(final String prefix, final String marker, String delimiter, final int maxResults) 
	throws IOException {
		
		try {
			org.jets3t.service.model.S3Object[] objects= null;
			if (0 < maxResults) {
				objects= _restS3Service.listObjects(_bucket, prefix, delimiter);
			}
			else {
				objects= _restS3Service.listObjects(_bucket, prefix, delimiter, maxResults);
			}
			
			S3QueryResults queryResults= new S3QueryResults();
			queryResults.matchingPrefixes= new ArrayList<String>();
			queryResults.matchingObjects= new ArrayList<S3Object>();
			for (org.jets3t.service.model.S3Object object : objects) {
				AmazonS3Object amazonS3Object= new AmazonS3Object(this, object.getKey());
				queryResults.matchingObjects.add(amazonS3Object);
			}
			
			return queryResults;
		} catch (S3ServiceException e) {
			e.printStackTrace();
			throw new IOException(e.getMessage(), e);
		}
	}


}
