package com.googlecode.contraildb.core.utils.tasks;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import com.googlecode.contraildb.core.utils.ClosableByteArrayOutputStream;
import com.googlecode.contraildb.core.utils.ContrailTask;
import com.googlecode.contraildb.core.utils.ExternalizationManager;
import com.googlecode.contraildb.core.utils.Logging;
import com.googlecode.contraildb.core.utils.TaskUtils;


/**
 * Persists the given object to a byte array using the Contrail externalization 
 * scheme.
 */
public class ExternalizationTask extends ContrailTask<byte[]>  {
	ClosableByteArrayOutputStream _byteStream= new ClosableByteArrayOutputStream();
	Object _item;
	public ExternalizationTask(Object item) {
		_item= item;
	}
	
	@Override
	public ExternalizationTask submit() {
		super.submit();
		return this;
	}
	
	@Override
	public ExternalizationTask submit(List<ContrailTask<?>> dependentTasks) {
		super.submit(dependentTasks);
		return this;
	}
	
	protected void run() throws IOException {
		try {
			ExternalizationManager.writeExternal(new DataOutputStream(_byteStream), _item);
			setResult(_byteStream.toByteArray());
		}
		catch (Throwable x) {
			if (!isCancelled()) { // if task was canceled then we can ignore the error.
				x.printStackTrace();
				TaskUtils.throwSomething(x, IOException.class);
			}
		}
	}
	
	public byte[] get() {
		return super.get();
	}
	
	protected void stop() {
		/*
		 * If/when this task is canceled we close the byte stream which causes 
		 * the above call to ExternalizationManager.writeExternal to fail.
		 */
		try { _byteStream.close(); } catch (Throwable t) { Logging.warning(t); }
	}
}
