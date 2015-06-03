package com.anotherservice.rest;

import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import java.util.*;

/**
 * This is the abstract handler class for the REST service.
 * Do not extend this class as only the <code>Index</code> and <code>Action</code>
 * subclasses will be taken into account by the <code>Router</code>.
 * This class also provides convenient methods for handling the hyerarchy of the REST service.
 *
 * @author  Simon Uyttendaele
 * @see com.anotherservice.rest.Index
 * @see com.anotherservice.rest.Action
 */
public abstract class Handler
{
	private static final int ERROR_CLASS_ID = 0x6000;
	
	/**
	 * Returns the string representation of this handler.
	 * @return	The main mapping of this handler, or "unmapped" if no mapping is defined
	 * @see #getMapping()
	 */
	public String toString()
	{
		if( this.mappings.size() == 0 )
			return "unmapped";
		else
			return this.getMapping();
	}
	
	//=====================================
	// DESCRIPTION
	//=====================================
	
	/**
	 * The description of this handler
	 * Used when requesting the help of this handler.
	 */
	public String description = "";
	
	/**
	 * Gets the description of this handler.
	 * @return	The handler description
	 */
	public String getDescription()
	{
		return this.description;
	}
	
	/**
	 * Sets the description of this handler.
	 * @param	description	The handler description
	 */
	public void setDescription(String description)
	{
		this.description = description;
	}
	
	//=====================================
	// MAPPING
	//=====================================
	
	private Collection<String> mappings = new ArrayList<String>();
	
	/**
	 * Adds a mapping for this handler. Mappings are always <b>lowercase</b> for case insensitivity in the latter URL request.
	 * @note	null or empty mapping is silently ignored
	 * @see com.anotherservice.rest.core.Router#route()
	 * @param	mapping	The mapping to add for this handler
	 */
	public void addMapping(String mapping)
	{
		if( mapping != null && mapping.length() > 0 && !this.mappings.contains(mapping.toLowerCase()) )
			this.mappings.add(mapping.toLowerCase());
	}
	
	/**
	 * Adds all mappings to this handler.
	 * @note	null or empty mappings are silently ignored
	 * @param	mappings	The mappings to add for this handler
	 */
	public void addMapping(Collection<String> mappings)
	{
		if( mappings == null || mappings.size() == 0 )
			return;

		for( String m : mappings )
		{
			if( m != null && m.length() > 0 && !this.mappings.contains(m.toLowerCase()) )
				this.mappings.add(m.toLowerCase());
		}
	}
	
	/**
	 * Adds all mappings to this handler.
	 * @note	null or empty mappings are silently ignored
	 * @param	mappings	The mappings to add for this handler
	 */
	public void addMapping(String[] mappings)
	{
		if( mappings == null || mappings.length == 0 )
			return;

		for( String m : I.iterable(mappings) )
		{
			if( m != null && m.length() > 0 && !this.mappings.contains(m.toLowerCase()) )
				this.mappings.add(m.toLowerCase());
		}
	}
	
	/**
	 * Checks whether or not this handler has the specified mapping
	 * @param	mapping	The mapping to check
	 * @return	<code>true</code> if the handler matches this mapping, <code>false</code> otherwise
	 */
	public boolean hasMapping(String mapping)
	{
		if( mapping == null )
			return false;
		return this.mappings.contains(mapping.toLowerCase());
	}
	
	/**
	 * Checks whether or not this handler has any of the specified mappings
	 * @param	mappings	The list mapping to check
	 * @return	<code>true</code> if the handler matches any of the mappings, <code>false</code> if none matches
	 */
	public boolean hasMapping(Collection<String> mappings)
	{
		if( mappings == null || mappings.size() == 0 )
			return false;

		for( String m : mappings )
		{
			if( m == null )
				continue;
			if( this.mappings.contains(m.toLowerCase()) )
				return true;
		}
		
		return false;
	}
	
