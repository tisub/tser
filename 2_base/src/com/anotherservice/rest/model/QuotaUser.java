package com.anotherservice.rest.model;

import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import java.util.*;
import java.lang.*;

public class QuotaUser extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "user", "users", "member", "members" });
		index.description = "Manages quota limitations. Note that there is no implicit enforcement on the quota to be strictly bounded" +
			"to \"min <= used <= max\" ; this is resource-specific. Moreover, the quota values are integers, this means plain numbers. " +
			"Because anyhow at some point, rounding occurs, so just choose the units accordingly!";
		Handler.addHandler(Initializer.path + "/quota", index);
		
		initializeInsert(index);
		initializeDelete(index);
		initializeSelect(index);
		initializeUpdate(index);
		initializeIncrement(index);
		initializeDecrement(index);
		initializeCheck(index);
		initializeOverflow(index);
		initializeUnderflow(index);
		
		Self.selfize(Initializer.path + "/quota/user/select");
		Self.selfize(Initializer.path + "/quota/user/check");
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> user = getParameter("user").getValues();
				Collection<String> quota = getParameter("quota").getValues();
				String used = getParameter("used").getValue();
				String max = getParameter("max").getValue();
				String min = getParameter("min").getValue();
				
				if( used == null ) used = "0";
				if( max == null ) max = "0";
				if( min == null ) min = "0";
				
				String where = "";
				
				if( user != null && user.size() > 0 )
				{
					where += " AND (1>1";
					for( String u : user )
					{
						if( u.matches("^[0-9]+$") )
							where += " OR u.user_id = " + Security.escape(u);
						else
							where += " OR u.user_name = '" + Security.escape(u) + "'";
					}
					where += ")";
				}
				
				where += " AND (1>1";
				for( String q : quota )
				{
					if( q.matches("^[0-9]+$") )
						where += " OR q.quota_id = " + Security.escape(q);
					else
						where += " OR q.quota_name = '" + Security.escape(q) + "'";
				}
				where += ")";
				
				Initializer.db.insert("INSERT IGNORE INTO user_quota (user_id, quota_id, quota_used, quota_max, quota_min) " +
					"SELECT DISTINCT u.user_id, q.quota_id, " + used + ", " + max + ", " + min + " FROM users u, quotas q " +
					"WHERE 1=1 " + where);
				return "OK";
			}
		};
		
		insert.addMapping(new String[] { "insert", "add", "create" });
		insert.description = "Adds tracking of the users quotas. If the user is not specified, it applies to all users that do not yet have that quota setup.";
		insert.returnDescription = "OK";
		insert.addGrant(new String[] { "access", "quota_user_insert" });
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.isMultipleValues = true;
		user.description = "The user name(s) or id(s)";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "uid", "user_id", "user_names", "usernames", "logins", "users", "uids", "user_ids" });
		insert.addParameter(user);
		
		Parameter quota = new Parameter();
		quota.isOptional = false;
		quota.minLength = 1;
		quota.maxLength = 30;
		quota.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		quota.allowInUrl = true;
		quota.isMultipleValues = true;
		quota.description = "The quota name(s) or id(s)";
		quota.addAlias(new String[]{ "quota", "quota_name", "qid", "quota_id", "quota_names", "quotas", "qids", "quota_ids" });
		insert.addParameter(quota);
		
		Parameter used = new Parameter();
		used.isOptional = true;
		used.minLength = 1;
		used.maxLength = 20;
		used.mustMatch = "^\\-?[0-9]+$";
		used.description = "The quota's current value. Default zero.";
		used.addAlias(new String[]{ "value", "quota_value", "used", "quota_used" });
		insert.addParameter(used);
		
		Parameter max = new Parameter();
		max.isOptional = true;
		max.minLength = 1;
		max.maxLength = 20;
		max.mustMatch = "^\\-?[0-9]+$";
		max.description = "The quota's maximum value. Default zero.";
		max.addAlias(new String[]{ "max", "quota_max", "maximum" });
		insert.addParameter(max);
		
		Parameter min = new Parameter();
		min.isOptional = true;
		min.minLength = 1;
		min.maxLength = 20;
		min.mustMatch = "^\\-?[0-9]+$";
		min.description = "The quota's minimum value. Default zero.";
		min.addAlias(new String[]{ "min", "quota_min", "minimum" });
		insert.addParameter(min);
		
		index.addOwnHandler(insert);
	}

	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> user = getParameter("user").getValues();
				Collection<String> quota = getParameter("quota").getValues();
				
				String where = " AND (1>1";
				for( String u : user )
				{
					if( u.matches("^[0-9]+$") )
						where += " OR u.user_id = " + Security.escape(u);
					else
						where += " OR u.user_name = '" + Security.escape(u) + "'";
				}
				
				where += ") AND (1>1";
				for( String q : quota )
				{
					if( q.matches("^[0-9]+$") )
						where += " OR q.quota_id = " + Security.escape(q);
					else
						where += " OR q.quota_name = '" + Security.escape(q) + "'";
				}
				where += ")";
				
				Initializer.db.delete("DELETE uq FROM user_quota uq " +
					"LEFT JOIN users u ON(u.user_id = uq.user_id) " +
					"LEFT JOIN quotas q ON(q.quota_id = uq.quota_id) " +
					"WHERE 1=1 " + where);
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove" });
		delete.description = "Removes tracking of the users quotas";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "quota_user_delete" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.allowInUrl = true;
		user.isMultipleValues = true;
		user.description = "The user name(s) or id(s)";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "uid", "user_id", "user_names", "usernames", "logins", "users", "uids", "user_ids" });
		delete.addParameter(user);
		
		Parameter quota = new Parameter();
		quota.isOptional = false;
		quota.minLength = 1;
		quota.maxLength = 30;
		quota.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		quota.isMultipleValues = true;
		quota.description = "The quota name(s) or id(s)";
		quota.addAlias(new String[]{ "quota", "quota_name", "qid", "quota_id", "quota_names", "quotas", "qids", "quota_ids" });
		delete.addParameter(quota);
		
		index.addOwnHandler(delete);
	}
	
	private void initializeSelect(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				String user = getParameter("user").getValue();
				String quota = getParameter("quota").getValue();
				String used = getParameter("used").getValue();
				String min = getParameter("min").getValue();
				String max = getParameter("max").getValue();
				String limit = getParameter("limit").getValue();
				
				if( user == null && quota == null )
					throw new Exception("The user, quota or both must be specified");

				String fields = "uq.quota_used, uq.quota_max, uq.quota_min";
				
				String where = "";
				if( user != null )
				{
					if( user.matches("^[0-9]+$") )
						where += " AND u.user_id = " + Security.escape(user);
					else
						where += " AND u.user_name = '" + Security.escape(user) + "'";
						
					fields += ", q.quota_id, q.quota_name";
				}
				else
				{
					if( quota.matches("^[0-9]+$") )
						where += " AND q.quota_id = " + Security.escape(quota);
					else
						where += " AND q.quota_name = '" + Security.escape(quota) + "'";
						
					fields += ", u.user_id, u.user_name";
				}
				
				if( used != null )
					where += " AND uq.quota_used " + (used.matches("^(\\s*(>|<|=|<=|>=)\\s*)\\-?[0-9]+$") ? "" : " = " ) + used;
				if( min != null )
					where += " AND uq.quota_min " + (min.matches("^(\\s*(>|<|=|<=|>=)\\s*)\\-?[0-9]+$") ? "" : " = " ) + min;
				if( max != null )
					where += " AND uq.quota_max " + (max.matches("^(\\s*(>|<|=|<=|>=)\\s*)\\-?[0-9]+$") ? "" : " = " ) + max;
				
				if( limit != null )
					limit = " ORDER BY uq.quota_used DESC LIMIT 0," + limit;
				else
					limit = " ORDER BY uq.quota_used DESC LIMIT 0,30";
					
				return Initializer.db.select("SELECT " + fields + " FROM users u " +
						"LEFT JOIN user_quota uq ON(u.user_id = uq.user_id) " +
						"LEFT JOIN quotas q ON(q.quota_id = uq.quota_id) " +
						"WHERE q.quota_id IS NOT NULL " + where + limit);
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves all quotas of a user or all users of a quota matching the specified criteria. Different search criteria will be AND'd.";
		select.returnDescription = "The matching users or quotas [{'name', 'id', 'min, 'max', 'used'},...]";
		select.addGrant(new String[] { "access", "quota_user_select" });
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user name or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "uid", "user_id" });
		select.addParameter(user);
		
		Parameter quota = new Parameter();
		quota.isOptional = true;
		quota.minLength = 1;
		quota.maxLength = 30;
		quota.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		quota.description = "The quota name or id";
		quota.addAlias(new String[]{ "quota", "quota_name", "qid", "quota_id" });
		select.addParameter(quota);
		
		Parameter used = new Parameter();
		used.isOptional = true;
		used.minLength = 1;
		used.maxLength = 20;
		used.mustMatch = "^(\\s*(>|<)?=?\\s*)?\\-?[0-9]+$";
		used.description = "The quota's current value. You may specify a comparison criteria : >, <, >=, <=, = or none which is the same as =.";
		used.addAlias(new String[]{ "value", "quota_value", "used", "quota_used" });
		select.addParameter(used);
		
		Parameter max = new Parameter();
		max.isOptional = true;
		max.minLength = 1;
		max.maxLength = 20;
		max.mustMatch = "^(\\s*(>|<)?=?\\s*)?\\-?[0-9]+$";
		max.description = "The quota's maximum value. You may specify a comparison criteria : >, <, >=, <=, = or none which is the same as =.";
		max.addAlias(new String[]{ "max", "quota_max", "maximum" });
		select.addParameter(max);
		
		Parameter min = new Parameter();
		min.isOptional = true;
		min.minLength = 1;
		min.maxLength = 20;
		min.mustMatch = "^(\\s*(>|<)?=?\\s*)?\\-?[0-9]+$";
		min.description = "The quota's minimum value. You may specify a comparison criteria : >, <, >=, <=, = or none which is the same as =.";
		min.addAlias(new String[]{ "min", "quota_min", "minimum" });
		select.addParameter(min);

		Parameter limit = new Parameter();
		limit.isOptional = true;
		limit.minLength = 1;
		limit.maxLength = 11;
		limit.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		limit.description = "Limit results";
		limit.addAlias(new String[]{ "limit" });
		select.addParameter(limit);
		
		index.addOwnHandler(select);
	}
	
	private void initializeUpdate(Index index)
	{
		Action update = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> user = getParameter("user").getValues();
				Collection<String> quota = getParameter("quota").getValues();
				String used = getParameter("used").getValue();
				String max = getParameter("max").getValue();
				String min = getParameter("min").getValue();
				
				if( used == null && max == null && min == null )
					throw new Exception("At lease one of 'used', 'min', or 'max' must be specified");
					
				String where = "";
				
				if( user != null && user.size() > 0 )
				{
					where += " AND (1>1";
					for( String u : user )
					{
						if( u.matches("^[0-9]+$") )
							where += " OR u.user_id = " + Security.escape(u);
						else
							where += " OR u.user_name = '" + Security.escape(u) + "'";
					}
					where += ")";
				}
				
				if( quota != null && quota.size() > 0 )
				{
					where += " AND (1>1";
					for( String q : quota )
					{
						if( q.matches("^[0-9]+$") )
							where += " OR q.quota_id = " + Security.escape(q);
						else
							where += " OR q.quota_name = '" + Security.escape(q) + "'";
					}
					where += ")";
				}
				
				String set = "";
				if( used != null )
					set += ", uq.quota_used = " + used; 
				if( min != null )
					set += ", uq.quota_min = " + min;
				if( max != null )
					set += ", uq.quota_max = " + max;
				
				Initializer.db.update("UPDATE user_quota uq " + 
					"LEFT JOIN users u ON(u.user_id = uq.user_id) " + 
					"LEFT JOIN quotas q ON(q.quota_id = uq.quota_id) " + 
					"SET uq.quota_id=uq.quota_id" + set +
					" WHERE 1=1 " + where);
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Modifies tracking of the users quotas. If the user is not specified, it applies to all users, so be careful." + 
			"If the quota is not specified, it applies to all quotas, so be careful. If neither user and quota are specified, it applies to " + 
			"all quotas of all users, so be extra careful.";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "quota_user_update" });
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.isMultipleValues = true;
		user.description = "The user name(s) or id(s)";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "uid", "user_id", "user_names", "usernames", "logins", "users", "uids", "user_ids" });
		update.addParameter(user);
		
		Parameter quota = new Parameter();
		quota.isOptional = true;
		quota.minLength = 1;
		quota.maxLength = 30;
		quota.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		quota.isMultipleValues = true;
		quota.description = "The quota name(s) or id(s)";
		quota.addAlias(new String[]{ "quota", "quota_name", "qid", "quota_id", "quota_names", "quotas", "qids", "quota_ids" });
		update.addParameter(quota);
		
		Parameter used = new Parameter();
		used.isOptional = true;
		used.minLength = 1;
		used.maxLength = 20;
		used.mustMatch = "^\\-?[0-9]+$";
		used.description = "The quota's current value. If not set, the value is unchanged.";
		used.addAlias(new String[]{ "value", "quota_value", "used", "quota_used" });
		update.addParameter(used);
		
		Parameter max = new Parameter();
		max.isOptional = true;
		max.minLength = 1;
		max.maxLength = 20;
		max.mustMatch = "^\\-?[0-9]+$";
		max.description = "The quota's maximum value. If not set, the value is unchanged.";
		max.addAlias(new String[]{ "max", "quota_max", "maximum" });
		update.addParameter(max);
		
		Parameter min = new Parameter();
		min.isOptional = true;
		min.minLength = 1;
		min.maxLength = 20;
		min.mustMatch = "^\\-?[0-9]+$";
		min.description = "The quota's minimum value. If not set, the value is unchanged.";
		min.addAlias(new String[]{ "min", "quota_min", "minimum" });
		update.addParameter(min);
		
		index.addOwnHandler(update);
	}
	
	private void initializeIncrement(Index index)
	{
		Action increment = new Action()
		{
			public Object execute() throws Exception
			{
				String user = getParameter("user").getValue();
				Collection<String> quota = getParameter("quota").getValues();
				
				String where = "";
				if( user != null )
				{
					if( user.matches("^[0-9]+$") )
						where += " AND u.user_id = " + Security.escape(user);
					else
						where += " AND u.user_name = '" + Security.escape(user) + "'";
				}
				
				where += ") AND (1>1";
				for( String q : quota )
				{
					if( q.matches("^[0-9]+$") )
						where += " OR q.quota_id = " + Security.escape(q);
					else
						where += " OR q.quota_name = '" + Security.escape(q) + "'";
				}
				where += ")";
				
				Initializer.db.insert("UPDATE user_quota uq " + 
					"LEFT JOIN users u ON(u.user_id = uq.user_id) " + 
					"LEFT JOIN quotas q ON(q.quota_id = uq.quota_id) "+
					"SET uq.quota_used = uq.quota_used + 1 WHERE 1=1 " + where);
				return "OK";
			}
		};
		
		increment.addMapping(new String[] { "increment", "increase", "++" });
		increment.description = "Raises the quota value by one. This is a shorthand for 'update' +1.";
		increment.returnDescription = "OK";
		increment.addGrant(new String[] { "access", "quota_user_update" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.allowInUrl = true;
		user.description = "The user name or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "uid", "user_id" });
		increment.addParameter(user);
		
		Parameter quota = new Parameter();
		quota.isOptional = false;
		quota.minLength = 1;
		quota.maxLength = 30;
		quota.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		quota.isMultipleValues = true;
		quota.description = "The quota name(s) or id(s)";
		quota.addAlias(new String[]{ "quota", "quota_name", "qid", "quota_id", "quota_names", "quotas", "qids", "quota_ids" });
		increment.addParameter(quota);
		
		index.addOwnHandler(increment);
	}
	
	private void initializeDecrement(Index index)
	{
		Action decrement = new Action()
		{
			public Object execute() throws Exception
			{
				String user = getParameter("user").getValue();
				Collection<String> quota = getParameter("quota").getValues();
				
				String where = "";
				if( user != null )
				{
					if( user.matches("^[0-9]+$") )
						where += " AND u.user_id = " + Security.escape(user);
					else
						where += " AND u.user_name = '" + Security.escape(user) + "'";
				}
				
				where += ") AND (1>1";
				for( String q : quota )
				{
					if( q.matches("^[0-9]+$") )
						where += " OR q.quota_id = " + Security.escape(q);
					else
						where += " OR q.quota_name = '" + Security.escape(q) + "'";
				}
				where += ")";
				
				Initializer.db.insert("UPDATE user_quota uq " + 
					"LEFT JOIN users u ON(u.user_id = uq.user_id) " + 
					"LEFT JOIN quotas q ON(q.quota_id = uq.quota_id) "+
					"SET uq.quota_used = uq.quota_used - 1 WHERE 1=1 " + where);
				return "OK";
			}
		};
		
		decrement.addMapping(new String[] { "decrement", "decrease", "--" });
		decrement.description = "Lowers the quota value by one. This is a shorthand for 'update' -1.";
		decrement.returnDescription = "OK";
		decrement.addGrant(new String[] { "access", "quota_user_update" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.allowInUrl = true;
		user.description = "The user name or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "uid", "user_id" });
		decrement.addParameter(user);
		
		Parameter quota = new Parameter();
		quota.isOptional = false;
		quota.minLength = 1;
		quota.maxLength = 30;
		quota.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		quota.isMultipleValues = true;
		quota.description = "The quota name(s) or id(s)";
		quota.addAlias(new String[]{ "quota", "quota_name", "qid", "quota_id", "quota_names", "quotas", "qids", "quota_ids" });
		decrement.addParameter(quota);
		
		index.addOwnHandler(decrement);
	}
	
	private void initializeCheck(Index index)
	{
		Action check = new Action()
		{
			public Object execute() throws Exception
			{
				String user = getParameter("user").getValue();
				Collection<String> quota = getParameter("quota").getValues();
				
				String where = "";
				if( user != null )
				{
					if( user.matches("^[0-9]+$") )
						where += " AND u.user_id = " + Security.escape(user);
					else
						where += " AND u.user_name = '" + Security.escape(user) + "'";
				}
				
				if( quota != null && quota.size() > 0 )
				{
					where += " AND (1>1";
					for( String q : quota )
					{
						if( q.matches("^[0-9]+$") )
							where += " OR q.quota_id = " + Security.escape(q);
						else
							where += " OR q.quota_name = '" + Security.escape(q) + "'";
					}
					where += ")";
				}
				
				Vector<Map<String,String>> rows = Initializer.db.select("SELECT q.quota_name, uq.quota_max, uq.quota_min, uq.quota_used " + 
					"FROM user_quota uq " + 
					"LEFT JOIN users u ON(u.user_id = uq.user_id) " + 
					"LEFT JOIN quotas q ON(q.quota_id = uq.quota_id) "+
					"WHERE 1=1 " + where);
				
				for( Map<String,String> r : rows )
				{
					long used = Long.parseLong(r.get("quota_used"));
					long min = Long.parseLong(r.get("quota_min"));
					long max = Long.parseLong(r.get("quota_max"));
					
					if( used > max )
						r.put("margin", "" + Math.abs(max - used));
					else if( used < min )
						r.put("margin", "-" + Math.abs(min - used));
					else
						r.put("margin", "0");
						
					r.remove("quota_min");
					r.remove("quota_max");
					r.remove("quota_used");
				}
				
				return rows;
			}
		};
		
		check.addMapping(new String[] { "check", "remain", "verify" });
		check.description = "Checks whether the current value of the quota is within the bounds of 'min' and 'max'. This is a shorthand for 'select'.";
		check.returnDescription = "[{'quota', 'margin'},...] The quota margin is calculated as follows : (1) if the current value is within min and max, the margin is zero ; " + 
			"(2) if the current value is lower than min, the margin is a negative value of the difference between current and min ; " +
			"(3) if the current value is higher than max, the margin is a positive value of the difference between max and current. " +
			"Note that if -for any reason- max < min then the max rule applies.";
		check.addGrant(new String[] { "access", "quota_user_select" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.allowInUrl = true;
		user.description = "The user name or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "uid", "user_id" });
		check.addParameter(user);
		
		Parameter quota = new Parameter();
		quota.isOptional = true;
		quota.minLength = 1;
		quota.maxLength = 30;
		quota.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		quota.isMultipleValues = true;
		quota.description = "The quota name(s) or id(s). If ommitted, all quotas are selected.";
		quota.addAlias(new String[]{ "quota", "quota_name", "qid", "quota_id", "quota_names", "quotas", "qids", "quota_ids" });
		check.addParameter(quota);
		
		index.addOwnHandler(check);
	}
	
	private void initializeOverflow(Index index)
	{
		Action overflow = new Action()
		{
			public Object execute() throws Exception
			{
				String quota = getParameter("quota").getValue();
				String value = getParameter("value").getValue();
				String limit = getParameter("limit").getValue();
				
				String where = "";
				if( quota.matches("^[0-9]+$") )
					where = " q.quota_id = " + Security.escape(quota);
				else
					where = " q.quota_name = '" + Security.escape(quota) + "'";

				if( limit != null )
					limit = " ORDER BY uq.quota_used DESC LIMIT 0," + limit;
				else
					limit = " ORDER BY uq.quota_used DESC LIMIT 0,30";
				
				return Initializer.db.select("SELECT u.user_name, u.user_id, uq.quota_used, uq.quota_max " +
					"FROM users u " + 
					"LEFT JOIN user_quota uq ON(uq.user_id = u.user_id) " +
					"LEFT JOIN quotas q ON(q.quota_id = uq.quota_id) " + 
					"WHERE uq.quota_used > uq.quota_max AND " + where + limit);
			}
		};
		
		overflow.addMapping(new String[] { "overflow", "over", "overquota", "exceed" });
		overflow.description = "Retrieves all users that are over the max quota value.";
		overflow.returnDescription = "The overquota users [{'user_name', 'user_id', 'quota_used', 'quota_max'},...]";
		overflow.addGrant(new String[] { "access", "quota_user_select" });

		Parameter limit = new Parameter();
		limit.isOptional = true;
		limit.minLength = 1;
		limit.maxLength = 11;
		limit.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		limit.description = "Limit results";
		limit.addAlias(new String[]{ "limit" });
		overflow.addParameter(limit);
		
		Parameter quota = new Parameter();
		quota.isOptional = false;
		quota.minLength = 1;
		quota.maxLength = 30;
		quota.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		quota.description = "The quota name or id";
		quota.addAlias(new String[]{ "quota", "quota_name", "qid", "quota_id" });
		overflow.addParameter(quota);
		
		index.addOwnHandler(overflow);
	}
	
	private void initializeUnderflow(Index index)
	{
		Action underflow = new Action()
		{
			public Object execute() throws Exception
			{
				String quota = getParameter("quota").getValue();
				
				String where = "";
				if( quota.matches("^[0-9]+$") )
					where = " q.quota_id = " + Security.escape(quota);
				else
					where = " q.quota_name = '" + Security.escape(quota) + "'";
				
				return Initializer.db.select("SELECT u.user_name, u.user_id, uq.quota_used, uq.quota_min " +
					"FROM users u " + 
					"LEFT JOIN user_quota uq ON(uq.user_id = u.user_id) " +
					"LEFT JOIN quotas q ON(q.quota_id = uq.quota_id) " + 
					"WHERE uq.quota_used < uq.quota_min AND " + where);
			}
		};
		
		underflow.addMapping(new String[] { "underflow", "under", "underquota" });
		underflow.description = "Retrieves all users that are under the min quota value.";
		underflow.returnDescription = "The underquota users [{'user_name', 'user_id', 'quota_used', 'quota_min'},...]";
		underflow.addGrant(new String[] { "access", "quota_user_select" });
		
		Parameter quota = new Parameter();
		quota.isOptional = false;
		quota.minLength = 1;
		quota.maxLength = 30;
		quota.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		quota.description = "The quota name or id";
		quota.addAlias(new String[]{ "quota", "quota_name", "qid", "quota_id" });
		underflow.addParameter(quota);
		
		index.addOwnHandler(underflow);
	}
}