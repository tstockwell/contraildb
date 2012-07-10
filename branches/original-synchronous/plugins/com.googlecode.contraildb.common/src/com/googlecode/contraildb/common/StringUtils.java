package com.googlecode.contraildb.common;

import java.util.StringTokenizer;

/**
 * Miscellaneouse String utilities
 * 
 * Some of these utilities use the "wildmat" pattern. In a wildmat pattern a ?
 * in the pattern matches any single character and a * matches any string of one
 * or more characters.
 * 
 * @author ted stockwell
 */
public class StringUtils {
    /**
     * Replaces all instances of oldSubstring in source with newSubstring and
     * returns the result.
     */
    static public String replaceAll(String source, String oldSubstring, String newSubstring) {
        int oldLength = oldSubstring.length();
        int newLength = newSubstring.length();
        int lastIndex = 0;
        for (;;) {
            if ((lastIndex = source.indexOf(oldSubstring, lastIndex)) < 0) {
                break;
            }
            source = source.substring(0, lastIndex) + newSubstring + source.substring(lastIndex + oldLength);
            lastIndex += newLength;
        }
        return source;
    }

    /**
     * What character marks an inverted character class?
     */
    protected static char NEGATE_CLASS = '^';
    protected static int TRUE = 1;
    protected static int FALSE = 0;
    protected static int ABORT = -1;
    protected static int BEGIN = -2;
    protected static int IGNORE = -1;

    private static class MatchResult {
        protected static final MatchResult FALSE = new MatchResult(StringUtils.FALSE, -1, -1, -1);

        protected MatchResult(int _rc, int l, int e, int c) {
            rc = _rc;
            lastMatched = l;
            exactlyMatched = e;
            lastConsecutiveMatched = c;
        }

        /**
         * Return code: TRUE, FALSE, ABORT, or BEGIN.
         */
        public int rc = StringUtils.FALSE;

        public int lastMatched; // 1-based
        public int exactlyMatched;
        public int lastConsecutiveMatched;
    }

    public static class SearchResults {
        public SearchResults(boolean f, int l, int e, int c) {
            found = f;
            lastMatched = l;
            exactlyMatched = e;
            lastConsecutiveMatched = c;
        }
        /**
         * true if an instance of the pattern was found
         */
        public boolean found;

        /**
         * The index of the last character in the search string that matched the
         * pattern + 1 So, for a target string of /kjh/kllkjh/x.jsp and a
         * pattern equal to *.jsp lastMatched == 17.
         */
        public int lastMatched;

        /**
         * The # of characters in the search string that explicitly matched the
         * pattern So, for a target string of /kjh/kllkjh/x.jsp and a pattern
         * equal to *.jsp exactlyMatched == 4.
         */
        public int exactlyMatched;

        /**
         * The index of the last consecutive character in the search string,
         * from the beginning of the search string, that explicitly matched the
         * pattern + 1. So, for a target string of /kjh/kllkjh/x.jsp and a
         * pattern equal to /kjh/*.jsp lastConsecutiveExactMatched == 5.
         */
        public int lastConsecutiveMatched;
    }

