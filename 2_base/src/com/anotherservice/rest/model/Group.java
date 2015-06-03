package com.anotherservice.rest.model;

import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import java.util.*;

public class Group extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "group", "groups" });
		index.description = "Manages groups";
		Handler.addHandler(Initializer.path + "/", index);
		
		initializeInsert(index);
		initializeUpdate(index);
		initializeDelete(index);
		initializeSelect(index);
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				String group = getParameter("name").getValue();
				
				if( group.matches("^[0-9]+$") || group.matches("(^[\\._\\-\\s]|[\\._\\-\\s]$)") )
					throw new Exception("The group name may not be numeric and may not start or end with special characters");
				
				Long uid = Initializer.db.insert("INSERT INTO groups (group_name) VALUES ('" + Security.escape(group) + "')");
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("name", group);
				result.put("id", uid);
				return result;
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Creates a new group";
		insert.returnDescription = "The newly created group {'name', 'id'}";
		insert.addGrant(new String[] { "access", "group_insert" });
		
		Parameter group = new Parameter();
		group.isOptional = false;
		group.minLength = 3;
		group.maxLength = 30;
		group.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		group.allowInUrl = true;
		group.description = "The group name";
		group.addAlias(new String[]{ "name", "group_name" });
		insert.addParameter(group);
		
		index.addOwnHandler(insert);
	}

	private void initializeUpdate(Index index)
	{
		Action update = new Action()
		{
			public Object execute() throws Exception
			{
				String group = getParameter("name").getValue();
				String id = getParameter("id").getValue();
				
				if( group.matches("^[0-9]+$") || group.matches("(^[\\._\\-\\s]|[\\._\\-\\s]$)") )
					throw new Exception("The group name may not be numeric and may not start or end with special characters");
				
				Initializer.db.update("UPDATE groups SET group_name = '" + Security.escape(group) + "' WHERE group_id = " + Security.escape(id));
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Changes the group name";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "group_update" });
		
		Parameter group = new Parameter();
		group.isOptional = false;
		group.minLength = 3;
		group.maxLength = 30;
		group.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		group.description = "The group name";
		group.addAlias(new String[]{ "name", "group_name" });
		update.addParameter(group);
		
		Parameter id = new Parameter();
		id.isOptional = false;
		id.allowInUrl = true;
		id.minLength = 1;
		id.maxLength = 30;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.description = "The group id";
		id.addAlias(new String[]{ "id", "gid", "group_id" });
		update.addParameter(id);
		
		index.addOwnHandler(update);
	}
	
	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				String group = getParameter("name").getValue();
				
				if( !group.matches("^[0-9]+$") )
				{
					Map<String,String> r = Initializer.db.selectOne("SELECT group_id FROM groups WHERE group_name = '" + Security.escape(group) + "'");
					if( r == null || r.get("group_id") == null )
						throw new Exception("Unknown group");
					group = r.get("group_id");
				}
				
				Initializer.db.delete("DELETE FROM groups WHERE group_id = " + group);
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy" });
		delete.description = "Removes a group";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "group_delete" });
		
		Parameter group = new Parameter();
		group.isOptional = false;
		group.minLength = 1;
		group.maxLength = 30;
		group.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		group.allowInUrl = true;
		group.description = "The group name or id";
		group.addAlias(new String[]{ "name", "group_name", "id", "group_id", "gid" });
		delete.addParameter(group);
		
		index.addOwnHandler(delete);
	}
	
	private void initializeSelect(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> group = getParameter("name").getValues();
				
				String where = "";
				if( group.size() > 0 )
				{
					where += " AND (1>1";
					for( String g : group )
					{
						if( g.matches("^[0-9]+$") )
							where += " OR group_id = " + Security.escape(g);
						else
							where += " OR group_name LIKE '%" + Security.escape(g) + "%'";
					}
					where += ")";
				}
				
				return Initializer.db.select("SELECT group_id, group_name FROM groups WHERE 1=1 " + where);
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves information about a group. Multiple search values for a single property will be OR'd and different criteria will be AND'd.";
		select.returnDescription = "The matching groups [{'name', 'id'},...]";
		select.addGrant(new String[] { "access", "group_select" });
		
		Parameter group = new Parameter();
		group.isOptional = true;
		group.minLength = 1;
		group.maxLength = 30;
		group.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		group.allowInUrl = true;
		group.isMultipleValues = true;
		group.description = "The group name or id (will match *[group]* if not a number or an exact group id match if numeric)";
		group.addAlias(new String[]{ "name", "group_name", "id", "gid", "group_id", "group", "names", "group_names", "groups", "ids", "gids", "group_ids" });
		select.addParameter(group);
		
		index.addOwnHandler(select);
	}
}