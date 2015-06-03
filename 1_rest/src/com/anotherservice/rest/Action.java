package com.anotherservice.rest;

import com.anotherservice.util.*;
import java.util.*;

/**
 * This is the basic Action handler class for the REST service.
 * This class provides methods for handling parameters but does not implement the <code>execute()</code> method.
 * Extend this class in order to implement custom handling of the <code>execute()</code> method.
 * <p><br />
 * See the {@link com.anotherservice.rest.Parameter} class for information on how to use parameters<br />
 * See the {@link com.anotherservice.rest} package description for sample code</p>
 * @author  Simon Uyttendaele
 */
public abstract class Action extends Handler
{
	/**
	 * Default constructor
	 */
	public Action()
	{
	}
	
	/**
	 * Constructor with a name, a description and a return description
	 */
	public Action(String name, String description, String returnDescription)
	{
		this.addMapping(name);
		this.description = description;
		this.returnDescription = returnDescription;
	}
	
	/**
	 * Constructor with a name, a description, a return description and a list of supported parameters
	 */
	public Action(String name, String description, String returnDescription, Collection<Parameter> parameters)
	{
		this.addMapping(name);
		this.description = description;
		this.returnDescription = returnDescription;
		
		if( parameters != null && parameters.size() > 0 )
			for( Parameter p : parameters )
				if( p != null )
					this.addParameter(p);
	}
	
	/**
	 * Constructor with a name, a description, a return description and a list of supported parameters
	 */
	public Action(String name, String description, String returnDescription, Parameter[] parameters)
	{
		this.addMapping(name);
		this.description = description;
		this.returnDescription = returnDescription;
		
		if( parameters != null && parameters.length > 0 )
			for( Parameter p : I.iterable(parameters) )
				if( p != null )
					this.addParameter(p);
	}
	
	//=====================================
	// RETURNS
	//=====================================
	
	/**
	 * The description of the return value of this action.
	 * Used when requesting the help of this action.
	 */
	public String returnDescription = "";
	
	/**
	 * Gets the description of the return value of this action.
	 * @return	The action's return value description
	 */
	public String getReturnDescription()
	{
		return this.returnDescription;
	}
	
	/**
	 * Sets the description of the return value of this action.
	 * @param	returnDescription	The action's return value description
	 */
	public void setReturnDescription(String returnDescription)
	{
		this.returnDescription = returnDescription;
	}
	
	//=====================================
	// PARAMETERS
	//=====================================
	
	private ArrayList<Parameter> parameters = new ArrayList<Parameter>();
	
	/**
	 * Adds a supported parameter to this action.
	 * @note	null parameter is silently ignored
	 * @param	parameter	The target parameter to add to this action
	 */
	public final void addParameter(Parameter parameter)
	{
		if( parameter == null )
			return;
		
		if( !this.parameters.contains(parameter) )
		{
			this.parameters.add(parameter);
			Logger.finest("Adding parameter " + parameter + " to " + this);
		}
	}
	
	/**
	 * Gets the current registered parameters in this action.
	 *
	 * @note	the list of parameters returned is a shallow copy, so adding/removing elements against that list 
	 * will not affect the action. However, modifying the parameters themselves will be repercuted. If you wish 
	 * to add parameters to this action, you <strong>should</strong> use the <code>addParameter()</code> and 
	 * <code>removeParameter()</code> methods.
	 * @return	a collection of Parameters registered in this action
	 */
	public final Collection<Parameter> getParameters()
	{
		return new ArrayList<Parameter>(this.parameters);
	}
	
	/**
	 * Gets the first parameter for this alias currently registered in this action.
	 *
	 * @param	alias	the parameter alias
	 * @return	the first Parameter in this action that has a matching alias. <code>null</code> is returned if none matches
	 */
	public final Parameter getParameter(String alias)
	{
		if( alias == null )
			return null;

		for( Parameter p : this.parameters )
			if( p.hasAlias(alias) )
				return p;
		
		return null;
	}
	
	/**
	 * Gets the first parameter for any of those aliases currently registered in this action.
	 *
	 * @param	aliases	the list of aliases to check against
	 * @return	the first Parameter in this action that has a matching alias. <code>null</code> is returned if none matches
	 */
	public final Parameter getParameter(Collection<String> aliases)
	{
		if( aliases == null || aliases.size() == 0 )
			return null;

		Parameter p;
		for( String alias : aliases )
		{
			if( alias == null )
				continue;

			p = this.getParameter(alias);
			if( p != null )
				return p;
		}
		
		return null;
	}
	
