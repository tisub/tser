package com.busit.rest;

import com.anotherservice.db.*;
import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import com.busit.security.*;
import java.util.*;
import java.text.*;

public class Plan extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "plan", "plans" });
		index.description = "Manages plans";
		Handler.addHandler("/busit/", index);
		
		initializeInsert(index);
		initializeUpdate(index);
		initializeDelete(index);
		initializeSelect(index);

		Self.selfize("/busit/plan/select");
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				String user = getParameter("user").getValue();
				
				if( !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
				
				Database.getInstance().insert("INSERT INTO credit_plan (plan_user) VALUES (" + user + ")");

				return "OK";
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Add a new plan";
		insert.returnDescription = "OK";
		insert.addGrant(new String[] { "access", "plan_insert" });

		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		insert.addParameter(user);
		
		index.addOwnHandler(insert);
	}

	
	private void initializeUpdate(Index index)
	{
		Action update = new Action()
		{
			public Object execute() throws Exception
			{
				String sla = getParameter("sla").getValue();
				String user = getParameter("user").getValue();
				
				if( !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
				
				Database.getInstance().update("UPDATE credit_plan SET plan_sla = " + sla + " WHERE plan_user = " + user);

				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Update a plan";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "plan_update" });

		Parameter sla = new Parameter();
		sla.isOptional = false;
		sla.minLength = 1;
		sla.maxLength = 11;
		sla.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		sla.description = "The user SLA";
		sla.addAlias(new String[]{ "sla", "level" });
		update.addParameter(sla);
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		update.addParameter(user);
		
		index.addOwnHandler(update);
	}
	
	private void initializeDelete(Index index)
	{
		
	}
	
	private void initializeSelect(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				String user = getParameter("user").getValue();
				
				if( !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
					
				String where = "";
				if( user != null )
					where += " AND plan_user = " + user;
				
				Vector<Map<String, String>> result = Database.getInstance().select("SELECT plan_user, plan_window, plan_factor, plan_root, plan_free, plan_sla FROM credit_plan WHERE 1=1 " + where);
				
				return result;
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves information about the plan of a user.";
		select.returnDescription = "The matching plan [{'user', 'window', 'factor'},...]";
		select.addGrant(new String[] { "access", "plan_select" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		select.addParameter(user);
		
		index.addOwnHandler(select);
	}
}
