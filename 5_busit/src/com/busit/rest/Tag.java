package com.busit.rest;

import com.anotherservice.rest.*;
import com.anotherservice.db.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import java.util.*;
import java.util.regex.*;

public class Tag extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "tag", "tags" });
		index.description = "Manages tags";
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
				String tag = getParameter("name").getValue();
				
				if( tag.matches("^[0-9]+$") || tag.matches("(^[\\._\\-\\s]|[\\._\\-\\s]$)") )
					throw new Exception("The tag name may not be numeric and may not start or end with special characters");
				if( Pattern.compile("(:?^[\\s_\\-\\.])|[\\s_\\-\\.]{2,}|(:?[\\s_\\-\\.]$)").matcher(tag).find() )
					throw new Exception("The tag name may not start, end or contain consecutive special characters");
				
				Long uid = Database.getInstance().insert("INSERT INTO tags (tag_name) VALUES ('" + Security.escape(tag) + "')");
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("name", tag);
				result.put("id", uid);
				return result;
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Creates a new tag";
		insert.returnDescription = "The newly created tag {'name', 'id'}";
		insert.addGrant(new String[] { "access", "tag_insert" });
		
		Parameter tag = new Parameter();
		tag.isOptional = false;
		tag.minLength = 3;
		tag.maxLength = 30;
		tag.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		tag.allowInUrl = true;
		tag.description = "The tag name";
		tag.addAlias(new String[]{ "name", "tag_name" });
		insert.addParameter(tag);
		
		index.addOwnHandler(insert);
	}

	private void initializeUpdate(Index index)
	{
		Action update = new Action()
		{
			public Object execute() throws Exception
			{
				String tag = getParameter("name").getValue();
				String id = getParameter("id").getValue();
				
				if( tag.matches("^[0-9]+$") || tag.matches("(^[\\._\\-\\s]|[\\._\\-\\s]$)") )
					throw new Exception("The tag name may not be numeric and may not start or end with special characters");
				if( Pattern.compile("(:?^[\\s_\\-\\.])|[\\s_\\-\\.]{2,}|(:?[\\s_\\-\\.]$)").matcher(tag).find() )
					throw new Exception("The tag name may not start, end or contain consecutive special characters");
				
				Database.getInstance().update("UPDATE tags SET tag_name = '" + Security.escape(tag) + "' WHERE tag_id = " + Security.escape(id));
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Changes the tag name";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "tag_update" });
		
		Parameter tag = new Parameter();
		tag.isOptional = false;
		tag.minLength = 3;
		tag.maxLength = 30;
		tag.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		tag.description = "The tag name";
		tag.addAlias(new String[]{ "name", "tag_name" });
		update.addParameter(tag);
		
		Parameter id = new Parameter();
		id.isOptional = false;
		id.allowInUrl = true;
		id.minLength = 1;
		id.maxLength = 30;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.description = "The tag id";
		id.addAlias(new String[]{ "id", "tid", "tag_id" });
		update.addParameter(id);
		
		index.addOwnHandler(update);
	}
	
	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> tag = getParameter("name").getValues();
				
				String where = "1>1";
				for( String t : tag )
				{
					if( !t.matches("^[0-9]+$") )
						where += " OR tag_name = '" + Security.escape(t) + "'";
					else
						where += " OR tag_id = " + t;
				}
				
				Database.getInstance().delete("DELETE FROM tags WHERE  " + where);
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy" });
		delete.description = "Removes a tag";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "tag_delete" });
		
		Parameter tag = new Parameter();
		tag.isOptional = false;
		tag.minLength = 1;
		tag.maxLength = 30;
		tag.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		tag.allowInUrl = true;
		tag.isMultipleValues = true;
		tag.description = "The tag name(s) or id(s)";
		tag.addAlias(new String[]{ "name", "tag_name", "id", "tag_id", "tid", "names", "tag_names", "ids", "tag_ids", "tids" });
		delete.addParameter(tag);
		
		index.addOwnHandler(delete);
	}
	
	private void initializeSelect(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> tag = getParameter("name").getValues();
				
				String where = "";
				if( tag.size() > 0 )
				{
					where += " AND (1>1";
					for( String t : tag )
					{
						if( t.matches("^[0-9]+$") )
							where += " OR tag_id = " + Security.escape(t);
						else
							where += " OR tag_name LIKE '%" + Security.escape(t) + "%'";
					}
					where += ")";
				}
				
				return Database.getInstance().select("SELECT tag_id, tag_name FROM tags WHERE 1=1 " + where);
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves information about a tag. Multiple search values for a single property will be OR'd and different criteria will be AND'd.";
		select.returnDescription = "The matching tags [{'name', 'id'},...]";
		select.addGrant(new String[] { "access", "tag_select" });
		
		Parameter tag = new Parameter();
		tag.isOptional = true;
		tag.minLength = 1;
		tag.maxLength = 30;
		tag.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		tag.allowInUrl = true;
		tag.isMultipleValues = true;
		tag.description = "The tag name or id (will match *[tag]* if not a number or an exact tag id match if numeric)";
		tag.addAlias(new String[]{ "name", "tag_name", "id", "tid", "tag_id", "tag", "names", "tag_names", "tags", "ids", "tids", "tag_ids" });
		select.addParameter(tag);
		
		index.addOwnHandler(select);
	}
}