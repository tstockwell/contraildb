package com.googlecode.contraildb.core.impl;

import com.googlecode.contraildb.core.Identifier;

public class PathUtils {
	
	private static final String REVISION_NUMBER = "revisionNumber";
	
	static public long getRevisionNumber(Identifier path) {
		
		Long l= (Long)path.getProperty(REVISION_NUMBER);
		if (l == null) {
			String s= path.getName();
			int i= s.indexOf('-');
			if (i < 0) {
				l= -1L;
			}
			else {
				try {
					l= Long.parseLong(s.substring(i+1));
				}
				catch (NumberFormatException x) {
					l= -1L;
				}
			}
			path.setProperty(REVISION_NUMBER, l);
		}
		return l;
	}
	static public void setRevisionNumber(Identifier path, Long revisionNumber) {
		path.setProperty(REVISION_NUMBER, revisionNumber);
	}

}
