package com.googlecode.contraildb.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unchecked")
public class ContrailQuery implements Serializable {
	private static final long serialVersionUID = 1L;

	public static enum FilterOperator {
		LESS_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL, EQUAL, NOT_EQUAL, IS_NULL, NOT_NULL, AND, OR
	}

	public static enum SortDirection {
		ASCENDING, DESCENDING
	}
	
	public static enum Quantifier {
		SOME, ALL
	}
	
	public static class QuantifiedValues implements Serializable {
		private static final long serialVersionUID = 1L;
		public static long getSerialVersionUID() {
			return serialVersionUID;
		}
		private final Quantifier _type;
		private final Comparable<?>[] _values;
		public QuantifiedValues(Comparable<?> value) {
			_type= Quantifier.ALL;
			_values= new Comparable[] { value };
		}
		public QuantifiedValues(Quantifier type, Comparable<?>[] values) {
			_type= type;
			_values= values;
		}
		public Quantifier getType() {
			return _type;
		}
		public <K extends Comparable<K> & Serializable> K[] getValues() {
			return (K[]) _values;
		}
		
		@Override
		public String toString() {
			StringBuffer s= new StringBuffer();
			s.append(_type);
			s.append(" {");
			for (int i= 0; i < _values.length; i++) {
				if (0 < i)
					s.append(", ");
				s.append(_values[i]);
			}
			s.append('}');
			return s.toString();
		}
	}
	
	public static class FilterPredicate implements Serializable {
		private static final long serialVersionUID = 1L;

		private String _propertyName;
		private FilterOperator _operator;
		private List<FilterPredicate> _clauses;
		private QuantifiedValues _values;

		public <T extends Comparable<T>> FilterPredicate(String propertyName, FilterOperator operator, QuantifiedValues values) {
			if (propertyName == null)
				throw new IllegalArgumentException("propertName is null");
			if (values == null)
				throw new IllegalArgumentException("values is null");
			_propertyName = propertyName;
			_operator = operator;
			_values= values;
		}
		public FilterPredicate(FilterOperator operator, String propertyName, QuantifiedValues values) {
			if (operator != FilterOperator.EQUAL && operator != FilterOperator.NOT_EQUAL )
				throw new IllegalArgumentException("Operator must be "+FilterOperator.EQUAL + " or " + FilterOperator.NOT_EQUAL);
			if (propertyName == null)
				throw new IllegalArgumentException("propertName is null");
			if (values == null)
				throw new IllegalArgumentException("values are null");
			_propertyName = propertyName;
			_operator = operator;
			_values= values;
		}
		public FilterPredicate(FilterOperator operator, String propertyName) {
			if (operator != FilterOperator.IS_NULL && operator != FilterOperator.NOT_NULL )
				throw new IllegalArgumentException("Operator must be "+FilterOperator.IS_NULL + " or " + FilterOperator.NOT_NULL);
			if (propertyName == null)
				throw new IllegalArgumentException("propertName is null");
			_propertyName = propertyName;
			_operator = operator;
			_values= null;
		}
		public FilterPredicate(FilterOperator operator, FilterPredicate... clauses) {
			if (operator != FilterOperator.OR && operator != FilterOperator.AND )
				throw new IllegalArgumentException("Operator must be AND or OR");
			if (clauses == null || clauses.length <= 0)
				throw new IllegalArgumentException("must specifiy at least one clause:"+operator);
			_propertyName = null;
			_operator = operator;
			_clauses= new ArrayList<FilterPredicate>(clauses.length);
			for (int i= 0; i < clauses.length; i++)
				_clauses.add(clauses[i]);
			_values= null;
		}
		public FilterPredicate(FilterOperator operator, FilterPredicate clause) {
			if (operator != FilterOperator.OR && operator != FilterOperator.AND )
				throw new IllegalArgumentException("Operator must be AND or OR");
			if (clause == null)
				throw new IllegalArgumentException("clause is null");
			_propertyName = null;
			_operator = operator;
			_clauses= new ArrayList<FilterPredicate>(1);
			_clauses.add(clause);
			_values= null;
		}
		public String getPropertyName() {
			return _propertyName;
		}

