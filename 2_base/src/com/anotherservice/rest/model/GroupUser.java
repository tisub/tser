package com.anotherservice.rest.model;

import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import java.util.*;

public class GroupUser extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "user", "users", "member", "members" });
		index.description = "Manages group memberships";
		Handler.addHandler(Initializer.path + "/group", index);
		
		initializeInsert(index);
		initializeDelete(index);
		initializeSelect(index);
		
		Self.selfize(Initializer.path + "/group/user/delete");
		Self.selfize(Initializer.path + "/group/user/select");
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> user = getParameter("user").getValues();
				Collection<String> group = getParameter("group").getValues();
				
				String where = " AND (1>1";
				for( String u : user )
				{
					if( u.matches("^[0-9]+$") )
						where += " OR u.user_id = " + Security.escape(u);
					else
						where += " OR u.user_name = '" + Security.escape(u) + "'";
				}
				
				where += ") AND (1>1";
				for( String g : group )
				{
					if( g.matches("^[0-9]+$") )
						where += " OR g.group_id = " + Security.escape(g);
					else
						where += " OR g.group_name = '" + Security.escape(g) + "'";
				}
				where += ")";
				
				Initializer.db.insert("INSERT IGNORE INTO user_group (user_id, group_id) " +
					"SELECT DISTINCT u.user_id, g.group_id FROM users u, groups g " +
					"WHERE 1=1 " + where);
				return "OK";
			}
		};
		
		insert.addMapping(new String[] { "insert", "link", "bind", "enter", "join", "add" });
		insert.description = "Makes the target users member of the provided groups";
		insert.returnDescription = "OK";
		insert.addGrant(new String[] { "access", "group_user_insert" });
		
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
		
		Parameter group = new Parameter();
		group.isOptional = false;
		group.minLength = 1;
		group.maxLength = 30;
		group.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		group.isMultipleValues = true;
		group.description = "The group name(s) or id(s)";
		group.addAlias(new String[]{ "group", "group_name", "gid", "group_id", "group_names", "groups", "gids", "group_ids" });
		insert.addParameter(group);
		
		index.addOwnHandler(insert);
	}

	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> user = getParameter("user").getValues();
				Collection<String> group = getParameter("group").getValues();
				
				String where = " AND (1>1";
				for( String u : user )
				{
					if( u.matches("^[0-9]+$") )
						where += " OR u.user_id = " + Security.escape(u);
					else
						where += " OR u.user_name = '" + Security.escape(u) + "'";
				}
				
				where += ") AND (1>1";
				for( String g : group )
				{
					if( g.matches("^[0-9]+$") )
						where += " OR g.group_id = " + Security.escape(g);
					else
						where += " OR g.group_name = '" + Security.escape(g) + "'";
				}
				where += ")";
				
				Initializer.db.delete("DELETE ug FROM user_group ug " +
					"LEFT JOIN users u ON(u.user_id = ug.user_id) " +
					"LEFT JOIN groups g ON(g.group_id = ug.group_id) " +
					"WHERE 1=1 " + where);
				Token.cleanup();
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "leave", "unbind", "exit", "unlink", "quit", "del", "remove" });
		delete.description = "Removes the target users from the provided groups";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "group_user_delete" });
		
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
		
		Parameter group = new Parameter();
		group.isOptional = false;
		group.minLength = 1;
		group.maxLength = 30;
		group.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		group.isMultipleValues = true;
		group.description = "The group name(s) or id(s)";
		group.addAlias(new String[]{ "group", "group_name", "gid", "group_id", "group_names", "groups", "gids", "group_ids" });
		delete.addParameter(group);
		
		index.addOwnHandler(delete);
	}
	
	private void initializeSelect(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				String user = getParameter("user").getValue();
				String group = getParameter("group").getValue();
				
				if( (user == null) == (group == null) )
					throw new Exception("The user and group parameters are mutually exclusive but one of them must be specified");

				String where = "";
				if( user != null )
				{
					if( user.matches("^[0-9]+$") )
						where = " u.user_id = " + Security.escape(user);
					else
						where = " u.user_name = '" + Security.escape(user) + "'";
						
					return Initializer.db.select("SELECT g.group_name, g.group_id FROM users u " +
						"LEFT JOIN user_group ug ON(u.user_id = ug.user_id) " +
						"LEFT JOIN groups g ON(g.group_id = ug.group_id) " +
						"WHERE g.group_id IS NOT NULL AND " + where);
				}
				else
				{
					if( group.matches("^[0-9]+$") )
						where = " g.group_id = " + Security.escape(group);
					else
						where = " g.group_name = '" + Security.escape(group) + "'";
						
					return Initializer.db.select("SELECT u.user_name, u.user_id FROM groups g " +
						"LEFT JOIN user_group ug ON(g.group_id = ug.group_id) " +
						"LEFT JOIN users u ON(u.user_id = ug.user_id) " +
						"WHERE u.user_id IS NOT NULL AND " + where);
				}
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves all groups of a user or all users of a group.";
		select.returnDescription = "The matching users or groups [{'name', 'id'},...]";
		select.addGrant(new String[] { "access", "group_user_select" });
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user name or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "uid", "user_id" });
		select.addParameter(user);
		
		Parameter group = new Parameter();
		group.isOptional = true;
		group.minLength = 1;
		group.maxLength = 30;
		group.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		group.description = "The group name or id";
		group.addAlias(new String[]{ "group", "group_name", "gid", "group_id" });
		select.addParameter(group);
		
		index.addOwnHandler(select);
	}
}