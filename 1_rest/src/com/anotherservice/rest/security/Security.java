package com.anotherservice.rest.security;

import com.anotherservice.rest.*;
import com.anotherservice.rest.core.*;

/**
 * This class is a Thread safe Singleton getter for a {@link com.anotherservice.rest.security.ISecurity} for the REST service
 * The security instance cannot be changed after setting it.
 *
 * @author  Simon Uyttendaele
 */
public class Security
{
	private static final int ERROR_CLASS_ID = 0x1000;
	
	private Security() { }
	private static ISecurity implementation;
	/**
	 * Returns the <code>ISecurity</code> instance
	 * @return	the unique <code>ISecurity</code> instance
	 * @throws	RuntimeException	if the instance has not been set
	 */
	public synchronized static ISecurity getInstance()
	{
		if( implementation == null )
			throw new RestException("Security implementation not defined", 
				RestException.NOT_RECOVERABLE | RestException.CAUSE_CONFIG | RestException.DUE_TO_SECURITY | ERROR_CLASS_ID | 0x1);
		
		return implementation;
	}
	
	/**
	 * Sets the <code>ISecurity</code> instance
	 * @param	instance	the unique <code>ISecurity</code> instance
	 * @throws	RuntimeException	if the instance has already been set
	 */
	public synchronized static void setInstance(ISecurity instance)
	{
		if( implementation != null )
			throw new RestException("Security implementation is already defined", 
				RestException.NOT_RECOVERABLE | RestException.CAUSE_CONFIG | RestException.DUE_TO_SECURITY | ERROR_CLASS_ID | 0x2);
		
		implementation = instance;
	}
	
	/**
	 * Escapes the provided text for safe usage in a SQL statement.
	 * Adds a leading backslash ("\") to the following characters <code>0x00, \n, \r, ', ", \, 0x1A</code>
	 * @param	text	some text
	 * @return	the escaped text or an empty string if the original text is <code>null</code>
	 */
	public static String escape(String text)
	{
		return escape(text, false);
	}
	
	/**
	 * Escapes the provided text for safe usage in a SQL statement.
	 * Adds a leading backslash ("\") to the following characters depending on the strict mode:
	 * <ul><li>strict: <code>0x00, \t, \n, \r, ', ", \, 0x1A, 0x08, %, _</code></li>
	 * <li>non-strict: <code>0x00, \n, \r, ', ", \, 0x1A</code></li></ul>
	 * @param	text	some text
	 * @param	strict	strict mode
	 * @return	the escaped text or an empty string if the original text is <code>null</code>
	 */
	public static String escape(String text, boolean strict)
	{
		/* http://dev.mysql.com/doc/refman/5.7/en/string-literals.html
		9.1 Special Character Escape Sequences
		Escape Sequence		Character Represented by Sequence
		\0 					An ASCII NUL (0x00) character.
		\' 					A single quote ("'") character.
		\" 					A double quote (""") character.
		\b 					A backspace character.
		\n 					A newline (linefeed) character.
		\r 					A carriage return character.
		\t 					A tab character.
		\Z 					ASCII 26 (Control+Z). See note following the table.
		\\ 					A backslash ("\") character.
		\% 					A "%" character. See note following the table.
		\_ 					A "_" character. See note following the table.
		*/
		if( text == null )
			return "";

		if( strict )
			return text.replaceAll("(\\x00|\\t|\\n|\\r|'|\"|\\\\|\\x1a|\\x08|%|_)", "\\\\$1");
		else
			return text.replaceAll("(\\x00|\\n|\\r|'|\"|\\\\|\\x1a)", "\\\\$1");
	}
	
	/**
	 * Unescapes the text that was previousely escaped.
	 * Removes leading backslash ("\") to the following characters <code>0x00, \t, \n, \r, ', ", \, 0x1A, 0x08, %, _</code>
	 * @param	text	some text
	 * @return	the unescaped text or an empty string if the original text is <code>null</code>
	 */
	public static String unescape(String text)
	{
		return unescape(text, false);
	}
	
	/**
	 * Unescapes the text that was previousely escaped.
	 * Removes leading backslash ("\") to the following characters depending on the strict mode:
	 * <ul><li>strict: <code>0x00, \t, \n, \r, ', ", \, 0x1A, 0x08, %, _</code></li>
	 * <li>non-strict: <code>0x00, \n, \r, ', ", \, 0x1A</code></li></ul>
	 * @param	text	some text
	 * @param	strict	strict mode
	 * @return	the unescaped text or an empty string if the original text is <code>null</code>
	 */
	public static String unescape(String text, boolean strict)
	{
		if( text == null )
			return "";

		if( strict )
			return text.replaceAll("\\\\(\\x00|\\t|\\n|\\r|'|\"|\\\\|\\x1a|\\x08|%|_)", "$1");
		else
			return text.replaceAll("\\\\(\\x00|\\n|\\r|'|\"|\\\\|\\x1a)", "$1");
	}
}