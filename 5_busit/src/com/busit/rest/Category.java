package com.busit.rest;

import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import com.anotherservice.db.*;
import java.util.*;

public class Category extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "category", "categories" });
		index.description = "Manages categories";
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
				String name = getParameter("name").getValue();
				
				if( name.matches("^[0-9]+$") || name.matches("(^[\\._\\-\\s]|[\\._\\-\\s]$)") )
					throw new Exception("The category name may not be numeric and may not start or end with special characters");
				
				Long uid = Database.getInstance().insert("INSERT INTO categories (category_name) VALUES ('" + Security.escape(name) + "')");
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("name", name);
				result.put("id", uid);
				return result;
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Creates a new category";
		insert.returnDescription = "The newly created category {'name', 'id'}";
		insert.addGrant(new String[] { "access", "category_insert" });
		
		Parameter name = new Parameter();
		name.isOptional = false;
		name.minLength = 3;
		name.maxLength = 30;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		name.allowInUrl = true;
		name.description = "The category name";
		name.addAlias(new String[]{ "name", "category_name" });
		insert.addParameter(name);
		
		index.addOwnHandler(insert);
	}

	private void initializeUpdate(Index index)
	{
		Action update = new Action()
		{
			public Object execute() throws Exception
			{
				String category = getParameter("name").getValue();
				String id = getParameter("id").getValue();
				
				if( category.matches("^[0-9]+$") || category.matches("(^[\\._\\-\\s]|[\\._\\-\\s]$)") )
					throw new Exception("The category name may not be numeric and may not start or end with special characters");
					
				Database.getInstance().update("UPDATE categories SET category_name = '" + Security.escape(category) + "' WHERE category_id = " + Security.escape(id));
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Changes the category name";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "category_update" });
		
		Parameter category = new Parameter();
		category.isOptional = false;
		category.minLength = 3;
		category.maxLength = 30;
		category.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		category.description = "The category name";
		category.addAlias(new String[]{ "name", "category_name" });
		update.addParameter(category);
		
		Parameter id = new Parameter();
		id.isOptional = false;
		id.allowInUrl = true;
		id.minLength = 1;
		id.maxLength = 11;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.description = "The category id";
		id.addAlias(new String[]{ "id", "cid", "category_id" });
		update.addParameter(id);
		
		index.addOwnHandler(update);
	}
	
	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> category = getParameter("name").getValues();
				
				String where = "1>1";
				for( String c : category )
				{
					if( !c.matches("^[0-9]+$") )
						where += " OR category_name = '" + Security.escape(c) + "'";
					else
						where += " OR category_id = " + c;
				}
				
				Database.getInstance().delete("DELETE FROM categories WHERE  " + where);
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy" });
		delete.description = "Removes a category";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "category_delete" });
		
		Parameter category = new Parameter();
		category.isOptional = false;
		category.minLength = 1;
		category.maxLength = 30;
		category.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		category.allowInUrl = true;
		category.isMultipleValues = true;
		category.description = "The category name(s) or id(s)";
		category.addAlias(new String[]{ "name", "category_name", "id", "category_id", "cid", "names", "category_names", "ids", "category_ids", "cids" });
		delete.addParameter(category);
		
		index.addOwnHandler(delete);
	}
	
	private void initializeSelect(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> category = getParameter("name").getValues();
				
				String where = "";
				if( category.size() > 0 )
				{
					where += " AND (1>1";
					for( String c : category )
					{
						if( c.matches("^[0-9]+$") )
							where += " OR category_id = " + Security.escape(c);
						else
							where += " OR category_name LIKE '%" + Security.escape(c) + "%'";
					}
					where += ")";
				}
				
				return Database.getInstance().select("SELECT category_id, category_name FROM categories WHERE 1=1 " + where + " ORDER BY category_name ASC");
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves information about a category. Multiple search values for a single property will be OR'd and different criteria will be AND'd.";
		select.returnDescription = "The matching categories [{'name', 'id'},...]";
		select.addGrant(new String[] { "access", "category_select" });
		
		Parameter category = new Parameter();
		category.isOptional = true;
		category.minLength = 1;
		category.maxLength = 30;
		category.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		category.allowInUrl = true;
		category.isMultipleValues = true;
		category.description = "The category name or id (will match *[category]* if not a number or an exact category id match if numeric)";
		category.addAlias(new String[]{ "name", "category_name", "id", "cid", "category_id", "category", "categories", "names", "category_names", "ids", "cids", "category_ids" });
		select.addParameter(category);
		
		index.addOwnHandler(select);
	}
}