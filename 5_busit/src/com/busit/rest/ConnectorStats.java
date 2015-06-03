package com.busit.rest;

import com.anotherservice.db.*;
import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import com.busit.security.*;
import java.util.*;
import java.util.regex.*;

public class ConnectorStats extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "stats" });
		index.description = "Manages connector stats";
		Handler.addHandler("/busit/connector/", index);
		
		initializeInsert(index);
		initializeSelect(index);
		
		Self.selfize("/busit/connector/stats/select");
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				String connector = getParameter("connector").getValue();
				String timestamp = getParameter("timestamp").getValue();
				String messages = getParameter("messages").getValue();
				String size = getParameter("size").getValue();
				String credits_user = getParameter("credits_user").getValue();
				String credits_developer = getParameter("credits_developer").getValue();
				String credits_busit = getParameter("credits_busit").getValue();

				Map<String, String> check = Database.getInstance().selectOne("SELECT stat_id FROM connector_stats WHERE stat_connector = '" + Security.escape(connector) + "' AND stat_timestamp = " + Security.escape(timestamp));
				if( check != null && check.get("stat_id") != null ) 
				{
					Database.getInstance().update("UPDATE connector_stats SET stat_messages = '" + Security.escape(messages) + "', stat_size = '" + Security.escape(size) + "', stat_credits_user = '" + Security.escape(credits_user) + "', stat_credits_developer = '" + Security.escape(credits_developer) + "', stat_credits_busit = '" + Security.escape(credits_busit) + "' WHERE stat_connector = '" + Security.escape(connector) + "' AND stat_timestamp = " + Security.escape(timestamp));
				}
				else
				{
					Database.getInstance().insert("INSERT INTO connector_stats (stat_connector, stat_timestamp, stat_messages, stat_size, stat_credits_user, stat_credits_developer, stat_credits_busit) " + 
					"VALUES ('" + Security.escape(connector) + "', '" + Security.escape(timestamp) + "', '" + Security.escape(messages) + "', '" + Security.escape(size) + "', '" + Security.escape(credits_user) + "', '" + Security.escape(credits_developer) + "', '" + Security.escape(credits_busit) + "')");
				}
				
				return "OK";
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Add a new stat";
		insert.returnDescription = "OK";
		insert.addGrant(new String[] { Config.gets("com.busit.rest.brokerGrantName") });

		Parameter connector = new Parameter();
		connector.isOptional = false;
		connector.minLength = 1;
		connector.maxLength = 40;
		connector.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		connector.description = "The connector id";
		connector.addAlias(new String[]{ "connector" });
		insert.addParameter(connector);	
		
		Parameter timestamp = new Parameter();
		timestamp.isOptional = false;
		timestamp.minLength = 1;
		timestamp.maxLength = 40;
		timestamp.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		timestamp.description = "The timestamp";
		timestamp.addAlias(new String[]{ "timestamp" });
		insert.addParameter(timestamp);	
		
		Parameter messages = new Parameter();
		messages.isOptional = false;
		messages.minLength = 1;
		messages.maxLength = 100;
		messages.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		messages.description = "Number of messages";
		messages.addAlias(new String[]{ "messages" });
		insert.addParameter(messages);
		
		Parameter size = new Parameter();
		size.isOptional = false;
		size.minLength = 1;
		size.maxLength = 100;
		size.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		size.description = "Total size";
		size.addAlias(new String[]{ "size" });
		insert.addParameter(size);
		
		Parameter credits_user = new Parameter();
		credits_user.isOptional = false;
		credits_user.minLength = 1;
		credits_user.maxLength = 100;
		credits_user.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		credits_user.description = "User credits";
		credits_user.addAlias(new String[]{ "user_credits", "credits_user" });
		insert.addParameter(credits_user);
		
		Parameter credits_developer = new Parameter();
		credits_developer.isOptional = false;
		credits_developer.minLength = 1;
		credits_developer.maxLength = 100;
		credits_developer.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		credits_developer.description = "Developer credits";
		credits_developer.addAlias(new String[]{ "developer_credits", "credits_developer" });
		insert.addParameter(credits_developer);
		
		Parameter credits_busit = new Parameter();
		credits_busit.isOptional = false;
		credits_busit.minLength = 1;
		credits_busit.maxLength = 100;
		credits_busit.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		credits_busit.description = "Busit credits";
		credits_busit.addAlias(new String[]{ "busit_credits", "credits_busit" });
		insert.addParameter(credits_busit);
		
		index.addOwnHandler(insert);
	}
	
	private void initializeSelect(Index index)
	{
		Action select = new DelegateAction()
		{
			public Object execute() throws Exception
			{
				String connector = getParameter("connector").getValue();
				String from = getParameter("from").getValue();
				String to = getParameter("to").getValue();
				String start = getParameter("start").getValue();
				String limit = getParameter("limit").getValue();
				String order = getParameter("order").getValue();
				String ordered = getParameter("ordered").getValue();
				String user = getParameter("user").getValue();
				String group = getParameter("group").getValue();
				
				if( user != null && connector != null )
				{
					if( !IdentityChecker.getInstance().isUserConnector(user, connector) )
						throw new Exception("The current user is not an administrator of the provided connector");
				}
				
				if( user != null && !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
				
				String limitation = "";
				String where = "";
				if( connector != null )
					where += " AND stat_connector = " + Security.escape(connector);
				if( user != null )
					where += " AND connector_user = " + user;		
				if( from != null )
					where += " AND stat_timestamp >= " + Security.escape(from);
				if( to != null )
					where += " AND stat_timestamp <= " + Security.escape(to);
				if( start != null && limit != null )
					limitation += start + ", " + limit;
				else
					limitation += "0, 10";
				if( order != null )
					order = Security.escape(order);
				else
					order = "stat_timestamp";
	
				if( ordered != null )
					ordered = Security.escape(ordered);
				else
					ordered = "DESC";

				if( group != null )
				{
					return Database.getInstance().select("SELECT SUM(stat_messages) as messages, SUM(stat_size) as size, SUM(stat_credits_user) as credits_user, SUM(stat_credits_developer) as credits_developer, SUM(stat_credits_busit) as credits_busit, " + group + "(FROM_UNIXTIME(stat_timestamp)) as " + group + " " + 
						"FROM connector_stats " + 
						"LEFT JOIN connectors ON(connector_id = stat_connector) " +
						"WHERE 1" + where + " GROUP BY " + group + "(FROM_UNIXTIME(stat_timestamp))");
				}
				 
				return Database.getInstance().select("SELECT SUM(stat_messages) as messages, SUM(stat_size) as size, SUM(stat_credits_user) as credits_user, SUM(stat_credits_developer) as credits_developer, SUM(stat_credits_busit) as credits_busit " + 
						"FROM connector_stats " + 
						"LEFT JOIN connectors ON(connector_id = stat_connector) " +
						"WHERE 1" + where + " ORDER BY " + order + " " + ordered + " LIMIT " + limitation);	
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Select connector stats.";
		select.returnDescription = "The matching logs [{'identifier', 'connector'},...]";
		select.addGrant(new String[] { "access", "connector_select" });
		
		Parameter connector = new Parameter();
		connector.isOptional = true;
		connector.minLength = 1;
		connector.maxLength = 20;
		connector.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		connector.description = "The connector id";
		connector.addAlias(new String[]{ "connector", "id", "connector_id", "cid" });
		select.addParameter(connector);

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
		to.description = "To timestamp";
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

		Parameter group = new Parameter();
		group.isOptional = true;
		group.minLength = 1;
		group.maxLength = 5;
		group.mustMatch = PatternBuilder.getRegex(PatternBuilder.UPPER);
		group.description = "Count grouped by?";
		group.addAlias(new String[]{ "group", "group_by" });
		select.addParameter(group);
		
		index.addOwnHandler(select);	
	}
}
