package com.anotherservice.rest.model;

import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import java.util.*;

public class Quota extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "quota", "quotas" });
		index.description = "Manages quotas";
		Handler.addHandler(Initializer.path + "/", index);
		
		initializeInsert(index);
		initializeUpdate(index);
		initializeDelete(index);
		initializeSelect(index);
		
		com.anotherservice.rest.security.Quota.setInstance(new QuotaImpl());
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				String quota = getParameter("name").getValue();
				
				if( quota.matches("^[0-9]+$") || quota.matches("(^[\\._\\-\\s]|[\\._\\-\\s]$)") )
					throw new Exception("The quota name may not be numeric and may not start or end with special characters");
				
				Long uid = Initializer.db.insert("INSERT INTO quotas (quota_name) VALUES ('" + Security.escape(quota) + "')");
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("name", quota);
				result.put("id", uid);
				return result;
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Creates a new quota";
		insert.returnDescription = "The newly created quota {'name', 'id'}";
		insert.addGrant(new String[] { "access", "quota_insert" });
		
		Parameter quota = new Parameter();
		quota.isOptional = false;
		quota.minLength = 3;
		quota.maxLength = 30;
		quota.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		quota.allowInUrl = true;
		quota.description = "The quota name";
		quota.addAlias(new String[]{ "name", "quota_name" });
		insert.addParameter(quota);
		
		index.addOwnHandler(insert);
	}

	private void initializeUpdate(Index index)
	{
		Action update = new Action()
		{
			public Object execute() throws Exception
			{
				String quota = getParameter("name").getValue();
				String id = getParameter("id").getValue();
				
				if( quota.matches("^[0-9]+$") || quota.matches("(^[\\._\\-\\s]|[\\._\\-\\s]$)") )
					throw new Exception("The quota name may not be numeric and may not start or end with special characters");
				
				Initializer.db.update("UPDATE quotas SET quota_name = '" + Security.escape(quota) + "' WHERE quota_id = " + Security.escape(id));
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Changes the quota name";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "quota_update" });
		
		Parameter quota = new Parameter();
		quota.isOptional = false;
		quota.minLength = 3;
		quota.maxLength = 30;
		quota.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		quota.description = "The quota name";
		quota.addAlias(new String[]{ "name", "quota_name" });
		update.addParameter(quota);
		
		Parameter id = new Parameter();
		id.isOptional = false;
		id.allowInUrl = true;
		id.minLength = 1;
		id.maxLength = 30;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.description = "The quota id";
		id.addAlias(new String[]{ "id", "qid", "quota_id" });
		update.addParameter(id);
		
		index.addOwnHandler(update);
	}
	
	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				String quota = getParameter("name").getValue();
				
				if( !quota.matches("^[0-9]+$") )
				{
					Map<String,String> r = Initializer.db.selectOne("SELECT quota_id FROM quotas WHERE quota_name = '" + Security.escape(quota) + "'");
					if( r == null || r.get("quota_id") == null )
						throw new Exception("Unknown quota");
					quota = r.get("quota_id");
				}
				
				Initializer.db.delete("DELETE FROM quotas WHERE quota_id = " + quota);
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy" });
		delete.description = "Removes a quota";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "quota_delete" });
		
		Parameter quota = new Parameter();
		quota.isOptional = false;
		quota.minLength = 1;
		quota.maxLength = 30;
		quota.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		quota.allowInUrl = true;
		quota.description = "The quota name or id";
		quota.addAlias(new String[]{ "name", "quota_name", "id", "quota_id", "qid" });
		delete.addParameter(quota);
		
		index.addOwnHandler(delete);
	}
	
	private void initializeSelect(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> quota = getParameter("name").getValues();
				
				String where = "";
				if( quota.size() > 0 )
				{
					where += " AND (1>1";
					for( String q : quota )
					{
						if( q.matches("^[0-9]+$") )
							where += " OR quota_id = " + Security.escape(q);
						else
							where += " OR quota_name LIKE '%" + Security.escape(q) + "%'";
					}
					where += ")";
				}
				
				return Initializer.db.select("SELECT quota_id, quota_name FROM quotas WHERE 1=1 " + where);
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves information about a quota. Multiple search values for a single property will be OR'd and different criteria will be AND'd.";
		select.returnDescription = "The matching quotas [{'name', 'id'},...]";
		select.addGrant(new String[] { "access", "quota_select" });
		
		Parameter quota = new Parameter();
		quota.isOptional = true;
		quota.minLength = 1;
		quota.maxLength = 30;
		quota.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		quota.allowInUrl = true;
		quota.isMultipleValues = true;
		quota.description = "The quota name or id (will match *[quota]* if not a number or an exact quota id match if numeric)";
		quota.addAlias(new String[]{ "name", "quota_name", "id", "qid", "quota_id", "quota", "names", "quota_names", "quotas", "ids", "qids", "quota_ids" });
		select.addParameter(quota);
		
		index.addOwnHandler(select);
	}

	// ===============================
	// GENERAL QUOTA UTILS
	// ===============================
	class QuotaImpl implements IQuota
	{
		public void increment(String user, String quota) throws Exception
		{
			add(user, quota, 1, false);
		}
		
		public void increment(String user, String quota, boolean bypass) throws Exception
		{
			add(user, quota, 1, bypass);
		}
		
		public void add(String user, String quota, int quantity, boolean bypass) throws Exception
		{
			if( user == null )
				user = Security.getInstance().getUser();
			
			String where = "";
			if( user.matches("^[0-9]+$") )
				where = "u.user_id = " + user;
			else
				where = "u.user_name = '" + Security.escape(user) + "'";
			
			if( quota.matches("^[0-9]+$") )
				where += " AND q.quota_id = " + quota;
			else
				where += " AND q.quota_name = '" + Security.escape(quota) + "'";
				
			if( !bypass )
				where += " AND (uq.quota_used + " + quantity + ") <= uq.quota_max";
			
			long count = Initializer.db.update("UPDATE user_quota uq " + 
						"LEFT JOIN users u ON(u.user_id = uq.user_id) " + 
						"LEFT JOIN quotas q ON(q.quota_id = uq.quota_id) " +
						"SET uq.quota_used = uq.quota_used + " + quantity + " " + 
						"WHERE " + where);

			if( count == 0 )
				throw new Exception("The quota " + quota + " of " + user + " could not be updated either because it reached its limit or because it is not set");
		}
		
		public void decrement(String user, String quota) throws Exception
		{
			substract(user, quota, 1, false);
		}
		
		public void decrement(String user, String quota, boolean bypass) throws Exception
		{
			substract(user, quota, 1, bypass);
		}
		
		public void substract(String user, String quota, int quantity, boolean bypass) throws Exception
		{
			if( user == null )
				user = Security.getInstance().getUser();
			
			String where = "";
			if( user.matches("^[0-9]+$") )
				where = "u.user_id = " + user;
			else
				where = "u.user_name = '" + Security.escape(user) + "'";
			
			if( quota.matches("^[0-9]+$") )
				where += " AND q.quota_id = " + quota;
			else
				where += " AND q.quota_name = '" + Security.escape(quota) + "'";
				
			if( !bypass )
				where += " AND (uq.quota_used - " + quantity + ") >= uq.quota_min";
			
			long count = Initializer.db.update("UPDATE user_quota uq " + 
						"LEFT JOIN users u ON(u.user_id = uq.user_id) " + 
						"LEFT JOIN quotas q ON(q.quota_id = uq.quota_id) " +
						"SET uq.quota_used = uq.quota_used - " + quantity + " " + 
						"WHERE " + where);

			if( count == 0 )
				throw new Exception("The quota " + quota + " of " + user + " could not be updated either because it reached its limit or because it is not set");
		}
	}
}