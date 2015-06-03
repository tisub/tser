package com.anotherservice.rest.model;

import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import java.util.*;

public class Grant extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "grant", "grants" });
		index.description = "Manages grants";
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
				String grant = getParameter("name").getValue();
				
				if( grant.matches("^[0-9]+$") || grant.matches("(^[\\._\\-\\s]|[\\._\\-\\s]$)") )
					throw new Exception("The grant name may not be numeric and may not start or end with special characters");
				
				Long uid = Initializer.db.insert("INSERT INTO grants (grant_name) VALUES ('" + Security.escape(grant) + "')");
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("name", grant);
				result.put("id", uid);
				return result;
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Creates a new grant";
		insert.returnDescription = "The newly created grant {'name', 'id'}";
		insert.addGrant(new String[] { "access", "grant_insert" });
		
		Parameter grant = new Parameter();
		grant.isOptional = false;
		grant.minLength = 3;
		grant.maxLength = 30;
		grant.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		grant.allowInUrl = true;
		grant.description = "The grant name";
		grant.addAlias(new String[]{ "name", "grant_name" });
		insert.addParameter(grant);
		
		index.addOwnHandler(insert);
	}

	private void initializeUpdate(Index index)
	{
		Action update = new Action()
		{
			public Object execute() throws Exception
			{
				String grant = getParameter("name").getValue();
				String id = getParameter("id").getValue();
				
				if( grant.matches("^[0-9]+$") || grant.matches("(^[\\._\\-\\s]|[\\._\\-\\s]$)") )
					throw new Exception("The grant name may not be numeric and may not start or end with special characters");
				
				Initializer.db.update("UPDATE grants SET grant_name = '" + Security.escape(grant) + "' WHERE grant_id = " + Security.escape(id));
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Changes the grant name";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "grant_update" });
		
		Parameter grant = new Parameter();
		grant.isOptional = false;
		grant.minLength = 3;
		grant.maxLength = 30;
		grant.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		grant.description = "The grant name";
		grant.addAlias(new String[]{ "name", "grant_name" });
		update.addParameter(grant);
		
		Parameter id = new Parameter();
		id.isOptional = false;
		id.allowInUrl = true;
		id.minLength = 1;
		id.maxLength = 30;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.description = "The grant id";
		id.addAlias(new String[]{ "id", "gid", "grant_id" });
		update.addParameter(id);
		
		index.addOwnHandler(update);
	}
	
	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				String grant = getParameter("name").getValue();
				
				if( !grant.matches("^[0-9]+$") )
				{
					Map<String,String> r = Initializer.db.selectOne("SELECT grant_id FROM grants WHERE grant_name = '" + Security.escape(grant) + "'");
					if( r == null || r.get("grant_id") == null )
						throw new Exception("Unknown grant");
					grant = r.get("grant_id");
				}
				
				Initializer.db.delete("DELETE FROM grants WHERE grant_id = " + grant);
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy" });
		delete.description = "Removes a grant";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "grant_delete" });
		
		Parameter grant = new Parameter();
		grant.isOptional = false;
		grant.minLength = 1;
		grant.maxLength = 30;
		grant.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		grant.allowInUrl = true;
		grant.description = "The grant name or id";
		grant.addAlias(new String[]{ "name", "grant_name", "id", "grant_id", "gid" });
		delete.addParameter(grant);
		
		index.addOwnHandler(delete);
	}
	
	private void initializeSelect(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> grant = getParameter("name").getValues();
				
				String where = "";
				if( grant.size() > 0 )
				{
					where += " AND (1>1";
					for( String g : grant )
					{
						if( g.matches("^[0-9]+$") )
							where += " OR grant_id = " + Security.escape(g);
						else
							where += " OR grant_name LIKE '%" + Security.escape(g) + "%'";
					}
					where += ")";
				}
				
				return Initializer.db.select("SELECT grant_id, grant_name FROM grants WHERE 1=1 " + where);
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves information about a grant. Multiple search values for a single property will be OR'd and different criteria will be AND'd.";
		select.returnDescription = "The matching grants [{'name', 'id'},...]";
		select.addGrant(new String[] { "access", "grant_select" });
		
		Parameter grant = new Parameter();
		grant.isOptional = true;
		grant.minLength = 1;
		grant.maxLength = 30;
		grant.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		grant.allowInUrl = true;
		grant.isMultipleValues = true;
		grant.description = "The grant name or id (will match *[grant]* if not a number or an exact grant id match if numeric)";
		grant.addAlias(new String[]{ "name", "grant_name", "id", "gid", "grant_id", "grant", "names", "grant_names", "grants", "ids", "gids", "grant_ids" });
		select.addParameter(grant);
		
		index.addOwnHandler(select);
	}
}