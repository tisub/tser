package com.anotherservice.util;

import java.util.*;
import java.lang.*;
import java.text.*;
import java.io.InputStream;

/**
 * This class handles JSON serialized data.
 *
 * @author  Simon Uyttendaele
 */
public class Json
{
	private Json() { }
	
	private static final int TOKEN_NONE = 0;
	private static final int TOKEN_CURLY_OPEN = 1;
	private static final int TOKEN_CURLY_CLOSE = 2;
	private static final int TOKEN_SQUARED_OPEN = 3;
	private static final int TOKEN_SQUARED_CLOSE = 4;
	private static final int TOKEN_COLON = 5;
	private static final int TOKEN_COMMA = 6;
	private static final int TOKEN_STRING = 7;
	private static final int TOKEN_NUMBER = 8;
	private static final int TOKEN_TRUE = 9;
	private static final int TOKEN_FALSE = 10;
	private static final int TOKEN_NULL = 11;
	private static final int BUILDER_CAPACITY = 2000;

	//=====================================
	// JSON DECODE
	//=====================================
	
	/**
	 * Decodes a JSON serialized string.
	 * This method calls {@link #decode(String, boolean)} with <em>allString</em> set to <code>false</code>.
	 * @param	json	the JSON string
	 * @return	the decoded data
	 */
	public static Any decode(String json) throws Exception
	{
		return decode(json, false);
	}
	
	/**
	 * Decodes a JSON serialized string.
	 * <p>The conversion from JSON to Object is performed as follows : <ul>
	 * <li>A JSON object is converted to <code>HashMap (Map&lt;String, Object&gt;)</code></li>
	 * <li>A JSON array is converted to <code>ArrayList (Collection&lt;Object&gt;)</code></li>
	 * <li>A JSON string is converted to <code>String</code></li>
	 * <li>A JSON number is converted to <code>double</code></li>
	 * <li>A JSON boolean is converted to <code>boolean</code></li>
	 * <li>A JSON null is converted to <code>null</code></li>
	 * </ul>
	 * The <code>Object</code> returned may be any of the above values. Note that the <em>map</em> values and the <em>collection</em> elements may also be any of the above values.
	 * </p>
	 * @param	json	the JSON string
	 * @param	allString	whether or not to convert all value types to <code>String</code> instead of <code>boolean, double, null</code>.
	 * @return	the decoded data
	 */
	public static Any decode(String json, boolean allString) throws Exception
	{
		if (json == null)
			return Any.wrap(null);

		final IntByRef index = new IntByRef(0);
		Object value = parseValue(json.toCharArray(), index, allString);
		return Any.wrap(value);
	}

	private static Map<String, Object> parseObject(char[] json, final IntByRef index, boolean allString) throws Exception
	{
		HashMap<String, Object> table = new HashMap<String, Object>();
		int token;

		// {
		nextToken(json, index);

		while (true)
		{
			token = lookAhead(json, index.value);
			if (token == TOKEN_NONE)
				throw new Exception("Value expected");
			else if (token == TOKEN_COMMA)
				nextToken(json, index);
			else if (token == TOKEN_CURLY_CLOSE)
			{
				nextToken(json, index);
				break;
			}
			else
			{
				// name
				String name = parseString(json, index);

				// :
				token = nextToken(json, index);
				if (token != TOKEN_COLON)
					throw new Exception("Colon expected after key name");

				// value
				Object value = parseValue(json, index, allString);
				table.put(name, value);
			}
		}

		return table;
	}

	private static ArrayList parseArray(char[] json, final IntByRef index, boolean allString) throws Exception
	{
		ArrayList array = new ArrayList();

		// [
		nextToken(json, index);

		while (true)
		{
			int token = lookAhead(json, index.value);
			if (token == TOKEN_NONE)
				throw new Exception("Value expected");
			else if (token == TOKEN_COMMA)
				nextToken(json, index);
			else if (token == TOKEN_SQUARED_CLOSE)
			{
				nextToken(json, index);
				break;
			}
			else
				array.add(parseValue(json, index, allString));
		}

		return array;
	}

