package com.googlecode.contraildb.core.storage.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;

import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.async.TaskUtils;
import com.googlecode.contraildb.core.utils.ContrailAction;
import com.googlecode.contraildb.core.utils.ContrailTask;


/**
 * Implementation of the IStorageProvider interface that stores items in files 
 * on the local file system.
 * In order to improve performance this implementation performs many operations 
 * asynchronously.  
 * This implementation is very naive for a database storage facility, it just 
 * writes all items to separate files.
 * On the other hand, it works plenty fast and has some advantages.  For instance 
 * it makes it possible to do incremental backup of the database using  
 * off-the-shelf backup routines.  
 * 
 * This implementation is only meant for embedded use by a single process.
 * Another class, ServerStorageProvider, implements an HTTP API on top of 
 * this class that provides multi-user, client-server access to a file store. 
 *  
 * @see ServerStorageProvider for client/server access to a file store 
 * 
 * @author Ted Stockwell
 */
public class FileStorageProvider extends AbstractStorageProvider {
	
	static final String LOCK_FILE = ".lock";
	static final String CONTENT_FILE= ".content"; 
	static final long RETRY_MILLIS= 100; 

	
	private File _root;
	
	class DeleteAction extends ContrailAction {
		File _file;
		DeleteAction(Identifier id, File file) {
			super(id, Operation.DELETE);
			_file= file;
		}
		@Override
		protected void action() {
			File[] files= _file.listFiles();
			if (files != null) {
				ArrayList<IResult<Void>> tasks= new ArrayList<IResult<Void>>();
				for (final File file2: files)
					tasks.add(new DeleteAction(Identifier.create(getId(), file2.getName()), file2).submit());
				TaskUtils.combineResults(tasks).get();
			}
			for (int i= 0; i < 10; i++) {
				if (_file.delete()) {
					return;
				}
				if (!_file.exists())
					return;

				try { Thread.sleep(10); } catch (InterruptedException x) { }
			}
			throw new RuntimeException(new IOException("Failed to delete file "+_file));
		}
	}
	
	public FileStorageProvider(File root) {
		root.mkdirs();
		_root= root;		
	}
	public FileStorageProvider(File root, boolean clean) throws IOException {
		if (clean) {
			if (root.exists()) {
				try {
					new DeleteAction(Identifier.create(""), root).submit().get();
				}
				catch (Throwable t) {
					TaskUtils.throwSomething(t, IOException.class);
				}
			}
		}
		root.mkdirs();
		_root= root;
	}
	
	public File getRoot() {
		return _root;
	}
	
	@Override
	public IResult<IStorageProvider.Session> connect() {
		return TaskUtils.asResult(new FileStorageSession());
	}
	
	
	class FileStorageSession extends AbstractStorageProvider.Session {
		
		@Override
		protected IResult<Void> doClose() {
			// do nothing
			return TaskUtils.asResult(null);
		}

		@Override
		protected IResult<byte[]> doFetch(final Identifier path) {
			return new ContrailTask<byte[]>() {
				@Override protected byte[] run() throws Exception {
					return fetchFile(new File(new File(_root, path.toString()), CONTENT_FILE));
				}
			}.submit();
		}

		@Override
		protected IResult<Void> doFlush() {
			// do nothing
			return TaskUtils.asResult(null);
		}

		@Override
		protected Collection<Identifier> doList(Identifier path) {
			ArrayList<Identifier> paths= new ArrayList<Identifier>();
			File parent= new File(_root, path.toString());
			File[] files= parent.listFiles();
			if (files != null) {
				for (File file: files) {
					if (file.isDirectory())
						paths.add(Identifier.create(path, file.getName()));
				}
			}
			return paths;
		}

		@Override
		protected IResult<Void> doStore(final Identifier path, final byte[] byteArray) 
		{
			return new ContrailAction() {
				@Override protected void action() throws Exception {
					File folder= new File(_root, path.toString());
					folder.mkdirs();
					File file= new File(folder, CONTENT_FILE);
					OutputStream out= new FileOutputStream(file);
					try {
						out.write(byteArray);
						out.flush();
					}
					finally {
						out.close(); 
					}
				}
			}.submit();
		}
		
		@Override
		protected IResult<Void> doDelete(Identifier path) {
			return new DeleteAction(path, new File(_root, path.toString())).submit();
		}
		
		@Override
		protected IResult<Boolean> exists(Identifier path) {
			return TaskUtils.asResult(new File(_root, path.toString()).exists());
		}

		@Override
		protected IResult<Boolean> doCreate(final Identifier path, final byte[] byteArray, final long waitMillis) {
			return new ContrailTask<Boolean>() {
				@Override protected Boolean run() throws Exception {
					boolean success= false;
					long start= System.currentTimeMillis();
					File folder= new File(_root, path.toString());
					while(!success) {
						if (folder.mkdirs()) {
							// ok, we created this folder, now just write the contents and we're done...
							File file= new File(folder, CONTENT_FILE);
							OutputStream out= new FileOutputStream(file);
							try {
								out.write(byteArray);
								out.flush();
							}
							finally {
								out.close(); 
							}
							success= true; 
						}
						else {
							// check to see if we've timed out
							if (waitMillis < System.currentTimeMillis() - start)
								break;
							
							// do something else for a while
							yield(100);
						}
					}
					return success;
				}
			}.submit();
		}
	}
	
	
	static byte[] fetchFile(File file) throws IOException {
		InputStream in= null;
		try {
			in= new FileInputStream(file);
			int length= (int)file.length();
			byte[] content= new byte[length];
			int count= 0; 
			while (true) {
				int c= in.read(content, count, length);
				if (c < 0)
					throw new IOException("Premature end of file reached");
				if ((length-= c) <= 0)
					break;
				count+= c;
			}
			return content;
		}
		catch (FileNotFoundException x) {
			return null;
		}
		finally {
			if (in != null) {
				try { in.close(); } catch (Throwable t) { }
			}
		}
	}


}
