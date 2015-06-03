package com.anotherservice.rest;

import com.anotherservice.util.*;
import com.anotherservice.rest.core.*;
import java.util.*;

/**
 * This is the basic Index handler class for the REST service.
 * This class provides a default <code>execute()</code> method as well as 
 * handling handler hooks and mappings.
 * Extend this class if you wish to implement custom handling of the <code>execute()</code> method.
 *
 * @note	This class wll be instanciated for the ROOT handler and for non-existing intermediate handlers when adding a new one
 * @author  Simon Uyttendaele
 */
public class Index extends Handler
{
	private static final int ERROR_CLASS_ID = 0x5000;
	
	/**
	 * Default constructor
	 */
	public Index()
	{
	}
	
	/**
	 * Constructor with a mapping and a description
	 */
	public Index(String mapping, String description)
	{
		this.addMapping(mapping);
		this.description = description;
	}
	
	/**
	 * Constructor with a mapping, a description and a list of sub-handlers
	 */
	public Index(String mapping, String description, Collection<Handler> handlers)
	{
		this.addMapping(mapping);
		this.description = description;
		
		if( handlers != null && handlers.size() > 0 )
			for( Handler h : handlers )
				if( h != null )
					this.addOwnHandler(h);
	}
	
	/**
	 * Constructor with a mapping, a description and a list of sub-handlers
	 */
	public Index(String mapping, String description, Handler[] handlers)
	{
		this.addMapping(mapping);
		this.setDescription(description);
		
		if( handlers != null && handlers.length > 0 )
			for( Handler h : I.iterable(handlers) )
				if( h != null )
					this.addOwnHandler(h);
	}
	
	//=====================================
	// (UN)REGISTER
	//=====================================
	
	/**
	 * Registers the current ClassLoader context for this handler instance
	 */
	synchronized private static void register(Handler handler)
	{
		if( !Context.registered(handler) )
		{
			Logger.fine("Registering context for " + handler);
			Context.add(handler);
		}
		else
			throw new RestException("This handler instance is already registered", 
				RestException.NOT_RECOVERABLE | RestException.CAUSE_CONFIG | RestException.DUE_TO_ROUTING | ERROR_CLASS_ID | 0x1);
	}
	
	/**
	 * Unregisters the ClassLoader context of this handler instance
	 */
	synchronized private static void unregister(Handler handler)
	{
		if( Context.registered(handler) )
		{
			Logger.fine("Unregistering context for " + handler);
			Context.remove(handler);
		}
	}
	
	//=====================================
	// HANDLERS
	//=====================================
	
	private ArrayList<Handler> handlers = new ArrayList<Handler>();
	
	/**
	 * Hooks a handler to this index.
	 *
	 * @note	<ul><li>that a handler instance may be added to <strong>only one</strong> index. If trying to add a same instance 
	 * to multiple locations will throw an exception. In order to relocate a handler, first remove it from its actual parent</li>
	 * <li>if multiple handlers have the same mapping, the first one found will be targetted (for removal, for execution,...)</li>
	 * <li><code>null</code> handler is silently ignored</li></ul>
	 * @param	handler	The target handler to append to this index. The path to this handler is the path to this index "/" the mapping of the handler.
	 * @throws	RuntimeException	if the target handler is already hooked to any other index
	 */
	public final void addOwnHandler(Handler handler)
	{
		if( handler == null )
			return;
		
		register(handler);
		this.handlers.add(handler);
		Logger.finest("Adding handler " + handler + " to " + this);
	}
	
	/**
	 * Gets the current registered handlers in this index.
	 *
	 * @note	the list of handlers returned is a shallow copy, so adding/removing elements against that list 
	 * will not affect the index. However, modifying the handlers themselves will be repercuted. If you wish 
	 * to add or remove handlers from this index, you <strong>should</strong> use the <code>addOwnHandler()</code>
	 * and <code>removeOwnHandler()</code> methods.
	 * @return	a collection of Handlers registered in this index
	 */
	public final Collection<Handler> getOwnHandlers()
	{
		return new ArrayList<Handler>(this.handlers);
	}
	
	/**
	 * Gets the first handler for this mapping currently registered in this index.
	 *
	 * @param	mapping	the mapping of the handler
	 * @return	the first Handler in this index that has a matching mapping. <code>null</code> is returned if none matches
	 */
	public final Handler getOwnHandler(String mapping)
	{
		if( mapping == null )
			return null;

		for( Handler h : this.handlers )
			if( h.hasMapping(mapping) )
				return h;
		
		return null;
	}
	
	/**
	 * Gets the first handler for any of those mappings currently registered in this index.
	 *
	 * @param	mappings	the list of mappings to check against
	 * @return	the first Handler in this index that matches any of the provided mappings. <code>null</code> is returned if none matches
	 */
	public final Handler getOwnHandler(Collection<String> mappings)
	{
		if( mappings == null || mappings.size() == 0 )
			return null;

		Handler h;
		for( String mapping : mappings )
		{
			h = this.getOwnHandler(mapping);
			if( h != null )
				return h;
		}
		
		return null;
	}
	
	/**
	 * Gets the first handler for any of those mappings currently registered in this index.
	 *
	 * @param	mappings	the list of mappings to check against
	 * @return	the first Handler in this index that matches any of the provided mappings. <code>null</code> is returned if none matches
	 */
	public final Handler getOwnHandler(String[] mappings)
	{
		if( mappings == null || mappings.length == 0 )
			return null;

		Handler h;
		for( String mapping : I.iterable(mappings) )
		{
			h = this.getOwnHandler(mapping);
			if( h != null )
				return h;
		}
		
		return null;
	}
	
