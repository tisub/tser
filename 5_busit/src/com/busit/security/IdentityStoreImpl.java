package com.busit.security;

import java.io.InputStream;
import com.busit.security.*;
import java.util.regex.*;
import com.anotherservice.util.*;
import com.anotherservice.io.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.db.*;
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
			InputStream key = getResourceAsStream(Config.gets("com.busit.rest.authority.privateKeyFile"));
			Config.set("com.busit.rest.authority.privateKeyFile", "");
			String priv = FileReader.readFile(key);
			key.close();
			
			key = getResourceAsStream(Config.gets("com.busit.rest.authority.certificateFile"));
			Config.set("com.busit.rest.authority.certificateFile", "");
			String cert = FileReader.readFile(key);
			key.close();
			
			global.put("authority", new Identity(priv, cert));
			Logger.config("Initialized Identity Store with authority principal : " + global.get("authority").getSubjectPrincipal().getName());
			
			key = getResourceAsStream(Config.gets("com.busit.rest.authentic.privateKeyFile"));
			Config.set("com.busit.rest.authentic.privateKeyFile", "");
			priv = FileReader.readFile(key);
			key.close();
			
			key = getResourceAsStream(Config.gets("com.busit.rest.authentic.certificateFile"));
			Config.set("com.busit.rest.authentic.certificateFile", "");
			cert = FileReader.readFile(key);
			key.close();
			
			global.put("authentic", new Identity(priv, cert));
			Logger.config("Initialized Identity Store with authentic principal : " + global.get("authentic").getSubjectPrincipal().getName());
			
			key = getResourceAsStream(Config.gets("com.busit.rest.unchecked.privateKeyFile"));
			Config.set("com.busit.rest.unchecked.privateKeyFile", "");
			priv = FileReader.readFile(key);
			key.close();
			
			key = getResourceAsStream(Config.gets("com.busit.rest.unchecked.certificateFile"));
			Config.set("com.busit.rest.unchecked.certificateFile", "");
			cert = FileReader.readFile(key);
			key.close();
			
			global.put("unchecked", new Identity(priv, cert));
			Logger.config("Initialized Identity Store with unchecked principal : " + global.get("unchecked").getSubjectPrincipal().getName());
		}
		catch(Exception e)
		{
			throw new RuntimeException("Could not initialize authority/everyone key pair", e);
		}
	}
	
	private static InputStream getResourceAsStream(String name)
	{
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
	}
	
	// =========================================
	// IIdentityStore
	// =========================================
	public Identity getIdentity(Principal principal)
	{
		if( principal == null )
			return null;
			
		if( getUnchecked().equals(principal) )
			return getUnchecked();
		if( getAuthentic().equals(principal) )
			return getAuthentic();
		if( getAuthority().equals(principal) )
			return getAuthority();
		
		if( PrincipalUtil.isLocal(principal) )
			return loadLocal(principal);
		else
			return loadRemote(principal);
	}
	
	public Identity getIdentity(String anything)
	{
		if( anything == null || anything.length() == 0 )
			return null;

		if( getUnchecked().equals(anything) )
			return getUnchecked();
		if( getAuthentic().equals(anything) )
			return getAuthentic();
		if( getAuthority().equals(anything) )
			return getAuthority();

		if( PrincipalUtil.isPrincipal(anything) )
			return getIdentity(PrincipalUtil.parse(anything));
		else
			return loadLocal(anything, null);
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
		//throw new RuntimeException("Loading remote identities is not supported at this time");
		// TODO
		Logger.finest("Loading remote identity : " + name);
		try { throw new Exception("Remote loading is not allowed at this time"); } catch(Exception e) { Logger.finest(e); }
		return null;
	}
	
	private Identity loadLocal(Principal principal)
	{
		return loadLocal(PrincipalUtil.shortName(principal), PrincipalUtil.orgName(principal));
	}
	
	private Identity loadLocal(String identity, String org)
	{
		try
		{
			Logger.finest("Loading local identity : " + identity + (org != null ? " of org " + org : ""));
			
			String where = "";
			if( identity.matches("^[0-9]+$") )
				where = " identity_id = " + identity;
			else
				where = " identity_principal = '" + Security.escape(PrincipalUtil.parse(identity).getName()) + "'";
			
			if( org != null )
			{
				if( identity.matches("^[0-9]+$") )
					where += " AND u.user_org AND u.user_id = " + org;
				else
					where += " AND u.user_org AND u.user_name = '" + Security.escape(org) + "'";
			}

			String sql = "SELECT i.identity_id, i.identity_key_public, i.identity_key_private, i.identity_principal " + 
				"FROM identities i " + 
				"LEFT JOIN users u ON (u.user_id = i.identity_user) " + 
				"WHERE " + where;

			Map<String,String> r = Database.getInstance().selectOne(sql);
			if( r == null || r.get("identity_id") == null || r.get("identity_key_private") == null )
				throw new IllegalArgumentException("Invalid identity");
			
			// private keys are crypted with the authority identity so we must decrypt it
			String privatekey = Crypto.toString(Crypto.parsePrivateKey(Crypto.decrypt(getAuthority(), r.get("identity_key_private"))));
			
			return new Identity(privatekey, r.get("identity_key_public"));
		}
		catch(Exception e)
		{
			Logger.finer(e);
			return null;
		}
	}
}