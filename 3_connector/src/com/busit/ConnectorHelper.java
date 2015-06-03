package com.busit;

import java.util.*;

public interface ConnectorHelper
{
	/**
	 * Reports errors or warnings to the connector owner.
	 * @param	message	the message to report to the user (consider the current {@link #locale()})
	 * @param	data	any additionnal stuff that might help troubleshooting
	 */
	public void notifyOwner(String message, Map<String, Object> data);
	
	/**
	 * Reports errors or warnings to the end user.
	 * @param	message	the message to report to the user (consider the current {@link #locale()})
	 */
	public void notifyUser(String message);
	
	/** 
	 * Returns the value of the connector config parameter for the provided key.
	 * @param	key	the config key to retrieve
	 * @return	the associated value or <code>null</code> if the key does not exist
	 */
	public String config(String key);
	
	/** 
	 * Returns all configuration parameters.
	 * @return	all configuration parameters
	 */
	public Map<String, String> config();
	
	/**
	 * Returns the output interface that matches the provided <em>key</em> (not <em>name</em>).
	 * @param	key 	the output interface key
	 * @return	the matching interface or <code>null</code> if the key does not exist
	 */
	public Interface output(String key);
	
	/**
	 * Returns all output interfaces.
	 * @return	all output interface
	 */
	public List<Interface> output();
	
	/**
	 * Returns the input interface that matches the provided <em>key</em> (not <em>name</em>).
	 * @param	key 	the input interface key
	 * @return	the matching interface or <code>null</code> if the key does not exist
	 */
	public Interface input(String key);
	
	/**
	 * Returns all input interfaces.
	 * @return	all input interfaces
	 */
	public List<Interface> input();
	
	/**
	 * Returns the end user's prefered language.
	 * @return the 2 letters ISO639-1 language code
	 * @note See: http://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
	 */
	public String locale();
	
	/**
	 * Returns a unique instance identifier.
	 * The instance identifier will always be the same fo the same user instance of the connector.
	 * No different instances of any connector will ever have the same identifier.
	 */
	public String id();
}