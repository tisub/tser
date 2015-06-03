package com.anotherservice.util;

/**
 * This class is a helper for building UTF-8 compliant RegExp patterns.
 *
 * @author  Simon Uyttendaele
 */
public class PatternBuilder
{
	private PatternBuilder() { }
	/**
	 * 0-9
	 */
	public static final int NUMBER = 1;
	public static final String CLASS_NUMBER = "0-9";
	/**
	 * a-z
	 */
	public static final int LOWER = 2;
	public static final String CLASS_LOWER = "a-z";
	/**
	 * A-Z
	 */
	public static final int UPPER = 4;
	public static final String CLASS_UPPER = "A-Z";
	/**
	 * LOWER + UPPER
	 */
	public static final int ALPHA = 6;
	public static final String CLASS_ALPHA = CLASS_LOWER + CLASS_UPPER;
	/**
	 * ALPHA + NUMBER
	 */
	public static final int ALPHANUM = 7;
	public static final String CLASS_ALPHANUM = CLASS_LOWER + CLASS_UPPER + CLASS_NUMBER;
	/**
	 * 0x20
	 */
	public static final int SPACE = 8;
	public static final String CLASS_SPACE = "\\x20";
	/**
	 * 0xC380-0xC3BF
	 */
	public static final int ACCENT = 16;
	public static final String CLASS_ACCENT = "a]|\\x{c3}[\\x{80}-\\x{bf}]|[a";
	/**
	 * ._-
	 */
	public static final int PUNCT = 32;
	public static final String CLASS_PUNCT = "\\._\\-";
	/**
	 * PUNCT + ACCENT + SPACE + ALPHANUM
	 */
	public static final int PHRASE = 63;
	public static final String CLASS_PHRASE = CLASS_PUNCT + CLASS_ACCENT + CLASS_SPACE + CLASS_ALPHANUM;
	/**
	 * 0x09 + 0x21-0x2F + 0x3A-0x40 + 0x5B-0x60 + 0x7B-0x7E + 0xC2A1-0xC2BF
	 */
	public static final int SPECIAL = 64;
	public static final String CLASS_SPECIAL = "\\x09\\x21-\\x2f\\x3a-\\x40\\x5b-\\x60\\x7b-\\x7e]|\\x{c2}[\\x{a1}-\\x{bf}]|[a";
	/**
	 * \r + \n
	 */
	public static final int NEWLINE = 128;
	public static final String CLASS_NEWLINE = "\\r\\n";
	/**
	 * NEWLINE + SPECIAL + PHRASE
	 */
	public static final int ALL = 255;
	public static final String CLASS_ALL = CLASS_NEWLINE + CLASS_SPECIAL;
	
	public static final String EMAIL = "^[_\\w\\.\\+-]+@[a-zA-Z0-9\\.-]{1,100}\\.[a-zA-Z0-9]{2,6}$";
	
	/**
	 * Returns the proper simple pattern for the provided rules.
	 * The pattern returned is of the following form : <br />
	 * <code>^(?s)([</code> <em>rules</em> <code>]*+)$</code>
	 * Note the 'possessive quantifier' (*+) is used for faster searches as those patterns are mainly used for
	 * a character-class search. This may have undesired results in some cases. Read the documentation of Regular Expressions
	 * for more details
	 * @param	rule	the rules of the pattern
	 * @return	the regex pattern
	 */
	public static String getRegex(int rule)
	{
		// Caution, do NOT set the '/u' flag otherwise UTF8 will NOT pass.
		String regex = "^(?s)(?:[";
		if( (rule & NUMBER) > 0 )
			regex += "0-9";
		if( (rule & LOWER) > 0 )
			regex += "a-z";
		if( (rule & UPPER) > 0 )
			regex += "A-Z";
		if( (rule & SPACE) > 0 )
			regex += "\\x20";
		if( (rule & ACCENT) > 0 )
			regex += "a]|\\x{c3}[\\x{80}-\\x{bf}]|[a"; // fix for UTF8. We assume that if accents are allowed, then the letter 'a' also is. This prevents an empty '[]' at the front or end of the regexp
		if( (rule & PUNCT) > 0 )
			regex += "\\._\\-";
		if( (rule & SPECIAL) > 0 )
			regex += "\\x21-\\x2f\\x3a-\\x40\\x5b-\\x60\\x7b-\\x7e]|\\x{c2}[\\x{a1}-\\x{bf}]|[a"; // fix for UTF8. We assume that if special characters are allowed, then the letter 'a' also is. This prevents an empty '[]' at the end of the regexp
		if( (rule & NEWLINE) > 0 )
			regex += "\\r\\n";
		
		regex += "])*+$";

		return regex;
	}
	
	/**
	 * Returns the proper simple pattern for the provided character classes.
	 * The pattern returned is of the following form : <br />
	 * <code>^(?s)([</code> <em>rules</em> <code>]*+)$</code>
	 * Note the 'possessive quantifier' (*+) is used for faster searches as those patterns are mainly used for
	 * a character-class search. This may have undesired results in some cases. Read the documentation of Regular Expressions
	 * for more details
	 * @param	classes	the rules of the pattern
	 * @return	the regex pattern
	 */
	public static String getRegex(String classes)
	{
		// Caution, do NOT set the '/u' flag otherwise UTF8 will NOT pass.
		String regex = "^(?s)(?:[" + classes + "])*+$";
		return regex;
	}
}