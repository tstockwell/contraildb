package com.googlecode.contraildb.core.storage;

import java.io.DataInput;
import java.io.IOException;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.utils.ExternalizationManager.Serializer;


/**
 * A marker that is written into a revision folder that indicates that the 
 * revision is committed. 
 */
public class CommitMarker extends Entity {
	private static final long serialVersionUID = 1L;

	public static Identifier createId(Entity parent) {
		return Identifier.create(parent.id, "commitMarker");
	}
	long finalCommitNumber;
	
	public CommitMarker(RevisionFolder revision, long finalCommitNumber) {
		super(createId(revision));
		this.finalCommitNumber= finalCommitNumber;
	}

	protected CommitMarker() { }

	public static final Serializer<CommitMarker> SERIALIZER= new Serializer<CommitMarker>() {
		private final String typeCode= CommitMarker.class.getName();
		public CommitMarker readExternal(java.io.DataInput in) 
		throws IOException {
			CommitMarker journal= new CommitMarker();
			readExternal(in, journal);
			return journal;
		};
		public void writeExternal(java.io.DataOutput out, CommitMarker journal) 
		throws IOException {
			Entity.SERIALIZER.writeExternal(out, journal);
			out.writeLong(journal.finalCommitNumber);
		};
		public void readExternal(DataInput in, CommitMarker journal)
		throws IOException {
			Entity.SERIALIZER.readExternal(in, journal);
			journal.finalCommitNumber= in.readLong();
		}
		public String typeCode() {
			return typeCode;
		}
	};
}