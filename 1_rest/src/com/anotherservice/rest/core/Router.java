package com.anotherservice.rest.core;

import com.anotherservice.rest.*;
import java.util.*;
import java.util.regex.*;
import com.anotherservice.util.*;

/**
 * This class loactes the proper {@link com.anotherservice.rest.Handler} for the REST service and <code>execute()</code> it.<br />
 * It relies on the {@link com.anotherservice.util.Config} key : <em>com.anotherservice.rest.core.helpActionPath</em> (if not set, <em>"help"</em> is used as value).
 * @see com.anotherservice.rest.core.Request
 * @author  Simon Uyttendaele
 */
public class Router
{
	private static final int ERROR_CLASS_ID = 0x9000;
	private Router() { }
	
	/**
	 * Forwards the request to a new <code>Handler</code>. 
	 * This method calls {@link #forward(String, boolean)} with <em>alias</em> set to <code>false</code>.
	 * @param	path	the <code>Handler</code> mapping
	 */
	public static Object forward(String path) throws Exception { return forward(path, false); }
	
	/**
	 * Forwards the request to a new <code>Handler</code>. 
	 * If the path may be absolute or forward-relative to the current <code>Handler</code> (no "../")
	 * @param	path	the <code>Handler</code> mapping
	 * @param	alias	if <code>true</code>, the last traversed path fragment is discarded
	 * @return the response
	 */
	public static Object forward(String path, boolean alias) throws Exception
	{
		if( path.charAt(0) == '/' )
			Request.resetPath();
		else if( alias )
			Request.popPath();
		
		String[] actions = path.split("\\s*(,|;|\\s|/)\\s*");
		for( int i = actions.length-1; i >= 0; i-- )
			if( actions[i] != null && actions[i].length() > 0 )
				Request.addAction(actions[i], false);
		
		Logger.fine("Forwarding request to " + path);
		return route();
	}
	
	/**
	 * Finds and executes the proper <code>Handler</code>.
	 * <br /><span style="color: red; font-weight: bold; font-size: 1.2em;">You should not need to call 
	 * this method manually. The {@link com.anotherservice.rest.core.Servlet} does it for you</span><br />
	 * Based on the <code>Request</code> information, the <code>Router</code> traverses the
	 * handlers starting at {@link com.anotherservice.rest.Handler#getRoot()} and follows the mapping chain
	 * until no further <code>Handler</code> is found.
	 * Then, the {@link com.anotherservice.rest.Handler#execute()} method is called.
	 * <p>If the path encounters the <em>helpActionPath</em> keyword defined in the {@link com.anotherservice.util.Config} the <em>help</em> for the nearest instance is
	 * directly sent as a response without calling the execute method and without continuing the routing loop.
	 * <br />If the execute method returns a <code>Handler</code> instance, then the <em>help</em> for that instance will be sent as a response.
	 * <br />If the execute method throws or returns an exception, then it will be processed and sent as a response.</p>
	 * @return the response
	 * @throws Exception	if the <code>Handler</code> is not an {@link com.anotherservice.rest.Index} or {@link com.anotherservice.rest.Action}
	 * or if the current path is inconsistent (i.e. after a <code>forward()</code>)
	 */
	public static Object route() throws Exception
	{
		Logger.fine("Routing to " + Request.getCurrentURI());
		
		Index index = Handler.getRoot();
		Collection<String> path = Request.getPathParts();
		String helpPath = Config.gets("com.anotherservice.rest.core.helpActionPath");
		if( helpPath == null )
			helpPath = "help";

		// first, start from where we are
		for( String mapping : path )
		{
			Handler handler = index.getOwnHandler(mapping);
			if( !(handler instanceof Index) )
				throw new RestException("Routing path inconsistency", 
					RestException.CAUSE_RUNTIME | RestException.DUE_TO_ROUTING | ERROR_CLASS_ID | 0x1);

			index = (Index) handler;
		}
		
		// load specific config if any
		Collection<String> parts = Request.getActionParts();
		if( parts.size() > 0 )
		{
			Matcher m = Pattern.compile("^([\\w\\.]+):(.*)$").matcher(parts.iterator().next());
			if( m.matches() )
			{
				Config.get(Servlet.tmp.get(), m.group(1), new ArrayList<String>(Arrays.asList(new String[]{m.group(2)})));
				Request.skipAction(1);
			}
		}
		
		// then find the next action
		while( Request.hasAction() )
		{
			String currentPath = Request.getCurrentPath();
			String action = Request.getAction(false, false); // do not pop the action until we have confirmed it exists
			
			Logger.finest(" -- ROUTING : " + currentPath + " -> " + action);

			if( action != null && action.equalsIgnoreCase(helpPath) )
			{
				Logger.fine("Routing to help of " + index);
				index.checkAuth();
				return index;
			}
			
			if( index.hasOwnHandler(action) )
			{
				// now truly pop the action since we know it exists
				Request.getAction();
				
				Handler handler = index.getOwnHandler(action);
				if( handler instanceof Action )
				{
					// check for help here as well
					String next = Request.getAction(false, false);
					if( next != null && next.equalsIgnoreCase(helpPath) )
					{
						Logger.finest(" -- ROUTING : " + currentPath + "/" + action + " -> " + next);
						Logger.fine("Routing to help of " + handler);
						handler.checkAuth();
						return handler;
					}
					else
					{
						Logger.fine("Routing to action handler of " + Request.getCurrentPath());
						return handler.wrapExecute();
					}
				}
				else if( handler instanceof Index )
					index = (Index) handler;
				else
				{
					Logger.severe("Invalid handler type. Found '" + handler.getClass().getCanonicalName() + "' for '" + handler + "'");
					throw new RestException("Invalid handler type", 
						RestException.CAUSE_RUNTIME | RestException.DUE_TO_ROUTING | ERROR_CLASS_ID | 0x2);
				}
			}
			else
			{
				Logger.fine("No handler registered for '" + action + "'. Routing to index handler of " + currentPath);
				return index.wrapExecute();
			}
		}
		
		if( index == Handler.getRoot() )
		{
			Logger.fine("Routing to root " + Request.getCurrentPath());
			return index.wrapExecute();
		}
		else
		{
			Logger.fine("Routing to index handler of " + Request.getCurrentPath());
			return index.wrapExecute();
		}
	}
}