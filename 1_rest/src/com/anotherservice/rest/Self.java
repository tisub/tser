package com.anotherservice.rest;

import com.anotherservice.rest.security.*;
import com.anotherservice.rest.core.*;
import com.anotherservice.util.*;
import java.util.*;

public class Self extends InitializerOnce
{
	private static final int ERROR_CLASS_ID = 0x10000;
	
	static
	{
		new Self().initialize();
	}
	
	public void initialize()
	{
		if( Handler.hasHandler("/self") )
			return;

		Index index = new Index();
		index.addMapping(new String[] { "self", "own", "me", "myself" });
		index.description = "This is a privilegied shortcut for using other methods with the user parameter set to the current user.";
		Handler.addHandler("/", index);
	}
	
	public static void selfize(String forward)
	{
		selfize(forward, null, null, null);
	}
	
	public static void selfize(String forward, String userParameterName)
	{
		selfize(forward, userParameterName, null, null);
	}
	
	public static void selfize(String forward, String userParameterName, String[] requiredGrants)
	{
		selfize(forward, userParameterName, requiredGrants, null);
	}
	
	public static void selfize(String forward, String userParameterName, String[] requiredGrants, String[] givenGrants)
	{
		Handler h = Handler.getHandler(forward);
		
		if( h == null || !(h instanceof Action) )
		{
			Logger.severe("Impossible to add a SELF mapping for " + forward);
			return;
		}
		
		if( userParameterName == null )
			userParameterName = "user";
		
		Action destination = (Action) h;
		if( !destination.hasParameter(userParameterName) )
			throw new RestException("The target action does not have a parameter '" + userParameterName + "' and cannot be selfized", 
				RestException.CAUSE_CONFIG | RestException.NOT_RECOVERABLE | RestException.DUE_TO_ROUTING | ERROR_CLASS_ID | 0x1 );
		
		// set the mapping, description, return description
		Action me = new SelfForward(userParameterName, givenGrants, destination);
		me.addMapping(destination.getMappings());
		me.description = destination.getDescription();
		me.returnDescription = destination.getReturnDescription();
		
		// set the required grants
		if( requiredGrants == null )
		{
			ArrayList<String> grants = new ArrayList<String>();
			for( String g : destination.getGrants() )
				grants.add("SELF_" + g);
			me.addGrant(grants);
		}
		else
			me.addGrant(requiredGrants);
			
		// set the parameters
		for( Parameter p : destination.getParameters() )
			if( !p.hasAlias(userParameterName) )
				me.addParameter(p);
		
		String path = "/self" + Handler.getPath(destination).replaceFirst("/[^/]+/?$", "/");
		Handler.addHandler(path, me);
	}
	
	private static class SelfForward extends Action
	{
		Action destination = null;
		String[] givenGrants = null;
		String userParameterName = null;
		
		public void checkAuth() throws Exception
		{
			try
			{
				// try our auth with SELF_*
				super.checkAuth();
			}
			catch(Exception e)
			{
				// if it fails, try the real Action's auth
				this.destination.checkAuth();
			}
		}
		
		public SelfForward(String userParameterName, String[] givenGrants, Action destination)
		{
			this.userParameterName = userParameterName;
			this.givenGrants = givenGrants;
			this.destination = destination;
		}
		
		public Object execute() throws Exception
		{
			// 1) unset all values for the user parameter and its aliases
			Request.clearParam(destination.getParameter(userParameterName).getAliases());
			
			// 2) re-set the user parameter with the current user id
			Request.addParam(userParameterName, "" + Security.getInstance().userId());
			
			// 3) give the grants
			Collection<String> t = (Collection<String>) Servlet.tmp.get().get("com.anotherservice.rest.model.securityTmpGrants");
			if( t == null )
			{
				t = new ArrayList<String>();
				Servlet.tmp.get().put("com.anotherservice.rest.model.securityTmpGrants", t);
			}
			
			if( givenGrants == null )
			{
				// grab all destination's grants
				t.addAll(destination.getGrants());
			}
			else
			{
				for( String g : I.iterable(givenGrants) )
					t.add(g);
			}
				
			// 4) forward to destination
			return Router.forward(Handler.getPath(destination));
			
			// 5) return no result
			//return null;
		}
	}
}