	/**
	 * Checks whether or not this handler has any of the specified mappings
	 * @param	mappings	The list mapping to check
	 * @return	<code>true</code> if the handler matches any of the mappings, <code>false</code> if none matches
	 */
	public boolean hasMapping(String[] mappings)
	{
		if( mappings == null || mappings.length == 0 )
			return false;

		for( String m : I.iterable(mappings) )
		{
			if( m == null )
				continue;
			if( this.mappings.contains(m.toLowerCase()) )
				return true;
		}
		
		return false;
	}
	
	/**
	 * Returns the first (main) mapping of this handler
	 * @return	The first mapping of this handler
	 */
	public String getMapping()
	{
		for( String m : this.mappings )
			return m;
		return null;
	}
	
	/**
	 * Returns all mappings of this handler
	 * @return	The list of mappings of this handler
	 */
	public Collection<String> getMappings()
	{
		return this.mappings;
	}
	
	//=====================================
	// GRANTS
	//=====================================
	
	private Collection<String> grants = new ArrayList<String>();
	
	/**
	 * Adds a required access grant for this handler. Grants are always <b>lowercase</b>.
	 * @note	<ul><li>A <code>Security</code> provider must be defined in order to use grants</li>
	 * <li><code>null</code> or empty grant is silently ignored</li></ul>
	 * @param	grant	The grant to add to this handler
	 * @see com.anotherservice.rest.security.Security
	 * @see com.anotherservice.rest.security.ISecurity
	 */
	public void addGrant(String grant)
	{
		if( grant != null && grant.length() > 0 && !this.grants.contains(grant.toLowerCase()) )
			this.grants.add(grant.toLowerCase());
	}
	
	/**
	 * Adds all grants to this handler
	 * @note	null or empty grants are silently ignored
	 * @param	grants	The grants to add to this handler
	 */
	public void addGrant(Collection<String> grants)
	{
		if( grants == null || grants.size() == 0 )
			return;

		for( String g : grants )
		{
			if( g != null && g.length() > 0 && !this.grants.contains(g.toLowerCase()) )
				this.grants.add(g.toLowerCase());
		}
	}
	
	/**
	 * Adds all grants to this handler
	 * @note	null or empty grants are silently ignored
	 * @param	grants	The grants to add to this handler
	 */
	public void addGrant(String[] grants)
	{
		if( grants == null || grants.length == 0 )
			return;

		for( String g : I.iterable(grants) )
		{
			if( g != null && g.length() > 0 && !this.grants.contains(g.toLowerCase()) )
				this.grants.add(g.toLowerCase());
		}
	}
	
	/**
	 * Returns the list of required grants of this handler
	 * @return The list of grants of this handler
	 */
	public Collection<String> getGrants()
	{
		return this.grants;
	}
	
	//=====================================
	// EXECUTE
	//=====================================
	
	/**
	 * Changes the ClassLoader context to the one registered using the <code>register()</code> method,
	 * then calls the intercal <code>checkAuth()</code> method,
	 * then calls the internal <code>execute()</code> method.
	 * @return The result of the execute method
	 * @see #execute()
	 * @see #checkAuth()
	 */
	public final Object wrapExecute() throws Exception
	{
		Context.wrap(this);
		checkAuth();
		return execute();
	}
	
	/**
	 * Perform security checks.
	 * Default behavior is to call {@link com.anotherservice.rest.security.ISecurity#hasGrants(Collection)} with this instance's {@link #getGrants()}.
	 * @throws Exception if security checks do not pass.
	 * @note Overriding this method is touchy but may be required in very specific situations. 
	 * In this case, <strong>know what you do</strong> as security is not something to play with !
	 * @see com.anotherservice.rest.security.Security
	 */
	public void checkAuth() throws Exception
	{
		if( this.grants.size() == 0 )
			return;

		if( !Security.getInstance().hasGrants(this.grants) )
			throw new RestException("Unsufficient privileges", 
				RestException.CAUSE_USER | RestException.DUE_TO_SECURITY | ERROR_CLASS_ID | 0x1);
	}
	
