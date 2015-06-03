package com.anotherservice.rest;

import com.anotherservice.rest.core.*;
import com.anotherservice.util.*;
import java.util.*;

/**
 * This class is part of the REST service and defines the rules of compliance for potential {@link com.anotherservice.rest.Action} parameters. <br />
 * The <em>alias</em> is used to retrieve the <em>param</em> name in the {@link com.anotherservice.rest.core.Request} class. A <code>Parameter</code> can have
 * several aliases in case the <em>param</em> name may be "one of" the defined names.
 * @author	Simon Uyttendaele
 */
public class Parameter
{
	/**
	 * Default constructor
	 */
	public Parameter()
	{
	}
	
	/**
	 * Constructor with the parameter alias
	 */
	public Parameter(String alias)
	{
		addAlias(alias);
	}

	/**
	 * Whether or not the next action can be used as value for this parameter. <br />
	 * If {@link com.anotherservice.rest.core.Request#hasParam(String)} is <code>false</code>, then the value is 
	 * taken from {@link com.anotherservice.rest.core.Request#getAction()}.<br />
	 * Default value : <code>false</code>
	 */
	public boolean allowInUrl = false;
	/**
	 * Whether or not this parameter allows multiple values. <br />
	 * If <code>true</code>, the parameter value will be split using {@link #multipleValueDelimiterRegex}
	 * <br />Default value : <code>false</code>
	 */
	public boolean isMultipleValues = false;
	/**
	 * The regex pattern used to split the parameter value. <br />
	 * Used only if {@link #isMultipleValues} is <code>true</code>
	 * <br />Default value : <code>(,|;|\s)</code>
	 */
	public String multipleValueDelimiterRegex = "(,|;|\\s)";
	/**
	 * Minimum number of multiple values. <br />
	 * Used only if {@link #isMultipleValues} is <code>true</code>
	 * <br />Default value : <code>1</code>
	 */
	public int minMultipleValues = 1;
	/**
	 * Maximum number of multiple values. <br />
	 * Used only if {@link #isMultipleValues} is <code>true</code>
	 * <br />Default value : <code>0</code>
	 */
	public int maxMultipleValues = 255;
	/**
	 * Whether or not the value is optional (can be missing). 
	 * @note an empty value is not considered to be ommited
	 * <br />Default value : <code>true</code>
	 */
	public boolean isOptional = true;
	/**
	 * Minimum length of the value. <br />
	 * Considered only if > 0
	 * <br />Default value : <code>0</code>
	 */
	public int minLength = 0;
	/**
	 * Maximum length of the value. <br />
	 * Considered only if > 0
	 * <br />Default value : <code>0</code>
	 */
	public int maxLength = 0;
	/**
	 * Custom regex pattern validation check. <br />
	 * Considered only if not <code>null</code>. The check is performed using <code>Pattern.matches()</code>.
	 * <br />Default value : <code>null</code>
	 */
	public String mustMatch = null;
	/**
	 * The description of this parameter. 
	 * Used when requesting the help of the handler.
	 */
	public String description = "";
	
	/**
	 * Shorthand for most common restrictions : isOptional, minLength, maxLength and mustMatch
	 * @param	isOptional	Whether or not the value is optional
	 * @param	minLength	Minimum length of the value
	 * @param	maxLength	Maximum length of the value
	 * @param	mustMatch	Custom regex pattern validation check
	 */
	 public void enforce(boolean isOptional, int minLength, int maxLength, String mustMatch)
	 {
		 this.isOptional = isOptional;
		 this.minLength = minLength;
		 this.maxLength = maxLength;
		 this.mustMatch = mustMatch;
	 }
	 
	 /**
	 * Shorthand for most common restrictions : isOptional, minLength, and maxLength
	 * @param	isOptional	Whether or not the value is optional
	 * @param	minLength	Minimum length of the value
	 * @param	maxLength	Maximum length of the value
	 */
	 public void enforce(boolean isOptional, int minLength, int maxLength)
	 {
		 this.isOptional = isOptional;
		 this.minLength = minLength;
		 this.maxLength = maxLength;
	 }
	 
	 /**
	 * Shorthand for most common restrictions : minLength and maxLength
	 * @param	minLength	Minimum length of the value
	 * @param	maxLength	Maximum length of the value
	 */
	 public void enforce(int minLength, int maxLength)
	 {
		 this.minLength = minLength;
		 this.maxLength = maxLength;
		 this.mustMatch = mustMatch;
	 }
	 
	 /**
	 * Shorthand for most common restrictions : minLength, maxLength and mustMatch
	 * @param	minLength	Minimum length of the value
	 * @param	maxLength	Maximum length of the value
	 * @param	mustMatch	Custom regex pattern validation check
	 */
	 public void enforce(int minLength, int maxLength, String mustMatch)
	 {
		 this.minLength = minLength;
		 this.maxLength = maxLength;
		 this.mustMatch = mustMatch;
	 }
	
	//=====================================
	// GET VALUE
	//=====================================
	
