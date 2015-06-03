package com.busit.security;

import com.anotherservice.db.*;
import com.anotherservice.util.*;
import com.anotherservice.rest.*;
import com.anotherservice.rest.core.*;
import com.anotherservice.rest.security.*;
import java.util.*;

/**
CREATE TABLE delegates (
	delegate_id		INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
	delegate_value		TINYTEXT CHARSET UTF8 NOT NULL,
	delegate_lease		INT(11) UNSIGNED NOT NULL,
	delegate_name		TINYTEXT CHARSET UTF8 DEFAULT NULL,
	delegate_identity	BIGINT(20) UNSIGNED NOT NULL,
	delegate_instance	BIGINT(20) UNSIGNED DEFAULT NULL,
	delegate_space		BIGINT(20) UNSIGNED DEFAULT NULL,
	PRIMARY KEY (delegate_id),
	FOREIGN KEY (delegate_identity) REFERENCES identities (identity_id) ON DELETE CASCADE,
	FOREIGN KEY (delegate_instance) REFERENCES instances (instance_id) ON DELETE CASCADE,
	FOREIGN KEY (delegate_space) REFERENCES spaces (space_id) ON DELETE CASCADE
) ENGINE=INNODB;

CREATE TABLE delegate_grant (
  delegate_id	INT(11) UNSIGNED NOT NULL,
  grant_id		INT(11) UNSIGNED NOT NULL,
  PRIMARY KEY (delegate_id, grant_id),
  FOREIGN KEY (delegate_id) REFERENCES delegates (delegate_id) ON DELETE CASCADE,
  FOREIGN KEY (grant_id) REFERENCES grants (grant_id) ON DELETE CASCADE
) ENGINE=INNODB;
*/

public abstract class DelegateAction extends Action
{
	public void checkAuth() throws Exception
	{
		try
		{
			super.checkAuth();
		}
		catch(Exception e)
		{
			if( !this.hasGrants(this.getGrants()) )
				throw new Exception("Unsufficient privileges");
		}
	}
	
	private boolean hasGrants(Collection<String> grants) throws Exception
	{
		String credentials = Request.getParam(Config.gets("com.anotherservice.rest.model.authParameter"));
		
		if( credentials == null )
			return false;
		
		if( credentials.matches("^(s|i):[0-9]+:[^:]+") )
		{
			String[] parts = credentials.split(":");
			String type = parts[0];
			String id = parts[1];
			String value = parts[2];
			
			Vector<Map<String,String>> rows = Database.getInstance().select(
				"SELECT DISTINCT g.grant_name, g.grant_id, i.instance_id, i.instance_name, u.user_id, s.space_id, i2.identity_id " + 
				"FROM instances i " + 
				"LEFT JOIN instance_space is1 ON(is1.instance_id = i.instance_id) " + 
				"LEFT JOIN spaces s ON(s.space_id = is1.space_id) " + 
				"LEFT JOIN users o ON(o.user_id = i.instance_user OR s.space_user = o.user_id) " + 
				"LEFT JOIN org_member om ON(om.org_id = o.user_id) " + 
				"LEFT JOIN identities io2 ON(io2.identity_id = om.identity_id) " +
				"LEFT JOIN users u ON(io2.identity_user = u.user_id OR u.user_id = i.instance_user OR s.space_user = u.user_id) " + 
				"LEFT JOIN identities i2 ON(i2.identity_user = u.user_id) " + 
				"LEFT JOIN delegates d ON(i.instance_id = d.delegate_instance AND i2.identity_id = d.delegate_identity)" +
				"LEFT JOIN delegate_grant dg ON(d.delegate_id = dg.delegate_id) " + 
				"LEFT JOIN grants g ON(g.grant_id = dg.grant_id) " + 
				"WHERE d.delegate_value = '" + Security.escape(value) + "' " + 
				"AND " + (type.equals("s") ? "s.space_id" : "i.instance_id") + " = " + Security.escape(id) + " " +
				"AND (d.delegate_lease > UNIX_TIMESTAMP() OR d.delegate_lease = 0)");
				
			if( rows == null || rows.size() == 0 )
				return false;
			
			for( String g : grants )
			{
				if( g == null || g.length() == 0 )
					continue;
				
				boolean has = false;
				boolean isGrantName = !g.matches("^[0-9]+$");
				
				for( Map<String,String> row : rows )
				{
					if( row.get("instance_id") == null || row.get("instance_id").length() == 0 ||
						//row.get("space_id") == null || row.get("space_id").length() == 0 ||
						row.get("user_id") == null || row.get("user_id").length() == 0 )
						continue;

					if( (isGrantName && (row.get("grant_name").equalsIgnoreCase(g) || row.get("grant_name").replaceFirst("^(?i)SELF_", "").equalsIgnoreCase(g))) 
						|| (!isGrantName && row.get("grant_id").equals(g)) )
					{
						has = true;
						break;
					}
				}

				if( !has )
					return false;
			}
			
			if( this.hasParameter("user") )
			{
				Request.clearParam(this.getParameter("user").getAliases());
				Request.addParam("user", rows.get(0).get("user_id"));
			}
			
			if( this.hasParameter("identity") )
			{
				Request.clearParam(this.getParameter("identity").getAliases());
				Request.addParam("identity", rows.get(0).get("identity_id"));
			}
			
			if( this.hasParameter("space") )
			{
				Request.clearParam(this.getParameter("space").getAliases());
				Request.addParam("space", rows.get(0).get("space_id"));
			}
			
			if( this.hasParameter("instance") )
			{
				if( type.equals("i") )
				{
					Request.clearParam(this.getParameter("instance").getAliases());
					Request.addParam("instance", rows.get(0).get("instance_id"));
				}
				else
				{
					Collection<String> instances = new Vector<String>();
					if( this.getParameter("instance").isMultipleValues )
						instances.addAll(this.getParameter("instance").getValues());
					else
						instances.add(this.getParameter("instance").getValue());
					
					for( String instance : instances )
					{
						boolean has2 = false;
						for( Map<String,String> row : rows )
						{
							if( (instance.matches("^[0-9]+$") && row.get("instance_id").equals(instance)) || row.get("instance_name").equalsIgnoreCase(instance) )
							{
								has2 = true;
								break;
							}
						}
						
						if( !has2 )
							return false;
					}
				}
			}
			
			return true;
		}
		
		return false;
	}
}