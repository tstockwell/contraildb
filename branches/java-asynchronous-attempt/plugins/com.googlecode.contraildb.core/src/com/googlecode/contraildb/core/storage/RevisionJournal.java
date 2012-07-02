package com.googlecode.contraildb.core.storage;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.googlecode.contraildb.core.Identifier;
import com.googlecode.contraildb.core.utils.ExternalizationManager.Serializer;


/**
 * A list of all the items that were changed in a revision.
 */
public class RevisionJournal extends Entity {
	private static final long serialVersionUID = 1L;

	public static Identifier createId(Entity parent) {
		return Identifier.create(parent.id, "journal");
	}

	List<Identifier> reads;
	List<Identifier> inserts;
	List<Identifier> deletes;
	List<Identifier> updates;

	public RevisionJournal(RevisionFolder revision) {
		super(createId(revision));
		reads= Collections.emptyList();
		inserts= Collections.emptyList();
		deletes= Collections.emptyList();
		updates= Collections.emptyList();
	}

	public RevisionJournal(RevisionFolder revision, StorageSession storageSession) {
		super(createId(revision));

		// sort the paths before we save them, this will make it faster to 
		// find conflicting changes between revisions

		ArrayList<Identifier> reads= new ArrayList<Identifier>(storageSession._reads);
		Collections.sort(reads);
		this.reads= reads;

		ArrayList<Identifier> inserts= new ArrayList<Identifier>(storageSession._inserts);
		Collections.sort(inserts);
		this.inserts= inserts;

		ArrayList<Identifier> deletes= new ArrayList<Identifier>(storageSession._deletes);
		Collections.sort(deletes);
		this.deletes= deletes;

		ArrayList<Identifier> updates= new ArrayList<Identifier>(storageSession._updates);
		Collections.sort(updates);
		this.updates= updates;
	}

	protected RevisionJournal() { }

	public boolean confictsWith(RevisionJournal journal) {
		// The revision associated with this journal was committed before the 
		// revision associated with the give journal, check for conflicting changes.

		if (journal.reads.isEmpty())
			return false;

		for (List<Identifier> changes: Arrays.asList(new List[] { updates, deletes })) {
			if (!changes.isEmpty()) {
				Iterator<Identifier> ri= journal.reads.iterator();
				Iterator<Identifier> ui= changes.iterator();
				Identifier r= ri.next();
				Identifier u= ui.next();
				for (;;) {
					int c= r.compareTo(u);
					if (c == 0)
						return true;
					if (c < 0) {
						if (ri.hasNext())
							break;
						r= ri.next();
					}
					else {
						if (ui.hasNext())
							break;
						u= ui.next();
					}
				}
			}
		}

		return false;
	}


	public static final Serializer<RevisionJournal> SERIALIZER= new Serializer<RevisionJournal>() {
		private final int typeCode= RevisionJournal.class.getName().hashCode();
		public RevisionJournal readExternal(java.io.DataInput in) 
		throws IOException {
			RevisionJournal journal= new RevisionJournal();
			readExternal(in, journal);
			return journal;
		};
		public void writeExternal(java.io.DataOutput out, RevisionJournal journal) 
		throws IOException {
			Entity.SERIALIZER.writeExternal(out, journal);

			out.writeInt(journal.reads.size());
			for (Identifier identifier:journal.reads)
				Identifier.SERIALIZER.writeExternal(out, identifier);

			out.writeInt(journal.inserts.size());
			for (Identifier identifier:journal.inserts)
				Identifier.SERIALIZER.writeExternal(out, identifier);

			out.writeInt(journal.deletes.size());
			for (Identifier identifier:journal.deletes)
				Identifier.SERIALIZER.writeExternal(out, identifier);

			out.writeInt(journal.updates.size());
			for (Identifier identifier:journal.updates)
				Identifier.SERIALIZER.writeExternal(out, identifier);
		};
		public void readExternal(DataInput in, RevisionJournal journal)
		throws IOException {
			Entity.SERIALIZER.readExternal(in, journal);

			int size= in.readInt();
			journal.reads= new ArrayList<Identifier>(size);
			for (int i= size; 0 < i--;)
				journal.reads.add(Identifier.SERIALIZER.readExternal(in));

			size= in.readInt();
			journal.inserts= new ArrayList<Identifier>(size);
			for (int i= size; 0 < i--;)
				journal.inserts.add(Identifier.SERIALIZER.readExternal(in));

			size= in.readInt();
			journal.deletes= new ArrayList<Identifier>(size);
			for (int i= size; 0 < i--;)
				journal.deletes.add(Identifier.SERIALIZER.readExternal(in));

			size= in.readInt();
			journal.updates= new ArrayList<Identifier>(size);
			for (int i= size; 0 < i--;)
				journal.updates.add(Identifier.SERIALIZER.readExternal(in));
		}
		public int typeCode() {
			return typeCode;
		}
	};

}

