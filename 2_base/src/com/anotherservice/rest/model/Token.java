package com.anotherservice.rest.model;

import com.anotherservice.util.*;
import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import java.util.*;
import java.util.regex.*;

public class Token extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "token", "tokens" });
		index.description = "Manages tokens";
		Handler.addHandler(Initializer.path + "/", index);
		
		initializeInsert(index);
		initializeUpdate(index);
		initializeDelete(index);
		initializeSelect(index);
		initializeCleanup(index);
		initializePurge(index);
		
		Self.selfize(Initializer.path + "/token/insert");
		Self.selfize(Initializer.path + "/token/delete");
		Self.selfize(Initializer.path + "/token/update");
		Self.selfize(Initializer.path + "/token/select");
		Self.selfize(Initializer.path + "/token/purge");
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				String name = getParameter("name").getValue();
				String lease = getParameter("lease").getValue();
				String user = getParameter("user").getValue();
				Collection<String> grant = getParameter("grant").getValues();
				String wildcard = getParameter("wildcard").getValue();
				
				if( Pattern.compile("(:?^[\\s_\\-\\.])|[\\s_\\-\\.]{2,}|(:?[\\s_\\-\\.]$)").matcher(name).find() )
					throw new Exception("The token name may not start, end or contain consecutive special characters");

				if( lease == null || lease.length() == 0 )
					lease = (System.currentTimeMillis() / 1000 + (24 * 60 * 60)) + "";
				else if( lease.equals("0") )
					lease = "0";
				else if( Long.parseLong(lease) < System.currentTimeMillis() / 1000 )
					lease = (Long.parseLong(lease) + System.currentTimeMillis() / 1000) + "";
				
				String where = "";
				if( user.matches("^[0-9]+$") )
					where += " u.user_id = " + Security.escape(user);
				else
					where += " u.user_name = '" + Security.escape(user) + "'";
					
				String where2 = " AND (1>1";
				for( String g : grant )
				{
					if( g.matches("^[0-9]+$") )
						where2 += " OR k.grant_id = " + Security.escape(g);
					else
						where2 += " OR k.grant_name = '" + Security.escape(g) + "'";
				}
				where2 += ")";

				String value = MD5.hash(user + lease + System.currentTimeMillis());
				
				// CREATE THE NEW TOKEN
				Long uid = Initializer.db.insert("INSERT INTO tokens (token_user, token_lease, token_name, token_value) "+
					"VALUES ((SELECT u.user_id FROM users u WHERE " + where + "), " + Security.escape(lease) + ", '" + Security.escape(name) + "', '" + value + "')");

				// GIVE THE GRANTS TO THE TOKEN
				Initializer.db.insert("INSERT IGNORE INTO token_grant (token_id, grant_id) " +
					"SELECT DISTINCT " + uid + ", k.grant_id " +
					"	FROM users u " +
					"	LEFT JOIN user_grant uk ON(u.user_id = uk.user_id) " +
					"	LEFT JOIN user_group ug ON(u.user_id = ug.user_id) " +
					"	LEFT JOIN group_grant gk ON(ug.group_id = gk.group_id) " +
					"	LEFT JOIN grants k ON(k.grant_id = gk.grant_id OR k.grant_id = uk.grant_id) " +
					"	WHERE " + where + ( wildcard != null && wildcard.matches("^(?i)(yes|true|1)$") ? "" : where2 ));

				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("value", value);
				return result;
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add", "generate" });
		insert.description = "Creates a new token";
		insert.returnDescription = "The newly created token {'value'}";
		insert.addGrant(new String[] { "access", "token_insert" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.allowInUrl = true;
		user.description = "The user name or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "uid", "user_id" });
		insert.addParameter(user);
		
		Parameter name = new Parameter();
		name.isOptional = true;
		name.minLength = 0;
		name.maxLength = 50;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		name.description = "The token name";
		name.addAlias(new String[]{ "name", "token_name" });
		insert.addParameter(name);
		
		Parameter lease = new Parameter();
		lease.isOptional = true;
		lease.minLength = 0;
		lease.maxLength = 11;
		lease.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		lease.description = "The token lease time. If not specified, 1 day from now is the default value. It also accepts the value zero to never expire. If the value is greater than zero and lower than yesterday's timestamp then it is supposed to be a number of seconds from now. Otherwise, it is considered as the expiration date timestamp.";
		lease.addAlias(new String[]{ "lease", "expire", "token_lease", "until", "ttl" });
		insert.addParameter(lease);
		
		Parameter grant = new Parameter();
		grant.isOptional = true;
		grant.minLength = 1;
		grant.maxLength = 30;
		grant.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		grant.isMultipleValues = true;
		grant.description = "The grant name(s) or id(s) associated with the new token";
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

	private void initializeUpdate(Index index)
	{
		Action update = new Action()
		{
			public Object execute() throws Exception
			{
				String name = getParameter("name").getValue();
				String lease = getParameter("lease").getValue();
				String user = getParameter("user").getValue();
				String token = getParameter("token").getValue();
				
				if( name == null || lease == null )
					return "OK"; // nothing to update
				
				String values = "token_id = token_id";
				if( lease != null )
				{
					if( lease.length() == 0 )
						lease = (System.currentTimeMillis() / 1000 + (24 * 60 * 60)) + "";
					else if( lease.equals("0") )
						lease = "0";
					else if( Long.parseLong(lease) < System.currentTimeMillis() / 1000 )
						lease = (Long.parseLong(lease) + System.currentTimeMillis() / 1000) + "";
					
					values += ", token_lease = " + lease;
				}
				if( name != null )
				{
					if( Pattern.compile("(:?^[\\s_\\-\\.])|[\\s_\\-\\.]{2,}|(:?[\\s_\\-\\.]$)").matcher(name).find() )
						throw new Exception("The token name may not start, end or contain consecutive special characters");
					values += ", token_name = '" + Security.escape(name) + "'";
				}
				
				String where = "";
				if( user.matches("^[0-9]+$") )
					where += " u.user_id = " + Security.escape(user);
				else
					where += " u.user_name = '" + Security.escape(user) + "'";
				
				Initializer.db.update("UPDATE tokens SET " + values + " WHERE token_value = '" + Security.escape(token) + "' AND token_user = " +
					"(SELECT u.user_id FROM users u WHERE " + where + ")");
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change", "rename", "extend" });
		update.description = "Changes the token name or lease time";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "token_update" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user name or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "uid", "user_id" });
		update.addParameter(user);
		
		Parameter token = new Parameter();
		token.isOptional = false;
		token.minLength = 32;
		token.maxLength = 32;
		token.mustMatch = "^[a-fA-F0-9]{32,32}$";
		token.description = "The token to modify";
		token.allowInUrl = true;
		token.addAlias(new String[]{ "token", "token_value" });
		update.addParameter(token);
		
		Parameter name = new Parameter();
		name.isOptional = true;
		name.minLength = 0;
		name.maxLength = 50;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		name.description = "The token name";
		name.addAlias(new String[]{ "name", "token_name" });
		update.addParameter(name);
		
		Parameter lease = new Parameter();
		lease.isOptional = true;
		lease.minLength = 0;
		lease.maxLength = 11;
		lease.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		lease.description = "The token least time. If not specified, 1 day from now is the default value. It also accepts the value zero to never expire. If the value is greater than zero and lower than yesterday's timestamp then it is supposed to be a number of seconds from now. Otherwise, it is considered as the expiration date timestamp.";
		lease.addAlias(new String[]{ "lease", "expire", "token_lease", "until", "ttl" });
		update.addParameter(lease);
		
		index.addOwnHandler(update);
	}

	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> token = getParameter("token").getValues();
				String user = getParameter("user").getValue();
				
				String where = "";
				if( user.matches("^[0-9]+$") )
					where += " u.user_id = " + Security.escape(user);
				else
					where += " u.user_name = '" + Security.escape(user) + "'";
				
				where += " AND (1>1";
				for( String t : token )
					where += " OR token_value = '" + Security.escape(t) + "'";
				where += ")";
				
				Initializer.db.delete("DELETE t FROM tokens t LEFT JOIN users u ON(t.token_user = u.user_id) " +
					"WHERE " + where);
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy", "revoke" });
		delete.description = "Removes a token";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "token_delete" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user name or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "uid", "user_id" });
		delete.addParameter(user);

		Parameter token = new Parameter();
		token.isOptional = false;
		token.minLength = 32;
		token.maxLength = 32;
		token.mustMatch = "^[a-fA-F0-9]{32,32}$";
		token.description = "The token(s) to delete";
		token.allowInUrl = true;
		token.isMultipleValues = true;
		token.addAlias(new String[]{ "token", "token_value" });
		delete.addParameter(token);
		
		index.addOwnHandler(delete);
	}

	private void initializeSelect(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				String user = getParameter("user").getValue();
				String name = getParameter("name").getValue();
				String before = getParameter("before").getValue();
				String after = getParameter("after").getValue();
				Collection<String> token = getParameter("token").getValues();
				
				if( !Security.getInstance().hasGrants(Arrays.asList(new String[] { "access", "token_select" })) )
				{
					if( Security.getInstance().hasGrants(Arrays.asList(new String[] { "self_access", "self_token_select" })) )
					{
						if( user == null || user.length() == 0 )
							user = Security.getInstance().getUser();
						else if( !user.equalsIgnoreCase(Security.getInstance().getUser()) && !user.equalsIgnoreCase(Security.getInstance().userId() + "") )
							throw new Exception("Unsufficient privileges");
					}
					// the user is not logged in or does not have sufficient privileges
					// so check with the user/pass
					else if( !Security.getInstance().hasGrants(Arrays.asList(new String[] { "access" }), user, getParameter("pass").getValue()) )
						throw new Exception("Unsufficient privileges");
				}
				
				String where = " 1=1";
				if( user.matches("^[0-9]+$") )
					where += " AND u.user_id = " + Security.escape(user);
				else if( user.matches(PatternBuilder.EMAIL) )
					where += " AND u.user_mail = '" + Security.escape(user) + "'";
				else
					where += " AND u.user_name = '" + Security.escape(user) + "'";

				if( name != null )
					where += " AND t.token_name LIKE '%" + Security.escape(name) + "%'";
				
				if( before != null )
				{
					if( Long.parseLong(before) < 946684800000L ) // 1st Jan. 2000
						where += " AND token_lease > 0 AND token_lease < " + (Long.parseLong(before) + System.currentTimeMillis() / 1000);
					else
						where += " AND token_lease > 0 AND token_lease < " + Security.escape(before);
				}
				
				if( after != null )
				{
					if( Long.parseLong(after) < 946684800000L ) // 1st Jan. 2000
						where += " AND (token_lease = 0 OR token_lease > " + (Long.parseLong(after) + System.currentTimeMillis() / 1000) + ")";
					else
						where += " AND (token_lease = 0 OR token_lease > " + Security.escape(after) + ")";
				}
				
				if( token.size() > 0 )
				{
					where += " AND (1>1";
					for( String t : token )
						where += " OR token_value = '" + Security.escape(t) + "'";
					where += ")";
				}
				
				return Initializer.db.select("SELECT t.token_value, t.token_name, t.token_lease, u.user_name, u.user_id " + 
					"FROM tokens t LEFT JOIN users u ON(u.user_id = t.token_user) " +
					"WHERE " + where + " ORDER BY t.token_name");
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves information about tokens of a target user. Multiple search values for a single property will be OR'd and different criteria will be AND'd." + 
			"IMPORTANT : a user may select his own tokens by specifying his user name and password and NO AUTH. In this case, the user must own the \"access\" grant." +
			"In all other cases, both \"access\" and \"token_select\" grants are required";
		select.returnDescription = "The matching tokens [{'name', 'value', 'lease'},...]";
		//select.addGrant(new String[] { "access", "token_select" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 150;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		user.description = "The user name, id or email";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "uid", "user_id", "user_mail", "mail" });
		select.addParameter(user);
		
		Parameter pass = new Parameter();
		pass.isOptional = true;
		pass.minLength = 6;
		pass.maxLength = 100;
		pass.mustMatch = PatternBuilder.getRegex(PatternBuilder.PHRASE | PatternBuilder.SPECIAL);
		pass.description = "The user password. This is used only if no superseeding auth is provided";
		pass.addAlias(new String[]{ "pass", "password", "user_password", "user_pass" });
		select.addParameter(pass);
		
		Parameter name = new Parameter();
		name.isOptional = true;
		name.minLength = 1;
		name.maxLength = 50;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		name.description = "The token name (will match *[name]*)";
		name.addAlias(new String[]{ "name", "token_name" });
		select.addParameter(name);
		
		Parameter before = new Parameter();
		before.isOptional = true;
		before.minLength = 1;
		before.maxLength = 11;
		before.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		before.description = "Target tokens that expire before the provided timestamp. If a value before 1st Jan. 2000 is provided, it is converted to a number of seconds from now. (Tokens that never expire are always excluded when using this constraint)";
		before.addAlias(new String[]{ "before", "expired", "expire_before", "ended", "valid_before", "lease_before" });
		select.addParameter(before);
		
		Parameter after = new Parameter();
		after.isOptional = true;
		after.minLength = 1;
		after.maxLength = 11;
		after.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		after.description = "Target tokens that expire after the provided timestamp. If a value before 1st Jan. 2000 is provided, it is converted to a number of seconds from now. (Tokens that never expire are included when using this constraint (iif 'before' is not specified))";
		after.addAlias(new String[]{ "after", "expire", "expire_after", "end", "ends", "valid_after", "lease_after", "lease" });
		select.addParameter(after);
		
		Parameter token = new Parameter();
		token.isOptional = true;
		token.minLength = 32;
		token.maxLength = 32;
		token.mustMatch = "^[a-fA-F0-9]{32,32}$";
		token.description = "The token(s) to consider. If not provided, all user tokens are considered.";
		token.allowInUrl = true;
		token.isMultipleValues = true;
		token.addAlias(new String[]{ "token", "token_value", "tokens", "token_values" });
		select.addParameter(token);
		
		index.addOwnHandler(select);
	}

	public static void cleanup() throws Exception
	{
		Initializer.db.delete("DELETE tk FROM token_grant tk " + 
			"LEFT JOIN tokens t ON(t.token_id = tk.token_id) " + 
			"LEFT JOIN ( " + 
			"	SELECT DISTINCT k.grant_id, u.user_id FROM users u " + 
			"	LEFT JOIN user_grant uk ON(u.user_id = uk.user_id) " + 
			"	LEFT JOIN user_group ug ON(u.user_id = ug.user_id) " + 
			"	LEFT JOIN group_grant gk ON(ug.group_id = gk.group_id) " + 
			"	LEFT JOIN grants k ON(k.grant_id = gk.grant_id OR k.grant_id = uk.grant_id) " + 
			"	) tmp ON(t.token_user = tmp.user_id AND tk.grant_id = tmp.grant_id) " + 
			"WHERE tmp.grant_id IS NULL OR tmp.user_id IS NULL");
	}
	
	private void initializeCleanup(Index index)
	{
		Action cleanup = new Action()
		{
			public Object execute() throws Exception
			{
				cleanup();
				return "OK";
			}
		};
		
		cleanup.addMapping(new String[] { "cleanup" });
		cleanup.description = "Cleanup invalid token grants";
		cleanup.returnDescription = "OK";
		cleanup.addGrant(new String[] { "access", "token_cleanup" });
		
		index.addOwnHandler(cleanup);
	}
	
	private void initializePurge(Index index)
	{
		Action purge = new Action()
		{
			public Object execute() throws Exception
			{
				String token = getParameter("token").getValue();
				String user = getParameter("user").getValue();
				
				String where = "";
				if( user.matches("^[0-9]+$") )
					where += " u.user_id = " + Security.escape(user);
				else
					where += " u.user_name = '" + Security.escape(user) + "'";
				
				Initializer.db.delete("DELETE t FROM tokens t LEFT JOIN users u ON(t.token_user = u.user_id) " + 
					"WHERE token_lease IS NOT NULL AND token_lease < UNIX_TIMESTAMP() AND " + where);
				return "OK";
			}
		};
		
		purge.addMapping(new String[] { "purge" });
		purge.description = "Removes outdated tokens";
		purge.returnDescription = "OK";
		purge.addGrant(new String[] { "access", "token_delete" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user name or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "uid", "user_id" });
		purge.addParameter(user);
		
		index.addOwnHandler(purge);
	}
}