	private static Object parseValue(char[] json, final IntByRef index, boolean allString) throws Exception
	{
		switch (lookAhead(json, index.value))
		{
			case TOKEN_STRING:
				return parseString(json, index);
			case TOKEN_NUMBER:
				if( allString )
					return extractNumber(json, index);
				return parseNumber(extractNumber(json, index));
			case TOKEN_CURLY_OPEN:
				return parseObject(json, index, allString);
			case TOKEN_SQUARED_OPEN:
				return parseArray(json, index, allString);
			case TOKEN_TRUE:
				nextToken(json, index);
				if( allString )
					return "true";
				return true;
			case TOKEN_FALSE:
				nextToken(json, index);
				if( allString )
					return "false";
				return false;
			case TOKEN_NULL:
				nextToken(json, index);
				if( allString )
					return "null";
				return null;
			case TOKEN_NONE:
			default:
				throw new Exception("Value expected");
		}
	}

	private static String parseString(char[] json, final IntByRef index) throws Exception
	{
		StringBuilder s = new StringBuilder(BUILDER_CAPACITY);
		eatWhitespace(json, index);

		// skip the leading quote
		index.value++;

		for (char c = json[index.value++]; index.value < json.length; c = json[index.value++])
		{
			if (c == '"')
				break;

			if (index.value == json.length)
				throw new Exception("Unterminated String static finalant");

			if (c == '\\')
			{
				if (index.value == json.length-1)
					throw new Exception("Unterminated String static finalant");
				c = json[index.value++];
				switch (c)
				{
					case '"': s.append('"'); break;
					case '\\': s.append('\\'); break;
					case '/': s.append('/'); break;
					case 'b': s.append('\b'); break;
					case 'f': s.append('\f'); break;
					case 'n': s.append('\n'); break;
					case 'r': s.append('\r'); break;
					case 't': s.append('\t'); break;
					case 'u':
						if( json.length - index.value < 4 )
							throw new Exception("Incomplete unicode character code");
						// parse the 32 bit hex into an integer codepoint
						int codePoint = Integer.parseInt(new String(json, index.value, 4), 16);
						// convert the integer codepoint to a unicode char and add to String
						s.append(Character.toChars(codePoint));
						// skip 4 chars
						index.value += 4;
						break;
					default:
						throw new Exception("Unrecognized escape character");
				}
			}
			else
				s.append(c);
		}

		return s.toString();
	}
	
	private static String extractNumber(char[] json, final IntByRef index) throws Exception
	{
		eatWhitespace(json, index);

		int lastIndex = getLastIndexOfNumber(json, index.value);
		int charLength = (lastIndex - index.value) + 1;

		String number = new String(json, index.value, charLength);

		index.value = lastIndex + 1;
		return number;
	}

	private static double parseNumber(String number) throws Exception
	{
		return Double.parseDouble(number);
	}

	private static int getLastIndexOfNumber(char[] json, int index)
	{
		int lastIndex;

		for (lastIndex = index; lastIndex < json.length; lastIndex++)
		{
			if ("0123456789+-.eE".indexOf(json[lastIndex]) == -1)
			{
				break;
			}
		}
		return lastIndex - 1;
	}

	private static void eatWhitespace(char[] json, final IntByRef index)
	{
		for (; index.value < json.length; index.value++)
		{
			if (" \t\n\r".indexOf(json[index.value]) == -1)
			{
				break;
			}
		}
	}

	private static int lookAhead(char[] json, int index)
	{
		return nextToken(json, new IntByRef(index));
	}

