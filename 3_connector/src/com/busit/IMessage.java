package com.busit;

import java.util.*;
import java.io.InputStream;

/**
 * This interface defines the basic public properties of a <em>message</em> that wraps data accross {@link com.busit.ConnectorHelper connectors}.
 * All messages are encrypted and digitally signed, this interface voluntarily does not provide control over it.
 * The reason is : you cannot encrypt or sign a message on behalf of anyone because you are not a trusted authority.
 */
public interface IMessage
{
	/**
	 * Get a file attached to this message.
	 * @param	name	the file name
	 * @return	the file content or <code>null</code> if there is no matching file name
	 */
	public InputStream file(String name);
	
	/**
	 * Add or replace a file as attachment to this message
	 * @param	name	the file name
	 * @param	data	the file data
	 */
	public void file(String name, InputStream data);
	
	/**
	 * List all files attached to this message
	 * @return	the list of files as a name/content tuple
	 */
	public Map<String, InputStream> files();
	
	/**
	 * Get the message content
	 * @return	the message content, this is never <code>null</code>
	 */
	public IContent content();
	
	/**
	 * Set the message content
	 * @param	data	the message content. If <code>null</code>, it is converted to an empty content
	 */
	public void content(IContent data);
	
	/**
	 * Removes all files and sets an empty content
	 */
	public void clear();
	
	/**
	 * Returns a <strong>shallow</strong> copy of this message.
	 * The content will be deep copied, but the files content will be referenced and not copied.
	 * @return	the message copy
	 */
	public IMessage copy();
	
	/**
	 * Get the identity of the sender of this message
	 * @return	the sender identity principal (DN)
	 */
	public String from();
	
	/**
	 * Get the identity of the receiver of this message
	 * @return the receiver identity principal (DN)
	 */
	public String to();
}