package com.busit.rest;

import com.anotherservice.db.*;
import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import com.busit.security.*;
import java.util.*;
import java.util.regex.*;

public class InstanceError extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "error", "errors" });
		index.description = "Manages instance errors";
		Handler.addHandler("/busit/instance/", index);
		
		initializeInsert(index);
		initializeUpdate(index);
		initializeDelete(index);
		initializeSelect(index);
		initializeReadAll(index);
		
		Self.selfize("/busit/instance/error/select");
		Self.selfize("/busit/instance/error/delete");
		Self.selfize("/busit/instance/error/readall");
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				String instance = getParameter("instance").getValue();
				String key = getParameter("key").getValue();
				String message = getParameter("message").getValue();
				String code = getParameter("code").getValue();

				Database.getInstance().insert("INSERT INTO instance_error (error_instance, error_interface, error_date, error_code, error_message) " + 
				"VALUES('" + Security.escape(instance) + "', '" + Security.escape(key) + "', UNIX_TIMESTAMP(), '" + Security.escape(code) + "', '" +  Security.escape(message) + "')");
				
				return "OK";
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Add a new error";
		insert.returnDescription = "OK";
		insert.addGrant(new String[] { Config.gets("com.busit.rest.brokerGrantName") });

		Parameter instance = new Parameter();
		instance.isOptional = false;
		instance.minLength = 1;
		instance.maxLength = 40;
		instance.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		instance.description = "The instance id";
		instance.addAlias(new String[]{ "instance" });
		insert.addParameter(instance);	

		Parameter key = new Parameter();
		key.isOptional = false;
		key.minLength = 1;
		key.maxLength = 40;
		key.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		key.description = "The instance interface";
		key.addAlias(new String[]{ "key", "interface" });
		insert.addParameter(key);	
	
		Parameter message = new Parameter();
		message.isOptional = true;
		message.minLength = 1;
		message.maxLength = 5000;
		message.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		message.description = "The message";
		message.addAlias(new String[]{ "message" });
		insert.addParameter(message);	

		Parameter code = new Parameter();
		code.isOptional = true;
		code.minLength = 1;
		code.maxLength = 40;
		code.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		code.description = "The code number";
		code.addAlias(new String[]{ "code", "error_code" });
		insert.addParameter(code);			
		
		index.addOwnHandler(insert);
	}

	private void initializeUpdate(Index index)
	{
		Action update = new Action()
		{
			public Object execute() throws Exception
			{
				String id = getParameter("id").getValue();
				String ack = getParameter("ack").getValue();
				String read = getParameter("read").getValue();
				
				String update = "";			
				if( ack != null )
					update = ", error_ack = '" + Security.escape(ack) + "' ";
				if( read != null )
					update = ", error_read = '" + Security.escape(read) + "' ";
					
				Database.getInstance().update("UPDATE instance_error SET error_id = error_id " + update + " WHERE error_id = " + id);
				
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Update a log entry";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "log_update" });

		Parameter id = new Parameter();
		id.isOptional = false;
		id.minLength = 1;
		id.maxLength = 20;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.description = "The error id";
		id.addAlias(new String[]{ "id", "error", "error_id" });
		update.addParameter(id);	
		
		Parameter ack = new Parameter();
		ack.isOptional = true;
		ack.minLength = 1;
		ack.maxLength = 1;
		ack.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		ack.description = "The ack";
		ack.addAlias(new String[]{ "ack" });
		update.addParameter(ack);

		Parameter read = new Parameter();
		read.isOptional = true;
		read.minLength = 1;
		read.maxLength = 1;
		read.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		read.description = "The read";
		read.addAlias(new String[]{ "read" });
		update.addParameter(read);
		
		index.addOwnHandler(update);		
	}

	private void initializeReadAll(Index index)
	{
		Action readall = new Action()
		{
			public Object execute() throws Exception
			{
				String user = getParameter("user").getValue();
				
				if( !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";

				String sql = "UPDATE instance_error SET error_read = TRUE WHERE error_instance IN (SELECT instance_id FROM instances WHERE instance_user = " + user + ")";
				Database.getInstance().update(sql);
				
				return "OK";
			}
		};
		
		readall.addMapping(new String[] { "readall", "read" });
		readall.description = "Mark all log entries as read";
		readall.returnDescription = "OK";
		readall.addGrant(new String[] { "access", "log_update" });

		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		readall.addParameter(user);
		
		index.addOwnHandler(readall);		
	}
	
	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				String id = getParameter("id").getValue();
				String user = getParameter("user").getValue();

				if( user != null && !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
				
				String where = "";
				if( user != null )
					where += " AND instance_user = " + user;
				
				Database.getInstance().delete("DELETE instance_error FROM instance_error LEFT JOIN instances ON(instance_id = error_instance) WHERE error_id = " + id + where);
			
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy" });
		delete.description = "Delete a log entry";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "log_delete" });

		Parameter id = new Parameter();
		id.isOptional = false;
		id.minLength = 1;
		id.maxLength = 20;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.description = "The error id";
		id.addAlias(new String[]{ "id", "error", "error_id" });
		delete.addParameter(id);
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		delete.addParameter(user);
		
		index.addOwnHandler(delete);	
	}
	
	private void initializeSelect(Index index)
	{
		Action select = new DelegateAction()
		{
			public Object execute() throws Exception
			{
				String id = getParameter("id").getValue();
				String instance = getParameter("instance").getValue();
				String from = getParameter("from").getValue();
				String to = getParameter("to").getValue();
				String start = getParameter("start").getValue();
				String limit = getParameter("limit").getValue();
				String order = getParameter("order").getValue();
				String ordered = getParameter("ordered").getValue();
				String user = getParameter("user").getValue();
				String count = getParameter("count").getValue();
				String group = getParameter("group").getValue();
				String credits = getParameter("credits").getValue();
				String developer = getParameter("developer").getValue();
				String ack = getParameter("ack").getValue();
				String read = getParameter("read").getValue();
				
				if( user != null && id != null )
				{
					if( !IdentityChecker.getInstance().isUserInstanceUser(user, id) )
						throw new Exception("The current user is not an administrator of the provided instance");
				}
				
				if( user != null && !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
				
				String limitation = "";
				String where = "";
				if( id != null )
					where += " AND error_id = " + Security.escape(id);
				if( instance != null )
					where += " AND error_instance = " + Security.escape(instance);
				if( user != null )
					where += " AND instance_user = " + user;
				if( credits != null && credits.matches("^(?i)(yes|true|1)$") )
					where += " AND error_code = 509";
				else if( credits != null )
					where += " AND error_code != 509";		
				if( developer != null && developer.matches("^(?i)(yes|true|1)$") )
					where += " AND error_code = 500";
				else if( developer != null )
					where += " AND error_code != 500";
				if( from != null )
					where += " AND error_date >= " + Security.escape(from);
				if( to != null )
					where += " AND error_date <= " + Security.escape(to);
				if( ack != null && ack.matches("^(?i)(yes|true|1)$") )
					where += " AND error_ack = 1";
				else if( ack != null )
					where += " AND error_ack = 0";
				if( read != null && read.matches("^(?i)(yes|true|1)$") )
					where += " AND error_read = 1";
				else if( read != null )
					where += " AND error_read = 0";
					
				if( start != null && limit != null )
					limitation += start + ", " + limit;
				else
					limitation += "0, 10";
			
				if( order != null )
					order = Security.escape(order);
				else
					order = "error_date";
	
				if( ordered != null )
					ordered = Security.escape(ordered);
				else
					ordered = "DESC";

				if( group != null )
					return Database.getInstance().select("SELECT COUNT(*) as count, " + group + "(FROM_UNIXTIME(error_date)) as " + group + " " + 
						"FROM instance_error " + 
						"LEFT JOIN instances ON(instance_id = error_instance) " +
						"WHERE 1" + where + " GROUP BY " + group + "(FROM_UNIXTIME(error_date))");
				if( count != null && count.matches("^(?i)(yes|true|1)$") )
					return Database.getInstance().select("SELECT COUNT(error_id) as count " + 
						"FROM instance_error " + 
						"LEFT JOIN instances ON(instance_id = error_instance) " +
						"WHERE 1" + where + " ORDER BY " + order + " " + ordered + " LIMIT " + limitation);	
					
				Vector<Map<String, String>> logs = Database.getInstance().select("SELECT error_id, error_read, error_instance, error_interface, error_date, instance_user as error_user, error_code, error_message, error_ack, instance_connector, instance_name, connector_use_price, connector_use_tax " + 
				"FROM instance_error " +
				"LEFT JOIN instances ON(error_instance = instance_id)" +
				"LEFT JOIN connectors ON(connector_id = instance_connector)" +
				"WHERE 1 " + where +  " ORDER BY " + order + " " + ordered + " LIMIT " + limitation);	
				
				return logs;
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Select instance errors.";
		select.returnDescription = "The matching errors [{'id', 'instance'},...]";
		select.addGrant(new String[] { "access", "log_select" });

		Parameter id = new Parameter();
		id.isOptional = true;
		id.minLength = 1;
		id.maxLength = 20;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.description = "The error id";
		id.addAlias(new String[]{ "id", "error", "error_id" });
		select.addParameter(id);	
		
		Parameter instance = new Parameter();
		instance.isOptional = true;
		instance.minLength = 1;
		instance.maxLength = 20;
		instance.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		instance.description = "The instance id";
		instance.addAlias(new String[]{ "instance", "instance_id", "iid" });
		select.addParameter(instance);

		Parameter from = new Parameter();
		from.isOptional = true;
		from.minLength = 1;
		from.maxLength = 11;
		from.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		from.description = "From timestamp";
		from.addAlias(new String[]{ "from" });
		select.addParameter(from);

		Parameter to = new Parameter();
		to.isOptional = true;
		to.minLength = 1;
		to.maxLength = 11;
		to.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		to.description = "From timestamp";
		to.addAlias(new String[]{ "to" });
		select.addParameter(to);

		Parameter start = new Parameter();
		start.isOptional = true;
		start.minLength = 1;
		start.maxLength = 11;
		start.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		start.description = "Start parameter for limits (SQL like: LIMIT start,limit).";
		start.addAlias(new String[]{ "start", "limit_start" });
		select.addParameter(start);

		Parameter limit = new Parameter();
		limit.isOptional = true;
		limit.minLength = 1;
		limit.maxLength = 11;
		limit.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		limit.description = "Start parameter for limits (SQL like: LIMIT start,limit).";
		limit.addAlias(new String[]{ "limit" });
		select.addParameter(limit);		

		Parameter order = new Parameter();
		order.isOptional = true;
		order.minLength = 1;
		order.maxLength = 30;
		order.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		order.description = "Order by?";
		order.addAlias(new String[]{ "order", "order_by" });
		select.addParameter(order);

		Parameter ordered = new Parameter();
		ordered.isOptional = true;
		ordered.minLength = 1;
		ordered.maxLength = 5;
		ordered.mustMatch = PatternBuilder.getRegex(PatternBuilder.UPPER);
		ordered.description = "Order by?";
		ordered.addAlias(new String[]{ "ordered", "order_type" });
		select.addParameter(ordered);
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		select.addParameter(user);

		Parameter count = new Parameter();
		count.isOptional = true;
		count.minLength = 1;
		count.maxLength = 10;
		count.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		count.description = "Count messages?";
		count.addAlias(new String[]{ "count" });
		select.addParameter(count);
		
		Parameter group = new Parameter();
		group.isOptional = true;
		group.minLength = 1;
		group.maxLength = 5;
		group.mustMatch = PatternBuilder.getRegex(PatternBuilder.UPPER);
		group.description = "Count grouped by?";
		group.addAlias(new String[]{ "group", "group_by" });
		select.addParameter(group);

		Parameter credits = new Parameter();
		credits.isOptional = true;
		credits.minLength = 1;
		credits.maxLength = 10;
		credits.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		credits.description = "Select only credits error?";
		credits.addAlias(new String[]{ "credits" });
		select.addParameter(credits);
	
		Parameter developer = new Parameter();
		developer.isOptional = true;
		developer.minLength = 1;
		developer.maxLength = 10;
		developer.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		developer.description = "Select only developer error?";
		developer.addAlias(new String[]{ "developer" });
		select.addParameter(developer);
		
		Parameter ack = new Parameter();
		ack.isOptional = true;
		ack.minLength = 1;
		ack.maxLength = 10;
		ack.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		ack.description = "Select only non ack?";
		ack.addAlias(new String[]{ "ack" });
		select.addParameter(ack);

		Parameter read = new Parameter();
		read.isOptional = true;
		read.minLength = 1;
		read.maxLength = 10;
		read.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		read.description = "Select only non read?";
		read.addAlias(new String[]{ "read" });
		select.addParameter(read);
		
		index.addOwnHandler(select);	
	}
}
