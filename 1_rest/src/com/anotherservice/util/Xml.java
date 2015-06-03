package com.anotherservice.util;

import java.util.*;
import java.lang.*;
import java.text.*;
import java.util.regex.*;

/**
 * This class handles <em>simple</em> XML serialized data.
 * <ul><li>Does not support attributes</li>
 * <li>Supports comments</li>
 * <li>Supports CDATA</li>
 * <li>Does not support nested tags with the same tag name</li>
 * <li>Does not decode entities other than &amp;tl;, &amp;gl; and &amp;</li></ul>
 * @author  Simon Uyttendaele
 */
public class Xml
{
	//=====================================
	// XML ENCODE
	//=====================================
	
	/**
	 * Serializes an object to XML. 
	 * This method calls {@link #encode(Object, String)} with <em>rootTagName</em> set to <em>root</em>.
	 * @param	object	the object to encode
	 * @return	the XML string
	 */
	public static String encode(Object object) { return encode(object, "root"); }
	
	/**
	 * Serializes an object to XML. 
	 * <p>The serialization to XML is performed as follows : <ul>
	 * <li>A <code>Map</code> is converted to XML tags with the key as tag name and the value as content</li>
	 * <li>A <code>Collection</code> or <code>Object[]</code> is converted to sibling XML tags having the same parent tag name</li>
	 * <li>A <code>CharSequence</code> (this implies all string-derived types), <code>Character</code>, <code>char[]</code> or <code>byte[]</code> is converted to string</li>
	 * <li>A <code>Number</code> (this implies all primitive numeric value types) is converted to its <em>.toString()</em> string representation</li>
	 * <li>A <code>boolean</code> is converted to its string representation "true" or "false" respectively</li>
	 * <li>A <code>null</code> is converted to an empty string</li>
	 * <li>A <code>Date</code> is converted to its <em>"dd/MM/yyyy HH:mm:ss"</em> string representation</li>
	 * <li>Any other <code>Object</code> is converted to its <em>.toString()</em> string representation</li>
	 * </ul>
	 * </p>
	 * <p>The string conversion for an XML tag name is discarding all non <code>a-z, A-Z, 0-9, _.-:</code> characters.
	 * If the tag name is empty or starts with <code>0-9, .-:</code> then it is prefixed with <code>_</code>
	 * </p><p>The string conversion for the XML content is replacing <code>&lt;, &gt;, &amp;</code> with <code>&amp;lt;, &amp;gt;, &amp;amp;</code> respectively.
	 * However, if the string value contains any special character with codepoint &lt; 32 or &gt; 126, it is wrapped in a CDATA section and any <code>]]&gt;</code> is stripped
	 * </p><p>If two or more levels of Collection or Object[] are directly nested with the same parent tag name, the parent tag name is suffixed with <code>_</code>
	 * for the children
	 * </p>
	 * @param	object	the object to encode
	 * @param	rootTagName	the XML root tag name
	 * @return	the XML string
	 */
	public static String encode(Object object, String rootTagName) { return "<" + rootTagName + ">" + encodeRecurse(object, rootTagName) + "</" + rootTagName + ">"; }
	
	private static String encodeRecurse(Object object, String parentNode)
	{
		String xml = "";
		
		if( object instanceof Map )
		{
			for( Object key : ((Map)object).keySet() )
			{
				String tag = serializeTag(key);
				xml += "<" + tag + ">" + encodeRecurse(((Map)object).get(key), tag) + "</" + tag + ">";
			}
		}
		else if( object instanceof Collection )
		{
			for( Object item : (Collection)object )
				xml += encodeRecurse(item, parentNode + "_") + "</" + parentNode + "><" + parentNode + ">";
			
			if( xml.endsWith("</" + parentNode + "><" + parentNode + ">") )
				xml = xml.substring(0, xml.length() - ("</" + parentNode + "><" + parentNode + ">").length());
		}
		else if( object instanceof Object[] )
		{
			for( Object item : I.iterable((Object[])object) )
				xml += encodeRecurse(item, parentNode + "_") + "</" + parentNode + "><" + parentNode + ">";
			
			if( xml.endsWith("</" + parentNode + "><" + parentNode + ">") )
				xml = xml.substring(0, xml.length() - ("</" + parentNode + "><" + parentNode + ">").length());
		}
		else
		{
			xml += serializeValue(object);
		}
		
		return xml;
	}
	
	private static String serializeTag(Object value)
	{
		String tag = "";
		if (value == null)
			tag = "null";
		else if ((value instanceof Boolean) && ((Boolean)value == true))
			tag = "true";
		else if ((value instanceof Boolean) && ((Boolean)value == false))
			tag = "false";
		else if (value instanceof Number)
			tag = "_" + value;
		else if (value instanceof char[] || value instanceof byte[])
			tag = new String((char[])value);
		else if (value instanceof CharSequence || value instanceof Character)
			tag = serializeString(value.toString());
		else
			tag = serializeString(value.toString());
		
		tag.replaceAll("[^a-zA-Z0-9_\\-\\.:]", "");
		tag.replaceAll("^([0-9\\-\\.\\:])", "_$1");
		if( tag.length() == 0 )
			return "_";
		else
			return tag;
	}
	
	private static String serializeValue(Object value)
	{
		if (value == null)
			return "";
		else if (value instanceof Date)
			return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format((Date)value);
		else if ((value instanceof Boolean) && ((Boolean)value == true))
			return "true";
		else if ((value instanceof Boolean) && ((Boolean)value == false))
			return "false";
		else if (value instanceof Number)
			return "" + value;
		else if (value instanceof char[] || value instanceof byte[])
			return serializeString(new String((char[])value));
		else if (value instanceof CharSequence || value instanceof Character)
			return serializeString(value.toString());
		else
			return serializeString(value.toString());
	}
	
