package com.anotherservice.rest.model;

import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import java.util.*;

public class GrantToken extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "token", "tokens" });
		index.description = "Manages grant ownerships";
		Handler.addHandler(Initializer.path + "/grant", index);
		
		initializeInsert(index);
		initializeDelete(index);
		initializeSelect(index);
		
		Self.selfize(Initializer.path + "/grant/token/insert");
		Self.selfize(Initializer.path + "/grant/token/select");
		Self.selfize(Initializer.path + "/grant/token/delete");
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				String user = getParameter("user").getValue();
				Collection<String> token = getParameter("token").getValues();
				Collection<String> grant = getParameter("grant").getValues();
				
				String where = "";
				if( user.matches("^[0-9]+$") )
					where = " u.user_id = " + Security.escape(user);
				else
					where = " u.user_name = '" + Security.escape(user) + "'";

				where += " AND (1>1";
				for( String t : token )
					where += " OR t.token_value = '" + Security.escape(t) + "'";
				
				where += ") AND (1>1";
				for( String g : grant )
				{
					if( g.matches("^[0-9]+$") )
						where += " OR g.grant_id = " + Security.escape(g);
					else
						where += " OR g.grant_name = '" + Security.escape(g) + "'";
				}
				where += ")";

				Initializer.db.insert("INSERT IGNORE INTO token_grant (grant_id, token_id) " +
					"SELECT DISTINCT g.grant_id, t.token_id " +
					"FROM users u LEFT JOIN tokens t ON(u.user_id=t.token_user), grants g " + 
					"WHERE token_id IS NOT NULL AND " + where);
				return "OK";
			}
		};
		
		insert.addMapping(new String[] { "insert", "give", "allow", "add", "accept" });
		insert.description = "Gives ownership of the provided grants to target user tokens";
		insert.returnDescription = "OK";
		insert.addGrant(new String[] { "access", "grant_token_insert" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.allowInUrl = true;
		user.description = "The user name or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "uid", "user_id" });
		insert.addParameter(user);
		
		Parameter token = new Parameter();
		token.isOptional = false;
		token.minLength = 32;
		token.maxLength = 32;
		token.mustMatch = "^[a-fA-F0-9]{32,32}$";
		token.isMultipleValues = true;
		token.description = "The token(s)";
		token.addAlias(new String[]{ "token", "token_value", "tokens", "token_values" });
		insert.addParameter(token);
		
		Parameter grant = new Parameter();
		grant.isOptional = true;
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
				String user = getParameter("user").getValue();
				Collection<String> token = getParameter("token").getValues();
				Collection<String> grant = getParameter("grant").getValues();
				
				String where = "";
				if( user.matches("^[0-9]+$") )
					where = " u.user_id = " + Security.escape(user);
				else
					where = " u.user_name = '" + Security.escape(user) + "'";

				where += " AND (1>1";
				for( String t : token )
					where += " OR t.token_value = '" + Security.escape(t) + "'";
				
				where += ") AND (1>1";
				for( String g : grant )
				{
					if( g.matches("^[0-9]+$") )
						where += " OR g.grant_id = " + Security.escape(g);
					else
						where += " OR g.grant_name = '" + Security.escape(g) + "'";
				}
				where += ")";
				
				Initializer.db.delete("DELETE tg FROM token_grant tg " +
					"LEFT JOIN grants g ON(g.grant_id = tg.grant_id) " +
					"LEFT JOIN tokens t ON(t.token_id = tg.token_id) " +
					"LEFT JOIN users u ON(u.user_id = t.token_user) " +
					"WHERE " + where);
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "reject", "deny", "revoke", "del", "remove" });
		delete.description = "Removes ownership of the provided grants to target user tokens";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "grant_token_delete" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.allowInUrl = true;
		user.description = "The user name or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "uid", "user_id" });
		delete.addParameter(user);
		
		Parameter token = new Parameter();
		token.isOptional = false;
		token.minLength = 32;
		token.maxLength = 32;
		token.mustMatch = "^[a-fA-F0-9]{32,32}$";
		token.isMultipleValues = true;
		token.description = "The token(s)";
		token.addAlias(new String[]{ "token", "token_value", "tokens", "token_values" });
		delete.addParameter(token);
		
		Parameter grant = new Parameter();
		grant.isOptional = true;
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
				String token = getParameter("token").getValue();
				String grant = getParameter("grant").getValue();
				
				if( (token == null) == (grant == null) )
					throw new Exception("The token and grant parameters are mutually exclusive but one of them must be specified");

				String where = "";
				if( user.matches("^[0-9]+$") )
					where = " u.user_id = " + Security.escape(user);
				else
					where = " u.user_name = '" + Security.escape(user) + "'";
						
				if( token != null )
				{
					where += " AND t.token_value = '" + Security.escape(token) + "'";
					return Initializer.db.select("SELECT g.grant_name, g.grant_id FROM grants g " +
						"LEFT JOIN token_grant tg ON(g.grant_id = tg.grant_id) " +
						"LEFT JOIN tokens t ON(t.token_id = tg.token_id) " +
						"LEFT JOIN users u ON(t.token_user = u.user_id) " +
						"WHERE " + where);
				}
				else
				{
					if( grant.matches("^[0-9]+$") )
						where += " AND g.grant_id = " + Security.escape(grant);
					else
						where += " AND g.grant_name = '" + Security.escape(grant) + "'";
						
					return Initializer.db.select("SELECT t.token_value FROM users u " +
						"LEFT JOIN tokens t ON(t.token_user = u.user_id) " +
						"LEFT JOIN token_grant tg ON(t.token_id = tg.token_id) " +
						"LEFT JOIN grants g ON(g.grant_id = tg.grant_id) " +
						"WHERE " + where);
				}
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves all grants of a user token or all user tokens of a grant.";
		select.returnDescription = "The matching tokens [{'value'},...] or grants [{'name', 'id'},...]";
		select.addGrant(new String[] { "access", "grant_token_select" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user name or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "uid", "user_id" });
		select.addParameter(user);
		
		Parameter token = new Parameter();
		token.isOptional = true;
		token.minLength = 32;
		token.maxLength = 32;
		token.mustMatch = "^[a-fA-F0-9]{32,32}$";
		token.description = "The token";
		token.addAlias(new String[]{ "token", "token_value" });
		select.addParameter(token);
		
		Parameter grant = new Parameter();
		grant.isOptional = true;
		grant.minLength = 1;
		grant.maxLength = 30;
		grant.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		grant.description = "The grant name or id";
		grant.addAlias(new String[]{ "grant", "grant_name", "gid", "grant_id" });
		select.addParameter(grant);
		
		index.addOwnHandler(select);
	}
}