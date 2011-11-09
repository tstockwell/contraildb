package com.googlecode.contraildb.provider.s3;
import java.io.IOException;
import java.util.Collection;


import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.security.AWSCredentials;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.storage.provider.AbstractStorageProvider;
import com.googlecode.contraildb.core.storage.provider.ContrailStorageException;


public class S3StorageProvider extends AbstractStorageProvider {
	
	private String _accessKey;
	private String _secretKey;
	private String _bucketName;
	
	
	private RestS3Service _restS3Service;
	private S3Bucket _bucket;

	
	public S3StorageProvider(String accessKey, String secretKey, String bucketName, boolean create)
	{
		try {
			_accessKey= accessKey;
			_secretKey= secretKey;
			_bucketName= bucketName;
			AWSCredentials credentials= new AWSCredentials(accessKey, secretKey);
			_restS3Service= new RestS3Service(credentials);
			if (!_restS3Service.isBucketAccessible(_bucketName)) {
				if (create) {
					_bucket= _restS3Service.createBucket(bucketName);
				}
				else
					throw new ContrailStorageException("Bucket does not exist: "+bucketName);
			}
			else 
				_bucket= new S3Bucket(_bucketName);
		} 
		catch (S3ServiceException e) {
			throw new ContrailStorageException("Failed to connect to S3", e);
		}
	}
	

	@Override
	public com.googlecode.contraildb.core.storage.provider.IStorageProvider.Session connect()
	throws IOException {
		return new S3StorageSession();
	}
	
	
	class S3StorageSession extends AbstractStorageProvider.Session {

		@Override
		protected void doClose() throws IOException {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void doDelete(Identifier path) throws IOException {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected byte[] doFetch(Identifier path) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected void doFlush() throws IOException {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected Collection<Identifier> doList(Identifier path)
				throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected void doStore(Identifier path, byte[] byteArray)
				throws IOException {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected boolean exists(Identifier path) throws IOException {
			// TODO Auto-generated method stub
			return false;
		}
		
	}

}
