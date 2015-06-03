package com.busit.rest;

import com.anotherservice.db.*;
import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import com.busit.security.*;
import java.util.*;
import java.util.regex.*;
import java.security.Principal;

public class Delegate extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "delegate", "delegates" });
		index.description = "Manages grants delegation";
		Handler.addHandler("/busit", index);
		
		initializeInsert(index);
		initializeDelete(index);
		initializeUpdate(index);
		initializeSelect(index);
		
		Self.selfize("/busit/delegate/insert");
		Self.selfize("/busit/delegate/update");
		Self.selfize("/busit/delegate/delete");
		Self.selfize("/busit/delegate/select");
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				String user = getParameter("user").getValue();
				String identity = getParameter("identity").getValue();
				String space = getParameter("space").getValue();
				String instance = getParameter("instance").getValue();
				String name = getParameter("name").getValue();
				String lease = getParameter("lease").getValue();
				String wildcard = getParameter("wildcard").getValue();
				Collection<String> grants = getParameter("grants").getValues();
				
				if( (space == null) == (instance == null) )
					throw new Exception("The space and instance parameters are mutually exclusive but one of them must be specified");
				if( !IdentityChecker.getInstance().isUserIdentityOrImpersonate(user, identity) )
					throw new Exception("The target user does not own the specified identity");
				if( space != null && !IdentityChecker.getInstance().isUserSpace(user, space) )
					throw new Exception("The target user does not own the specified space");
				if( instance != null && !IdentityChecker.getInstance().isUserInstanceAdmin(user, instance) )
					throw new Exception("The target user does not own the specified instance");

				// ====================
				// IDENTITY
				// ====================
				if( !identity.matches("^[0-9]+$") )
				{
					Principal origin = PrincipalUtil.parse(identity);
					if( PrincipalUtil.isRemote(origin) )
						throw new Exception("Unknown identity"); // this is not an identity we manage
					identity = "(SELECT identity_id FROM identities WHERE identity_principal = '" + Security.escape(origin.getName()) + "')";
				}
				
				// ====================
				// USER
				// ====================
				if( !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users WHERE user_name = '" + Security.escape(user) + "')";
					
				// ====================
				// INSTANCE
				// ====================
				if( instance != null )
				{
					if( !instance.matches("^[0-9]+$") )
						instance = "(SELECT instance_id FROM instances WHERE instance_name = '" + Security.escape(instance) + "' AND instance_user = " + user + ")";
				}
					
				// ====================
				// SPACE
				// ====================
				if( space != null )
				{
					if( !space.matches("^[0-9]+$") )
						space = "(SELECT space_id FROM spaces WHERE space_name = '" + Security.escape(instance) + "' AND space_user = " + user + ")";
				}
				
				// ====================
				// LEASE
				// ====================
				if( lease == null || lease.length() == 0 )
					lease = (System.currentTimeMillis() / 1000 + (24 * 60 * 60)) + "";
				else if( lease.equals("0") )
					lease = "0";
				else if( Long.parseLong(lease) < System.currentTimeMillis() / 1000 )
					lease = (Long.parseLong(lease) + System.currentTimeMillis() / 1000) + "";
				
				// ====================
				// GRANT
				// ====================
				String whereGrant = " AND (1>1";
				for( String g : grants )
				{
					if( g.matches("^[0-9]+$") )
						whereGrant += " OR k.grant_id = " + Security.escape(g);
					else
						whereGrant += " OR k.grant_name = '" + Security.escape(g) + "'";
				}
				whereGrant += ")";
				
				String value = MD5.hash(user + lease + System.currentTimeMillis());
				
				// CREATE THE NEW TOKEN
				Long uid = Database.getInstance().insert("INSERT INTO delegates (delegate_identity, delegate_instance, delegate_space, delegate_lease, delegate_name, delegate_value) "+
					"VALUES (" + 
					identity + ", " + 
					instance + ", " +
					space + ", " +
					lease + ", " + 
					"'" + Security.escape(name) + "', " + 
					"'" + value + "')");

				// GIVE THE GRANTS TO THE TOKEN
				Database.getInstance().insert("INSERT IGNORE INTO delegate_grant (delegate_id, grant_id) " +
					"SELECT DISTINCT " + uid + ", k.grant_id " +
					"	FROM users u " +
					"	LEFT JOIN user_grant uk ON(u.user_id = uk.user_id) " +
					"	LEFT JOIN user_group ug ON(u.user_id = ug.user_id) " +
					"	LEFT JOIN group_grant gk ON(ug.group_id = gk.group_id) " +
					"	LEFT JOIN grants k ON(k.grant_id = gk.grant_id OR k.grant_id = uk.grant_id) " +
					"	WHERE u.user_id = " + user + ( wildcard != null && wildcard.matches("^(?i)(yes|true|1)$") ? "" : whereGrant ));

				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("value", value);
				return result;
			}
		};
		
		insert.addMapping(new String[] { "insert", "add", "create", "allow", "authorize" });
		insert.description = "Creates a new delegation token";
		insert.returnDescription = "OK";
		insert.addGrant(new String[] { "access", "delegate_insert" });
		
		Parameter instance = new Parameter();
		instance.isOptional = true;
		instance.minLength = 1;
		instance.maxLength = 30;
		instance.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		instance.description = "The instance name or id. Mutually exclusive with 'space'.";
		instance.addAlias(new String[]{ "instance", "instance_name", "instance_id", "delegate_instance" });
		insert.addParameter(instance);
		
		Parameter space = new Parameter();
		space.isOptional = true;
		space.minLength = 1;
		space.maxLength = 50;
		space.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		space.description = "The space name or id. Mutually exclusive with 'instance'.";
		space.addAlias(new String[]{ "space", "space_name", "space_id", "sid", "delegate_space" });
		insert.addParameter(space);
		
		Parameter identity = new Parameter();
		identity.isOptional = false;
		identity.minLength = 1;
		identity.maxLength = 100;
		identity.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "=\\\\,\"@");
		identity.description = "The identity name, principal or id";
		identity.addAlias(new String[]{ "identity", "identity_name", "identity_id", "principal", "identity_principal", "delegate_identity" });
		insert.addParameter(identity);
	
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user name or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "uid", "user_id", "delegate_user" });
		insert.addParameter(user);
		
		Parameter name = new Parameter();
		name.isOptional = true;
		name.minLength = 0;
		name.maxLength = 50;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		name.description = "The delegation token name";
		name.addAlias(new String[]{ "name", "token_name", "delegate_name" });
		insert.addParameter(name);
		
		Parameter lease = new Parameter();
		lease.isOptional = true;
		lease.minLength = 0;
		lease.maxLength = 11;
		lease.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		lease.description = "The delegation token lease time. If not specified, 1 day from now is the default value. It also accepts the value zero to never expire. If the value is greater than zero and lower than yesterday's timestamp then it is supposed to be a number of seconds from now. Otherwise, it is considered as the expiration date timestamp.";
		lease.addAlias(new String[]{ "lease", "expire", "token_lease", "delegate_lease", "until", "ttl", "delegate_lease" });
		insert.addParameter(lease);
		
		Parameter grant = new Parameter();
		grant.isOptional = true;
		grant.minLength = 1;
		grant.maxLength = 30;
		grant.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		grant.isMultipleValues = true;
		grant.description = "The grant name(s) or id(s) associated with the new delegation token";
		grant.addAlias(new String[]{ "grant", "grant_name", "gid", "grant_id", "grant_names", "grants", "gids", "grant_ids" });
		insert.addParameter(grant);
		
		Parameter wildcard = new Parameter();
		wildcard.isOptional = true;
		wildcard.minLength = 1;
		wildcard.maxLength = 5;
		wildcard.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		wildcard.description = "Setting 'wildcard' to true will include all available grants of the of the user and the parameter 'grant' will be ignored";
		wildcard.addAlias(new String[]{ "wildcard", "all" });
		insert.addParameter(wildcard);

		index.addOwnHandler(insert);
	}
	
	private void initializeDelete(Index index)
	{
		Action delete = new DelegateAction()
		{
			public Object execute() throws Exception
			{
				String delegate = getParameter("delegate").getValue();
				String user = getParameter("user").getValue();
				
				String where = "";
				if( user.matches("^[0-9]+$") )
					where += " u.user_id = " + Security.escape(user);
				else
					where += " u.user_name = '" + Security.escape(user) + "'";
				
				where += " AND d.delegate_value = '" + Security.escape(delegate) + "'";
				
				Database.getInstance().delete("DELETE d FROM delegates d " +
					"LEFT JOIN identities i ON (i.identity_id = d.delegate_identity) " +
					"LEFT JOIN users u ON(i.identity_user = u.user_id) " +
					"WHERE " + where);
				
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "revoke", "deny" });
		delete.description = "Removes a delegation token";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "delegate_delete" });
	
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user name or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "uid", "user_id" });
		delete.addParameter(user);

		Parameter delegate = new Parameter();
		delegate.isOptional = false;
		delegate.minLength = 32;
		delegate.maxLength = 32;
		delegate.mustMatch = "^[a-fA-F0-9]{32,32}$";
		delegate.description = "The delegation token to delete";
		delegate.allowInUrl = true;
		delegate.addAlias(new String[]{ "delegate", "delegate_value" });
		delete.addParameter(delegate);

		index.addOwnHandler(delete);
	}
	
	private void initializeUpdate(Index index)
	{
		// TODO
	}
	
	private void initializeSelect(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				String user = getParameter("user").getValue();
				String instance = getParameter("instance").getValue();
				
				if( !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users WHERE user_name = '" + Security.escape(user) + "')";
				
				if( instance != null && instance.length() > 0 )
				{
					if( instance.matches("^[0-9]+$") )
						user += " AND i2.instance_id = " + Security.escape(instance);
					else
						user += " AND i2.instance_name LIKE '%" + Security.escape(instance) + "%'";
				}
				
				Vector<Map<String,String>> rows = Database.getInstance().select("SELECT d.delegate_name, d.delegate_value, d.delegate_lease, " +
					"i.identity_id, i.identity_principal, " +
					"s.space_id, s.space_name, " +
					"i2.instance_id, i2.instance_name, i2.instance_connector, " +
					"GROUP_CONCAT(g.grant_id) as gid, GROUP_CONCAT(g.grant_name) as gn " +
					"FROM delegates d " +
					"LEFT JOIN identities i ON(i.identity_id = d.delegate_identity) " +
					"LEFT JOIN users u ON(u.user_id = i.identity_user) " +
					"LEFT JOIN spaces s ON(s.space_id = d.delegate_space) " +
					"LEFT JOIN instances i2 ON(i2.instance_id = d.delegate_instance) " +
					"LEFT JOIN delegate_grant dg ON(dg.delegate_id = d.delegate_id) " +
					"LEFT JOIN grants g ON(g.grant_id = dg.grant_id) " +
					"WHERE u.user_id = " + user + " GROUP BY d.delegate_id ORDER BY d.delegate_name");
				
				Vector<Map<String, Object>> result = new Vector<Map<String, Object>>();
				for( Map<String, String> row : rows )
				{
					Map<String, Object> r = new HashMap<String, Object>();
					r.put("delegate_name", row.get("delegate_name"));
					r.put("delegate_value", row.get("delegate_value"));
					r.put("delegate_lease", row.get("delegate_lease"));
					
					Map<String, String> identity = new HashMap<String, String>();
					identity.put("identity_id", row.get("identity_id"));
					identity.put("identity_name", PrincipalUtil.shortName(row.get("identity_principal")));
					r.put("identity", identity);
					
					Map<String, String> space = null;
					if( row.get("space_id") != null && row.get("space_id").length() > 0 )
					{
						space = new HashMap<String, String>();
						space.put("space_id", row.get("space_id"));
						space.put("space_name", row.get("space_name"));
					}
					r.put("space", space);
					
					Map<String, String> inst = null;
					if( row.get("instance_id") != null && row.get("instance_id").length() > 0 )
					{
						inst = new HashMap<String, String>();
						inst.put("instance_id", row.get("instance_id"));
						inst.put("instance_name", row.get("instance_name"));
						inst.put("instance_connector", row.get("instance_connector"));
					}
					r.put("instance", inst);
					
					Vector<Map<String, String>> grants = new Vector<Map<String, String>>();
					String[] ids = row.get("gid").split(",");
					String[] names = row.get("gn").split(",");
					for( int i = 0; i < ids.length; i++ )
					{
						Map<String, String> g = new HashMap<String, String>();
						g.put("grant_id", ids[i]);
						g.put("grant_name", names[i]);
						grants.add(g);
					}
					r.put("grants", grants);
					
					result.add(r);
				}
				
				return result;
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves information about delegation tokens ";
		select.returnDescription = "The matching delegation tokens [{'name', 'value', 'lease'},...]";
		select.addGrant(new String[] { "access", "delegate_select" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user name or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "uid", "user_id" });
		select.addParameter(user);
		
		Parameter instance = new Parameter();
		instance.isOptional = true;
		instance.minLength = 1;
		instance.maxLength = 30;
		instance.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		instance.description = "The instance name or id (will match *[instance]* if not a number or an exact instance id match if numeric)";
		instance.addAlias(new String[]{ "name", "instance_name", "id", "iid", "instance_id", "instance" });
		select.addParameter(instance);
		
		index.addOwnHandler(select);
	}
}