    /**
     * Match text and p, return TRUE, FALSE, ABORT, or BEGIN.
     */
    protected static MatchResult DoMatch(String text, String pattern, int ignoreCase) {
        int iPattern = 0, iText = 0;
        int lenPattern = pattern.length();
        int lenText = text.length();

        /*
         * not the most efficient way to handle case but the easiest to
         * implement...
         */
        if (ignoreCase == TRUE) {
            text = text.toUpperCase();
            pattern = pattern.toUpperCase();
        }

        for (; iPattern < lenPattern; iText++, iPattern++) {
            /*
             * if at the end of the text string then return TRUE if the rest of
             * the pattern is nothing but wildcard characters otherwise return
             * FALSE;
             */
            if (lenText <= iText) {
                for (; iPattern < lenPattern; iPattern++) {
                    if (pattern.charAt(iPattern) != '*') {
                        return MatchResult.FALSE;
                    }
                }
                return new MatchResult(TRUE, lenText, lenText, lenText);
            }

            char cPattern = pattern.charAt(iPattern);
            switch (cPattern) {
            case '\\':
                /* Literal match with following character. */
                if (++iPattern < lenPattern) {
                    cPattern = pattern.charAt(iPattern);
                }
            /* FALLTHROUGH */
            default:
                if (text.charAt(iText) != cPattern) {
                    return MatchResult.FALSE;
                }
                continue;
            case '?':
                /* Match anything. */
                continue;
            case '*':
                /* Consecutive stars act just like one. */
                for (; iPattern + 1 < lenPattern && pattern.charAt(iPattern + 1) == '*';)
                    iPattern++;
                /* Trailing star matches everything. */
                if (lenPattern <= iPattern + 1) {
                    return new MatchResult(TRUE, lenText, iText, iText);
                }
                String p = pattern.substring(iPattern + 1);
                MatchResult rc = MatchResult.FALSE;
                int iTextSave = iText;
                for (; iText < lenText; iText++) {
                    rc = DoMatch(text.substring(iText), p, IGNORE);
                    if (rc.rc == TRUE) {
                        break;
                    }
                }
                if (rc.rc != FALSE) {
                    rc.lastMatched += iText;
                    rc.exactlyMatched += iTextSave;
                    rc.lastConsecutiveMatched = iTextSave;
                }
                return rc;
            case '[': {
                char cText = text.charAt(iText);
                cPattern = pattern.charAt(++iPattern);

                int reverse = FALSE;
                if (cPattern == NEGATE_CLASS) {
                    reverse = TRUE;
                    cPattern = pattern.charAt(++iPattern);
                }
                int matched = FALSE;
                if (cPattern == ']' || cPattern == '-') {
                    if (cPattern == cText) {
                        matched = TRUE;
                    }
                    cPattern = pattern.charAt(++iPattern);
                }

                char last = cPattern;
                for (;;) {
                    if (cPattern == '-') {
                        if ((cPattern = pattern.charAt(++iPattern)) == ']') {
                            if (cText == '-') {
                                matched = TRUE;
                            }
                            break;
                        } else if (cText <= cPattern && cText >= last) {
                            matched = TRUE;
                        }
                    } else if (cText == ']') {
                        break;
                    } else if (cText == cPattern) {
                        matched = TRUE;
                    }
                    last = cPattern;
                    cPattern = pattern.charAt(++iPattern);
                }
                if (matched == reverse) {
                    return MatchResult.FALSE;
                }
            }
                continue;
            }
        }

        /*
         * if at the end of the text string then return TRUE if the rest of the
         * pattern is nothing but wildcard characters otherwise return FALSE;
         */
        if (lenText <= iText) {
            for (; iPattern < lenPattern; iPattern++) {
                if (pattern.charAt(iPattern) != '*') {
                    return MatchResult.FALSE;
                }
            }
            return new MatchResult(TRUE, lenText, iText, iText);
        }

        /* the beginning of the string was matched */
        return new MatchResult(BEGIN, iText, iText, iText);
    }

    /**
     * Returns true if the given text String matches the given "wildmat"
     * pattern. The compare is case-sensitive,
     * 
     * @param text
     *            string to search
     * @param pattern
     *            pattern to compare
     */
    public static boolean wildmat(String text, String pattern) {
        if (pattern.equals("*")) {
            return true;
        }
        return DoMatch(text, pattern, FALSE).rc == TRUE;
    }

    /**
     * Returns true if the given text String matches the given "wildmat"
     * pattern.
     * 
     * @param text
     *            string to search
     * @param pattern
     *            pattern to compare
     * @param ignoreCase
     *            If true then the compare is case-sensitive otherwise the
     *            compare is case-insensitive.
     */
    public static boolean wildmat(String text, String pattern, boolean ignoreCase) {
        if (pattern.equals("*")) {
            return true;
        }
        return DoMatch(text, pattern, ignoreCase ? TRUE : FALSE).rc == TRUE;
    }

    /**
     * Returns true if the given text String matches the given "wildmat"
     * pattern. The compare is case-insensitive,
     * 
     * @param text
     *            string to search
     * @param pattern
     *            wildmat pattern to compare
     */
    public static boolean wildmatIgnoreCase(String text, String pattern) {
        if (pattern.equals("*")) {
            return true;
        }
        return DoMatch(text, pattern, TRUE).rc == TRUE;
    }

    /**
     * Returns true if the given text String starts with the given pattern. The
     * search is case-sensitive,
     * 
     * @param text
     *            string to search
     * @param pattern
     *            pattern to look for
     */
    public static boolean startsWithWildPattern(String text, String pattern) {
        if (pattern.equals("*")) {
            return true;
        }
        return DoMatch(text, pattern, FALSE).rc != FALSE;
    }

