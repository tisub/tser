package com.busit.rest;

import com.anotherservice.db.*;
import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import com.busit.security.*;
import java.util.*;
import java.util.regex.*;

public class ConnectorError extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "error", "errors" });
		index.description = "Manages connector error";
		Handler.addHandler("/busit/connector/", index);

		initializeSelect(index);
		
		Self.selfize("/busit/connector/error/select");
	}

	private void initializeSelect(Index index)
	{
		Action select = new DelegateAction()
		{
			public Object execute() throws Exception
			{
				String identifier = getParameter("identifier").getValue();
				String id = getParameter("id").getValue();
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
				String ack = getParameter("ack").getValue();
				String read = getParameter("read").getValue();

				if( user != null && id != null )
				{
					if( !IdentityChecker.getInstance().isUserConnector(user, id) )
						throw new Exception("The current user is not an administrator of the provided connector");
				}
				
				if( user != null && !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
				
				String limitation = "";
				String where = "";
				if( identifier != null )
					where += " AND error_id = " + Security.escape(identifier);
				if( id != null )
					where += " AND instance_connector = " + Security.escape(id);
				if( user != null )
					where += " AND connector_user = " + user;
				if( credits != null && credits.matches("^(?i)(yes|true|1)$") )
					where += " AND error_code = 509";
				else if( credits != null )
					where += " AND error_code != 509";				
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
						"LEFT JOIN connectors ON(connector_id = instance_connector) " +
						"WHERE 1" + where + " GROUP BY " + group + "(FROM_UNIXTIME(error_date))");
				if( count != null && count.matches("^(?i)(yes|true|1)$") )
					return Database.getInstance().select("SELECT COUNT(error_id) as count " + 
						"FROM instance_error " + 
						"LEFT JOIN instances ON(instance_id = error_instance) " +
						"LEFT JOIN connectors ON(connector_id = instance_connector) " +
						"WHERE 1" + where + " ORDER BY " + order + " " + ordered + " LIMIT " + limitation);	
				
				Vector<Map<String, String>> logs = Database.getInstance().select("SELECT error_id, error_read, error_instance, error_interface, error_date, instance_user as error_user, error_code, error_message, error_ack, instance_connector, instance_name " + 
				"FROM instance_error " +
				"LEFT JOIN instances ON(error_instance = instance_id)" +
				"LEFT JOIN connectors ON(connector_id = instance_connector) " +
				"WHERE 1 " + where +  " ORDER BY " + order + " " + ordered + " LIMIT " + limitation);	
				
				return logs;
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Select instance logs.";
		select.returnDescription = "The matching logs [{'identifier', 'instance'},...]";
		select.addGrant(new String[] { "access", "instance_select" });

		Parameter identifier = new Parameter();
		identifier.isOptional = true;
		identifier.minLength = 1;
		identifier.maxLength = 40;
		identifier.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER);
		identifier.description = "The identifier";
		identifier.addAlias(new String[]{ "identifier" });
		select.addParameter(identifier);	
		
		Parameter id = new Parameter();
		id.isOptional = true;
		id.minLength = 1;
		id.maxLength = 20;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.description = "The connector id";
		id.addAlias(new String[]{ "connector", "id", "connector_id", "cid" });
		select.addParameter(id);

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
