package com.anotherservice.rest.model;

import java.util.*;
import java.lang.*;
import java.math.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import com.anotherservice.rest.core.*;

/**
 * This is the default implementation of <code>ISecurity</code> using the predefined model.
 * <code>null</code> or empty grants are ignored. Grant names are case insensitive.
 * <br /><br />
 * Relies on the config variable : <ul><li><em>com.anotherservice.rest.model.superadminGrantName</em> which is the 
 * <strong>name</strong> of the super admin grant that superseeds all other grants. If such grant does not exist, 
 * do not set it or leave it empty.</li><li><em>com.anotherservice.rest.model.authParameter</em> which is a list of possible
 * request parameter name in which the authentication info can be found.</li></ul>
 * <br /><br />
 * The {@link com.anotherservice.rest.core.Servlet#tmp} will be checked by the {@link #hasGrants(Collection)} method in order to
 * determine if temporarily given grants are present. To use this behavior, the <em>com.anotherservice.rest.model.securityTmpGrants</em>
 * key should be used to store a <code>Collection&lt;String&gt;</code> that contains the <strong>name</strong> of the grants that the
 * current requestor will be considered to have. Use this, for instance, to forward a request to a different handler that requires other grants
 * that the current user may not have.
 * <br /><br />
 * Additionnally, the <em>com.anotherservice.rest.model.securityTmpCache.*</em> keys will be used to cache the {@link #getUser()}, {@link #userId(String)}, 
 * {@link #hasGrants(Collection, String)}, and {@link #hasGrants(Collection, String, String)} return values in order to speed up successive calls.
 */
public class SecurityImpl implements ISecurity
{
	public boolean isAuthenticated()
	{
		try
		{
			String auth = getCredentials();
			if( auth == null || auth.length() < 3 )
				return false;
			
			String[] parts = auth.split(":");
			if( parts.length != 2 )
				return false;
			
			// get from cache
			Object cache = Servlet.tmp.get().get("com.anotherservice.rest.model.securityTmpCache.isAuthenticated");
			if( cache instanceof Boolean )
				return (Boolean) cache;
			
			boolean isAuth = false;
			
			Map<String,String> r = Initializer.db.selectOne("SELECT u.user_name, u.user_id " + 
				"FROM users u " +
				"LEFT JOIN tokens t ON(t.token_user = u.user_id) " +
				"WHERE t.token_value = '" + Security.escape(parts[1]) + "' " + 
				"AND u." + (parts[0].matches("^[0-9]+$") ? "user_id = " + parts[0] : "user_name = '" + Security.escape(parts[0]) + "'") + " " +
				"AND (t.token_lease > UNIX_TIMESTAMP() OR t.token_lease = 0)");
			if( r == null || r.get("user_id") == null )
				isAuth = false;
			else
			{
				isAuth = true;
				Servlet.tmp.get().put("com.anotherservice.rest.model.securityTmpCache.userId", Long.parseLong(r.get("user_id")));
				Servlet.tmp.get().put("com.anotherservice.rest.model.securityTmpCache.userName", r.get("user_name"));
			}
			
			Servlet.tmp.get().put("com.anotherservice.rest.model.securityTmpCache.isAuthenticated", isAuth);
			return isAuth;
		}
		catch(Exception e)
		{
			return false;
		}
	}
	
	public String getUser()
	{
		if( !isAuthenticated() )
			return null;
		else
			return (String)Servlet.tmp.get().get("com.anotherservice.rest.model.securityTmpCache.userName");
	}
	
	public String getCredentials()
	{
		return Request.getParam(Config.gets("com.anotherservice.rest.model.authParameter"));
	}
	
	public long userId()
	{
		if( !isAuthenticated() )
			return -1;
		else
			return (Long)Servlet.tmp.get().get("com.anotherservice.rest.model.securityTmpCache.userId");
	}
	