	/**
	 * Checks whether or not a handler is registered in this index with the provided mapping.
	 *
	 * @param	mapping	the mapping of the handler
	 * @return	<code>true</code> if a handler is registered with this mapping in this index. <code>false</code> otherwise
	 */
	public final boolean hasOwnHandler(String mapping)
	{
		if( mapping == null )
			return false;

		for( Handler h : this.handlers )
			if( h.hasMapping(mapping) )
				return true;
		
		return false;
	}
	
	/**
	 * Checks whether or not a handler is registered in this index with any of the provided mappings.
	 *
	 * @param	mappings	the list of mappings to check against
	 * @return	<code>true</code> if a handler is registered with any of those mappings in this index. <code>false</code> otherwise
	 */
	public final boolean hasOwnHandler(Collection<String> mappings)
	{
		if( mappings == null || mappings.size() == 0 )
			return false;

		for( String mapping : mappings )
			if( this.hasOwnHandler(mapping) )
				return true;
		
		return false;
	}
	
	/**
	 * Checks whether or not a handler is registered in this index with any of the provided mappings.
	 *
	 * @param	mappings	the list of mappings to check against
	 * @return	<code>true</code> if a handler is registered with any of those mappings in this index. <code>false</code> otherwise
	 */
	public final boolean hasOwnHandler(String[] mappings)
	{
		if( mappings == null || mappings.length == 0 )
			return false;

		for( String mapping : I.iterable(mappings) )
			if( this.hasOwnHandler(mapping) )
				return true;
		
		return false;
	}
	
	/**
	 * Checks whether or not the provided handler instance is registered in this index
	 *
	 * @param	handler	the handler to check
	 * @return	<code>true</code> if the provided handler is registered in this index. <code>false</code> otherwise
	 */
	public final boolean hasOwnHandler(Handler handler)
	{
		if( handler == null )
			return false;

		return this.handlers.contains(handler);
	}
	
	/**
	 * Removes the matching handler from this index.
	 * 
	 * @note	<ul><li>if no handler is registered with this mapping, nothing happens</li>
	 * <li>removing an index handler <strong>does not</strong> remove its own handlers recursively</li></ul>
	 * @param	mapping	the mapping of the handler to remove
	 */
	public final void removeOwnHandler(String mapping)
	{
		if( mapping == null )
			return;

		Handler h = getOwnHandler(mapping);
		if( h != null )
			removeOwnHandler(h);
	}
	
	/**
	 * Removes all the matching handlers from this index.
	 * 
	 * @note	<ul><li>if a handler is not found for some mappings, nothing happens</li>
	 * <li>removing an index handler <strong>does not</strong> remove its own handlers recursively</li>
	 * <li><strong>important :</strong> if several handlers registered in this index have the same mapping, only the first one is removed</li></ul>
	 * @param	mappings	the list of mappings to remove
	 */
	public final void removeOwnHandler(Collection<String> mappings)
	{
		if( mappings == null || mappings.size() == 0 )
			return;

		Handler h = getOwnHandler(mappings);
		if( h != null )
			removeOwnHandler(h);
	}
	
	/**
	 * Removes all the matching handlers from this index.
	 * 
	 * @note	<ul><li>if a handler is not found for some mappings, nothing happens</li>
	 * <li>removing an index handler <strong>does not</strong> remove its own handlers recursively</li>
	 * <li><strong>important :</strong> if several handlers registered in this index have the same mapping, only the first one is removed</li></ul>
	 * @param	mappings	the list of mappings to remove
	 */
	public final void removeOwnHandler(String[] mappings)
	{
		if( mappings == null || mappings.length == 0 )
			return;

		Handler h = getOwnHandler(mappings);
		if( h != null )
			removeOwnHandler(h);
	}
	
	/**
	 * Removes the target handler from this index.
	 * 
	 * @note	<ul><li>if the handler is not registered in this index, nothing happens</li>
	 * <li>removing an index handler <strong>does not</strong> remove its own handlers recursively</li></ul>
	 * @param	handler	the handler to remove
	 */
	public final void removeOwnHandler(Handler handler)
	{
		if( handler == null )
			return;

		this.handlers.remove(handler);
		Logger.finest("Removing handler " + handler + " to " + this);
		unregister(handler);
	}
	
	/**
	 * Removes all handlers from this index.
	 *
	 * @note	removing an index handler <strong>does not</strong> remove its own handlers recursively
	 */
	public final void removeAllOwnHandlers()
	{
		while( this.handlers.size() > 0 )
			removeOwnHandler(this.handlers.get(0));
	}
	
	/**
	 * Removes all handlers from this index.
	 * @see #removeAllOwnHandlers
	 */
	public final void clear()
	{
		removeAllOwnHandlers();
	}
	
	/**
	 * Removes all handlers from this index recursively.
	 */
	public final void removeAllOwnHandlersRecursively()
	{
		while( this.handlers.size() > 0 )
		{
			Handler h = this.handlers.get(0);
			removeOwnHandler(h);
			
			if( h instanceof Index )
				((Index)h).removeAllOwnHandlersRecursively();
		}
	}
	
	//=====================================
	// EXECUTE
	//=====================================
	
	/**
	 * Executes this handler. Default implementation returns the help for this handler. Extend this class for custom behavior.
	 */
	public Object execute() throws Exception
	{
		// send help by default
		return this;
	}
	
	public abstract class UTF8Handler extends Handler
	{
		protected String name;
		protected Handler encoder;
		
		public UTF8Handler(String name)
		{
			this.name = name;
		}
		
		protected abstract boolean decodeCharset();
		
		public Object execute() throws Exception
		{
			encoder = this;
			return decodeCharset();
		}
		
		public final void apply() { try { this.execute(); } catch(Exception e) { } }
	}
}