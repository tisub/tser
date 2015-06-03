package com.busit.rest;

import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import com.anotherservice.db.*;
import java.util.*;

public class Status extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "status" });
		index.description = "Manages status codes";
		Handler.addHandler("/busit/", index);
		
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
				String status = getParameter("name").getValue();
				
				if( status.matches("^[0-9]+$") || status.matches("(^[\\._\\-\\s]|[\\._\\-\\s]$)") )
					throw new Exception("The status name may not be numeric and may not start or end with special characters");
				
				Long uid = Database.getInstance().insert("INSERT INTO status (status_name) VALUES ('" + Security.escape(status) + "')");
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("name", status);
				result.put("id", uid);
				return result;
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Creates a new status code";
		insert.returnDescription = "The newly created status {'name', 'id'}";
		insert.addGrant(new String[] { "access", "status_insert" });
		
		Parameter status = new Parameter();
		status.isOptional = false;
		status.minLength = 3;
		status.maxLength = 30;
		status.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		status.allowInUrl = true;
		status.description = "The status name";
		status.addAlias(new String[]{ "name", "status_name" });
		insert.addParameter(status);
		
		index.addOwnHandler(insert);
	}

	private void initializeUpdate(Index index)
	{
		Action update = new Action()
		{
			public Object execute() throws Exception
			{
				String status = getParameter("name").getValue();
				String id = getParameter("id").getValue();
				
				if( status.matches("^[0-9]+$") || status.matches("(^[\\._\\-\\s]|[\\._\\-\\s]$)") )
					throw new Exception("The status name may not be numeric and may not start or end with special characters");
				
				Database.getInstance().update("UPDATE status SET status_name = '" + Security.escape(status) + "' WHERE status_id = " + Security.escape(id));
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Changes the status name";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "status_update" });
		
		Parameter status = new Parameter();
		status.isOptional = false;
		status.minLength = 3;
		status.maxLength = 30;
		status.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		status.description = "The status name";
		status.addAlias(new String[]{ "name", "status_name" });
		update.addParameter(status);
		
		Parameter id = new Parameter();
		id.isOptional = false;
		id.allowInUrl = true;
		id.minLength = 1;
		id.maxLength = 11;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.description = "The status id";
		id.addAlias(new String[]{ "id", "sid", "status_id" });
		update.addParameter(id);
		
		index.addOwnHandler(update);
	}
	
	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> status = getParameter("name").getValues();
				
				String where = "1>1";
				for( String s : status )
				{
					if( !s.matches("^[0-9]+$") )
						where += " OR status_name = '" + Security.escape(s) + "'";
					else
						where += " OR status_id = " + s;
				}
				
				Database.getInstance().delete("DELETE FROM status WHERE  " + where);
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy" });
		delete.description = "Removes a status code";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "status_delete" });
		
		Parameter status = new Parameter();
		status.isOptional = false;
		status.minLength = 1;
		status.maxLength = 30;
		status.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		status.allowInUrl = true;
		status.isMultipleValues = true;
		status.description = "The status name(s) or id(s)";
		status.addAlias(new String[]{ "name", "status_name", "id", "status_id", "sid", "names", "status_names", "ids", "status_ids", "sids" });
		delete.addParameter(status);
		
		index.addOwnHandler(delete);
	}
	
	private void initializeSelect(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> status = getParameter("name").getValues();
				
				String where = "";
				if( status.size() > 0 )
				{
					where += " AND (1>1";
					for( String s : status )
					{
						if( s.matches("^[0-9]+$") )
							where += " OR status_id = " + Security.escape(s);
						else
							where += " OR status_name LIKE '%" + Security.escape(s) + "%'";
					}
					where += ")";
				}
				
				return Database.getInstance().select("SELECT status_id, status_name FROM status WHERE 1=1 " + where);
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves information about a status code. Multiple search values for a single property will be OR'd and different criteria will be AND'd.";
		select.returnDescription = "The matching status [{'name', 'id'},...]";
		select.addGrant(new String[] { "access", "status_select" });
		
		Parameter status = new Parameter();
		status.isOptional = true;
		status.minLength = 1;
		status.maxLength = 30;
		status.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		status.allowInUrl = true;
		status.isMultipleValues = true;
		status.description = "The status name or id (will match *[status]* if not a number or an exact status id match if numeric)";
		status.addAlias(new String[]{ "name", "status_name", "id", "sid", "status_id", "status", "names", "status_names", "ids", "sids", "status_ids" });
		select.addParameter(status);
		
		index.addOwnHandler(select);
	}
}