	public long userId(String user)
	{
		try
		{
			if( user.matches("^[0-9]+$") )
				return Long.parseLong(user);

			Map<String,String> r = Initializer.db.selectOne("SELECT user_id FROM users WHERE user_name = '" + Security.escape(user) + "'");
			if( r == null || r.get("user_id") == null )
				return -1;
			
			return Long.parseLong(r.get("user_id"));
		}
		catch(Exception e)
		{
			Logger.severe("Error while getting the user id from the database : " + e.getMessage());
			return -1;
		}
	}
	
	public boolean hasGrant(String grant) { return hasGrants(Arrays.asList(new String[]{ grant })); }
	public boolean hasGrants(Collection<String> grants)
	{
		if( !isAuthenticated() )
			return false;
		return hasGrants(grants, getCredentials());
	}
	
	public boolean hasGrant(String grant, String credentials) { return hasGrants(Arrays.asList(new String[]{ grant }), credentials); }
	public boolean hasGrants(Collection<String> grants, String credentials)
	{
		if( grants.size() == 0 )
			return true;

		if( credentials == null || credentials.length() < 3 )
			return false;
		
		String[] parts = credentials.split(":");
		if( parts.length != 2 )
			return false;
		
		try
		{
			Vector<Map<String,String>> rows = null;
			
			// get from cache
			Object cache = Servlet.tmp.get().get("com.anotherservice.rest.model.securityTmpCache." + credentials + ".hasGrants1");
			if( cache instanceof Vector )
				rows = (Vector<Map<String,String>>) cache;
			else
			{
				rows = Initializer.db.select("SELECT DISTINCT g.grant_name, g.grant_id " + 
					"FROM users u " +
					"LEFT JOIN tokens t ON(t.token_user = u.user_id) " +
					"LEFT JOIN token_grant tg ON(t.token_id = tg.token_id) " + 
					"LEFT JOIN grants g ON(g.grant_id = tg.grant_id) " + 
					"WHERE t.token_value = '" + Security.escape(parts[1]) + "' " + 
					"AND u." + (parts[0].matches("^[0-9]+$") ? "user_id = " + parts[0] : "user_name = '" + Security.escape(parts[0]) + "'") + " " +
					"AND (t.token_lease > UNIX_TIMESTAMP() OR t.token_lease = 0)");
				
				if( rows == null )
					return false;
					
				// store to cache
				Servlet.tmp.get().put("com.anotherservice.rest.model.securityTmpCache." + credentials + ".hasGrants1", rows);
			}
			
			boolean has = false;
			boolean isGrantName = false;
			Object temporary = Servlet.tmp.get().get("com.anotherservice.rest.model.securityTmpGrants");
			
			for( String g : grants )
			{
				if( g == null || g.length() == 0 )
					continue;

				has = false;
				isGrantName = !g.matches("^[0-9]+$");
				
				for( Map<String,String> row : rows )
				{
					if( (isGrantName && row.get("grant_name").equalsIgnoreCase(g)) || (!isGrantName && row.get("grant_id").equals(g)) )
					{
						has = true;
						break;
					}
				}
				
				if( !has )
				{
					// check if the grant is in the Servlet.tmp
					if( temporary instanceof Collection )
					{
						Collection<String> t = ((Collection<String>)temporary);
						for( String s : t )
						{
							if( g.equalsIgnoreCase(s) )
							{
								has = true;
								break;
							}
						}
					}
				}
				
				if( !has )
				{
					// last resort : check if there is a SUPER-ADMIN grant
					String superadmin = Config.gets("com.anotherservice.rest.model.superadminGrantName");
					if( superadmin != null && superadmin.length() > 0 )
					{
						for( Map<String,String> row : rows )
						{
							if( row.get("grant_name").equals(superadmin) )
								return true;
						}
					}

					return false;
				}
			}
			
			return true;
		}
		catch(Exception e)
		{
			Logger.severe("Error while getting the user grants from the database : " + e.getMessage());
			return false;
		}
	}
	