	private static int nextToken(char[] json, final IntByRef index)
	{
		eatWhitespace(json, index);

		if (index.value == json.length)
		{
			return TOKEN_NONE;
		}

		char c = json[index.value];
		index.value++;
		switch (c)
		{
			case '{':
				return TOKEN_CURLY_OPEN;
			case '}':
				return TOKEN_CURLY_CLOSE;
			case '[':
				return TOKEN_SQUARED_OPEN;
			case ']':
				return TOKEN_SQUARED_CLOSE;
			case ',':
				return TOKEN_COMMA;
			case '"':
				return TOKEN_STRING;
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
			case '-':
				return TOKEN_NUMBER;
			case ':':
				return TOKEN_COLON;
		}
		index.value--;

		int remainingLength = json.length - index.value;

		// false
		if (remainingLength >= 5)
		{
			if (json[index.value] == 'f' &&
				json[index.value + 1] == 'a' &&
				json[index.value + 2] == 'l' &&
				json[index.value + 3] == 's' &&
				json[index.value + 4] == 'e')
			{
				index.value += 5;
				return TOKEN_FALSE;
			}
		}

		// true
		if (remainingLength >= 4)
		{
			if (json[index.value] == 't' &&
				json[index.value + 1] == 'r' &&
				json[index.value + 2] == 'u' &&
				json[index.value + 3] == 'e')
			{
				index.value += 4;
				return TOKEN_TRUE;
			}
		}

		// null
		if (remainingLength >= 4)
		{
			if (json[index.value] == 'n' &&
				json[index.value + 1] == 'u' &&
				json[index.value + 2] == 'l' &&
				json[index.value + 3] == 'l')
			{
				index.value += 4;
				return TOKEN_NULL;
			}
		}

		return TOKEN_NONE;
	}

	//=====================================
	// JSON ENCODE
	//=====================================
	
	/**
	 * Serializes an object to JSON.
	 * This method calls {@link #encode(Object, boolean)} with <em>allString</em> set to <code>false</code>.
	 * @param	object	the object to encode
	 * @return	the JSON string
	 */
	public static String encode(Object object)
	{
		return encode(object, false);
	}
	
	/**
	 * Serializes an object to JSON.
	 * <p>The serialization to JSON is performed as follows : <ul>
	 * <li>A <code>Map</code> is converted to a JSON object with the map keys <em>.toString()</em></li>
	 * <li>A <code>Collection</code> or <code>Object[]</code> is converted to a JSON array</li>
	 * <li>A <code>CharSequence</code> (this implies all string-derived types), <code>Character</code>, <code>char[]</code> or <code>byte[]</code> is converted to a JSON string</li>
	 * <li>A <code>Number</code> (this implies all primitive numeric value types) is converted to a JSON number</li>
	 * <li>A <code>boolean</code> is converted to a JSON boolean</li>
	 * <li>A <code>null</code> is converted to a JSON null</li>
	 * <li>An <code>InputStream</code> is converted to its {@link com.anotherservice.util.Base64} string representation</li>
	 * <li>A <code>Date</code> is converted to its <em>"dd/MM/yyyy HH:mm:ss"</em> string representation as a JSON string</li>
	 * <li>Any other <code>Object</code> is converted to its <em>.toString()</em> string representation as a JSON string</li>
	 * </ul>
	* </p>
	 * @param	object	the object to encode
	 * @param	allString	whether or not to encode all value types as <code>String</code> instead of <code>boolean, double, null</code>.
	 * @return	the JSON string
	 */
	public static String encode(Object object, boolean allString)
	{
		StringBuilder builder = new StringBuilder(BUILDER_CAPACITY);
		serializeValue(object, builder, allString);
		return builder.toString();
	}

	private static void serializeValue(Object value, StringBuilder builder, boolean allString)
	{
		if (value == null)
			builder.append((allString ? "\"null\"" : "null"));
		else if (value instanceof Jsonizable)
			builder.append(((Jsonizable)value).toJson());
		else if (value instanceof Date)
			builder.append("\"" + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format((Date)value) + "\"");
		else if (value instanceof Map)
			serializeObject((Map)value, builder, allString);
		else if (value instanceof Collection)
			serializeArray((Collection)value, builder, allString);
		else if ((value instanceof Boolean) && ((Boolean)value == true))
			builder.append((allString ? "\"true\"" : "true"));
		else if ((value instanceof Boolean) && ((Boolean)value == false))
			builder.append((allString ? "\"false\"" : "false"));
		else if (value instanceof Number)
			builder.append((allString ? "\"" : "") + value + (allString ? "\"" : ""));
		else if (value instanceof byte[])
			serializeBytes((byte[])value, builder);
		else if (value instanceof char[])
			serializeString(new String((char[])value), builder);
		else if (value instanceof CharSequence || value instanceof Character)
			serializeString(value.toString(), builder);
		else if (value instanceof Object[])
			serializeArray(Arrays.asList((Object[])value), builder, allString);
		else if (value instanceof InputStream)
		{
			builder.append("\"");
			try { builder.append(Base64.encode((InputStream)value)); }
			catch(Exception e) { }
			builder.append("\"");
		}
		else
			serializeString(value.toString(), builder);
	}

