package com.anotherservice.rest.model;

import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import java.util.*;

public class GrantUser extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "user", "users" });
		index.description = "Manages grant ownerships";
		Handler.addHandler(Initializer.path + "/grant", index);
		
		initializeInsert(index);
		initializeDelete(index);
		initializeSelect(index);
		
		Self.selfize(Initializer.path + "/grant/user/delete");
		Self.selfize(Initializer.path + "/grant/user/select");
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> user = getParameter("user").getValues();
				Collection<String> grant = getParameter("grant").getValues();
				
				String where = " AND (1>1";
				for( String u : user )
				{
					if( u.matches("^[0-9]+$") )
						where += " OR u.user_id = " + Security.escape(u);
					else
						where += " OR u.user_name = '" + Security.escape(u) + "'";
				}
				
				where += ") AND (1>1";
				for( String g : grant )
				{
					if( g.matches("^[0-9]+$") )
						where += " OR g.grant_id = " + Security.escape(g);
					else
						where += " OR g.grant_name = '" + Security.escape(g) + "'";
				}
				where += ")";
				
				Initializer.db.insert("INSERT IGNORE INTO user_grant (user_id, grant_id) " +
					"SELECT DISTINCT u.user_id, g.grant_id FROM users u, grants g " +
					"WHERE 1=1 " + where);
				return "OK";
			}
		};
		
		insert.addMapping(new String[] { "insert", "give", "allow", "add", "accept" });
		insert.description = "Gives ownership of the provided grants to target users";
		insert.returnDescription = "OK";
		insert.addGrant(new String[] { "access", "grant_user_insert" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.allowInUrl = true;
		user.isMultipleValues = true;
		user.description = "The user name(s) or id(s)";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "uid", "user_id", "user_names", "usernames", "logins", "users", "uids", "user_ids" });
		insert.addParameter(user);
		
		Parameter grant = new Parameter();
		grant.isOptional = false;
		grant.minLength = 1;
		grant.maxLength = 30;
		grant.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		grant.isMultipleValues = true;
		grant.description = "The grant name(s) or id(s)";
		grant.addAlias(new String[]{ "grant", "grant_name", "gid", "grant_id", "grant_names", "grants", "gids", "grant_ids" });
		insert.addParameter(grant);
		
		index.addOwnHandler(insert);
	}

	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> user = getParameter("user").getValues();
				Collection<String> grant = getParameter("grant").getValues();
				
				String where = " AND (1>1";
				for( String u : user )
				{
					if( u.matches("^[0-9]+$") )
						where += " OR u.user_id = " + Security.escape(u);
					else
						where += " OR u.user_name = '" + Security.escape(u) + "'";
				}
				
				where += ") AND (1>1";
				for( String g : grant )
				{
					if( g.matches("^[0-9]+$") )
						where += " OR g.grant_id = " + Security.escape(g);
					else
						where += " OR g.grant_name = '" + Security.escape(g) + "'";
				}
				where += ")";
				
				Initializer.db.delete("DELETE ug FROM user_grant ug " +
					"LEFT JOIN users u ON(u.user_id = ug.user_id) " +
					"LEFT JOIN grants g ON(g.grant_id = ug.grant_id) " +
					"WHERE 1=1 " + where);
				Token.cleanup();
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "reject", "deny", "revoke", "del", "remove" });
		delete.description = "Removes ownership of the provided grants to target users";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "grant_user_delete" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.allowInUrl = true;
		user.isMultipleValues = true;
		user.description = "The user name(s) or id(s)";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "uid", "user_id", "user_names", "usernames", "logins", "users", "uids", "user_ids" });
		delete.addParameter(user);
		
		Parameter grant = new Parameter();
		grant.isOptional = false;
		grant.minLength = 1;
		grant.maxLength = 30;
		grant.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		grant.isMultipleValues = true;
		grant.description = "The grant name(s) or id(s)";
		grant.addAlias(new String[]{ "grant", "grant_name", "gid", "grant_id", "grant_names", "grants", "gids", "grant_ids" });
		delete.addParameter(grant);
		
		index.addOwnHandler(delete);
	}
	
	private void initializeSelect(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				String user = getParameter("user").getValue();
				String grant = getParameter("grant").getValue();
				String overall = getParameter("overall").getValue();
				
				if( (user == null) == (grant == null) )
					throw new Exception("The user and grant parameters are mutually exclusive but one of them must be specified");

				String where = "";
				if( user != null )
				{
					if( user.matches("^[0-9]+$") )
						where = " u.user_id = " + Security.escape(user);
					else
						where = " u.user_name = '" + Security.escape(user) + "'";
						
					String sql = "SELECT g.grant_name, g.grant_id FROM users u " +
						"LEFT JOIN user_grant ug ON(u.user_id = ug.user_id) " +
						"LEFT JOIN grants g ON(g.grant_id = ug.grant_id) " +
						"WHERE g.grant_id IS NOT NULL AND " + where;

					if( overall != null && overall.matches("^(?i)(yes|true|1)$") )
					{
						sql = "SELECT DISTINCT k.grant_name, k.grant_id " + 
								"FROM users u " +
								"LEFT JOIN user_grant uk ON(u.user_id = uk.user_id) " +
								"LEFT JOIN user_group ug ON(u.user_id = ug.user_id) " +
								"LEFT JOIN group_grant gk ON(ug.group_id = gk.group_id) " +
								"LEFT JOIN grants k ON(k.grant_id = gk.grant_id OR k.grant_id = uk.grant_id) " +
								"WHERE k.grant_id IS NOT NULL AND " + where;
					}
						
					return Initializer.db.select(sql);
				}
				else
				{
					if( grant.matches("^[0-9]+$") )
						where = " g.grant_id = " + Security.escape(grant);
					else
						where = " g.grant_name = '" + Security.escape(grant) + "'";
						
					return Initializer.db.select("SELECT u.user_name, u.user_id FROM grants g " +
						"LEFT JOIN user_grant ug ON(g.grant_id = ug.grant_id) " +
						"LEFT JOIN users u ON(u.user_id = ug.user_id) " +
						"WHERE u.user_id IS NOT NULL AND " + where);
				}
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves all grants of a user or all users of a grant.";
		select.returnDescription = "The matching users or grants [{'name', 'id'},...]";
		select.addGrant(new String[] { "access", "grant_user_select" });
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user name or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "uid", "user_id" });
		select.addParameter(user);
		
		Parameter grant = new Parameter();
		grant.isOptional = true;
		grant.minLength = 1;
		grant.maxLength = 30;
		grant.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		grant.description = "The grant name or id";
		grant.addAlias(new String[]{ "grant", "grant_name", "gid", "grant_id" });
		select.addParameter(grant);
		
		Parameter overall = new Parameter();
		overall.isOptional = true;
		overall.minLength = 1;
		overall.maxLength = 5;
		overall.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		overall.description = "When selecting all grants for a user, setting 'all' to true will include all inherited grants from the groups of the user";
		overall.addAlias(new String[]{ "overall", "all" });
		select.addParameter(overall);
		
		index.addOwnHandler(select);
	}
}