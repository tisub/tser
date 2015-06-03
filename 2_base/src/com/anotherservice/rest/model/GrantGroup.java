package com.anotherservice.rest.model;

import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import java.util.*;

public class GrantGroup extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "group", "groups" });
		index.description = "Manages grant ownerships";
		Handler.addHandler(Initializer.path + "/grant", index);
		
		initializeInsert(index);
		initializeDelete(index);
		initializeSelect(index);
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> group = getParameter("group").getValues();
				Collection<String> grant = getParameter("grant").getValues();
				
				String where = " AND (1>1";
				for( String g : group )
				{
					if( g.matches("^[0-9]+$") )
						where += " OR gr.group_id = " + Security.escape(g);
					else
						where += " OR gr.group_name = '" + Security.escape(g) + "'";
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
				
				Initializer.db.insert("INSERT IGNORE INTO group_grant (group_id, grant_id) " +
					"SELECT DISTINCT gr.group_id, g.grant_id FROM groups gr, grants g " +
					"WHERE 1=1 " + where);
				return "OK";
			}
		};
		
		insert.addMapping(new String[] { "insert", "give", "allow", "add", "accept" });
		insert.description = "Gives ownership of the provided grants to target groups";
		insert.returnDescription = "OK";
		insert.addGrant(new String[] { "access", "grant_group_insert" });
		
		Parameter group = new Parameter();
		group.isOptional = false;
		group.minLength = 1;
		group.maxLength = 30;
		group.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		group.allowInUrl = true;
		group.isMultipleValues = true;
		group.description = "The group name(s) or id(s)";
		group.addAlias(new String[]{ "group", "group_name", "groupname", "login", "uid", "group_id", "group_names", "groupnames", "logins", "groups", "uids", "group_ids" });
		insert.addParameter(group);
		
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
				Collection<String> group = getParameter("group").getValues();
				Collection<String> grant = getParameter("grant").getValues();
				
				String where = " AND (1>1";
				for( String g : group )
				{
					if( g.matches("^[0-9]+$") )
						where += " OR gr.group_id = " + Security.escape(g);
					else
						where += " OR gr.group_name = '" + Security.escape(g) + "'";
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
				
				Initializer.db.delete("DELETE gg FROM group_grant gg " +
					"LEFT JOIN groups gr ON(gr.group_id = gg.group_id) " +
					"LEFT JOIN grants g ON(g.grant_id = gg.grant_id) " +
					"WHERE 1=1 " + where);
				Token.cleanup();
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "reject", "deny", "revoke", "del", "remove" });
		delete.description = "Removes ownership of the provided grants to target groups";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "grant_group_delete" });
		
		Parameter group = new Parameter();
		group.isOptional = false;
		group.minLength = 1;
		group.maxLength = 30;
		group.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		group.allowInUrl = true;
		group.isMultipleValues = true;
		group.description = "The group name(s) or id(s)";
		group.addAlias(new String[]{ "group", "group_name", "group_id", "group_names", "groups", "group_ids" });
		delete.addParameter(group);
		
		Parameter grant = new Parameter();
		grant.isOptional = false;
		grant.minLength = 1;
		grant.maxLength = 30;
		grant.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		grant.isMultipleValues = true;
		grant.description = "The grant name(s) or id(s)";
		grant.addAlias(new String[]{ "grant", "grant_name", "grant_id", "grant_names", "grants", "grant_ids" });
		delete.addParameter(grant);
		
		index.addOwnHandler(delete);
	}
	
	private void initializeSelect(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				String group = getParameter("group").getValue();
				String grant = getParameter("grant").getValue();
				
				if( (group == null) == (grant == null) )
					throw new Exception("The group and grant parameters are mutually exclusive but one of them must be specified");

				String where = "";
				if( group != null )
				{
					if( group.matches("^[0-9]+$") )
						where = " gr.group_id = " + Security.escape(group);
					else
						where = " gr.group_name = '" + Security.escape(group) + "'";
						
					return Initializer.db.select("SELECT g.grant_name, g.grant_id FROM groups gr " +
						"LEFT JOIN group_grant gg ON(gr.group_id = gg.group_id) " +
						"LEFT JOIN grants g ON(g.grant_id = gg.grant_id) " +
						"WHERE g.grant_id IS NOT NULL AND " + where);
				}
				else
				{
					if( grant.matches("^[0-9]+$") )
						where = " g.grant_id = " + Security.escape(grant);
					else
						where = " g.grant_name = '" + Security.escape(grant) + "'";
						
					return Initializer.db.select("SELECT gr.group_name, gr.group_id FROM grants g " +
						"LEFT JOIN group_grant gg ON(g.grant_id = gg.grant_id) " +
						"LEFT JOIN groups gr ON(gr.group_id = gg.group_id) " +
						"WHERE gr.group_id IS NOT NULL AND " + where);
				}
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves all grants of a group or all groups of a grant.";
		select.returnDescription = "The matching groups or grants [{'name', 'id'},...]";
		select.addGrant(new String[] { "access", "grant_group_select" });
		
		Parameter group = new Parameter();
		group.isOptional = true;
		group.minLength = 1;
		group.maxLength = 30;
		group.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		group.description = "The group name or id";
		group.addAlias(new String[]{ "group", "group_name", "group_id" });
		select.addParameter(group);
		
		Parameter grant = new Parameter();
		grant.isOptional = true;
		grant.minLength = 1;
		grant.maxLength = 30;
		grant.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		grant.description = "The grant name or id";
		grant.addAlias(new String[]{ "grant", "grant_name", "grant_id" });
		select.addParameter(grant);
		
		index.addOwnHandler(select);
	}
}