		public FilterOperator getOperator() {
			return _operator;
		}

		public QuantifiedValues getQuantifiedValues() {
			return _values;
		}

		public List<FilterPredicate> getClauses() {
			return _clauses;
		}

		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			FilterPredicate that = (FilterPredicate) o;
			if (_operator != that._operator)
				return false;
			if (!_propertyName.equals(that._propertyName))
				return false;
			return _values.equals(that._values);
		}

		public int hashCode() {
			int result = _propertyName.hashCode();
			result = 31 * result + _operator.hashCode();
			result = 31 * result + _values.hashCode();
			return result;
		}
		
		
		@Override
		public String toString() {
			StringBuffer s= new StringBuffer();
			if (_clauses != null) {
				s.append('(');
				for (int i= 0; i < _clauses.size(); i++) {
					if (0 < i) {
						s.append(' ');
						s.append(_operator);
						s.append(' ');
					}
					s.append('(');
					s.append(_clauses.get(i));
					s.append(')');
				}
				s.append(')');
			}
			else {
				s.append(_propertyName);
				s.append(' ');
				s.append(_operator);
				if (_values != null) {
					s.append(' ');
					s.append(_values);
				}
			}
			return s.toString();
		}
	}
	
	public static final FilterPredicate and(FilterPredicate... clauses) {
		if (clauses == null || clauses.length < 1)
			throw new IllegalArgumentException("Must pass at least one clause");
		if (clauses.length < 2)
			return clauses[0];
		return new FilterPredicate(FilterOperator.AND, clauses);
	}
	
	public static final FilterPredicate or(FilterPredicate... clauses) {
		if (clauses == null || clauses.length < 1)
			throw new IllegalArgumentException("Must pass at least one clause");
		if (clauses.length < 2)
			return clauses[0];
		return new FilterPredicate(FilterOperator.OR, clauses);
	}

	public static final FilterPredicate id(Identifier value) {
		return new FilterPredicate(Item.KEY_ID, FilterOperator.EQUAL, new QuantifiedValues(value));
	}
	public static final <T extends Comparable<T>> FilterPredicate kind(T value) {
		return new FilterPredicate(Item.KEY_KIND, FilterOperator.EQUAL, new QuantifiedValues(value));
	}
	public static final <T extends Comparable<T>> FilterPredicate gt(String propertyName, T value) {
		return new FilterPredicate(propertyName, FilterOperator.GREATER_THAN, new QuantifiedValues(value));
	}
	public static final <T extends Comparable<T>> FilterPredicate ge(String propertyName, T value) {
		return new FilterPredicate(propertyName, FilterOperator.GREATER_THAN_OR_EQUAL, new QuantifiedValues(value));
	}
	public static final <T extends Comparable<T>> FilterPredicate le(String propertyName, T value) {
		return new FilterPredicate(propertyName, FilterOperator.LESS_THAN_OR_EQUAL, new QuantifiedValues(value));
	}
	public static final <T extends Comparable<T>> FilterPredicate lt(String propertyName, T value) {
		return new FilterPredicate(propertyName, FilterOperator.LESS_THAN, new QuantifiedValues(value));
	}
	public static final <T extends Comparable<T>> FilterPredicate eq(String propertyName, T value) {
		return new FilterPredicate(propertyName, FilterOperator.EQUAL, new QuantifiedValues(value));
	}
	public static final <T extends Comparable<T>> FilterPredicate ne(String propertyName, T value) {
		return new FilterPredicate(propertyName, FilterOperator.NOT_EQUAL, new QuantifiedValues(value));
	}
	public static final <T extends Comparable<T>> FilterPredicate eq(String propertyName, QuantifiedValues values) {
		return new FilterPredicate(propertyName, FilterOperator.EQUAL, values);
	}
	public static final <T extends Comparable<T>> FilterPredicate ne(String propertyName, QuantifiedValues values) {
		return new FilterPredicate(propertyName, FilterOperator.NOT_EQUAL, values);
	}
	public static final <T extends Comparable<T>> FilterPredicate any(String propertyName, T... values) {
		return new FilterPredicate(propertyName, FilterOperator.EQUAL, new QuantifiedValues(Quantifier.SOME, values));
	}
