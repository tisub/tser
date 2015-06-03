package com.busit.rest;

import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.db.*;
import com.anotherservice.util.*;
import java.util.*;

public class Cloud extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "cloud" });
		index.description = "Tnstance cloud";
		Handler.addHandler("/busit/", index);
		
		initializeSelect(index);
		initializeSelect2(index);
	}
	
	private void initializeSelect(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				String user = getParameter("user").getValue();
				String space = getParameter("space").getValue();
								
				Hashtable<String, Object> data = new Hashtable<String, Object>();
				String sql = "";
				
				// ===========
				// USER
				// ===========
				sql = "SELECT user_id as u FROM users ORDER BY user_id";
				if( user != null )
					sql = "SELECT user_id as u FROM users WHERE user_id = " + user + " ORDER BY user_id";
				if( space != null )
					sql = "SELECT user_id as u FROM users LEFT JOIN spaces ON(space_user = user_id) WHERE space_id = " + space + " ORDER BY user_id";
				data.put("u", Database.getInstance().select(sql));
				
				// ===========
				// SPACE
				// ===========
				sql = "SELECT space_id as s, space_user as u FROM spaces ORDER BY space_user, space_id";
				if( user != null )
					sql = "SELECT space_id as s, space_user as u FROM spaces WHERE space_user = " + user + " ORDER BY space_user, space_id";
				if( space != null )
					sql = "SELECT space_id as s, space_user as u FROM spaces WHERE space_id = " + space + " ORDER BY space_user, space_id";
				data.put("s", Database.getInstance().select(sql));
				
				// ===========
				// INSTANCE
				// ===========
				sql = "SELECT i.instance_id as i, isp.space_id as s, i.instance_user as u " + 
					"FROM instances i " + 
					"LEFT JOIN instance_space isp ON(isp.instance_id = i.instance_id) " + 
					"GROUP BY i.instance_id ORDER BY i.instance_user, isp.space_id";
				if( user != null )
					sql = "SELECT i.instance_id as i, isp.space_id as s, i.instance_user as u " + 
						"FROM instances i " +
						"LEFT JOIN instance_space isp ON(isp.instance_id = i.instance_id) " +
						"WHERE i.instance_user = " + user + " GROUP BY i.instance_id ORDER BY i.instance_user, isp.space_id";
				if( space != null )
					sql = "SELECT i.instance_id as i, isp.space_id as s, i.instance_user as u " + 
						"FROM instances i " + 
						"LEFT JOIN instance_space isp ON(isp.instance_id = i.instance_id) " +
						"WHERE isp.space_id = " + space + " GROUP BY i.instance_id ORDER BY i.instance_user, isp.space_id";
				data.put("i", Database.getInstance().select(sql));
				
				// ===========
				// LINK
				// ===========
				sql = "SELECT instance_from as f, instance_to as t FROM links";
				if( user != null )
					sql = "SELECT instance_from as f, instance_to as t FROM links " + 
						"LEFT JOIN instances ifrom ON(ifrom.instance_id = instance_from) " + 
						"LEFT JOIN instances ito ON(ito.instance_id = instance_to) " +
						"WHERE ifrom.instance_user = " + user + " AND ito.instance_user = " + user;
				if( space != null )
					sql = "SELECT instance_from as f, instance_to as t FROM links " + 
						"LEFT JOIN instances ifrom ON(ifrom.instance_id = instance_from) " + 
						"LEFT JOIN instance_space ispfrom ON(ispfrom.instance_id = instance_from) " +
						"LEFT JOIN instances ito ON(ito.instance_id = instance_to) " +
						"LEFT JOIN instance_space ispto ON(ispto.instance_id = instance_to) " +
						"WHERE ispfrom.space_id = " + space + " AND ispto.space_id = " + space;
				data.put("l", Database.getInstance().select(sql));
				
				return data;
			}
		};
		
		select.addMapping(new String[] { "select", "list", "view" });
		select.description = "Constructs the instance cloud";
		select.returnDescription = "Cloud data";
		select.addGrant(new String[] { Config.gets("com.busit.rest.admin.grant") });
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 20;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		user.description = "The user id";
		user.addAlias(new String[]{ "user" });
		select.addParameter(user);
		
		Parameter space = new Parameter();
		space.isOptional = true;
		space.minLength = 1;
		space.maxLength = 20;
		space.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		space.description = "The space id";
		space.addAlias(new String[]{ "space" });
		select.addParameter(space);
		
		index.addOwnHandler(select);
	}

	private void initializeSelect2(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<Map<String, String>> categories = Database.getInstance().select("SELECT category_id FROM categories");
				
				String fields = "";
				for( Map<String, String> cat : categories )
					fields += ", SUM(IF(cc.category_id = " + cat.get("category_id") + ", 1, 0)) as sum_" + cat.get("category_id") + " ";
				
				String sql = "SELECT u.user_name, u.user_lang, u.user_id, u.user_mail, u.user_firstname, u.user_date, u.user_org, u.user_lastname, u.user_version, COUNT(i.instance_id) as total " + fields +
					"FROM users u " + 
					"LEFT JOIN instances i ON(i.instance_user = u.user_id) " + 
					"LEFT JOIN connectors c ON(i.instance_connector = c.connector_id) " +
					"LEFT JOIN categories cc ON(c.connector_category = cc.category_id) " +
					"WHERE u.user_org = 0 GROUP BY u.user_id ORDER BY total DESC";
				
				return Database.getInstance().select(sql);
			}
		};
		
		select.addMapping(new String[] { "stats" });
		select.description = "Show connector usage statistics";
		select.returnDescription = "Stat data";
		select.addGrant(new String[] { Config.gets("com.busit.rest.admin.grant") });
		
		index.addOwnHandler(select);
	}
	
}