	/**
	 * Executes this handler.
	 * Must be implemented by subclasses.
	 * @note The execute method will be called on the single registered instance of <em>this</em> handler.
	 * Thus, the execution should be repeatable, thread safe and memory proof.
	 * @return The result of the handler execution
	 */
	protected abstract Object execute() throws Exception;
	
	//=====================================
	// ROOT HANDLER + STATIC UTILS
	//=====================================
	
	private static final Index ROOT = new Index("ROOT", "Root Description");
	static
	{
		Logger.fine("Registering context for the root");
		Context.add(ROOT);
	}
	
	/**
	 * Returns the root handler.
	 * @return The root <code>Index</code> handler
	 * @see com.anotherservice.rest.Index
	 */
	public static Index getRoot()
	{
		return ROOT;
	}
	
	/**
	 * Hooks the specified handler at the end (aka: after) in the chain of handlers defined by the <code>path</code>.
	 * @param	path	The absolute path leading to the parent handler to hook to. The path is composed of chained handler mappings separated by forward slashes ("/")
	 * @param	handler	The handler to append
	 */
	public static void addHandler(String path, Handler handler)
	{
		if( path == null || handler == null )
			throw new RestException("both path and handler cannot be null", 
				RestException.CAUSE_CONFIG | RestException.DUE_TO_ROUTING | ERROR_CLASS_ID | 0x2,
				new IllegalArgumentException());

		String addPath = "";
		String[] parts = path.split("/");
		Index root = ROOT;
		for( int i = 0; i < parts.length; i++ )
		{
			if( parts[i].length() == 0 )
				continue;

			if( root.hasOwnHandler(parts[i]) )
			{
				Handler h = root.getOwnHandler(parts[i]);
				if( h instanceof Index )
					root = (Index)h;
				else
					throw new RestException("Cannot hook the provided handler because an action already exists at that attach point", 
						RestException.CAUSE_CONFIG | RestException.DUE_TO_ROUTING | ERROR_CLASS_ID | 0x3,
						new IllegalArgumentException());
			}
			else
			{
				Logger.finer("Creating missing index handler " + parts[i]);
				Index index = new Index(parts[i], "");
				root.addOwnHandler(index);
				root = index;
			}
			
			addPath += "/" + root;
		}
		
		Logger.config("Adding handler " + addPath + "/" + handler);
		root.addOwnHandler(handler);
	}
	
	/**
	 * Removes the handler represented by the last part of the chain defined by <code>path</code>.
	 * This means that the last part of the path should be one of the handler's mappings
	 * @param	path	The absolute path leading to the handler to be removed
	 */
	public static void removeHandler(String path)
	{
		if( path == null || path.length() == 0 || path.equals("/") )
			throw new RestException("path cannot be null and cannot target the root handler", 
				RestException.CAUSE_CONFIG | RestException.DUE_TO_ROUTING | ERROR_CLASS_ID | 0x4,
				new IllegalArgumentException());
			
		String[] parts = path.split("/");
		Index root = ROOT;
		for( int i = 0; i < parts.length-1; i++ )
		{
			if( parts[i].length() == 0 )
				continue;

			if( root.hasOwnHandler(parts[i]) )
			{
				Handler h = root.getOwnHandler(parts[i]);
				if( h instanceof Index )
					root = (Index)h;
				else
					throw new RestException("Cannot remove the provided handler because is does not target a valid endpoint", 
						RestException.CAUSE_CONFIG | RestException.DUE_TO_ROUTING | ERROR_CLASS_ID | 0x5,
						new IllegalArgumentException());
			}
			else
				return;
		}
		
		Logger.config("Removing handler " + path);
		root.removeOwnHandler(parts[parts.length-1]);
	}
	