	private static String serializeString(String aString)
	{
		char[] charArray = aString.toCharArray();
		for (int i = 0; i < charArray.length; i++)
		{
			char c = charArray[i];
			int codepoint = (int)c;
			if (codepoint < 32 || codepoint > 126 )
			{
				return "<![CDATA[" + aString.replaceAll("\\]\\]>", "") + "]]>";
			}
		}

		return aString.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	}
	
	//=====================================
	// XML DECODE
	//=====================================
	
	/**
	 * Decodes a XML serialized string.
	 * This method calls {@link #decode(String, boolean)} with <em>allString</em> set to <code>false</code>.
	 * @param	xml	the XML string
	 * @return	the decoded data
	 */
	public static Map<String, Object> decode(String xml)
	{
		return decode(xml, false);
	}
	
	/**
	 * Decodes a XML serialized string.
	 * <p>The conversion from XML to Object is performed as follows : <ul>
	 * <li>A XML tag is converted to a <code>HashMap (Map&lt;String, Object&gt;)</code> with the tag name as key</li>
	 * <li>Sibling identical tag values are converted to an <code>ArrayList (Collection&lt;Object&gt;)</code></li>
	 * <li>A XML string is converted to <code>String</code> and sequences <code>&amp;lt;, &amp;gt;, &amp;amp;</code> 
	 * are replaced by <code>&lt;, &gt;, &amp;</code> respectively</li>
	 * <li>A XML number is converted to <code>double</code></li>
	 * <li>A XML boolean is converted to <code>boolean</code></li>
	 * <li>A XML null is converted to <code>null</code></li>
	 * </ul>
	 * Note that the <em>map</em> values and the <em>collection</em> elements may also be any of the above values.
	 * </p>
	 * @note	the root node is ignored
	 * @param	xml	the XML string
	 * @param	allString	whether or not to convert all value types to <code>String</code> instead of <code>boolean, double, null</code>.
	 * @return	the decoded data
	 */
	public static Map<String, Object> decode(String xml, boolean allString)
	{
		return new Xml(xml, allString).getResult();
	}
	
	private Xml(String xml, boolean allString)
	{
		this.allString = allString;
		xml = extractCDATA(xml);
		xml = stripComments(xml);
		Map<String, Object> data = (Map<String, Object>) parseRecurse(xml);
		
		if( data.size() == 1 )
		{
			Object item = data.values().iterator().next();
			if( item instanceof Map )
			{
				this.data = (Map<String, Object>)item;
				return;
			}
		}
		
		this.data = data;
	}
	
	private Map<String, Object> data;
	private boolean allString = false;
	private ArrayList<String> cdata = new ArrayList<String>();
	
	private Map<String, Object> getResult()
	{
		return this.data;
	}
	
	private Object parseRecurse(String xml)
	{
		if( xml == null || xml.length() == 0 )
			return null;

		Matcher m = Pattern.compile("<\\s*([a-z0-9_\\.-]+)\\s*>\\s*(.*?)\\s*</\\s*\\1\\s*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(xml);
		
		if( !m.find() )
			return insertCDATA(xml);
		
		HashMap<String, Object> node = new HashMap<String, Object>();
		do
		{
			String key = m.group(1);
			String value = m.group(2);
			
			if( node.containsKey(key) )
			{
				Object item = node.get(key);
				if( item instanceof Collection )
					((Collection)item).add(parseRecurse(value));
				else
				{
					ArrayList<Object> tmp = new ArrayList<Object>();
					tmp.add(item);
					tmp.add(parseRecurse(value));
					node.put(key, tmp);
				}
			}
			else
				node.put(key, parseRecurse(value));
		} while( m.find() );
		
		return node;
	}
	
	private String extractCDATA(String xml)
	{
		Matcher m = Pattern.compile("<!\\[CDATA\\[\\s*(.*?)\\s*\\]\\]>", Pattern.DOTALL).matcher(xml);
		StringBuffer sb = new StringBuffer();
		while( m.find() )
		{
			this.cdata.add(m.group(1));
			m.appendReplacement(sb, "#@_" + this.cdata.size() + "_@#");
		}
		m.appendTail(sb);
		
		return sb.toString();
	}
	
	private Object insertCDATA(String xml)
	{
		if( xml == null || xml.trim().length() == 0 )
			return parseValue(xml);

		Matcher m = Pattern.compile("#@_([0-9]+)_@#", Pattern.DOTALL).matcher(xml);
		StringBuffer sb = new StringBuffer();
		while( m.find() )
			m.appendReplacement(sb, this.cdata.get(Integer.parseInt(m.group(1))));
		m.appendTail(sb);
		
		return parseValue(sb.toString());
	}

	private String stripComments(String xml)
	{
		return xml.replaceAll("(?s)<!--.*?(?:-->|$)", "");
	}
	
	private Object parseValue(String value)
	{
		if( value == null )
			return (allString ? "" : null);
		else
			value = value.trim();

		if( value.length() == 0 || value.equalsIgnoreCase("null") )
			return (allString ? "" : null);
		
		if( value.equalsIgnoreCase("true") )
			return (allString ? "true" : true);
		
		if( value.equalsIgnoreCase("false") )
			return (allString ? "false" : true);
		
		if( allString )
			return value.replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&amp;", "&");

		try { double d = Double.parseDouble(value); return d; }
		catch(NumberFormatException nfe) { }
		
		return value.replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&amp;", "&");
	}
}