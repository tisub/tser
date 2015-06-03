package com.busit.rest;

import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.db.*;
import com.anotherservice.util.*;
import java.util.*;

public class Voucher extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "voucher", "vouchers" });
		index.description = "Manages credit vouchers";
		Handler.addHandler("/busit/credit/", index);
		
		initializeInsert(index);
		initializeUpdate(index);
		initializeDelete(index);
		initializeSelect(index);
		initializeActivate(index);
		
		Self.selfize("/busit/credit/voucher/activate");
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				String name = getParameter("name").getValue();
				String credits = getParameter("credits").getValue();
				String code = getParameter("code").getValue();
				String from = getParameter("from").getValue();
				String to = getParameter("to").getValue();
				String user = getParameter("user").getValue();
				String quantity = getParameter("quantity").getValue();
				String internal = getParameter("internal").getValue();
				
				if( name.matches("^[0-9]+$") )
					throw new Exception("The voucher name may not be numeric");
				if( user != null && !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
				else if( user == null ) user = "NULL";
				if( credits == null ) credits = "1000";
				if( from == null ) from = "NULL";
				if( to == null ) to = "NULL";
				if( quantity == null ) quantity = "1";
				else if( quantity.equals("0") ) quantity = "NULL";
				if( internal == null ) internal = "";
				
				String sql = "INSERT INTO vouchers (voucher_credits, voucher_code, voucher_date_from, voucher_date_to, voucher_user, voucher_quantity, voucher_name, voucher_internal) " + 
					"VALUES (" + credits + ", '" + Security.escape(Security.getInstance().hash(code)) 
					+ "', " + from + ", " + to + ", " + user + ", " + quantity + ", '" + Security.escape(name) + 
					"', '" + Security.escape(internal) + "')";
				Long id = Database.getInstance().insert(sql);
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("name", name);
				result.put("id", id);
				return result;
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Creates a new voucher";
		insert.returnDescription = "The newly created voucher {'name', 'id'}";
		insert.addGrant(new String[] { "access", "voucher_insert" });
		
		Parameter name = new Parameter();
		name.isOptional = false;
		name.minLength = 3;
		name.maxLength = 30;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		name.description = "The voucher name";
		name.addAlias(new String[]{ "name", "voucher_name" });
		insert.addParameter(name);
		
		Parameter credits = new Parameter();
		credits.isOptional = true;
		credits.minLength = 1;
		credits.maxLength = 11;
		credits.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		credits.description = "The voucher credit amount (default 1000)";
		credits.addAlias(new String[]{ "credits", "voucher_credits" });
		insert.addParameter(credits);
		
		Parameter code = new Parameter();
		code.isOptional = false;
		code.minLength = 3;
		code.maxLength = 100;
		code.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM);
		code.description = "The voucher cleartext code";
		code.addAlias(new String[]{ "code", "voucher_code" });
		insert.addParameter(code);
		
		Parameter from = new Parameter();
		from.isOptional = true;
		from.minLength = 11;
		from.maxLength = 11;
		from.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		from.description = "The voucher validity date from (unix timestamp seconds)";
		from.addAlias(new String[]{ "from", "voucher_from", "date_from", "voucher_date_from" });
		insert.addParameter(from);
		
		Parameter to = new Parameter();
		to.isOptional = true;
		to.minLength = 11;
		to.maxLength = 11;
		to.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		to.description = "The voucher validity date to (unix timestamp seconds)";
		to.addAlias(new String[]{ "to", "voucher_to", "date_to", "voucher_date_to" });
		insert.addParameter(to);
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 20;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The voucher user exclusivity (only that user can activate)";
		user.addAlias(new String[]{ "user", "voucher_user", "login", "user_id", "uid", "username" });
		insert.addParameter(user);
		
		Parameter quantity = new Parameter();
		quantity.isOptional = true;
		quantity.minLength = 1;
		quantity.maxLength = 11;
		quantity.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		quantity.description = "The voucher quantity (default 1) set to 0 for unlimited";
		quantity.addAlias(new String[]{ "quantity", "voucher_quantity" });
		insert.addParameter(quantity);
		
		Parameter internal = new Parameter();
		internal.isOptional = true;
		internal.minLength = 0;
		internal.maxLength = 30000;
		internal.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		internal.description = "The voucher internal description";
		internal.addAlias(new String[]{ "internal", "voucher_internal", "description", "voucher_description" });
		insert.addParameter(internal);
		
		index.addOwnHandler(insert);
	}

	private void initializeUpdate(Index index)
	{
		Action update = new Action()
		{
			public Object execute() throws Exception
			{
				String id = getParameter("id").getValue();
				String name = getParameter("name").getValue();
				String credits = getParameter("credits").getValue();
				String code = getParameter("code").getValue();
				String from = getParameter("from").getValue();
				String to = getParameter("to").getValue();
				String user = getParameter("user").getValue();
				String quantity = getParameter("quantity").getValue();
				String internal = getParameter("internal").getValue();
				
				String set = "";
				
				if( name != null && name.matches("^[0-9]+$") )
					throw new Exception("The voucher name may not be numeric");
				if( name != null )
					set = ", voucher_name = '" + Security.escape(name) + "'";
				if( user != null && !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
				if( user != null ) 
					set = ", voucher_user = " + (user.length() > 0 ? user : "NULL");
				if( credits != null )
					set = ", voucher_credits = " + (credits.length() > 0 ? credits : "1000");
				if( from != null )
					set = ", voucher_date_from = " + (from.length() > 0 ? from : "NULL");
				if( to != null )
					set = ", voucher_date_to = " + (to.length() > 0 ? to : "NULL");
				if( quantity != null )
					set = ", voucher_quantity = " + (quantity.length() > 0 ? (quantity.equals("0")?"NULL":quantity) : "1");
				if( internal != null ) 
					set = ", voucher_internal = '" + Security.escape(internal) + "'";
				if( code != null )
					set = ", voucher_code = '" + Security.escape(Security.getInstance().hash(code)) + "'";
				
				Database.getInstance().update("UPDATE vouchers SET voucher_id = voucher_id" + set + " WHERE voucher_id = " + Security.escape(id));
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Changes the voucher info";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "voucher_update" });
		
		Parameter id = new Parameter();
		id.isOptional = false;
		id.allowInUrl = true;
		id.minLength = 1;
		id.maxLength = 11;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.description = "The voucher id";
		id.addAlias(new String[]{ "id", "voucher_id" });
		update.addParameter(id);
		
		Parameter name = new Parameter();
		name.isOptional = true;
		name.minLength = 3;
		name.maxLength = 30;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		name.description = "The voucher name";
		name.addAlias(new String[]{ "name", "voucher_name" });
		update.addParameter(name);
		
		Parameter credits = new Parameter();
		credits.isOptional = true;
		credits.minLength = 1;
		credits.maxLength = 11;
		credits.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		credits.description = "The voucher credit amount (default 1000)";
		credits.addAlias(new String[]{ "credits", "voucher_credits" });
		update.addParameter(credits);
		
		Parameter code = new Parameter();
		code.isOptional = true;
		code.minLength = 3;
		code.maxLength = 100;
		code.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM);
		code.description = "The voucher cleartext code";
		code.addAlias(new String[]{ "code", "voucher_code" });
		update.addParameter(code);
		
		Parameter from = new Parameter();
		from.isOptional = true;
		from.minLength = 0;
		from.maxLength = 11;
		from.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		from.description = "The voucher validity date from (unix timestamp seconds)";
		from.addAlias(new String[]{ "from", "voucher_from", "date_from", "voucher_date_from" });
		update.addParameter(from);
		
		Parameter to = new Parameter();
		to.isOptional = true;
		to.minLength = 0;
		to.maxLength = 11;
		to.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		to.description = "The voucher validity date to (unix timestamp seconds)";
		to.addAlias(new String[]{ "to", "voucher_to", "date_to", "voucher_date_to" });
		update.addParameter(to);
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 0;
		user.maxLength = 20;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The voucher user exclusivity (only that user can activate)";
		user.addAlias(new String[]{ "user", "voucher_user", "login", "user_id", "uid", "username" });
		update.addParameter(user);
		
		Parameter quantity = new Parameter();
		quantity.isOptional = true;
		quantity.minLength = 1;
		quantity.maxLength = 11;
		quantity.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		quantity.description = "The voucher quantity (default 1) set to 0 for unlimited";
		quantity.addAlias(new String[]{ "quantity", "voucher_quantity" });
		update.addParameter(quantity);
		
		Parameter internal = new Parameter();
		internal.isOptional = true;
		internal.minLength = 0;
		internal.maxLength = 30000;
		internal.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		internal.description = "The voucher internal description";
		internal.addAlias(new String[]{ "internal", "voucher_internal", "description", "voucher_description" });
		update.addParameter(internal);
		
		index.addOwnHandler(update);
	}
	
	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				String voucher = getParameter("name").getValue();
				
				if( voucher.matches("^[0-9]+$") )
					voucher = "voucher_id = " + voucher;
				else
					voucher = "voucher_name = '" + Security.escape(voucher) + "'";
				
				Database.getInstance().delete("DELETE FROM vouchers WHERE " + voucher);
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy" });
		delete.description = "Removes a voucher";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "voucher_delete" });
		
		Parameter voucher = new Parameter();
		voucher.isOptional = false;
		voucher.minLength = 1;
		voucher.maxLength = 100;
		voucher.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		voucher.description = "The voucher name or id";
		voucher.addAlias(new String[]{ "name", "voucher_name", "id", "voucher_id" });
		delete.addParameter(voucher);
		
		index.addOwnHandler(delete);
	}
	
	private void initializeSelect(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				String voucher = getParameter("voucher").getValue();
				String code = getParameter("code").getValue();
				
				if( voucher != null && voucher.matches("^[0-9]+$") )
					voucher = "AND voucher_id = " + voucher;
				else if( voucher != null )
					voucher = "AND voucher_name = '" + Security.escape(voucher) + "'";
				
				if( code == null ) 
					code = "";
				else 
					code = " AND code = '" + Security.escape(Security.getInstance().hash(code)) + "'";
				
				return Database.getInstance().select("SELECT voucher_id, voucher_credits, voucher_code, voucher_date_from, voucher_date_to, voucher_user, voucher_quantity, voucher_name, voucher_internal " + 
					"FROM vouchers WHERE 1=1 " + voucher + code + " ORDER BY voucher_name ASC");
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves information about a voucher";
		select.returnDescription = "The matching vouchers [{'name', 'id'},...]";
		select.addGrant(new String[] { "access", "voucher_select" });
		
		Parameter voucher = new Parameter();
		voucher.isOptional = true;
		voucher.minLength = 1;
		voucher.maxLength = 100;
		voucher.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		voucher.description = "The voucher name or id";
		voucher.addAlias(new String[]{ "name", "voucher_name", "id", "voucher_id", "voucher" });
		select.addParameter(voucher);
		
		Parameter code = new Parameter();
		code.isOptional = true;
		code.minLength = 3;
		code.maxLength = 100;
		code.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM);
		code.description = "The voucher cleartext code";
		code.addAlias(new String[]{ "code", "voucher_code" });
		select.addParameter(code);
		
		index.addOwnHandler(select);
	}

	private void initializeActivate(Index index)
	{
		Action activate = new Action()
		{
			public Object execute() throws Exception
			{
				String code = getParameter("code").getValue();
				String user = getParameter("user").getValue();
				
				String sqluser = user;	
				if( !user.matches("^[0-9]+$") )
					sqluser = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
				
				Any data = Any.wrap(Database.getInstance().selectOne("SELECT v.voucher_credits, v.voucher_id, v.voucher_quantity, COUNT(a.user_id) as used " + 
					"FROM vouchers v " + 
					"LEFT JOIN voucher_activated a ON(a.voucher_id = v.voucher_id) " +
					"WHERE v.voucher_code = '" + Security.escape(Security.getInstance().hash(code)) + "' AND " + 
					"(v.voucher_date_from IS NULL OR v.voucher_date_from < UNIX_TIMESTAMP()) AND " + 
					"(v.voucher_date_to IS NULL OR v.voucher_date_to > UNIX_TIMESTAMP())"));
				
				if( data.isEmpty() )
					throw new Exception("Invalid voucher");
				
				int quantity = data.get("voucher_quantity").isNull() ? Integer.MAX_VALUE : Integer.parseInt(data.<String>value("voucher_quantity"));
				if( Integer.parseInt(data.<String>value("used")) >= quantity )
					throw new Exception("This voucher is already used");
				
				try
				{
					Database.getInstance().insert("INSERT INTO voucher_activated (voucher_id, user_id, activated_date) " + 
						"VALUES (" + data.<String>value("voucher_id") + ", " + sqluser + ", UNIX_TIMESTAMP())");
				}
				catch(Exception e)
				{
					throw new Exception("You already activated this voucher");
				}
				
				// add credits
				int credits = Integer.parseInt(data.<String>value("voucher_credits"));
				Quota.getInstance().add(user, Config.gets("com.busit.rest.quota.credit"), credits, true);
					
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("credits", credits);
				return result;
			}
		};
		
		activate.addMapping(new String[] { "activate" });
		activate.description = "Activate a voucher";
		activate.returnDescription = "The number of credits received";
		activate.addGrant(new String[] { "access" });
		
		Parameter code = new Parameter();
		code.isOptional = false;
		code.minLength = 3;
		code.maxLength = 100;
		code.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM);
		code.description = "The voucher cleartext code";
		code.addAlias(new String[]{ "code", "voucher_code" });
		activate.addParameter(code);
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 20;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user name or id";
		user.addAlias(new String[]{ "user", "login", "user_id", "uid", "username" });
		activate.addParameter(user);
		
		index.addOwnHandler(activate);
	}
}