	/**
	 * Checks whether or not the specified path leads to a handler.
	 * @param	path	The path to check
	 * @return <code>true</code> if the complete path leads to a valid handler. <code>false</code> otherwise
	 */
	public static boolean hasHandler(String path)
	{
		if( path == null )
			return false;

		String[] parts = path.split("/");
		Index root = ROOT;
		for( int i = 0; i < parts.length-1; i++ )
		{
			if( parts[i].length() == 0 )
				continue;

			if( root.hasOwnHandler(parts[i]) )
			{
				Handler h = root.getOwnHandler(parts[i]);
				if( h instanceof Index )
					root = (Index)h;
				else
					return false;
			}
			else
				return false;
		}
		
		return root.hasOwnHandler(parts[parts.length-1]);
	}
	
	/**
	 * Returns the handler targetted by the specified <code>path</code>.
	 * @param	path	The path to the handler
	 * @return	The handler matching that path or <code>null</code> if no handler matches
	 */
	public static Handler getHandler(String path)
	{
		if( path == null )
			return null;

		String[] parts = path.split("/");
		Index root = ROOT;
		for( int i = 0; i < parts.length-1; i++ )
		{
			if( parts[i].length() == 0 )
				continue;

			if( root.hasOwnHandler(parts[i]) )
			{
				Handler h = root.getOwnHandler(parts[i]);
				if( h instanceof Index )
					root = (Index)h;
				else
					return null;
			}
			else
				return null;
		}
		
		return root.getOwnHandler(parts[parts.length-1]);
	}
	
	/**
	 * Returns the last handler found for the specified <code>path</code>.
	 * If the path targets an existing handler, this method has the same effect as <code>getHandler()</code>.
	 * If a handler is not found at the target path, the parent handler is returned. At worst, the root handler is returned.
	 * @param	path	The path to the potential handler
	 * @return	The nearest handler in the path chain
	 */
	public static Handler getNearest(String path)
	{
		if( path == null )
			return ROOT;

		String[] parts = path.split("/");
		Index root = ROOT;
		for( int i = 0; i < parts.length-1; i++ )
		{
			if( parts[i].length() == 0 )
				continue;

			if( root.hasOwnHandler(parts[i]) )
			{
				Handler h = root.getOwnHandler(parts[i]);
				if( h instanceof Index )
					root = (Index)h;
				else
					return h;
			}
			else
				return root;
		}
		
		return root;
	}
	
	/**
	 * Returns the parent handler (index) of the specified handler.
	 * @param	h	The target handler
	 * @return	The parent of the target handler or <code>null</code> if no parent is found
	 */
	public static Index getParent(Handler h)
	{
		if( h == null )
			return null;

		return _getParent(h, ROOT);
	}
	private static Index _getParent(Handler h, Index parent)
	{
		for( Handler child : parent.getOwnHandlers() )
		{
			if( child == h )
				return parent;
			else if( child instanceof Index )
			{
				Index match = _getParent(h, (Index)child);
				if( match instanceof Index )
					return match;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the current parent handler
	 */
	public final Index parent()
	{
		return getParent(this);
	}
	
	/**
	 * Returns the main path leading to the specified handler.
	 * @note	The <em>main</em> path is the chain of all <code>getMapping()</code> leading to that handler
	 * @param	handler	The target handler
	 * @return	The absolute path to the target handler or <code>null</code> if the handler is not part of the chain or cannot be found
	 */
	public static String getPath(Handler handler)
	{
		if( handler == null )
			return null;

		return _getPath(handler, ROOT);
	}
	private static String _getPath(Handler h, Index parent)
	{
		for( Handler child : parent.getOwnHandlers() )
		{
			if( child == h )
				return (parent==ROOT?"":parent) + "/" + h;
			else if( child instanceof Index )
			{
				String path = _getPath(h, (Index)child);
				if( path != null )
					return (parent==ROOT?"":parent) + "/" + path;
			}
		}
		
		return null;
	}
}