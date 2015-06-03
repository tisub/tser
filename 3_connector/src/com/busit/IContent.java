package com.busit;

import com.anotherservice.util.*;
import java.util.regex.*;
import java.lang.*;
import java.util.*;

/**
 * This interface defines the properties of a {@link com.busit.IMessage message} content.
 * See this as a kind of loose multi-field type wrapper around any sort of data.
 * Give it a nice {@link #name()}, a ideally unique {@link #id()} and you have your own custom data type.
 *
 * @note	all properties contained via the {@link java.util.Map} interface must be json compatible. That is,
 * if you set a random <code>Object</code> or another <code>IContent</code> as value, it will be converted to its closest json representation
 * but will not be interpreted as the same type of <code>Object</code> when reading it afterwards.
 */
public interface IContent extends Map<String, Object>
{
	/**
	 * Get the content type id
	 * @return	the <em>ideally unique</em> id
	 */
	public int id();
	
	/**
	 * Set the content type id
	 * @param	id	the <em>ideally unique</em> id
	 */
	public void id(int id);
	
	/**
	 * Get the content type name
	 * @return	the type name
	 */
	public String name();
	
	/**
	 * Set the content type name
	 * @param	name	the type name
	 */
	public void name(String name);
	
	/**
	 * Ask whether the current type is compatible with another type.
	 * @param	id	another content type id
	 * @return	whether or not this type is <em>compatible</em> with (aka: shares the same properties as) another one
	 */
	public boolean compatible(int id);
	
	/**
	 * Declare that the current type is compatible (or not) with another type.
	 * @param	id	the other content type id
	 * @param	value	whether or not the current type is compatible with it (aka: shares the same properties as).
	 */
	public void compatible(int id, boolean value);
	
	/**
	 * Get the list of content type ids that are declared to be compatible with the current one.
	 * @return	the list of compatible content type ids
	 */
	public List<Integer> compatibility();
	
	/**
	 * Set the list of content type ids that are compatible with the current one.
	 * @param	compat	the list of compatible content type ids
	 */
	public void compatibility(List<Integer> compat);

	/**
	 * Get the simple text format template
	 * @return	the simple text format
	 */
	public String textFormat();
	
	/**
	 * Set the simple text format template.
	 * The template will be used by the {@link #toText()} method to print a nice formatted text with substituted values.<br />
	 * The substituable parts should be enclosed in double curly braces : <code>{{value}}</code> and the value should be
	 * the name of a property stored in this content type. In order to access nested properties (sub-map or sub-array), you
	 * should separate values by a single dot.<br />
	 * Data representation in JSON:
	 * <pre>
	 * {
	 *     "contacts": [
	 *         { "name": "sam" },
	 *         { "name": "yann" },
	 *         { "name": "simon" }
	 *     ]
	 * }
	 * </pre><br />
	 * Sample format text:
	 * <pre>"The first contact name is : {{contacts.0.name}}"</pre>
	 * If the value is not found, the substitution group is ignored and replaced by an empty string.
	 * @param	format	the format template
	 */
	public void textFormat(String format);
	
	/**
	 * Get a simple text formatted string representation of the data
	 * @return	simple text data
	 */
	public String toText();
	
	/**
	 * Get a simple text formatted string representation of the data using the specified format
	 * @param	format	the format to apply
	 * @return	simple text data
	 */
	public String toText(String format);
	
	/**
	 * Get the html format template
	 * @return	the html format
	 */
	public String htmlFormat();
	
	/**
	 * Set the html format template.
	 * The template will be used by the {@link #toHtml()} method to print a nice formatted text with substituted values.<br />
	 * The substituable parts should be enclosed in double curly braces : <code>{{value}}</code> and the value should be
	 * the name of a property stored in this content type. In order to access nested properties (sub-map or sub-array), you
	 * should separate values by a single dot.<br />
	 * Beware that in order to avoid malicious scripting or invasive content, some tags are ignored such as 
	 * (non-exhaustive) <code>script, object, embed</code>.<br />
	 * Data representation in JSON:
	 * <pre>
	 * {
	 *     "contacts": [
	 *         { "name": "sam" },
	 *         { "name": "yann" },
	 *         { "name": "simon" }
	 *     ]
	 * }
	 * </pre><br />
	 * Sample format text:
	 * <pre>"The [b]first[/b] contact name is : [i]{{contacts.0.name}}[/i]"</pre>
	 * If the value is not found, the substitution group is ignored and replaced by an empty string.
	 * @param	format	the format template
	 */
	public void htmlFormat(String format);
	
	/**
	 * Get a html formatted string representation of the data
	 * @return	html data
	 */
	public String toHtml();
	
	/**
	 * Get a html formatted string representation of the data using the specified format
	 * @param	format	the format to apply
	 * @return	html data
	 */
	public String toHtml(String format);
	
	/**
	 * Get the JSON representation of the content
	 * @return the json content
	 */
	public String toJson();
	
	/**
	 * Merges all properties of the specified content type into this one.
	 * All compatibilities of the merged content type will be copied.
	 * The name, id, textFormat and htmlFormat of the current content type will not be altered.
	 * @param	content	the content type to merge into this one
	 */
	public void merge(IContent content);
}