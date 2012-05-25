package com.googlecode.contraildb.core.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.googlecode.contraildb.core.Item;
import com.googlecode.contraildb.core.ContrailQuery.SortPredicate;


public class SortComparator<T extends Item> implements Comparator<T> {
	
	List<PropertyComparator<T>> _comparators= new ArrayList<PropertyComparator<T>>();

	public SortComparator(List<SortPredicate> sorts) {
		for (SortPredicate sortPredicate: sorts)
			_comparators.add(new PropertyComparator<T>(sortPredicate));
	}

	@Override
	public int compare(T o1, T o2) {
		for (PropertyComparator<T> comparator: _comparators) {
			int result= comparator.compare(o1, o2);
	        if (result != 0)
	            return result;
		}
		return 0;
	}

}