	/**
	 * Gets the first parameter for any of those aliases currently registered in this action.
	 *
	 * @param	aliases	the list of aliases to check against
	 * @return	the first Parameter in this action that has a matching alias. <code>null</code> is returned if none matches
	 */
	public final Parameter getParameter(String[] aliases)
	{
		if( aliases == null || aliases.length == 0 )
			return null;

		Parameter p;
		for( String alias : I.iterable(aliases) )
		{
			if( alias == null )
				continue;

			p = this.getParameter(alias);
			if( p != null )
				return p;
		}
		
		return null;
	}
	
	/**
	 * Checks whether or not a parameter is registered in this action with the provided alias.
	 *
	 * @param	alias	the alias of the parameter
	 * @return	<code>true</code> if a parameter is registered with this alias in this action. <code>false</code> otherwise
	 */
	public final boolean hasParameter(String alias)
	{
		if( alias == null )
			return false;

		for( Parameter p : this.parameters )
			if( p.hasAlias(alias) )
				return true;
		
		return false;
	}
	
	/**
	 * Checks whether or not a parameter is registered in this action with any of the provided aliases.
	 *
	 * @param	aliases	the list of aliases to check against
	 * @return	<code>true</code> if a parameter is registered with any of those aliases in this action. <code>false</code> otherwise
	 */
	public final boolean hasParameter(Collection<String> aliases)
	{
		if( aliases == null || aliases.size() == 0 )
			return false;

		for( String alias : aliases )
			if( this.hasParameter(alias) )
				return true;
		
		return false;
	}
	
	/**
	 * Checks whether or not a parameter is registered in this action with any of the provided aliases.
	 *
	 * @param	aliases	the list of aliases to check against
	 * @return	<code>true</code> if a parameter is registered with any of those aliases in this action. <code>false</code> otherwise
	 */
	public final boolean hasParameter(String[] aliases)
	{
		if( aliases == null || aliases.length == 0 )
			return false;

		for( String alias : I.iterable(aliases) )
			if( this.hasParameter(alias) )
				return true;
		
		return false;
	}
	
	/**
	 * Checks whether or not the provided parameter instance is registered in this action
	 *
	 * @param	parameter	the parameter to check
	 * @return	<code>true</code> if the provided parameter is registered in this action. <code>false</code> otherwise
	 */
	public final boolean hasParameter(Parameter parameter)
	{
		if( parameter == null )
			return false;

		return this.parameters.contains(parameter);
	}
	
	/**
	 * Removes the matching parameter from this action.
	 * 
	 * @note	if no parameter is registered with this alias, nothing happens
	 * @param	alias	the alias of the parameter to remove
	 */
	public final void removeParameter(String alias)
	{
		if( alias == null )
			return;

		Parameter p = getParameter(alias);
		if( p != null )
			removeParameter(p);
	}
	
	/**
	 * Removes all the matching parameters from this action.
	 * 
	 * @note	<ul><li>if a parameter is not found for some aliases, nothing happens</li>
	 * <li><strong>important :</strong> if several parameters registered in this action have the same alias, only the first one is removed</li></ul>
	 * @param	aliases	the list of aliases to remove
	 */
	public final void removeParameter(Collection<String> aliases)
	{
		if( aliases == null || aliases.size() == 0 )
			return;

		Parameter p = getParameter(aliases);
		if( p != null )
			removeParameter(p);
	}
	
	/**
	 * Removes all the matching parameters from this action.
	 * 
	 * @note	<ul><li>if a parameter is not found for some aliases, nothing happens</li>
	 * <li><strong>important :</strong> if several parameters registered in this action have the same alias, only the first one is removed</li></ul>
	 * @param	aliases	the list of aliases to remove
	 */
	public final void removeParameter(String[] aliases)
	{
		if( aliases == null || aliases.length == 0 )
			return;

		Parameter p = getParameter(aliases);
		if( p != null )
			removeParameter(p);
	}
	
	/**
	 * Removes the target parameter from this action.
	 * 
	 * @note	if the parameter is not registered in this action, nothing happens
	 * @param	parameter	the parameter to remove
	 */
	public final void removeParameter(Parameter parameter)
	{
		if( parameter == null )
			return;

		this.parameters.remove(parameter);
		Logger.finest("Removing parameter " + parameter + " to " + this);
	}
	
	/**
	 * Removes all parameters from this action.
	 */
	public final void removeAllParameters()
	{
		while( this.parameters.size() > 0 )
			removeParameter(this.parameters.get(0));
	}
}