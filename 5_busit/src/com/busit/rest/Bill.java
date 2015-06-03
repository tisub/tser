package com.busit.rest;

import com.anotherservice.db.*;
import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import com.busit.security.*;
import java.util.*;
import java.text.*;
import java.math.RoundingMode;

public class Bill extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "bill", "bills" });
		index.description = "Manages bills";
		Handler.addHandler("/busit/", index);
		
		initializeInsert(index);
		initializeUpdate(index);
		initializeDelete(index);
		initializeSelect(index);
		initializeInsertLine(index);
		initializeDeleteLine(index);
		
		Self.selfize("/busit/bill/insert");
		Self.selfize("/busit/bill/select");
		Self.selfize("/busit/bill/delete");
		Self.selfize("/busit/bill/insertline");
		Self.selfize("/busit/bill/deleteline");
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
				
				Date now = new Date();
				SimpleDateFormat ftYear = new SimpleDateFormat("yyyy");
				SimpleDateFormat ftMonth = new SimpleDateFormat("MM");
     
				String year = ftYear.format(now);
				String month = ftMonth.format(now);

				Long uid = Database.getInstance().insert("INSERT INTO bills (bill_user, bill_date) VALUES (" + user + ", UNIX_TIMESTAMP())");
				String id = String.format("%07d", uid);
				
				Map<String, String> info = Database.getInstance().selectOne("SELECT user_name FROM users WHERE user_id = " + user);
				Database.getInstance().update("UPDATE bills SET bill_name = 'CO-BI" + year + "-" + id + "', bill_ref = '" + info.get("user_name") + "/" + year + "/" + month + "/" + uid + "' WHERE bill_id = " + uid);
				
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("id", uid);
				return result;
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Add a new bill";
		insert.returnDescription = "The created bill {'id'}";
		insert.addGrant(new String[] { "access", "bill_insert" });

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
				String id = getParameter("id").getValue();
				String status = getParameter("status").getValue();
				
				Database.getInstance().update("UPDATE bills SET bill_status = '" + Security.escape(status) + "' WHERE bill_id = " + Security.escape(id));
				
				if( status.equals("2") )
				{
					int uid = 0;
					Map<String, String> info = Database.getInstance().selectOne("SELECT bill_real_id FROM bills WHERE 1 ORDER BY bill_real_id DESC");
					if( info.get("bill_real_id") != null )
						uid =  Integer.parseInt(info.get("bill_real_id"));
					
					uid = uid+1;
					String formatuid = String.format("%07d", uid);
					
					Date now = new Date();
					SimpleDateFormat ftYear = new SimpleDateFormat("yyyy");
					SimpleDateFormat ftMonth = new SimpleDateFormat("MM");
					String year = ftYear.format(now);
					String month = ftMonth.format(now);
				
					Database.getInstance().update("UPDATE bills SET bill_real_id = " + uid +", bill_name = 'BI" + year + "-" + formatuid + "' WHERE bill_id = " + id);
				}
				
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Update a bill";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "bill_update" });

		Parameter id = new Parameter();
		id.isOptional = false;
		id.minLength = 1;
		id.maxLength = 11;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.description = "The bill id";
		id.addAlias(new String[]{ "id", "bid", "bill_id" });
		update.addParameter(id);
		
		Parameter status = new Parameter();
		status.isOptional = false;
		status.minLength = 1;
		status.maxLength = 2;
		status.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		status.description = "The bills status name";
		status.addAlias(new String[]{ "status", "bill_status" });
		update.addParameter(status);
		
		index.addOwnHandler(update);		
	}
	
	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				String id = getParameter("id").getValue();
				String user = getParameter("user").getValue();
				
				if( user != null )
				{
					if( !IdentityChecker.getInstance().isUserBill(user, id) )
						throw new Exception("The current user is not an administrator of the provided bill");
				}
				
				Database.getInstance().delete("DELETE FROM bills WHERE bill_id = " + Security.escape(id) + " AND bill_status = 0");
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy" });
		delete.description = "Delete a bill";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "bill_delete" });

		Parameter id = new Parameter();
		id.isOptional = false;
		id.minLength = 1;
		id.maxLength = 11;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.description = "The bill id";
		id.addAlias(new String[]{ "id", "bid", "bill_id" });
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
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> bill = getParameter("bill").getValues();
				String limit = getParameter("limit").getValue();
				String from = getParameter("from").getValue();
				String to = getParameter("to").getValue();
				String group = getParameter("group").getValue();
				String status = getParameter("status").getValue();
				String user = getParameter("user").getValue();
				
				if( user != null && !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
					
				String where = "";
				if( bill.size() > 0 )
				{
					where += " AND (1>1";
					for( String b : bill )
						where += " OR bill_id = " + b;
					where += ")";
				}

				if( user != null )
					where += " AND bill_user = " + user;
				if( from != null )
					where += " AND bill_date >= " + Security.escape(from);
				if( to != null )
					where += " AND bill_date <= " + Security.escape(to);
				if( status != null )
					where += " AND bill_status = " + status;
				
				if( limit != null )
					limit = " ORDER BY bill_date DESC LIMIT 0," + limit;
				else
					limit = " ORDER BY bill_date DESC LIMIT 0,1000";
				
				if( group != null )
					return Database.getInstance().select("SELECT COUNT(bill_id) as count, SUM(bill_amount_ati) as amount_ati, SUM(bill_amount_et) as amount_et, " + group + " (FROM_UNIXTIME(bill_date)) as " + group + " FROM bills WHERE 1=1 " + where + " GROUP BY " + group + " (FROM_UNIXTIME(bill_date))");
			
				Vector<HashMap<String, Object>> result = new Vector<HashMap<String, Object>>();
				Vector<Map<String, String>> bills = Database.getInstance().select("SELECT bill_id, bill_real_id, bill_name, bill_ref, bill_user, bill_date, bill_status, bill_amount_et, bill_amount_ati FROM bills WHERE 1=1 " + where + limit);
				
				for( Map<String, String> b : bills )
				{
					HashMap<String, Object> row = new HashMap<String, Object>();
					
					Vector<Map<String, String>> lines = Database.getInstance().select("SELECT line_id, line_bill, line_name, line_description, line_vat, line_amount_et, line_amount_ati, line_credits FROM bill_line WHERE line_bill = " + b.get("bill_id"));
					row.put("lines", lines);
					
					for( String key : b.keySet() )
						row.put(key, b.get(key));

					result.add(row);
				}
				
				return result;
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves information about a bill. Multiple search values for a single property will be OR'd and different criteria will be AND'd.";
		select.returnDescription = "The matching bill [{'id', 'user', 'date'},...]";
		select.addGrant(new String[] { "access", "bill_select" });
		
		Parameter bill = new Parameter();
		bill.isOptional = true;
		bill.minLength = 1;
		bill.maxLength = 11;
		bill.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		bill.allowInUrl = true;
		bill.isMultipleValues = true;
		bill.description = "The bill id(s)";
		bill.addAlias(new String[]{ "bill", "bill_id", "id", "bills", "bid", "ids", "bids" });
		select.addParameter(bill);

		Parameter limit = new Parameter();
		limit.isOptional = true;
		limit.minLength = 1;
		limit.maxLength = 11;
		limit.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		limit.description = "The limit";
		limit.addAlias(new String[]{ "limit", "limitation" });
		select.addParameter(limit);

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
		
		Parameter group = new Parameter();
		group.isOptional = true;
		group.minLength = 1;
		group.maxLength = 11;
		group.mustMatch = PatternBuilder.getRegex(PatternBuilder.UPPER);
		group.description = "Group by?";
		group.addAlias(new String[]{ "group", "group_by" });
		select.addParameter(group);
	
		Parameter status = new Parameter();
		status.isOptional = true;
		status.minLength = 1;
		status.maxLength = 2;
		status.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		status.description = "The bills status";
		status.addAlias(new String[]{ "status", "bill_status" });
		select.addParameter(status);
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		select.addParameter(user);
		
		index.addOwnHandler(select);
	}
	
	
	private void initializeInsertLine(Index index)
	{
		Action insertline = new Action()
		{
			public Object execute() throws Exception
			{
				String id = getParameter("id").getValue();
				String name = getParameter("name").getValue();
				String description = getParameter("description").getValue();
				String amount = getParameter("amount").getValue();
				String credits = getParameter("credits").getValue();
				String vat = getParameter("vat").getValue();
				String user = getParameter("user").getValue();
				
				if( user != null )
				{
					if( !IdentityChecker.getInstance().isUserBill(user, id) )
						throw new Exception("The current user is not an administrator of the provided bill");
				}

				float famount = Float.parseFloat(amount);
				float fvat = Float.parseFloat(vat);
				float amountAti = famount+(famount*(fvat/100));
				DecimalFormat df = new DecimalFormat("#####.##");
				df.setRoundingMode(RoundingMode.DOWN);
				String roundedAmountAti = df.format(amountAti);
				
				Database.getInstance().insert("INSERT INTO bill_line (line_bill, line_name, line_description, line_amount_et, line_vat, line_amount_ati, line_credits) VALUES (" + id + ", '" + Security.escape(name) + "', '" + Security.escape(description) + "', '" + Security.escape(amount) + "', '" + Security.escape(vat) + "', '" + roundedAmountAti + "', '" + Security.escape(credits) + "')");
				
				Vector<Map<String, String>> lines = Database.getInstance().select("SELECT line_amount_ati, line_amount_et FROM bill_line WHERE line_bill = " + id);
				
				float totalEt = 0;
				float totalAti = 0;
				for( Map<String, String> l : lines )
				{
					totalAti = totalAti+Float.parseFloat(l.get("line_amount_ati"));
					totalEt = totalEt+Float.parseFloat(l.get("line_amount_et"));
				}
				
				Database.getInstance().update("UPDATE bills SET bill_amount_et = '" + totalEt + "', bill_amount_ati = '" + totalAti + "' WHERE bill_id = " + id);
				
				return "OK";
			}
		};
		
		insertline.addMapping(new String[] { "insertline", "insert_line" });
		insertline.description = "Insert a new line in a bill";
		insertline.returnDescription = "OK";
		insertline.addGrant(new String[] { "access", "bill_update" });
		
		Parameter id = new Parameter();
		id.isOptional = false;
		id.minLength = 1;
		id.maxLength = 11;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.allowInUrl = true;
		id.isMultipleValues = true;
		id.description = "The bill id";
		id.addAlias(new String[]{ "bill", "bill_id", "id" });
		insertline.addParameter(id);

		Parameter name = new Parameter();
		name.isOptional = false;
		name.minLength = 1;
		name.maxLength = 150;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		name.description = "The name of the line";
		name.addAlias(new String[]{ "name", "line_name" });
		insertline.addParameter(name);

		Parameter description = new Parameter();
		description.isOptional = false;
		description.minLength = 1;
		description.maxLength = 150;
		description.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		description.description = "The description of the line";
		description.addAlias(new String[]{ "desc", "description", "line_description" });
		insertline.addParameter(description);

		Parameter amount = new Parameter();
		amount.isOptional = false;
		amount.minLength = 1;
		amount.maxLength = 10;
		amount.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		amount.description = "The amount of the line";
		amount.addAlias(new String[]{ "amount", "line_amount" });
		insertline.addParameter(amount);

		Parameter credits = new Parameter();
		credits.isOptional = false;
		credits.minLength = 1;
		credits.maxLength = 11;
		credits.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		credits.description = "The number of credits of the line";
		credits.addAlias(new String[]{ "credits", "line_credits" });
		insertline.addParameter(credits);
		
		Parameter vat = new Parameter();
		vat.isOptional = false;
		vat.minLength = 1;
		vat.maxLength = 5;
		vat.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		vat.description = "The VAT percentage";
		vat.addAlias(new String[]{ "vat", "line_vat" });
		insertline.addParameter(vat);
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		insertline.addParameter(user);
		
		index.addOwnHandler(insertline);
	}
	
	
	private void initializeDeleteLine(Index index)
	{

	}
	
}