	public boolean hasGrant(String grant, String user, String pass) { return hasGrants(Arrays.asList(new String[]{ grant }), user, pass); }
	public boolean hasGrants(Collection<String> grants, String user, String pass)
	{
		if( grants.size() == 0 )
			return true;

		if( user == null || user.length() == 0 || pass == null || pass.length() == 0 )
			return false;
		
		try
		{
			Vector<Map<String,String>> rows = null;
			
			// get from cache
			Object cache = Servlet.tmp.get().get("com.anotherservice.rest.model.securityTmpCache." + user + ".hasGrants2");
			if( cache instanceof Vector )
				rows = (Vector<Map<String,String>>) cache;
			else
			{
				if( user.matches("^[0-9]+$") )
					user = "user_id = " + user;
				else if( user.matches(PatternBuilder.EMAIL) )
					user = "user_mail = '" + Security.escape(user) + "'";
				else
					user = "user_name = '" + Security.escape(user) + "'";
					
				rows = Initializer.db.select("SELECT DISTINCT k.grant_name, k.grant_id " + 
					"FROM users u " +
					"LEFT JOIN user_grant uk ON(u.user_id = uk.user_id) " +
					"LEFT JOIN user_group ug ON(u.user_id = ug.user_id) " +
					"LEFT JOIN group_grant gk ON(ug.group_id = gk.group_id) " +
					"LEFT JOIN grants k ON(k.grant_id = gk.grant_id OR k.grant_id = uk.grant_id) " + 
					"WHERE u." + user + " " +
					"AND u.user_password = '" + Security.escape(hash(pass)) + "'");
				
				if( rows == null )
					return false;
				
				// store to cache
				Servlet.tmp.get().put("com.anotherservice.rest.model.securityTmpCache." + user + ".hasGrants2", rows);
			}
			
			boolean has = false;
			boolean isGrantName = false;
			Object temporary = Servlet.tmp.get().get("com.anotherservice.rest.model.securityTmpGrants");
			
			for( String g : grants )
			{
				if( g == null || g.length() == 0 )
					continue;

				has = false;
				isGrantName = !g.matches("^[0-9]+$");
				
				for( Map<String,String> row : rows )
				{
					if( (isGrantName && row.get("grant_name").equalsIgnoreCase(g)) || (!isGrantName && row.get("grant_id").equals(g)) )
					{
						has = true;
						break;
					}
				}
				
				if( !has )
				{
					// check if the grant is in the Servlet.tmp
					if( temporary instanceof Collection )
					{
						Collection<String> t = ((Collection<String>)temporary);
						for( String s : t )
						{
							if( g.equalsIgnoreCase(s) )
							{
								has = true;
								break;
							}
						}
					}
				}
				
				if( !has )
				{
					// last resort : check if there is a SUPER-ADMIN grant
					String superadmin = Config.gets("com.anotherservice.rest.model.superadminGrantName");
					if( superadmin != null && superadmin.length() > 0 )
					{
						for( Map<String,String> row : rows )
						{
							if( row.get("grant_name").equals(superadmin) )
								return true;
						}
					}

					return false;
				}
			}
			
			return true;
		}
		catch(Exception e)
		{
			Logger.severe("Error while getting the user grants from the database using the user/pass : " + e.getMessage());
			return false;
		}
	}
	
	public String hash(String text)
	{
		if( text == null || text.length() == 0 ) return text;
		
		try
		{
			int rounds = Integer.parseInt(Config.gets("com.anotherservice.rest.model.hashRounds"));
			boolean complex = Config.gets("com.anotherservice.rest.model.hashComplexify").matches("(yes|1|true)");
			
			text = MD5.hash(text);
			
			for( int i = 1; i < rounds; i++ )
			{
				if( complex )
					text += new BigInteger(text, 16).toString(Character.MAX_RADIX);
				text = MD5.hash(text);
			}
			
			return text;
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}