    /**
     * Returns true if the given text String starts with the given pattern.
     * 
     * @param text
     *            string to search
     * @param pattern
     *            pattern to look for
     * @param ignoreCase
     *            If true then the search is case-sensitive otherwise the search
     *            is case-insensitive.
     */
    public static boolean startsWithWildPattern(String text, String pattern, boolean ignoreCase) {
        if (pattern.equals("*")) {
            return true;
        }
        return DoMatch(text, pattern, ignoreCase ? TRUE : FALSE).rc != FALSE;
    }

    /**
     * Returns true if the given text String starts with the given pattern.
     * 
     * @param text
     *            string to search
     * @param pattern
     *            pattern to look for
     * @param ignoreCase
     *            If true then the search is case-sensitive otherwise the search
     *            is case-insensitive.
     */
    public static SearchResults wildPatternStartResults(String text, String pattern, boolean ignoreCase) {
        if (pattern.equals("*")) {
            return new SearchResults(true, text.length(), 0, 0);
        }
        MatchResult rc = DoMatch(text, pattern, ignoreCase ? TRUE : FALSE);
        if (rc.rc == FALSE) {
            return new SearchResults(false, -1, -1, 0);
        }
        return new SearchResults(true, rc.lastMatched, rc.exactlyMatched, rc.lastConsecutiveMatched);
    }

    /**
     * Searches the given text for long words and breaks the long words into
     * chunks by inserting a delimiter.
     * 
     * @param text
     *            text that may contain large words.
     * @param delimiters
     *            a list of delimiters that separate words in the given text.
     * @param separator
     *            the characters to insert into large words to break them up.
     * @param maxWordSize
     *            the largest allowable word size. Words larger than this size
     *            are broken up.
     * 
     * @return the given text with the large words broken up.
     */
    public static String breakupLargeWords(String text, String delimiters, String separator, int maxWordSize) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        String result = "";
        StringTokenizer tokenizer = new StringTokenizer(text, delimiters);
        for (; tokenizer.hasMoreTokens();) {
            String nextToken = tokenizer.nextToken();
            String token = nextToken;

            int tokenStart = text.indexOf(token);
            result += text.substring(0, tokenStart); // add delimiters preceding the word

            while (maxWordSize < token.length()) {
                result += token.substring(0, maxWordSize) + separator;
                token = token.substring(maxWordSize);
            }
            result += token;
            text = text.substring(tokenStart + nextToken.length());
        }
        result += text; // add any delimiters at the end of the text
        return result;
    }


    /**
     * prepends the given padding string to the given value until the value's
     * length is at least the given total length.
     */
    static public String prepad(String value, String pad, int totalLength) {
        if (value == null) {
            value = "";
        }
        while (value.length() < totalLength) {
            value = pad + value;
        }
        return value;
    }

    /**
     * appends the given padding string to the given value until the value's
     * length is at least the given total length.
     */
    static public String pad(String value, String pad, int totalLength) {
        if (value == null) {
            value = "";
        }
        while (value.length() < totalLength) {
            value += pad;
        }
        return value;
    }

    /**
     * returns the end portion of a string.
     * 
     * @param length
     *            maximum number of characters to include from the end of the
     *            given string.
     */
    static public String tail(String value, int length) {
        if (value.length() <= length) {
            return value;
        }
        return value.substring(value.length() - length);
    }

    /**
     * returns the beginning portion of a string.
     * 
     * @param length
     *            maximum number of characters to include
     */
    static public String head(String value, int length) {
        if (value.length() <= length) {
            return value;
        }
        return value.substring(0, length);
    }

    /**
     * Finds the position of the last work break that occurs before a given end
     * position.
     * 
     * @return the index of the beginning of a word
     */
    static public int findLastWordBreak(String text, String delimiters, int endPosition) {
        if (text.length() <= 0) {
            return -1;
        }
        if (delimiters.length() <= 0) {
            return -1;
        }
        for (int i = Math.min(endPosition, text.length()); 0 < i--;) {
            if (0 <= delimiters.indexOf(text.charAt(i))) {
                return i + 1;
            }
        }
        return -1;
    }

    static public boolean isNotBlank(String value) {
        return value != null && value.trim().length() != 0;
    }

    static public boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
