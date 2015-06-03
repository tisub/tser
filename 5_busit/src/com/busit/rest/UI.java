package com.busit.rest;

import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import com.anotherservice.db.*;
import java.util.*;

public class UI extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "ui", "gui" });
		index.description = "Manages UI data";
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
				String key = getParameter("key").getValue();
				String data = getParameter("data").getValue();
				String user = getParameter("user").getValue();
				
				if( !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
				
				Long id = Database.getInstance().insert("INSERT INTO ui (ui_uid, ui_data, ui_user) VALUES (" + 
					(key == null ? "NULL" : "'" + Security.escape(key) + "'") + ", '" + 
					Security.escape(data) + "', " + 
					user + ")");
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("key", key);
				result.put("id", id);
				return result;
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Creates a new UI data container";
		insert.returnDescription = "The newly created UI data container {'id', 'key'}";
		insert.addGrant(new String[] { "access" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "ui_user" });
		insert.addParameter(user);
		
		Parameter data = new Parameter();
		data.isOptional = true;
		data.minLength = 0;
		data.maxLength = 15000;
		data.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		data.description = "The ui data";
		data.addAlias(new String[]{ "data", "ui_data" });
		insert.addParameter(data);
		
		Parameter key = new Parameter();
		key.isOptional = true;
		key.minLength = 0;
		key.maxLength = 50;
		key.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER);
		key.description = "The ui key. Caution, you cannot change the key afterwards";
		key.addAlias(new String[]{ "key", "ui_key" });
		insert.addParameter(key);
		
		index.addOwnHandler(insert);
	}

	private void initializeUpdate(Index index)
	{
		Action update = new Action()
		{
			public Object execute() throws Exception
			{
				String id = getParameter("id").getValue();
				String key = getParameter("key").getValue();
				String data = getParameter("data").getValue();
				String user = getParameter("user").getValue();
				
				if( !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
				
				if( id == null && key == null )
					throw new Exception("One of id or key must be specified");
				
				String where = "ui_user = " + user;
				if( id != null ) where += " AND ui_id = " + id;
				if( key != null ) where += " AND ui_key = '" + Security.escape(key) + "'";
					
				Database.getInstance().update("UPDATE ui SET ui_data = '" + Security.escape(data) + "' " + 
					"WHERE " + where);
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Changes the ui data";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "ui_user" });
		update.addParameter(user);
		
		Parameter data = new Parameter();
		data.isOptional = true;
		data.minLength = 0;
		data.maxLength = 15000;
		data.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		data.description = "The ui data";
		data.addAlias(new String[]{ "data", "ui_data" });
		update.addParameter(data);
		
		Parameter key = new Parameter();
		key.isOptional = true;
		key.minLength = 0;
		key.maxLength = 50;
		key.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER);
		key.description = "The ui key";
		key.addAlias(new String[]{ "key", "ui_key" });
		update.addParameter(key);
		
		Parameter id = new Parameter();
		id.isOptional = true;
		id.minLength = 1;
		id.maxLength = 11;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.description = "The ui id";
		id.addAlias(new String[]{ "id", "ui_id" });
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
				String key = getParameter("key").getValue();
				String user = getParameter("user").getValue();
				
				if( !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
				
				if( id == null && key == null )
					throw new Exception("One of id or key must be specified");
				
				String where = "ui_user = " + user;
				if( id != null ) where += " AND ui_id = " + id;
				if( key != null ) where += " AND ui_key = '" + Security.escape(key) + "'";
					
				Database.getInstance().delete("DELETE FROM ui WHERE " + where);
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy" });
		delete.description = "Removes ui data";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "ui_user" });
		delete.addParameter(user);
		
		Parameter key = new Parameter();
		key.isOptional = true;
		key.minLength = 0;
		key.maxLength = 50;
		key.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER);
		key.description = "The ui key";
		key.addAlias(new String[]{ "key", "ui_key" });
		delete.addParameter(key);
		
		Parameter id = new Parameter();
		id.isOptional = true;
		id.minLength = 1;
		id.maxLength = 11;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.description = "The ui id";
		id.addAlias(new String[]{ "id", "ui_id" });
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
				String key = getParameter("key").getValue();
				String user = getParameter("user").getValue();
				
				if( !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
				
				if( id == null && key == null )
					throw new Exception("One of id or key must be specified");
				
				String where = "ui_user = " + user;
				if( id != null ) where += " AND ui_id = " + id;
				if( key != null ) where += " AND ui_key = '" + Security.escape(key) + "'";
					
				return Database.getInstance().select("SELECT * FROM ui WHERE " + where);
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves ui data";
		select.returnDescription = "The matching data {'data', 'id', 'key', 'user'}";
		select.addGrant(new String[] { "access" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "ui_user" });
		select.addParameter(user);
		
		Parameter key = new Parameter();
		key.isOptional = true;
		key.minLength = 0;
		key.maxLength = 50;
		key.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER);
		key.description = "The ui key";
		key.addAlias(new String[]{ "key", "ui_key" });
		select.addParameter(key);
		
		Parameter id = new Parameter();
		id.isOptional = true;
		id.minLength = 1;
		id.maxLength = 11;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.description = "The ui id";
		id.addAlias(new String[]{ "id", "ui_id" });
		select.addParameter(id);
		
		index.addOwnHandler(select);
	}
}