package com.busit.rest;

import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import com.anotherservice.db.*;
import java.util.*;

public class KnownType extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "knowntype", "type", "data" });
		index.description = "Manages known types";
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
				String data = getParameter("data").getValue();
				String name = getParameter("name").getValue();
				
				Long id = Database.getInstance().insert("INSERT INTO knowntype (knowntype_name, knowntype_data) VALUES ('" + 
					Security.escape(name) + "', '" + Security.escape(data) + "')");
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("name", name);
				result.put("id", id);
				return result;
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Creates a new known type";
		insert.returnDescription = "The newly created known type {'id', 'name'}";
		insert.addGrant(new String[] { "access", "knowntype_insert" });
		
		Parameter name = new Parameter();
		name.isOptional = false;
		name.minLength = 1;
		name.maxLength = 30;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT);
		name.description = "The known type name";
		name.addAlias(new String[]{ "name", "knowntype_name", "type_name" });
		insert.addParameter(name);
		
		Parameter data = new Parameter();
		data.isOptional = true;
		data.minLength = 0;
		data.maxLength = 15000;
		data.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		data.description = "The known type data";
		data.addAlias(new String[]{ "data", "knowntype_data" });
		insert.addParameter(data);
		
		index.addOwnHandler(insert);
	}

	private void initializeUpdate(Index index)
	{
		Action update = new Action()
		{
			public Object execute() throws Exception
			{
				String id = getParameter("id").getValue();
				String name = getParameter("name").getValue();
				String data = getParameter("data").getValue();
				
				if( name != null )
					name = ", knowntype_name = '" + Security.escape(name) + "'";
				if( data != null )
					data = ", knowntype_data = '" + Security.escape(data) + "'";
				
				Database.getInstance().update("UPDATE knowntype SET knowntype_id = knowntype_id" + name + data + " WHERE knowntype_id = " + id);
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Changes the known type";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "knowntype_update" });
		
		Parameter name = new Parameter();
		name.isOptional = true;
		name.minLength = 1;
		name.maxLength = 30;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT);
		name.description = "The known type name";
		name.addAlias(new String[]{ "name", "knowntype_name", "type_name" });
		update.addParameter(name);
		
		Parameter data = new Parameter();
		data.isOptional = true;
		data.minLength = 0;
		data.maxLength = 15000;
		data.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		data.description = "The known type data";
		data.addAlias(new String[]{ "data", "knowntype_data" });
		update.addParameter(data);
		
		Parameter id = new Parameter();
		id.isOptional = false;
		id.minLength = 1;
		id.maxLength = 11;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.description = "The known type id";
		id.addAlias(new String[]{ "id", "knowntype_id" });
		update.addParameter(id);
		
		index.addOwnHandler(update);
	}
	
	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				String id = getParameter("id").getValue();
					
				Database.getInstance().delete("DELETE FROM knowntype WHERE knowntype_id = " + id);
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy" });
		delete.description = "Removes a known type";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "knowntype_delete" });
		
		Parameter id = new Parameter();
		id.isOptional = false;
		id.minLength = 1;
		id.maxLength = 11;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.description = "The known type id";
		id.addAlias(new String[]{ "id", "knowntype_id" });
		delete.addParameter(id);
		
		index.addOwnHandler(delete);
	}
	
	private void initializeSelect(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				String id = getParameter("id").getValue();
				
				if( id.matches("^[0-9]+$") )
					id = "knowntype_id = " + id;
				else
					id = "knowntype_name = '" + Security.escape(id) + "'";
					
				return Database.getInstance().select("SELECT * FROM knowntype WHERE " + id);
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves knowntype data";
		select.returnDescription = "The matching data {'data', 'id', 'name'}";
		//select.addGrant(new String[] { }); // NO GANTS FOR KNOWNTYPE SELECT
		
		Parameter id = new Parameter();
		id.isOptional = false;
		id.minLength = 1;
		id.maxLength = 30;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT );
		id.description = "The known type name or id";
		id.addAlias(new String[]{ "id", "knowntype_id", "name", "knowntype_name" });
		select.addParameter(id);
		
		index.addOwnHandler(select);
	}
}