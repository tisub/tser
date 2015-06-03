package com.busit.broker;

import java.io.InputStream;
import com.busit.security.*;
import java.util.regex.*;
import com.anotherservice.util.*;
import com.anotherservice.io.*;
import com.anotherservice.rest.security.*;
import java.util.*;
import java.lang.*;
import java.security.Principal;

public class IdentityStoreImpl implements IIdentityStore
{
	// =========================================
	// GLOBAL IDENTITIES
	// =========================================
	private Map<String, Identity> global = new Hashtable<String, Identity>();
	public Identity getAuthority() { return global.get("authority"); }
	public Identity getAuthentic() { return global.get("authentic"); }
	public Identity getUnchecked() { return global.get("unchecked"); }
	
	public IdentityStoreImpl()
	{
		try
		{
			Hashtable<String, String> params = new Hashtable<String, String>();
			params.put("id", Config.gets("com.busit.broker.authorityPrincipal"));
			params.put("binary", "false");

			String priv = RestApi.request("identity/private", params, true).<String>value("key");
			String cert = RestApi.request("identity/public", params, true).<String>value("certificate");
			
			global.put("authority", new Identity(priv, cert));
			
			params.put("id", Config.gets("com.busit.broker.authenticPrincipal"));
			params.put("binary", "false");
			priv = RestApi.request("identity/private", params, true).<String>value("key");
			cert = RestApi.request("identity/public", params, true).<String>value("certificate");
			
			global.put("authentic", new Identity(priv, cert));
			
			params.put("id", Config.gets("com.busit.broker.uncheckedPrincipal"));
			params.put("binary", "false");
			priv = RestApi.request("identity/private", params, true).<String>value("key");
			cert = RestApi.request("identity/public", params, true).<String>value("certificate");
			
			global.put("unchecked", new Identity(priv, cert));
		}
		catch(Exception e)
		{
			throw new RuntimeException("Could not initialize authority/everyone key pair", e);
		}
	}
	
	// =========================================
	// IIdentityStore
	// =========================================
	public Identity getIdentity(Principal principal)
	{
		if( principal == null )
			return null;
		
		if( PrincipalUtil.isLocal(principal) )
			return loadLocal(principal);
		else
			return loadRemote(principal);
	}
	
	public Identity getIdentity(String anything)
	{
		if( PrincipalUtil.isPrincipal(anything) )
			return getIdentity(PrincipalUtil.parse(anything));
		else
			return loadLocal(anything);
	}
	
	// =========================================
	// LOADING
	// =========================================
	private Identity loadRemote(Principal principal)
	{
		return loadRemote(principal.getName());
	}
	
	private Identity loadRemote(String name)
	{
		// TODO
		return null;
	}
	
	private Identity loadLocal(Principal principal)
	{
		return loadLocal(principal.getName());
	}
	
	private Identity loadLocal(String principal)
	{
		try
		{
			Logger.finest("Loading local identity : " + principal);
			
			Hashtable<String, String> params = new Hashtable<String, String>();
			params.put("id", principal);
			params.put("binary", "false");
			String priv = RestApi.request("identity/private", params, true).<String>value("key");
			String cert = RestApi.request("identity/public", params, true).<String>value("certificate");
			
			return new Identity(priv, cert);
		}
		catch(Exception e)
		{
			Logger.finer(e);
			return null;
		}
	}
}