	/**
	 * Returns the value of the {@link com.anotherservice.rest.core.Request} <em>param</em> matching one of the aliases. <br />
	 * If the parameter allows multiple values, the first one is returned.<br />
	 * @return	the parameter value
	 * @throws	Exception	if the value does not comply to all rules
	 * @see	com.anotherservice.rest.core.Request#getCheckParam(Parameter)
	 */
	public String getValue() throws Exception
	{
		Object v = Request.getCheckParam(this);
		if( v instanceof Collection )
		{
			if( ((Collection)v).size() > 0 )
				return (String)((Collection)v).iterator().next();
			else
				return null;
		}
		else
			return (String)v;
	}
	
	/**
	 * Returns the value of the {@link com.anotherservice.rest.core.Request} <em>param</em> matching one of the aliases. <br />
	 * If the parameter does not allows multiple values, the value is wrapped in a <code>Collection</code>.<br />
	 * @return	the parameter values
	 * @throws	Exception	if any of the values does not comply to all rules
	 * @see	com.anotherservice.rest.core.Request#getCheckParam(Parameter)
	 */
	public Collection<String> getValues() throws Exception
	{
		Object v = Request.getCheckParam(this);
		if( v instanceof String )
		{
			ArrayList<String> a = new ArrayList<String>();
			a.add((String)v);
			return a;
		}
		else if( v == null )
			return new ArrayList<String>();
		else
			return (Collection<String>)v;
	}
	
	//=====================================
	// ALIAS
	//=====================================
	
	private Collection<String> alias = new ArrayList<String>();
	
	/**
	 * Checks whether or not this parameter has the provided alias. <br />
	 * Aliases are always lower case, thus a <code>.toLowerCase()</code> is automatically applied.
	 * @param	name	the alias to check
	 * @return	<code>true</code> if the parameter has this alias, <code>false</code> otherwise
	 */
	public boolean hasAlias(String name)
	{
		if( name == null )
			return false;

		return this.alias.contains(name.toLowerCase());
	}
	
	/**
	 * Checks whether or not this parameter has any of the provided aliases. <br />
	 * Aliases are always lower case, thus a <code>.toLowerCase()</code> is automatically applied.
	 * @param	names	the aliases to check
	 * @return	<code>true</code> if the parameter has any of the aliases, <code>false</code> otherwise
	 */
	public boolean hasAlias(Collection<String> names)
	{
		if( names == null || names.size() == 0 )
			return false;

		for( String n : names )
		{
			if( n == null )
				continue;

			if( this.alias.contains(n.toLowerCase()) )
				return true;
		}
				
		return false;
	}
	
	/**
	 * Checks whether or not this parameter has any of the provided aliases. <br />
	 * Aliases are always lower case, thus a <code>.toLowerCase()</code> is automatically applied.
	 * @param	names	the aliases to check
	 * @return	<code>true</code> if the parameter has any of the aliases, <code>false</code> otherwise
	 */
	public boolean hasAlias(String[] names)
	{
		if( names == null || names.length == 0 )
			return false;

		for( String n : I.iterable(names) )
		{
			if( n == null )
				continue;

			if( this.alias.contains(n.toLowerCase()) )
				return true;
		}
				
		return false;
	}
	
	/**
	 * Returns the first alias of this parameter. 
	 * @return	the parameter alias
	 */
	public String getAlias()
	{
		for( String a : this.alias )
			return a;
		return null;
	}
	
	/**
	 * Returns the aliases of this parameter. 
	 * @return	the parameter aliases
	 */
	public Collection<String> getAliases()
	{
		return this.alias;
	}
	
	/**
	 * Adds the specified alias. 
	 * @note	<code>null</code> or empty alias is silently ignored
	 * @param	alias	the alias to add
	 */
	public void addAlias(String alias)
	{
		if( alias != null && alias.length() > 0 && !this.alias.contains(alias) )
			this.alias.add(alias);
	}
	
	/**
	 * Adds the specified aliases. 
	 * @note	<code>null</code> or empty alias is silently ignored
	 * @param	alias	the list of aliases to add
	 */
	public void addAlias(Collection<String> alias)
	{
		if( alias == null || alias.size() == 0 )
			return;

		for( String a : alias )
		{
			if( a != null && a.length() > 0 && !this.alias.contains(a) )
				this.alias.add(a);
		}
	}
	
	/**
	 * Adds the specified aliases. 
	 * @note	<code>null</code> or empty alias is silently ignored
	 * @param	alias	the list of aliases to add
	 */
	public void addAlias(String[] alias)
	{
		if( alias == null || alias.length == 0 )
			return;

		for( String a : I.iterable(alias) )
		{
			if( a != null && a.length() > 0 && !this.alias.contains(a) )
				this.alias.add(a);
		}
	}
	
	/**
	 * Returns the string representation of this parameter. 
	 * @return	The first alias, or "undefined" if no alias is defined
	 * @see #getAlias()
	 */
	public String toString()
	{
		if( this.alias.size() == 0 )
			return "undefined";
		else
			return this.getAlias();
	}
}