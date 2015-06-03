package com.busit.rest;

import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import com.anotherservice.db.*;
import java.util.*;

public class TagObject extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "connector", "connectors" });
		index.description = "Manages connector tags";
		Handler.addHandler("/busit/tag", index);
		
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
				Collection<String> connector = getParameter("connector").getValues();
				Collection<String> tag = getParameter("tag").getValues();
				
				String where = " AND (1>1";
				for( String c : connector )
				{
					if( c.matches("^[0-9]+$") )
						where += " OR c.object_id = " + Security.escape(c);
					else
						where += " OR c.object_name = '" + Security.escape(c) + "'";
				}
				
				where += ") AND (1>1";
				for( String t : tag )
				{
					if( t.matches("^[0-9]+$") )
						where += " OR t.tag_id = " + Security.escape(t);
					else
						where += " OR t.tag_name = '" + Security.escape(t) + "'";
				}
				where += ")";
				
				Database.getInstance().insert("INSERT IGNORE INTO connector_tag (connector_id, tag_id) " +
					"SELECT DISTINCT c.object_id, t.tag_id FROM connectors c, tags t " +
					"WHERE 1=1 " + where);
				return "OK";
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Apply the selected tags to the target connector";
		insert.returnDescription = "OK";
		insert.addGrant(new String[] { "access", "tag_connector_insert" });
		
		Parameter connector = new Parameter();
		connector.isOptional = false;
		connector.minLength = 1;
		connector.maxLength = 30;
		connector.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		connector.allowInUrl = true;
		connector.isMultipleValues = true;
		connector.description = "The connector name(s) or id(s)";
		connector.addAlias(new String[]{ "connector", "connector_name", "cid", "connector_id", "connector_names", "connectors", "cids", "connector_ids" });
		insert.addParameter(connector);
		
		Parameter tag = new Parameter();
		tag.isOptional = false;
		tag.minLength = 1;
		tag.maxLength = 30;
		tag.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		tag.isMultipleValues = true;
		tag.description = "The tag name(s) or id(s)";
		tag.addAlias(new String[]{ "tag", "tag_name", "tid", "tag_id", "tag_names", "tags", "tids", "tag_ids" });
		insert.addParameter(tag);
		
		index.addOwnHandler(insert);
	}

	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> object = getParameter("object").getValues();
				Collection<String> tag = getParameter("tag").getValues();
				
				String where = " AND (1>1";
				for( String o : object )
				{
					if( o.matches("^[0-9]+$") )
						where += " OR o.object_id = " + Security.escape(o);
					else
						where += " OR o.object_name = '" + Security.escape(o) + "'";
				}
				
				where += ") AND (1>1";
				for( String t : tag )
				{
					if( t.matches("^[0-9]+$") )
						where += " OR t.tag_id = " + Security.escape(t);
					else
						where += " OR t.tag_name = '" + Security.escape(t) + "'";
				}
				where += ")";
				
				Database.getInstance().delete("DELETE ot FROM object_tag ot " +
					"LEFT JOIN objects o ON(o.object_id = ot.object_id) " +
					"LEFT JOIN tags t ON(t.tag_id = ot.tag_id) " +
					"WHERE 1=1 " + where);
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "destroy", "remove" });
		delete.description = "Removes the target tags from the provided objects";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "tag_object_delete" });
		
		Parameter object = new Parameter();
		object.isOptional = false;
		object.minLength = 1;
		object.maxLength = 30;
		object.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		object.allowInUrl = true;
		object.isMultipleValues = true;
		object.description = "The object name(s) or id(s)";
		object.addAlias(new String[]{ "object", "object_name", "oid", "object_id", "object_names", "objects", "oids", "object_ids" });
		delete.addParameter(object);
		
		Parameter tag = new Parameter();
		tag.isOptional = false;
		tag.minLength = 1;
		tag.maxLength = 30;
		tag.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		tag.isMultipleValues = true;
		tag.description = "The tag name(s) or id(s)";
		tag.addAlias(new String[]{ "tag", "tag_name", "tid", "tag_id", "tag_names", "tags", "tids", "tag_ids" });
		delete.addParameter(tag);
		
		index.addOwnHandler(delete);
	}
	
	private void initializeSelect(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				String connector = getParameter("connector").getValue();
				String tag = getParameter("tag").getValue();
				
				if( (connector == null) == (tag == null) )
					throw new Exception("The connector and tag parameters are mutually exclusive but one of them must be specified");

				String where = "";
				if( connector != null )
				{
					if( connector.matches("^[0-9]+$") )
						where = " c.object_id = " + Security.escape(connector);
					else
						where = " c.object_name = '" + Security.escape(connector) + "'";
						
					return Database.getInstance().select("SELECT t.tag_name, t.tag_id FROM connectors c " +
						"LEFT JOIN connector_tag ct ON(c.connector_id = ct.connector_id) " +
						"LEFT JOIN tags t ON(t.tag_id = ot.tag_id) " +
						"WHERE t.tag_id IS NOT NULL AND " + where);
				}
				else
				{
					if( tag.matches("^[0-9]+$") )
						where = " t.tag_id = " + Security.escape(tag);
					else
						where = " t.tag_name = '" + Security.escape(tag) + "'";
						
					return Database.getInstance().select("SELECT c.connector_name, c.connector_id FROM tags t " +
						"LEFT JOIN connector_tag ct ON(t.tag_id = ct.tag_id) " +
						"LEFT JOIN connectors c ON(c.connector_id = ct.object_id) " +
						"WHERE c.connector_id IS NOT NULL AND " + where);
				}
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves all tags of a object or all objects of a tag.";
		select.returnDescription = "The matching objects or tags [{'name', 'id'},...]";
		select.addGrant(new String[] { "access", "tag_object_select" });
		
		Parameter object = new Parameter();
		object.isOptional = true;
		object.minLength = 1;
		object.maxLength = 30;
		object.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		object.description = "The object name or id";
		object.addAlias(new String[]{ "object", "object_name", "oid", "object_id" });
		select.addParameter(object);
		
		Parameter tag = new Parameter();
		tag.isOptional = true;
		tag.minLength = 1;
		tag.maxLength = 30;
		tag.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		tag.description = "The tag name or id";
		tag.addAlias(new String[]{ "tag", "tag_name", "tid", "tag_id" });
		select.addParameter(tag);
		
		index.addOwnHandler(select);
	}
}