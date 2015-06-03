package com.busit.rest;

import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import com.anotherservice.db.*;
import java.util.*;

public class Flow extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "flow", "flows" });
		index.description = "Manages connector flows";
		Handler.addHandler("/busit/", index);
		
		initializeOn(index);
		initializeOff(index);
		Self.selfize("/busit/flow/on");
		Self.selfize("/busit/flow/off");
	}
	
	private void initializeOn(Index index)
	{
		Action on = new Action()
		{
			public Object execute() throws Exception
			{
				String flow = getParameter("flow").getValue();
				String user = getParameter("user").getValue();
				
				if( !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
				if( flow.matches("^[0-9]+$") )
					flow = "s.space_id = " + flow;
				else
					throw new Exception("Invalid flow ID");
				
				Database.getInstance().update("UPDATE links SET link_active = true " + 
					"WHERE instance_from IN(" + 
						"SELECT i.instance_id " + 
						"FROM spaces s " + 
						"LEFT JOIN instance_space _is ON(_is.space_id = s.space_id) " +
						"LEFT JOIN instances i ON(_is.instance_id = i.instance_id) " + 
						"WHERE s.space_user = " + user + " AND " + flow + ")" + 
					" AND instance_to IN(" + 
						"SELECT i.instance_id " + 
						"FROM spaces s " + 
						"LEFT JOIN instance_space _is ON(_is.space_id = s.space_id) " +
						"LEFT JOIN instances i ON(_is.instance_id = i.instance_id) " + 
						"WHERE s.space_user = " + user + " AND " + flow + ")");
				return "OK";
			}
		};
		
		on.addMapping(new String[] { "activate", "on", "enable" });
		on.description = "Enables messages to go through this flow (enables every link inside it)";
		on.returnDescription = "OK";
		on.addGrant(new String[] { "access", "link_update" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "ui_user" });
		on.addParameter(user);
		
		Parameter flow = new Parameter();
		flow.isOptional = false;
		flow.minLength = 1;
		flow.maxLength = 20;
		flow.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		flow.description = "The flow id";
		flow.addAlias(new String[]{ "id", "flow_id", "flow" });
		on.addParameter(flow);
		
		index.addOwnHandler(on);
	}

	private void initializeOff(Index index)
	{
		Action off = new Action()
		{
			public Object execute() throws Exception
			{
				String flow = getParameter("flow").getValue();
				String user = getParameter("user").getValue();
				
				if( !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
				if( flow.matches("^[0-9]+$") )
					flow = "s.space_id = " + flow;
				else
					throw new Exception("Invalid flow ID");
				
				Database.getInstance().update("UPDATE links SET link_active = false " + 
					"WHERE instance_from IN(" + 
						"SELECT i.instance_id " + 
						"FROM spaces s " + 
						"LEFT JOIN instance_space _is ON(_is.space_id = s.space_id) " +
						"LEFT JOIN instances i ON(_is.instance_id = i.instance_id) " + 
						"WHERE s.space_user = " + user + " AND " + flow + ")" + 
					" AND instance_to IN(" + 
						"SELECT i.instance_id " + 
						"FROM spaces s " + 
						"LEFT JOIN instance_space _is ON(_is.space_id = s.space_id) " +
						"LEFT JOIN instances i ON(_is.instance_id = i.instance_id) " + 
						"WHERE s.space_user = " + user + " AND " + flow + ")");
				return "OK";
			}
		};
		
		off.addMapping(new String[] { "deactivate", "off", "disable" });
		off.description = "Prevent messages from going through this flow (disables every link inside it)";
		off.returnDescription = "OK";
		off.addGrant(new String[] { "access", "link_update" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "ui_user" });
		off.addParameter(user);
		
		Parameter flow = new Parameter();
		flow.isOptional = false;
		flow.minLength = 1;
		flow.maxLength = 20;
		flow.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		flow.description = "The flow id";
		flow.addAlias(new String[]{ "id", "flow_id", "flow" });
		off.addParameter(flow);
		
		index.addOwnHandler(off);
	}
}