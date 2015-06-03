package com.busit.rest;

import com.anotherservice.rest.*;
import com.anotherservice.db.*;
import com.anotherservice.util.*;
import com.anotherservice.rest.security.*;
import com.busit.security.*;
import java.util.*;
import java.util.regex.*;

// TODO : warning, this class is not thread safe. Thus, concurrency issues may happen :(
// for possible fix, see : http://dev.mysql.com/doc/refman/5.0/en/innodb-locking-reads.html

public class Credit extends InitializerOnce
{
	public static int TRANSACTION_TYPE_UNKNOWN = 0;
	public static int TRANSACTION_TYPE_ONESHOT = 1;
	public static int TRANSACTION_TYPE_PERMESSAGE = 2;
	public static int TRANSACTION_TYPE_SHARE = 3;
	public static int TAX_TYPE_COUNT = 4;
	public static int TAX_TYPE_SIZE = 5;
	public static int TAX_TYPE_QOS = 6;
	
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "credit", "credits" });
		index.description = "Manages payments and transactions";
		Handler.addHandler("/busit/", index);
		
		initializePay(index);
		initializeHold(index);
		initializeAck(index);
		initializeConfirm(index);
		initializeRefund(index);
		initializeClean(index);
		
		//Self.selfize("/busit/credit/pay");
	}
	
	private void initializePay(Index index)
	{
		Action pay = new Action()
		{
			public Object execute() throws Exception
			{
				String from = getParameter("from").getValue();
				String to = getParameter("to").getValue();
				int price = Integer.parseInt(getParameter("price").getValue());
				
				if( price <= 0 )
					throw new Exception("The transaction amount must be greater than zero");
				
				if( !IdentityChecker.getInstance().isUserIdentity(null, from) && !IdentityChecker.getInstance().isUserIdentityAdmin(null, from) )
					throw new Exception("The current user cannot start a transaction on behalf of the provided identity");
				
				String where;
				
				if( from.matches("^[0-9]+$") )
					where = "identity_id = " + from;
				else 
					where = "identity_principal = '" + Security.escape(PrincipalUtil.parse(from).getName()) + "'";

				Map<String, String> tmp = Database.getInstance().selectOne("SELECT identity_user " + 
					"FROM identities WHERE " + where);
				if( tmp == null || tmp.get("identity_user") == null )
					throw new Exception("Unknown identity from");
				String user_from = tmp.get("identity_user");
				
				if( to.matches("^[0-9]+$") )
					where = "identity_id = " + to;
				else
					where = "identity_principal = '" + Security.escape(PrincipalUtil.parse(to).getName()) + "'";

				tmp = Database.getInstance().selectOne("SELECT identity_user " + 
					"FROM identities WHERE " + where);
				if( tmp == null || tmp.get("identity_user") == null )
					throw new Exception("Unknown identity to");
				String user_to= tmp.get("identity_user");
				
				long transaction = INTERNAL_HOLD(user_from, user_to, price, 0, 0, 0, 0, TRANSACTION_TYPE_UNKNOWN, "");
				INTERNAL_ACK(transaction + "");
				INTERNAL_CONFIRM(transaction + "");
				
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("id", transaction);
				return result;
			}
		};
		
		pay.addMapping(new String[] { "pay" });
		pay.description = "Transfers credits to another user";
		pay.returnDescription = "OK";
		pay.addGrant(new String[] { "access", "credit_pay" });

		Parameter from = new Parameter();
		from.isOptional = false;
		from.minLength = 1;
		from.maxLength = 100;
		from.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "=\\\\,\"");
		from.description = "The identity name, principal or id from which credits are taken";
		from.addAlias(new String[]{ "from", "origin" });
		pay.addParameter(from);
		
		Parameter to = new Parameter();
		to.isOptional = false;
		to.minLength = 1;
		to.maxLength = 100;
		to.mustMatch = PatternBuilder.getRegex(PatternBuilder.CLASS_ALPHANUM + PatternBuilder.CLASS_PUNCT + PatternBuilder.CLASS_SPACE + "=\\\\,\"");
		to.description = "The identity name, principal or id to which credits are given";
		to.addAlias(new String[]{ "to", "destination" });
		pay.addParameter(to);
		
		Parameter price = new Parameter();
		price.isOptional = false;
		price.minLength = 1;
		price.maxLength = 11;
		price.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		price.description = "The price of the transaction";
		price.addAlias(new String[]{ "price", "amount" });
		pay.addParameter(price);
		
		index.addOwnHandler(pay);
	}
	
	static long INTERNAL_BUY_CONNECTOR(String user, String connector) throws Exception
	{
		if( !connector.matches("^[0-9]+$") )
			connector = "(SELECT connector_id FROM connectors where connectors_name = '" + Security.escape(connector) + "')";
		String sqluser = user;
		if( !sqluser.matches("^[0-9]+$") )
			sqluser = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
		
		Map<String, String> data1 = Database.getInstance().selectOne("SELECT connector_id, connector_user, connector_buy_price, connector_buy_tax " + 
			"FROM connectors " + 
			"WHERE connector_id = " + connector);
		if( data1 != null && Integer.parseInt(data1.get("connector_buy_price")) + Integer.parseInt(data1.get("connector_buy_tax")) == 0 )
			return 0; // OK, the connector is free
		else if( data1 == null || data1.get("connector_id") == null )
			throw new Exception("Invalid connector");
		
		// check if the user already paid for this connector
		Map<String, String> data2 = Database.getInstance().selectOne("SELECT COUNT(i.instance_connector) as current " +
			"FROM connectors c " + 
			"LEFT JOIN instances i ON (i.instance_connector = c.connector_id) " + 
			"WHERE i.instance_user = " + sqluser + " AND c.connector_id = " + connector);
		Map<String, String> data3 = Database.getInstance().selectOne("SELECT COUNT(h.transaction_from) as history " +
			"FROM connectors c " + 
			"LEFT JOIN transaction_history h ON (h.transaction_type = " + TRANSACTION_TYPE_ONESHOT + " AND h.transaction_data = c.connector_id) " + 
			"WHERE h.transaction_from = " + sqluser + " AND c.connector_id = " + connector + " AND h.transaction_to <> " + Config.gets("com.busit.rest.admin.uid"));
		if( data2 != null && data3 != null && Integer.parseInt(data3.get("history")) > Integer.parseInt(data2.get("current")) )
			return 0; // OK, the user already paid
		
		long transaction = INTERNAL_HOLD(user, data1.get("connector_user"), Integer.parseInt(data1.get("connector_buy_price")), Integer.parseInt(data1.get("connector_buy_tax")), 0, 0, 0, TRANSACTION_TYPE_ONESHOT, data1.get("connector_id"));
		INTERNAL_ACK(transaction + "");
		INTERNAL_CONFIRM(transaction + "");

		return transaction;
	}
	
	private void initializeHold(Index index)
	{
		Action hold = new Action()
		{
			public Object execute() throws Exception
			{
				String user_from = getParameter("from").getValue();
				String user_to = getParameter("to").getValue();
				int price = Integer.parseInt(getParameter("price").getValue());
				int tax = Integer.parseInt(getParameter("tax").getValue());
				
				long uid = INTERNAL_HOLD(user_from, user_to, price, tax, 0, 0, 0, TRANSACTION_TYPE_UNKNOWN, "");
				
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("id", uid);
				return result;
			}
		};
		
		hold.addMapping(new String[] { "hold", "insert", "create" });
		hold.description = "Creates a new transaction";
		hold.returnDescription = "The transaction id {'id'}";
		hold.addGrant(new String[] { Config.gets("com.busit.rest.brokerGrantName") });
		
		Parameter from = new Parameter();
		from.isOptional = true;
		from.minLength = 1;
		from.maxLength = 30;
		from.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		from.description = "The user login or id from which credits are taken";
		from.addAlias(new String[]{ "from" });
		hold.addParameter(from);
		
		Parameter to = new Parameter();
		to.isOptional = true;
		to.minLength = 1;
		to.maxLength = 30;
		to.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		to.description = "The user login or id to which credits are given";
		to.addAlias(new String[]{ "to" });
		hold.addParameter(to);
		
		Parameter price = new Parameter();
		price.isOptional = false;
		price.minLength = 1;
		price.maxLength = 11;
		price.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		price.description = "The price of the transaction";
		price.addAlias(new String[]{ "price" });
		hold.addParameter(price);
		
		Parameter tax = new Parameter();
		tax.isOptional = false;
		tax.minLength = 1;
		tax.maxLength = 11;
		tax.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		tax.description = "The tax of the transaction";
		tax.addAlias(new String[]{ "tax" });
		hold.addParameter(tax);
		
		index.addOwnHandler(hold);
	}
	
	static long INTERNAL_HOLD_MESSAGE(String user_from, String user_to, int price, int tax, String connectorId) throws Exception
	{
		if( !user_from.matches("^[0-9]+$") )
			user_from = "(SELECT user_id FROM users where user_name = '" + Security.escape(user_from) + "')";
		if( user_to == null || user_to.length() == 0 )
			user_to = "null";
		else if( !user_to.matches("^[0-9]+$") )
			user_to = "(SELECT user_id FROM users where user_name = '" + Security.escape(user_to) + "')";
		
		// get the user plan
		Map<String, String> plan = Database.getInstance().selectOne("SELECT plan_user, plan_window, plan_factor, plan_root, plan_free " + 
			"FROM credit_plan WHERE plan_user = " + user_from);
		if( plan == null || plan.get("plan_window") == null )
			throw new Exception("Invalid user credit plan");
			
		int window = Integer.parseInt(plan.get("plan_window"));
		double factor = Double.parseDouble(plan.get("plan_factor"));
		double root = Double.parseDouble(plan.get("plan_root"));
		double free = Double.parseDouble(plan.get("plan_free"));
			
		// compute message cost
		Map<String, String> cost = Database.getInstance().selectOne("SELECT COUNT(*) as c, SUM(sliding_cost) as p FROM credit_sliding WHERE sliding_user = " + user_from + " AND sliding_time > (UNIX_TIMESTAMP()-" + window + ")");
		double count = Double.parseDouble(cost.get("c"));
		double paid = 0;
		if( cost.get("p") != null )
			paid = Double.parseDouble(cost.get("p"));
		
		// how much should I pay for this message (cumulated)
		double cumulated = Math.floor(factor * Math.pow(count + 1 - free, (1.0/root)));
		double countCost = (cumulated > paid ? 1 : 0);
		double sizeCost = 0;
		double qosCost = 0;
		
		long transaction = INTERNAL_HOLD(user_from, user_to, price, tax, countCost, sizeCost, qosCost, TRANSACTION_TYPE_PERMESSAGE, connectorId);
		
		Database.getInstance().insert("INSERT INTO credit_sliding (sliding_user, sliding_time, sliding_cost) VALUES (" + user_from + ", UNIX_TIMESTAMP(), " + (cumulated > paid ? 1 : 0) + ")");
		Database.getInstance().insert("DELETE FROM credit_sliding WHERE sliding_user = " + user_from + " AND sliding_time < (UNIX_TIMESTAMP()-" + window + ")");
		
		return transaction;
	}
	
	static long INTERNAL_HOLD_SHARE(String user_from, String user_to, double price) throws Exception
	{
		return INTERNAL_HOLD(user_from, user_to, price, 0, 0, 0, 0, TRANSACTION_TYPE_SHARE, "");
	}
	
	static long INTERNAL_HOLD(String user_from, String user_to, double price, double taxCost, double countCost, double sizeCost, double qosCost, int type, String data) throws Exception
	{
		if( !user_from.matches("^[0-9]+$") )
			user_from = "(SELECT user_id FROM users where user_name = '" + Security.escape(user_from) + "')";
		if( user_to == null || user_to.length() == 0 )
			user_to = "null";
		else if( !user_to.matches("^[0-9]+$") )
			user_to = "(SELECT user_id FROM users where user_name = '" + Security.escape(user_to) + "')";
		
		// V.A.T (TVA) on the price
		double vat = Double.parseDouble(Config.gets("com.busit.rest.vat.percent"));
		taxCost += Math.ceil(vat / 100.0 * price);
		
		double amount = price + taxCost + countCost + sizeCost + qosCost;
		
		Quota.getInstance().substract(user_from, Config.gets("com.busit.rest.quota.credit"), (int)amount, false);
		Logger.fine("Transaction accepted from " + user_from + " to " + user_to + " for " + price + " credits (+ " + taxCost + " as tax, " + countCost + " for count, " + sizeCost + " for size, " + qosCost + " for qos)");
		
		return Database.getInstance().insert("INSERT INTO credit_transaction (transaction_from, transaction_to, transaction_price, " + 
			"transaction_tax, transaction_count, transaction_size, transaction_qos, transaction_type, transaction_data, transaction_date) " + 
			"VALUES (" + user_from + ", " + user_to + ", " + price + ", " + 
			taxCost + ", " + countCost + ", " + sizeCost + ", " + qosCost + ", " + type + ", '" + Security.escape(data) + "', UNIX_TIMESTAMP())");
	}
	
	private void initializeAck(Index index)
	{
		Action ack = new Action()
		{
			public Object execute() throws Exception
			{
				String transaction = getParameter("transaction").getValue();
				
				INTERNAL_ACK(transaction);
					
				return "OK";
			}
		};
		
		ack.addMapping(new String[] { "ack", "validate" });
		ack.description = "Acknowledge a transaction";
		ack.returnDescription = "OK";
		ack.addGrant(new String[] { Config.gets("com.busit.rest.brokerGrantName") });
		
		Parameter transaction = new Parameter();
		transaction.isOptional = false;
		transaction.minLength = 1;
		transaction.maxLength = 20;
		transaction.mustMatch = "^([0-9]+|" + Pattern.quote(Config.gets("com.busit.rest.cron")) + ")$";
		transaction.description = "The transaction id";
		transaction.addAlias(new String[]{ "transaction", "id", "tid" });
		ack.addParameter(transaction);
		
		index.addOwnHandler(ack);
	}
	
	static void INTERNAL_ACK(String transaction) throws Exception
	{
		if( transaction == null )
			throw new Exception("Invalid transaction ID");
		if( transaction.equals(Config.gets("com.busit.rest.cron")) )
			return;
		if( !transaction.matches("^[0-9]+$") )
			throw new Exception("Invalid transaction ID");

		Long updated = Database.getInstance().update("UPDATE credit_transaction SET transaction_ack = TRUE WHERE transaction_id = " + transaction + " AND transaction_ack = FALSE");
		if( updated == 0 )
			throw new Exception("Invalid transaction");
	}
	
	private void initializeConfirm(Index index)
	{
		Action confirm = new Action()
		{
			public Object execute() throws Exception
			{
				String transaction = getParameter("transaction").getValue();
				
				INTERNAL_CONFIRM(transaction);
				
				return "OK";
			}
		};
		
		confirm.addMapping(new String[] { "confirm", "delete" });
		confirm.description = "Confirm a transaction";
		confirm.returnDescription = "OK";
		confirm.addGrant(new String[] { Config.gets("com.busit.rest.brokerGrantName") });
		
		Parameter transaction = new Parameter();
		transaction.isOptional = false;
		transaction.minLength = 1;
		transaction.maxLength = 20;
		transaction.mustMatch = "^([0-9]+|" + Pattern.quote(Config.gets("com.busit.rest.cron")) + ")$";
		transaction.description = "The transaction id";
		transaction.addAlias(new String[]{ "transaction", "id", "tid" });
		confirm.addParameter(transaction);
		
		index.addOwnHandler(confirm);
	}
	
	static void INTERNAL_CONFIRM(String transaction) throws Exception
	{
		if( transaction == null )
			throw new Exception("Invalid transaction ID");
		if( transaction.equals(Config.gets("com.busit.rest.cron")) )
			return;
		if( !transaction.matches("^[0-9]+$") )
			throw new Exception("Invalid transaction ID");
			
		Map<String, String> t = Database.getInstance().selectOne("SELECT transaction_from, transaction_to, transaction_price, transaction_tax, transaction_count, " + 
			"transaction_size, transaction_qos, transaction_date, transaction_type, transaction_data " + 
			"FROM credit_transaction " + 
			"WHERE transaction_id = " + transaction + " AND transaction_ack");
		if( t == null )
			throw new Exception("Invalid transaction");
		if( t.get("transaction_to") != null && !t.get("transaction_price").equals("0") )
		{
			Quota.getInstance().add(t.get("transaction_to"), Config.gets("com.busit.rest.quota.refund"), Integer.parseInt(t.get("transaction_price")), true);
			Database.getInstance().insert("INSERT INTO transaction_history (transaction_from, transaction_to, transaction_amount, transaction_date, transaction_type, transaction_data) " +
				"VALUES (" + t.get("transaction_from") + ", " + t.get("transaction_to") + ", " + t.get("transaction_price") + ", " + t.get("transaction_date") + ", " + t.get("transaction_type") + ", " + t.get("transaction_data") + ")");
		}
		if( !t.get("transaction_tax").equals("0") )
		{
			Quota.getInstance().add(Config.gets("com.busit.rest.admin.uid"), Config.gets("com.busit.rest.quota.refund"), Integer.parseInt(t.get("transaction_tax")), true);
			Database.getInstance().insert("INSERT INTO transaction_history (transaction_from, transaction_to, transaction_amount, transaction_date, transaction_type, transaction_data) " +
				"VALUES (" + t.get("transaction_from") + ", " + Config.gets("com.busit.rest.admin.uid") + ", " + t.get("transaction_tax") + ", " + t.get("transaction_date") + ", " + t.get("transaction_type") + ", " + t.get("transaction_data") + ")");
		}
		if( !t.get("transaction_count").equals("0") )
		{
			Quota.getInstance().add(Config.gets("com.busit.rest.admin.uid"), Config.gets("com.busit.rest.quota.refund"), Integer.parseInt(t.get("transaction_count")), true);
			Database.getInstance().insert("INSERT INTO transaction_history (transaction_from, transaction_to, transaction_amount, transaction_date, transaction_type, transaction_data) " +
				"VALUES (" + t.get("transaction_from") + ", " + Config.gets("com.busit.rest.admin.uid") + ", " + t.get("transaction_count") + ", " + t.get("transaction_date") + ", " + TAX_TYPE_COUNT + ", null)");
		}
		if( !t.get("transaction_size").equals("0") )
		{
			Quota.getInstance().add(Config.gets("com.busit.rest.admin.uid"), Config.gets("com.busit.rest.quota.refund"), Integer.parseInt(t.get("transaction_size")), true);
			Database.getInstance().insert("INSERT INTO transaction_history (transaction_from, transaction_to, transaction_amount, transaction_date, transaction_type, transaction_data) " +
				"VALUES (" + t.get("transaction_from") + ", " + Config.gets("com.busit.rest.admin.uid") + ", " + t.get("transaction_size") + ", " + t.get("transaction_date") + ", " + TAX_TYPE_SIZE + ", null)");
		}
		if( !t.get("transaction_qos").equals("0") )
		{
			Quota.getInstance().add(Config.gets("com.busit.rest.admin.uid"), Config.gets("com.busit.rest.quota.refund"), Integer.parseInt(t.get("transaction_qos")), true);
			Database.getInstance().insert("INSERT INTO transaction_history (transaction_from, transaction_to, transaction_amount, transaction_date, transaction_type, transaction_data) " +
				"VALUES (" + t.get("transaction_from") + ", " + Config.gets("com.busit.rest.admin.uid") + ", " + t.get("transaction_qos") + ", " + t.get("transaction_date") + ", " + TAX_TYPE_QOS + ", null)");
		}

		Database.getInstance().insert("DELETE FROM credit_transaction WHERE transaction_id = " + transaction);
	}
	
	private void initializeRefund(Index index)
	{
		Action refund = new Action()
		{
			public Object execute() throws Exception
			{
				String transaction = getParameter("transaction").getValue();
				INTERNAL_REFUND(transaction);
				return "OK";
			}
		};
		
		refund.addMapping(new String[] { "refund", "undo" });
		refund.description = "Rollback a transaction";
		refund.returnDescription = "OK";
		refund.addGrant(new String[] { Config.gets("com.busit.rest.brokerGrantName") });
		
		Parameter transaction = new Parameter();
		transaction.isOptional = false;
		transaction.minLength = 1;
		transaction.maxLength = 20;
		transaction.mustMatch = "^([0-9]+|" + Pattern.quote(Config.gets("com.busit.rest.cron")) + ")$";
		transaction.description = "The transaction id";
		transaction.addAlias(new String[]{ "transaction", "id", "tid" });
		refund.addParameter(transaction);
		
		index.addOwnHandler(refund);
	}
	
	/**
	 * Internal use only. Refunds a transaction.
	 * Every thransaction has a "price" (for the developper) and a "tax" (for busit)
	 * When we refund, if the transaction has been ACKed then it means it has been handled by the broker.
	 * Thus we consider we have done our part of the job and we do NOT refund the "tax".
	 * On the other hand, if the transaction has not been ACKed, then the broker did not handle it (yet)
	 * and thus it may be any error in the chain, so we refund the "price" + "tax".
	 * @param	transaction	the transaction ID
	 */
	static void INTERNAL_REFUND(String transaction) throws Exception
	{
		if( transaction == null )
			throw new Exception("Invalid transaction ID");
		if( transaction.equals(Config.gets("com.busit.rest.cron")) )
			return;
		if( !transaction.matches("^[0-9]+$") )
			throw new Exception("Invalid transaction ID");
			
		Map<String, String> t = Database.getInstance().selectOne("SELECT transaction_from, transaction_to, transaction_price, transaction_tax, transaction_count, transaction_ack, " + 
			"transaction_size, transaction_qos, transaction_date, transaction_type, transaction_data " + 
			"FROM credit_transaction WHERE transaction_id = " + transaction);
		
		if( t == null || t.get("transaction_from") == null )
			throw new Exception("Invalid transaction");
		
		int amount = Integer.parseInt(t.get("transaction_price")) + Integer.parseInt(t.get("transaction_tax"));
		if( amount > 0 )
			Quota.getInstance().add(t.get("transaction_from"), Config.gets("com.busit.rest.quota.credit"), amount, true);
		if( t.get("transaction_ack").equals("1") )
		{
			if( !t.get("transaction_count").equals("0") )
			{
				Quota.getInstance().add(Config.gets("com.busit.rest.admin.uid"), Config.gets("com.busit.rest.quota.refund"), Integer.parseInt(t.get("transaction_count")), true);
				Database.getInstance().insert("INSERT INTO transaction_history (transaction_from, transaction_to, transaction_amount, transaction_date, transaction_type, transaction_data) " +
					"VALUES (" + t.get("transaction_from") + ", " + Config.gets("com.busit.rest.admin.uid") + ", " + t.get("transaction_count") + ", " + t.get("transaction_date") + ", " + TAX_TYPE_COUNT + ", null)");
			}
			if( !t.get("transaction_size").equals("0") )
			{
				Quota.getInstance().add(Config.gets("com.busit.rest.admin.uid"), Config.gets("com.busit.rest.quota.refund"), Integer.parseInt(t.get("transaction_size")), true);
				Database.getInstance().insert("INSERT INTO transaction_history (transaction_from, transaction_to, transaction_amount, transaction_date, transaction_type, transaction_data) " +
					"VALUES (" + t.get("transaction_from") + ", " + Config.gets("com.busit.rest.admin.uid") + ", " + t.get("transaction_size") + ", " + t.get("transaction_date") + ", " + TAX_TYPE_SIZE + ", null)");
			}
			if( !t.get("transaction_qos").equals("0") )
			{
				Quota.getInstance().add(Config.gets("com.busit.rest.admin.uid"), Config.gets("com.busit.rest.quota.refund"), Integer.parseInt(t.get("transaction_qos")), true);
				Database.getInstance().insert("INSERT INTO transaction_history (transaction_from, transaction_to, transaction_amount, transaction_date, transaction_type, transaction_data) " +
					"VALUES (" + t.get("transaction_from") + ", " + Config.gets("com.busit.rest.admin.uid") + ", " + t.get("transaction_qos") + ", " + t.get("transaction_date") + ", " + TAX_TYPE_QOS + ", null)");
			}
		}
		Database.getInstance().insert("DELETE FROM credit_transaction WHERE transaction_id = " + transaction);
	}
	
	private void initializeClean(Index index)
	{
		Action clean = new Action()
		{
			public Object execute() throws Exception
			{
				Vector<Map<String,String>> data = Database.getInstance().select("SELECT transaction_id, transaction_price, transaction_tax " + 
					"FROM credit_transaction " + 
					"WHERE transaction_date < (UNIX_TIMESTAMP()-" + Config.gets("com.busit.rest.transactionLifetime") + ")");
				
				for( Map<String,String> t : data )
				{
					int price = Integer.parseInt(t.get("transaction_price"));
					int tax = Integer.parseInt(t.get("transaction_tax"));
					
					if( price > 0 || tax > 0 )
						INTERNAL_REFUND(t.get("transaction_id"));
					else
						Database.getInstance().insert("DELETE FROM credit_transaction WHERE transaction_id = " + t.get("transaction_id"));
				}
				
				return "OK";
			}
		};
		
		clean.addMapping(new String[] { "clean", "cleanup" });
		clean.description = "Clean and revert outdated transactions";
		clean.returnDescription = "OK";
		clean.addGrant(new String[] { Config.gets("com.busit.rest.brokerGrantName") });
		
		index.addOwnHandler(clean);
	}
}