	private static void serializeObject(Map anObject, StringBuilder builder, boolean allString)
	{
		builder.append("{");
		boolean first = true;
		for( Object key : anObject.keySet() )
		{
			if (!first)
				builder.append(", ");

			serializeString(key.toString(), builder);
			builder.append(":");
			serializeValue(anObject.get(key), builder, allString);
			
			first = false;
		}
		builder.append("}");
	}

	private static void serializeArray(Collection anArray, StringBuilder builder, boolean allString)
	{
		builder.append("[");

		boolean first = true;
		for( Object item : anArray )
		{
			if (!first)
				builder.append(", ");

			serializeValue(item, builder, allString);

			first = false;
		}
		builder.append("]");
	}

	private static void serializeBytes(byte[] bytes, StringBuilder builder)
	{
		//serializeString(Base64.encode(bytes));
		builder.append("\"");

		for (int i = 0; i < bytes.length; i++)
		{
			char c = (char)(bytes[i] & 0xFF);
			int codepoint = (int)c;
			if( codepoint < 32 && codepoint > 126 )
			{
				String unicode = Integer.toString(codepoint, 16);
				while( unicode.length() < 4 ) unicode = "0" + unicode;
				builder.append("\\u" + unicode);
			}
			else
				builder.append(c);
		}

		builder.append("\"");
	}

	private static void serializeString(String aString, StringBuilder builder)
	{
		builder.append("\"");
		
		// make sure input string is in UTF8
		aString = Hex.utf8ize(aString);

		//char[] charArray = aString.toCharArray();
		//for (int i = 0; i < charArray.length; i++)
		for (int i = 0; i < aString.length(); i++)
		{
			char c = (char)aString.codePointAt(i);//charArray[i];
			if (c == '"')
			{
				builder.append("\\\"");
			}
			else if (c == '\\')
			{
				builder.append("\\\\");
			}
			else if (c == '\b')
			{
				builder.append("\\b");
			}
			else if (c == '\f')
			{
				builder.append("\\f");
			}
			else if (c == '\n')
			{
				builder.append("\\n");
			}
			else if (c == '\r')
			{
				builder.append("\\r");
			}
			else if (c == '\t')
			{
				builder.append("\\t");
			}
			else
			{
				int codepoint = (int)c;
				boolean isASCII = (codepoint >= 32 && codepoint <= 126);
				/*boolean isUTF8 = false;
				
				if( codepoint == 194 && i+1 < charArray.length && (int)charArray[i+1] >= 161 && (int)charArray[i+1] <= 191)
					isUTF8 = true;
				else if( codepoint == 195 && i+1 < charArray.length && (int)charArray[i+1] >= 128 && (int)charArray[i+1] <= 191)
					isUTF8 = true;*/

				if( /*isUTF8 ||*/ isASCII )
				{
					builder.append(c);
				}
				else
				{
					String unicode = Integer.toString(codepoint, 16);
					while( unicode.length() < 4 ) unicode = "0" + unicode;
					builder.append("\\u" + unicode);
				}
				/*if( isUTF8 )
				{
					i++;
					builder.append(charArray[i]);
				}*/
			}
		}

		builder.append("\"");
	}
}

class IntByRef implements Comparable<IntByRef>
{
	public Integer value;
	
	public IntByRef(Integer value)
	{
		this.value = value;
	}
	
	public int compareTo(Integer other)
	{
		return this.value.compareTo(other);
	}
	
	public int compareTo(IntByRef other)
	{
		return this.value.compareTo(other.value);
	}
	
	public boolean equals(Object o)
	{
		if( o instanceof IntByRef )
			return this.value == ((IntByRef)o).value;
		else
			return this.value == o;
	}
}