//	public static final <T extends Comparable<T>> FilterPredicate none(String propertyName, T... values) {
//		return new FilterPredicate(propertyName, FilterOperator.NOT_EQUAL, new QuantifiedValues(Quantifier.ALL, values));
//	}
	public static final <T extends Comparable<T>> FilterPredicate all(String propertyName, T... values) {
		return new FilterPredicate(propertyName, FilterOperator.EQUAL, new QuantifiedValues(Quantifier.ALL, values));
	}
	public static final <T extends Comparable<T>> FilterPredicate xor(String propertyName, T... values) {
		return new FilterPredicate(propertyName, FilterOperator.NOT_EQUAL, new QuantifiedValues(Quantifier.SOME, values));
	}
	public static final FilterPredicate isNull(String propertyName) {
		return new FilterPredicate(FilterOperator.IS_NULL, propertyName);
	}
	public static final <T extends Comparable<T>> QuantifiedValues all(T... values) {
		return new QuantifiedValues(Quantifier.ALL, values);
	}
	public static final <T extends Comparable<T>> QuantifiedValues any(T... values) {
		return new QuantifiedValues(Quantifier.SOME, values);
	}

	public static final class SortPredicate implements Serializable {
		private static final long serialVersionUID = 1L;

		private final String _propertyName;
		private final SortDirection _direction;

		public SortPredicate(String propertyName, SortDirection direction) {
			if (propertyName == null)
				throw new IllegalArgumentException("Property name was null");
			if (direction == null) 
				throw new IllegalArgumentException("Direction was null");

			_propertyName = propertyName;
			_direction = direction;
		}

		public String getPropertyName() {
			return _propertyName;
		}

		public SortDirection getDirection() {
			return _direction;
		}

		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			SortPredicate that = (SortPredicate) o;
			if (_direction != that._direction)
				return false;
			return _propertyName.equals(that._propertyName);
		}

		public int hashCode() {
			int result = _propertyName.hashCode();
			result = 31 * result + _direction.hashCode();
			return result;
		}
	}

	private final List<SortPredicate> _sortPredicates= new ArrayList<SortPredicate>();
	private FilterPredicate _filterPredicate= null;

	public <T extends Comparable<T>> ContrailQuery addFilter(String propertyName, FilterOperator operator, T value) {
		FilterPredicate clause= new FilterPredicate(propertyName, operator, new QuantifiedValues(value));
		if (_filterPredicate == null) {
			_filterPredicate= clause;
		}
		else if (_filterPredicate._operator == FilterOperator.AND) {
			_filterPredicate._clauses.add(clause);
		}
		else
			_filterPredicate= and(_filterPredicate, clause);
		return this;
	}

	public ContrailQuery where(FilterPredicate predicate) {
		_filterPredicate= predicate;
		return this;
	}

	public ContrailQuery orderBy(String propertyName, SortDirection direction) {
		_sortPredicates.add(new SortPredicate(propertyName, direction));
		return this;
	}

	public ContrailQuery orderBy(String propertyName) {
		_sortPredicates.add(new SortPredicate(propertyName, SortDirection.ASCENDING));
		return this;
	}

	public List<FilterPredicate> getFilterPredicates() {
		if (_filterPredicate == null)
			return Collections.emptyList();
		if (_filterPredicate._operator == FilterOperator.AND)
			return Collections.unmodifiableList(_filterPredicate._clauses);
		return Arrays.asList(_filterPredicate);
	}

	public ContrailQuery addSort(String propertyName) {
		return addSort(propertyName, SortDirection.ASCENDING);
	}

	public ContrailQuery addSort(String propertyName, SortDirection direction) {
		_sortPredicates.add(new SortPredicate(propertyName, direction));
		return this;
	}

	public List<SortPredicate> getSortPredicates() {
		return Collections.unmodifiableList(_sortPredicates);
	}
}
