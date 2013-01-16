package com.googlecode.contraildb.core.utils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.TreeMap;

import com.googlecode.contraildb.core.Identifier;


/**
 * Miscellaneouse data conversion utilites
 * 
 * @author ted stockwell 
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ConversionUtils {
    public static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("###,###,###.00");
    public static final DecimalFormat DEFAULT_NUMBER_FORMAT = new DecimalFormat("##########.000");
    public static final BigDecimal ZERO_BIG_DECIMAL = new BigDecimal("0").setScale(2);

    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_STRING = 1;
    public static final int TYPE_INTEGER = 2;
    public static final int TYPE_BYTE = 3;
    public static final int TYPE_BOOLEAN = 4;
    public static final int TYPE_LONG = 5;
    public static final int TYPE_CHARACTER = 6;
    public static final int TYPE_DOUBLE = 7;
    public static final int TYPE_FLOAT = 8;
    public static final int TYPE_SHORT = 9;
    public static final int TYPE_BIG_DECIMAL = 10;
    public static final int TYPE_BIG_INTEGER = 11;
    public static final int TYPE_DATE = 12;
    public static final int TYPE_TIME_ZONE = 13;

    /**
     * Returns a code that indicates the type of the given object. The type code
     * is one of the following: TYPE_STRING : An instance of java.lang.String
     * TYPE_INTEGER : An instance of java.lang.Integer TYPE_BYTE : An instance
     * of java.lang.Byte TYPE_BOOLEAN : An instance of java.lang.Boolean
     * TYPE_LONG : An instance of java.lang.Long TYPE_CHARACTER : An instance of
     * java.lang.Character TYPE_DOUBLE : An instance of java.lang.Double
     * TYPE_FLOAT : An instance of java.lang.Float TYPE_SHORT : An instance of
     * java.lang.Short TYPE_BIG_DECIMAL : An instance of java.math.BigDecimal
     * TYPE_BIG_INTEGER : An instance of java.math.BigInteger TYPE_DATE : An
     * instance of java.lang.Date TYPE_TIME_ZONE : An instance of
     * java.lang.TimeZone
     * 
     * TYPE_UNKNOWN : Anything that's not one of the above
     */
    final static public int getTypeCode(Object obj) {
        if (obj instanceof String)
            return TYPE_STRING;
        if (obj instanceof Number) {
            if (obj instanceof Integer)
                return TYPE_INTEGER;
            if (obj instanceof Byte)
                return TYPE_BYTE;
            if (obj instanceof Long)
                return TYPE_LONG;
            if (obj instanceof Double)
                return TYPE_DOUBLE;
            if (obj instanceof Float)
                return TYPE_FLOAT;
            if (obj instanceof Short)
                return TYPE_SHORT;
            return TYPE_UNKNOWN;
        }
        if (obj instanceof Boolean)
            return TYPE_BOOLEAN;
        if (obj instanceof Character)
            return TYPE_CHARACTER;
        if (obj instanceof java.math.BigDecimal)
            return TYPE_BIG_DECIMAL;
        if (obj instanceof java.math.BigInteger)
            return TYPE_BIG_INTEGER;
        if (obj instanceof java.util.Date)
            return TYPE_DATE;
        if (obj instanceof java.util.TimeZone)
            return TYPE_TIME_ZONE;
        return TYPE_UNKNOWN;
    }

    final static public int getTypeCode(Class<?> obj) {
        if (String.class.isAssignableFrom(obj))
            return TYPE_STRING;
        if (Number.class.isAssignableFrom(obj)) {
            if (Integer.class.isAssignableFrom(obj))
                return TYPE_INTEGER;
            if (Byte.class.isAssignableFrom(obj))
                return TYPE_BYTE;
            if (Long.class.isAssignableFrom(obj))
                return TYPE_LONG;
            if (Double.class.isAssignableFrom(obj))
                return TYPE_DOUBLE;
            if (Float.class.isAssignableFrom(obj))
                return TYPE_FLOAT;
            if (Short.class.isAssignableFrom(obj))
                return TYPE_SHORT;
            return TYPE_UNKNOWN;
        }
        if (Boolean.class.isAssignableFrom(obj))
            return TYPE_BOOLEAN;
        if (Character.class.isAssignableFrom(obj))
            return TYPE_CHARACTER;
        if (java.math.BigDecimal.class.isAssignableFrom(obj))
            return TYPE_BIG_DECIMAL;
        if (java.math.BigInteger.class.isAssignableFrom(obj))
            return TYPE_BIG_INTEGER;
        if (java.util.Date.class.isAssignableFrom(obj))
            return TYPE_DATE;
        if (java.util.TimeZone.class.isAssignableFrom(obj))
            return TYPE_TIME_ZONE;
        return TYPE_UNKNOWN;
    }

    /**
     * Convenience method that returns a value as a date Returns null if the
     * given value cannot be converted to a Date.
     */
    static public Date toDate(Object o) {
        if (o == null)
            return null;
        if (o instanceof Date)
            return (Date) o;
        if (o instanceof java.sql.Timestamp)
            return new Date(((java.sql.Timestamp) o).getTime());
        if (o instanceof java.sql.Date)
            return new Date(((java.sql.Date) o).getTime());
        if (o instanceof java.lang.Long)
            return new Date(((java.lang.Long) o).longValue());
        Date result = null;

        // try converting it to a String and parsing the String
        String value = o.toString();
        try {
            return JAVA_DATE_FORMATTER.parse(value);
        } catch (Throwable t) {
        }
        try {
            return DATETIME_FORMAT.parse(value);
        } catch (Throwable t) {
        }
        try {
            return DATEFORMAT_DEFAULT_FORMAT.parse(value);
        } catch (Throwable t) {
        }
        try {
            return DATE_FORMAT.parse(value);
        } catch (Throwable t) {
        }
        try {
            return SHORT_DATE_FORMAT.parse(value);
        } catch (Throwable t) {
        }
        try {
            return SIMPLE_DATE_FORMAT.parse(value);
        } catch (Throwable t) {
        }
        try {
            return MYSQL_DATE_FORMAT.parse(value);
        } catch (Throwable t) {
        }
        try {
            return SIX_DATE_FORMAT.parse(value);
        } catch (Throwable t) {
        }

        long longValue = toSafeLong(o, -1);
        if (0 <= longValue)
            return new Date(longValue);

        // check to see if the object has a toDate() method...
        try {
            Method toDateMethod = o.getClass().getMethod("toDate", new Class[0]);
            return (Date) toDateMethod.invoke(o, new Object[0]);
        } catch (Throwable t) {
        }

        return result;
    }

    public static final DateFormat JAVA_DATE_FORMATTER = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US); // the
    public static final DateFormat DATETIME_FORMAT = DateFormat.getDateTimeInstance();
    public static final DateFormat DATEFORMAT_DEFAULT_FORMAT = DateFormat.getInstance();
    public static final DateFormat DATE_FORMAT = DateFormat.getDateInstance();
    public static final DateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy");
    public static final DateFormat MYSQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final DateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("MM/dd/yy");
    public static final DateFormat SIX_DATE_FORMAT = new SimpleDateFormat("MMddyy");

    // used
    // by
    // the
    // java.lang.Date.toString
    // method

    /**
     * Convenience method that returns a value as a time zone Returns null if
     * the given value cannot be converted to a TimeZone.
     */
    static public TimeZone toTimeZone(Object o) {
        if (o == null)
            return null;
        if (o instanceof TimeZone)
            return (TimeZone) o;
        TimeZone result = null;

        if (o instanceof Integer) {
            int rawOffset = ((Integer) o).intValue();
            String ids[] = TimeZone.getAvailableIDs(rawOffset);
            String id = (ids != null && 0 < ids.length) ? ids[0] : "";
            o = new SimpleTimeZone(rawOffset, id);
        }

        return result;
    }

    /**
     * Convenience method that returns a value as a date Returns null if the
     * given value cannot be converted to a Date.
     */
    static public java.sql.Timestamp toTimestamp(Object o) {
        if (o == null)
            return null;
        if (o instanceof java.sql.Timestamp)
            return (java.sql.Timestamp) o;
        Long l = toLong(o);
        if (l == null)
            return null;
        return new java.sql.Timestamp(l.longValue());
    }

    /**
     * Convenience method that returns a value as an Integer
     */
    static public Integer toInteger(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof Number)
            return new Integer(((Number) obj).intValue());
        if (obj instanceof Boolean)
            return (obj.equals(Boolean.FALSE)) ? new Integer(0) : new Integer(-1);

        try {
            String s = obj.toString();
            s = StringUtils.replaceAll(s, ",", "");
            return Integer.valueOf(s);
        } catch (Throwable t) {
        }
        return null;
    }

    /**
     * Convenience method that returns a value as an int
     */
    static public int toSafeInt(Object obj, int defaultValue) {
        if (obj instanceof Number)
            return ((Number) obj).intValue();
        try {
            String s = obj.toString();
            s = StringUtils.replaceAll(s, ",", "");
            return Integer.parseInt(s);
        } catch (Throwable t) {
        }
        return defaultValue;
    }

    /**
     * Convenience method that returns a value as a char
     */
    static public char toSafeChar(Object obj, char defaultValue) {
        if (obj instanceof Character)
            return ((Character) obj).charValue();
        if (obj instanceof Number) {
            int i = ((Number) obj).intValue();
            return (char) i;
        }
        String s = obj.toString();
        if (0 < s.length())
            return s.charAt(0);
        return defaultValue;
    }

    static public Character toCharacter(Object obj) {
        if (obj instanceof Character)
            return (Character) obj;
        if (obj instanceof Number) {
            int i = ((Number) obj).intValue();
            return new Character((char) i);
        }
        String s = obj.toString();
        if (0 < s.length())
            return new Character(s.charAt(0));
        return null;
    }

    /**
     * Convenience method that returns a value as a Double
     */
    static public double toSafeDouble(Object obj, double defaultValue) {
        if (obj == null)
            return defaultValue;
        if (obj instanceof Number)
            return ((Number) obj).doubleValue();
        try {
            String lText = obj.toString();
            lText = StringUtils.replaceAll(lText, ",", "");

            return Double.parseDouble(lText);
        } catch (Throwable t) {
        }
        return defaultValue;
    }

    static public Double toDouble(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof Number)
            return new Double(((Number) obj).doubleValue());
        if (obj instanceof Boolean)
            return (obj.equals(Boolean.FALSE)) ? new Double(0.0) : new Double(-1.0);

        try {
            String lText = obj.toString();

            // strip out any commas
            lText = StringUtils.replaceAll(lText, ",", "");

            return Double.valueOf(lText);
        } catch (Throwable t) {
        }
        return null;
    }

    static public Short toShort(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof Number)
            return new Short(((Number) obj).shortValue());
        if (obj instanceof Boolean)
            return (obj.equals(Boolean.FALSE)) ? new Short((short) 0) : new Short((short) -1);

        try {
            return Short.valueOf(obj.toString());
        } catch (Throwable t) {
        }
        return null;
    }

    static public Float toFloat(Object obj) {
        Double d = toDouble(obj);
        if (d == null)
            return null;
        return new Float(d.doubleValue());
    }

    static public java.math.BigDecimal toBigDecimal(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof java.math.BigDecimal)
            return (java.math.BigDecimal) obj;
        if (obj instanceof java.math.BigInteger)
            return new java.math.BigDecimal((java.math.BigInteger) obj);
        if (obj instanceof Number)
            return new java.math.BigDecimal(((Number) obj).toString());
        if (obj instanceof Boolean)
            return (obj.equals(Boolean.FALSE)) ? ZERO_BIG_DECIMAL : new BigDecimal("-1.00");

        try {
            String s = obj.toString();
            s = StringUtils.replaceAll(s, ",", "");
            return new java.math.BigDecimal(s);
        } catch (Throwable t) {
        }
        return null;
    }

    /**
     * Same as toBigDecimal(obj) but if the given value cannot be converted this
     * method will return defaultValue instead of null.
     */
    static public BigDecimal toSafeBigDecimal(Object obj, BigDecimal defaultValue) {
        BigDecimal value;
        return ((value = toBigDecimal(obj)) == null) ? defaultValue : value;
    }

    /**
     * Same as toSafeBigDecimal(obj, ZERO_BIG_DECIMAL).
     */
    static public final BigDecimal toSafeBigDecimal(Object obj) {
        return toSafeBigDecimal(obj, ZERO_BIG_DECIMAL);
    }

    static public java.math.BigInteger toBigInteger(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof java.math.BigInteger)
            return (java.math.BigInteger) obj;
        if (obj instanceof Boolean)
            return (obj.equals(Boolean.FALSE)) ? new java.math.BigInteger(new byte[] { 0 }) : new java.math.BigInteger(
                    new byte[] { -1 });

        try {
            String s = obj.toString();
            s = StringUtils.replaceAll(s, ",", "");
            return new java.math.BigInteger(s);
        } catch (Throwable t) {
        }
        return null;
    }

    /**
     * Convenience method that returns a value as a long
     */
    static public long toSafeLong(Object obj, long defaultValue) {
        Long l = toLong(obj);
        if (l == null)
            return defaultValue;
        return l.longValue();
    }

    static public Long toLong(Object obj) {
        if (obj instanceof Long)
            return (Long) obj;
        if (obj instanceof Number)
            return new Long(((Number) obj).longValue());
        if (obj instanceof java.util.Date)
            return new Long(((java.util.Date) obj).getTime());
        if (obj instanceof java.sql.Timestamp)
            return new Long(((java.sql.Timestamp) obj).getTime());
        try {
            String s = obj.toString();
            s = StringUtils.replaceAll(s, ",", "");
            return new Long(Long.parseLong(s));
        } catch (Throwable t) {
        }
        return null;
    }

    /**
     * Convenience method that returns a value as a byte
     */
    static public byte toSafeByte(Object obj, byte defaultValue) {
        if (obj instanceof Number)
            return ((Number) obj).byteValue();
        try {
            return Byte.parseByte(obj.toString());
        } catch (Throwable t) {
        }
        return defaultValue;
    }

    static public Byte toByte(Object obj) {
        if (obj instanceof Number)
            return new Byte(((Number) obj).byteValue());
        try {
            return new Byte(obj.toString());
        } catch (Throwable t) {
        }
        return null;
    }

    /**
     * Convenience method that returns a value as a boolean
     */
    static public boolean toSafeBoolean(Object obj, boolean defaultValue) {
        if (obj == null)
            return defaultValue;
        if (obj instanceof Boolean) {
            return ((Boolean) obj).booleanValue();
        } else if (obj instanceof Number) {
            return (((Number) obj).intValue() == 0) ? false : true;
        } else if (obj instanceof String) {
            String s = (String) obj;
            if (s.equalsIgnoreCase("true")) {
                return true;
            } else if (s.equalsIgnoreCase("false")) {
                return false;
            } else {
                try {
                    return Integer.parseInt((String) obj) != 0;
                } catch (Throwable t) {
                    return false;
                }
            }
        }
        return defaultValue;
    }

    /**
     * Convenience method that returns a value as a boolean
     */
    static public Boolean toBoolean(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        } else if (obj instanceof Number) {
            return (((Number) obj).intValue() == 0) ? Boolean.FALSE : Boolean.TRUE;
        } else if (obj instanceof String) {
            String s = ((String) obj).toLowerCase();
            if (s.startsWith("t")) {
                return Boolean.TRUE;
            } else if (s.startsWith("f")) {
                return Boolean.FALSE;
            } else if (s.startsWith("y")) {
                return Boolean.TRUE;
            } else if (s.startsWith("n")) {
                return Boolean.FALSE;
            } else {
                try {
                    return new Boolean(Integer.parseInt((String) obj) != 0);
                } catch (Throwable t) {
                    return Boolean.FALSE;
                }
            }
        }
        return null;
    }

    static public final Boolean toSafeBoolean(Object value, Boolean defaultValue) {
        Boolean v;
        return ((v = toBoolean(value)) == null) ? defaultValue : v;
    }

    static public final String toString(Object obj) {
        if (obj == null)
            return null;
        return obj.toString();
    }

	static public final <T> Collection<T> toCollection(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof Collection)
            return (Collection) obj;
        if (obj instanceof Object[])
            return Arrays.asList((T[]) obj);
        if (obj instanceof Enumeration) {
            Collection value = new ArrayList();
            Enumeration e = (Enumeration) obj;
            for (; e.hasMoreElements();)
                value.add(e.nextElement());
            return value;
        }
        if (obj instanceof Iterator) {
            Collection value = new ArrayList();
            Iterator e = (Iterator) obj;
            for (; e.hasNext();)
                value.add(e.next());
            return value;
        }
        Collection value = new ArrayList(1);
        value.add(obj);
        return value;
    }

    public static String toSafeString(String string) {
        return string != null ? string : "";
    }

	public static Identifier toPath(Object object) {
		if (object == null)
			return null;
		if (object instanceof Identifier)
			return (Identifier)object;
		if (object instanceof String)
			return Identifier.create((String)object);
		return null;
	}
	
	public static <T> List<T> toList(Collection<T> iterable) {
		if (iterable instanceof List)
			return (List<T>)iterable;
		ArrayList<T> list= new ArrayList<T>(iterable.size());
		for (T t: iterable)
			list.add(t);
		return list;
	}
	
	public static <T> List<T> toList(Iterable<T> iterable) {
		if (iterable instanceof List)
			return (List<T>)iterable;
		ArrayList<T> list= null;
		if (iterable instanceof Collection) {
			list= new ArrayList<T>(((Collection)iterable).size());
		}
		else
			list= new ArrayList<T>();
		for (T t: iterable)
			list.add(t);
		return list;
	}

	public static <T> List<T> asList(T... items) {
		ArrayList<T> list= new ArrayList<T>(items.length);
		for (T item:items)
			list.add(item);
		return list;
	}

	public static <K,V> Map<K, V> asMap(K key, V value) {
		TreeMap<K, V> map= new TreeMap<K, V>();
		map.put(key, value);
		return map;
	}
}
