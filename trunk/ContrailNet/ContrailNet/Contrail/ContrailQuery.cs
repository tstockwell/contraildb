using System;

namespace Contrail {

public enum FilterOperator {
	LESS_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL, EQUAL, NOT_EQUAL, IS_NULL, NOT_NULL, AND, OR
}

public enum SortDirection {
	ASCENDING, DESCENDING
}

public enum Quantifier {
	SOME, ALL
}

[Serializable]
public class ContrailQuery {

	[Serializable]
	public static class QuantifiedValues {

		private readonly Quantifier _type;
		private readonly IComparable[] _values;
		public QuantifiedValues(IComparable value) {
			_type= Quantifier.ALL;
			_values= new IComparable[] { value };
		}
		public QuantifiedValues(Quantifier type, Comparable[] values) {
			_type= type;
			_values= values;
		}
		public Quantifier getType() {
			return _type;
		}
		public Comparable[] getValues() {
			return _values;
		}
		
		public String ToString() {
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
	
	[Serializable]
	public static class FilterPredicate {

		private String _propertyName;
		private FilterOperator _operator;
		private List<FilterPredicate> _clauses;
		private QuantifiedValues _values;

		public FilterPredicate(String propertyName, FilterOperator op, QuantifiedValues values) {
			if (propertyName == null)
				throw new IllegalArgumentException("propertName is null");
			if (values == null)
				throw new IllegalArgumentException("values is null");
			_propertyName = propertyName;
			_operator = op;
			_values= values;
		}
		public FilterPredicate(FilterOperator op, String propertyName, QuantifiedValues values) {
			if (op != FilterOperator.EQUAL && op != FilterOperator.NOT_EQUAL )
				throw new IllegalArgumentException("Operator must be "+FilterOperator.EQUAL + " or " + FilterOperator.NOT_EQUAL);
			if (propertyName == null)
				throw new IllegalArgumentException("propertName is null");
			if (values == null)
				throw new IllegalArgumentException("values are null");
			_propertyName = propertyName;
			_operator = op;
			_values= values;
		}
		public FilterPredicate(FilterOperator op, String propertyName) {
			if (op != FilterOperator.IS_NULL && op != FilterOperator.NOT_NULL )
				throw new IllegalArgumentException("Operator must be "+FilterOperator.IS_NULL + " or " + FilterOperator.NOT_NULL);
			if (propertyName == null)
				throw new IllegalArgumentException("propertName is null");
			_propertyName = propertyName;
			_operator = op;
			_values= null;
		}
		public FilterPredicate(FilterOperator op, params FilterPredicate[] clauses) {
			if (op != FilterOperator.OR && op != FilterOperator.AND )
				throw new IllegalArgumentException("Operator must be AND or OR");
			if (clauses == null || clauses.length <= 0)
				throw new IllegalArgumentException("must specifiy at least one clause:"+op);
			_propertyName = null;
			_operator = op;
			_clauses= new ArrayList<FilterPredicate>(clauses.length);
			for (int i= 0; i < clauses.length; i++)
				_clauses.add(clauses[i]);
			_values= null;
		}
		public FilterPredicate(FilterOperator op, FilterPredicate clause) {
			if (op != FilterOperator.OR && op != FilterOperator.AND )
				throw new IllegalArgumentException("Operator must be AND or OR");
			if (clause == null)
				throw new IllegalArgumentException("clause is null");
			_propertyName = null;
			_operator = op;
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
		
		
		override public String ToString() {
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
	
	public static sealed FilterPredicate and(params FilterPredicate[] clauses) {
		if (clauses == null || clauses.length < 1)
			throw new IllegalArgumentException("Must pass at least one clause");
		if (clauses.length < 2)
			return clauses[0];
		return new FilterPredicate(FilterOperator.AND, clauses);
	}
	
	public static sealed FilterPredicate or(params FilterPredicate[] clauses) {
		if (clauses == null || clauses.length < 1)
			throw new IllegalArgumentException("Must pass at least one clause");
		if (clauses.length < 2)
			return clauses[0];
		return new FilterPredicate(FilterOperator.OR, clauses);
	}

	public static sealed FilterPredicate id(Identifier value) {
		return new FilterPredicate(Item.KEY_ID, FilterOperator.EQUAL, new QuantifiedValues(value));
	}
	public static sealed FilterPredicate kind<T>(T value) where T:IComparable {
		return new FilterPredicate(Item.KEY_KIND, FilterOperator.EQUAL, new QuantifiedValues(value));
	}
	public static sealed FilterPredicate gt<T>(String propertyName, T value) where T:IComparable {
		return new FilterPredicate(propertyName, FilterOperator.GREATER_THAN, new QuantifiedValues(value));
	}
	public static sealed FilterPredicate ge<T>(String propertyName, T value) where T:IComparable {
		return new FilterPredicate(propertyName, FilterOperator.GREATER_THAN_OR_EQUAL, new QuantifiedValues(value));
	}
	public static sealed FilterPredicate le<T>(String propertyName, T value) where T:IComparable {
		return new FilterPredicate(propertyName, FilterOperator.LESS_THAN_OR_EQUAL, new QuantifiedValues(value));
	}
	public static sealed FilterPredicate lt<T>(String propertyName, T value) where T:IComparable {
		return new FilterPredicate(propertyName, FilterOperator.LESS_THAN, new QuantifiedValues(value));
	}
	public static sealed FilterPredicate eq<T>(String propertyName, T value) where T:IComparable {
		return new FilterPredicate(propertyName, FilterOperator.EQUAL, new QuantifiedValues(value));
	}
	public static sealed FilterPredicate ne<T>(String propertyName, T value) where T:IComparable {
		return new FilterPredicate(propertyName, FilterOperator.NOT_EQUAL, new QuantifiedValues(value));
	}
	public static sealed FilterPredicate eq<T>(String propertyName, QuantifiedValues values) where T:IComparable {
		return new FilterPredicate(propertyName, FilterOperator.EQUAL, values);
	}
	public static sealed FilterPredicate ne<T>(String propertyName, QuantifiedValues values) where T:IComparable {
		return new FilterPredicate(propertyName, FilterOperator.NOT_EQUAL, values);
	}
	public static sealed FilterPredicate any<T>(String propertyName, params T[] values) where T:IComparable {
		return new FilterPredicate(propertyName, FilterOperator.EQUAL, new QuantifiedValues(Quantifier.SOME, values));
	}
//	public static final <T extends Comparable<T>> FilterPredicate none(String propertyName, T... values) {
//		return new FilterPredicate(propertyName, FilterOperator.NOT_EQUAL, new QuantifiedValues(Quantifier.ALL, values));
//	}
	public static sealed FilterPredicate all<T>(String propertyName, params T[] values) where T:IComparable {
		return new FilterPredicate(propertyName, FilterOperator.EQUAL, new QuantifiedValues(Quantifier.ALL, values));
	}
	public static sealed FilterPredicate xor<T>(String propertyName, params T[] values) where T:IComparable {
		return new FilterPredicate(propertyName, FilterOperator.NOT_EQUAL, new QuantifiedValues(Quantifier.SOME, values));
	}
	public static sealed FilterPredicate isNull(String propertyName) {
		return new FilterPredicate(FilterOperator.IS_NULL, propertyName);
	}
	public static sealed QuantifiedValues all<T>(params T[] values) where T:IComparable {
		return new QuantifiedValues(Quantifier.ALL, values);
	}
	public static sealed QuantifiedValues any<T>(params T[] values) where T:IComparable {
		return new QuantifiedValues(Quantifier.SOME, values);
	}

	[Serializable]
	public static class SortPredicate {

		private readonly String _propertyName;
		private readonly SortDirection _direction;

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

	private readonly List<SortPredicate> _sortPredicates= new ArrayList<SortPredicate>();
	private FilterPredicate _filterPredicate= null;

	public ContrailQuery addFilter<T>(String propertyName, FilterOperator op, T value) where T:IComparable {
		FilterPredicate clause= new FilterPredicate(propertyName, op, new QuantifiedValues(value));
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
}