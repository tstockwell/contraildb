package com.googlecode.contraildb.core.storage;

import java.io.DataInput;
import java.io.IOException;

import com.googlecode.contraildb.core.ContrailException;
import com.googlecode.contraildb.core.IResult;
import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.Magic;
import com.googlecode.contraildb.core.async.Handler;
import com.googlecode.contraildb.core.async.TaskUtils;
import com.googlecode.contraildb.core.utils.ExternalizationManager.Serializer;
import com.googlecode.contraildb.core.utils.Logging;


/**
 * A folder that is used to control access to some resource.
 * A process gains access to a resource by locking the lock folder.
 * This class uses a home-grown consensus algorithm which enables remote processes 
 * to decide among themselves who gets the lock.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class LockFolder extends Entity {
	private static final long serialVersionUID = 1L;
	
	
	public static Identifier createId(Entity parent) {
		return Identifier.create(parent.id, "lockfolder");
	}

	public LockFolder(Entity parent) {
		super(createId(parent));
	}
	
	protected LockFolder() { }
	
	public static class Lock extends Entity {
		private static final long serialVersionUID = 1L;
		String processId;
		Lock(Identifier lockfolderId, String processId) {
			super(Identifier.create(lockfolderId, "lock"));
			this.processId= processId;
		}
		protected Lock() { }


		public static final Serializer<Lock> SERIALIZER= new Serializer<Lock>() {
			private final int typeCode= Lock.class.getName().hashCode();
			public Lock readExternal(java.io.DataInput in) 
			throws IOException {
				Lock journal= new Lock();
				readExternal(in, journal);
				return journal;
			};
			public void writeExternal(java.io.DataOutput out, Lock journal) 
			throws IOException {
				Entity.SERIALIZER.writeExternal(out, journal);
				out.writeUTF(journal.processId);
			};
			public void readExternal(DataInput in, Lock journal)
			throws IOException {
				Entity.SERIALIZER.readExternal(in, journal);
				journal.processId= in.readUTF();
			}
			public int typeCode() {
				return typeCode;
			}
		};
	}

	/**
	 * Locks this folder using a consensus algorithm.
	 * The consensus algorithm enables many remote, anonymous processes to 
	 * decide among themselves who gets the lock.
	 * Locks expire after some amount of time, around one minute.
	 * 
	 * @param processId The id of the claiming process.
	 * @param waitForNext wait until current lock expires and then try to break it
	 * @return true if the process was awarded the lock   
	 * @throws IOException 
	 */
	public IResult<Boolean> lock(final String processId, final boolean waitForNext) 
	{
		final Lock lock= new Lock(id, processId);
		final long waitMillis= waitForNext? Magic.SESSION_MAX_MILLIS+1000 : 0;
		final IResult<Boolean> result= storage.create(lock, waitMillis);
		return new Handler(result) {
			protected IResult onSuccess() throws Exception {
				if (result.getResult()) {
					return TaskUtils.SUCCESS;
				}
				else if (waitForNext) {
					// break existing lock
					storage.delete(lock.getId());
					if (storage.create(lock, waitMillis).get()) {
						return TaskUtils.SUCCESS;
					}
					else
						throw new ContrailException("Database is locked, waitMillis="+waitMillis);
				}
				return TaskUtils.FAIL;
			};
		}.toResult();
	}


	public IResult<Void> unlock(final String processId)  
	{
		final Identifier lockId= Identifier.create(id, "lock");
		final IResult<Lock> lockResult= storage.fetch(lockId);
		return new Handler(lockResult) {
			protected IResult onSuccess() throws Exception {
				try {
					Lock lock= lockResult.getResult();
					if (lock == null || !lock.processId.equals(processId))
						throw new ContrailException("Internal Error: Session "+processId+" tried to unlock a folder that it did not own: "+id);
					spawn(storage.delete(lock.getId()));
					spawn(storage.flush());
				} 
				catch (Throwable e) {
					Logging.warning("Error while unlocking folder "+lockId, e);
				}
				return TaskUtils.DONE;
			};
		}.toResult();
	}
	


	public static final Serializer<LockFolder> SERIALIZER= new Serializer<LockFolder>() {
		private final int typeCode= LockFolder.class.getName().hashCode();
		public LockFolder readExternal(java.io.DataInput in) 
		throws IOException {
			LockFolder journal= new LockFolder();
			readExternal(in, journal);
			return journal;
		};
		public void writeExternal(java.io.DataOutput out, LockFolder journal) 
		throws IOException {
			Entity.SERIALIZER.writeExternal(out, journal);
		};
		public void readExternal(DataInput in, LockFolder journal)
		throws IOException {
			Entity.SERIALIZER.readExternal(in, journal);
		}
		public int typeCode() {
			return typeCode;
		}
	};
	
}