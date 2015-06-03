package com.anotherservice.rest.model;

import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import com.anotherservice.db.*;
import java.util.*;
import java.util.regex.*;

public class User extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "user", "users" });
		index.description = "Manages users";
		Handler.addHandler(Initializer.path + "/", index);
		
		try { initializeSchemaInsert(); } catch(Exception e) { Logger.severe(e); }
		
		initializeInsert(index);
		initializeUpdate(index);
		initializeDelete(index);
		initializeSelect(index);
		
		Self.selfize(Initializer.path + "/user/update");
		Self.selfize(Initializer.path + "/user/select");
		Self.selfize(Initializer.path + "/user/delete");
	}
	
	private void initializeSchemaInsert()
	{
		Action a = (Action) Handler.getHandler(Initializer.path + "/schema/users/insert");
		a.getParameter("user_name").enforce(false, 3, 30, PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT));
		a.getParameter("user_mail").mustMatch = PatternBuilder.EMAIL;
		a.getParameter("user_password").enforce(4, 100);
		a.getParameter("user_lang").mustMatch = PatternBuilder.getRegex(PatternBuilder.UPPER);
		a.getParameter("user_ip").mustMatch = "^(([0-9]{1,3}(\\.|$)){4})|(([a-fA-F0-9]{0,4}(:|$)){2,8})$";
		a.removeParameter("user_id");
		a.removeParameter("user_date");
		
		Table users = Schema.getInstance().table("users");
		users.beforeInsert.add(new Trigger()
		{
			public void apply(Table table, Row row)
			{
				String user = row.value("user_name");
				if( user.matches("^[0-9]+$") || user.matches("(^[\\._\\-]|[\\._\\-]$)") )
					throw new IllegalArgumentException("The user name may not be numeric and may not start or end with special characters");
				if( Pattern.compile("(:?^[\\s_\\-\\.])|[\\s_\\-\\.]{2,}|(:?[\\s_\\-\\.]$)").matcher(user).find() )
					throw new IllegalArgumentException("The token name may not start, end or contain consecutive special characters");
				
				// hash the password
				row.value("user_password", Security.getInstance().hash(row.value("user_password")));
				// add the date manually
				row.put(table.column("user_date"), "UNIX_TIMESTAMP()");
			}
		});
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				String user = getParameter("name").getValue();
				String pass = getParameter("pass").getValue();
				String firstname = getParameter("firstname").getValue();
				String lastname = getParameter("lastname").getValue();
				String mail = getParameter("mail").getValue();
				String ip = getParameter("ip").getValue();
				String lang = getParameter("lang").getValue();
				
				// TODO : remove this and replace by address (& setup ?)
				String inviter = getParameter("inviter").getValue();
				
				if( user.matches("^[0-9]+$") || user.matches("(^[\\._\\-]|[\\._\\-]$)") )
					throw new Exception("The user name may not be numeric and may not start or end with special characters");
				if( Pattern.compile("(:?^[\\s_\\-\\.])|[\\s_\\-\\.]{2,}|(:?[\\s_\\-\\.]$)").matcher(user).find() )
					throw new Exception("The token name may not start, end or contain consecutive special characters");
				
				String columns = "(user_name, user_password, user_date";
				String values = "('" + Security.escape(user) + "', '" + Security.escape(Security.getInstance().hash(pass)) + "', UNIX_TIMESTAMP()";
				
				if( firstname != null )
				{
					columns += ", user_firstname";
					values += ", '" + Security.escape(firstname) + "'";
				}
				if( lastname != null )
				{
					columns += ", user_lastname";
					values += ", '" + Security.escape(lastname) + "'";
				}
				if( mail != null )
				{
					columns += ", user_mail";
					values += ", '" + Security.escape(mail) + "'";
				}
				if( ip != null )
				{
					columns += ", user_ip";
					values += ", '" + Security.escape(ip) + "'";
				}
				if( lang != null )
				{
					columns += ", user_lang";
					values += ", '" + Security.escape(lang) + "'";
				}
				if( inviter != null )
				{
					columns += ", user_origin";
					values += ", '" + Security.escape(inviter) + "'";
				}
				
				columns += ")";
				values += ")";
				
				Long uid = Initializer.db.insert("INSERT INTO users " + columns + " VALUES " + values);
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("name", user);
				result.put("id", uid);
				return result;
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Creates a new user";
		insert.returnDescription = "The newly created user {'name', 'id'}";
		insert.addGrant(new String[] { "access", "user_insert" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 3;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.allowInUrl = true;
		user.description = "The user login";
		user.addAlias(new String[]{ "name", "user_name", "username", "login", "user" });
		insert.addParameter(user);
		
		Parameter pass = new Parameter();
		pass.isOptional = false;
		pass.minLength = 2;
		pass.maxLength = 100;
		pass.mustMatch = PatternBuilder.getRegex(PatternBuilder.PHRASE | PatternBuilder.SPECIAL);
		pass.description = "The user password";
		pass.addAlias(new String[]{ "pass", "password", "user_password", "user_pass" });
		insert.addParameter(pass);
		
		Parameter firstname = new Parameter();
		firstname.isOptional = true;
		firstname.minLength = 0;
		firstname.maxLength = 50;
		firstname.mustMatch = PatternBuilder.getRegex(PatternBuilder.PHRASE);
		firstname.description = "The user's first name";
		firstname.addAlias(new String[]{ "firstname", "givenname", "first_name", "user_firstname", "user_givenname", "user_first_name", "user_given_name" });
		insert.addParameter(firstname);
		
		Parameter lastname = new Parameter();
		lastname.isOptional = true;
		lastname.minLength = 0;
		lastname.maxLength = 50;
		lastname.mustMatch = PatternBuilder.getRegex(PatternBuilder.PHRASE);
		lastname.description = "The user's last name";
		lastname.addAlias(new String[]{ "lastname", "sn", "user_lastname", "user_sn", "last_name", "user_last_name" });
		insert.addParameter(lastname);
		
		Parameter mail = new Parameter();
		mail.isOptional = true;
		mail.minLength = 0;
		mail.maxLength = 150;
		mail.mustMatch = PatternBuilder.EMAIL;
		mail.description = "The user's email";
		mail.addAlias(new String[]{ "mail", "email", "address", "user_email", "user_mail", "user_address" });
		insert.addParameter(mail);
		
		Parameter ip = new Parameter();
		ip.isOptional = true;
		ip.minLength = 7;
		ip.maxLength = 70;
		ip.mustMatch = "^(([0-9]{1,3}(\\.|$)){4})|(([a-fA-F0-9]{0,4}(:|$)){2,8})$";
		ip.description = "The user's ip address";
		ip.addAlias(new String[]{ "ip", "user_ip", "origin", "user_origin" });
		insert.addParameter(ip);
		
		Parameter lang = new Parameter();
		lang.isOptional = true;
		lang.minLength = 2;
		lang.maxLength = 4;
		lang.mustMatch = PatternBuilder.getRegex(PatternBuilder.UPPER);
		lang.description = "The user's language";
		lang.addAlias(new String[]{ "language", "user_language", "lang", "user_lang" });
		insert.addParameter(lang);
		
		Parameter inviter = new Parameter();
		inviter.isOptional = true;
		inviter.minLength = 1;
		inviter.maxLength = 20;
		inviter.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		inviter.description = "The inviting user id";
		inviter.addAlias(new String[]{ "inviter", "origin", "origin_id", "origin_user" });
		insert.addParameter(inviter);
		
		index.addOwnHandler(insert);
	}

	private void initializeUpdate(Index index)
	{
		Action update = new Action()
		{
			public Object execute() throws Exception
			{
				String user = getParameter("name").getValue();
				String newname = getParameter("newname").getValue();
				String pass = getParameter("pass").getValue();
				String firstname = getParameter("firstname").getValue();
				String lastname = getParameter("lastname").getValue();
				String mail = getParameter("mail").getValue();
				String lang = getParameter("lang").getValue();
				String address = getParameter("address").getValue();
				String newsletter = getParameter("newsletter").getValue();
				String version = getParameter("version").getValue();
				String searchable = getParameter("public").getValue();
				String notification = getParameter("notification").getValue();
				String bankName = getParameter("bank_name").getValue();
				String bankAddress = getParameter("bank_address").getValue();
				String bankIban = getParameter("bank_iban").getValue();
				String bankBic =getParameter("bank_bic").getValue();
				
				if( newsletter != null && newsletter.matches("^(?i)(yes|true|1)$") )
					newsletter = "1";
				else if( newsletter != null )
					newsletter = "0";
				if( searchable != null && searchable.matches("^(?i)(yes|true|1)$") )
					searchable = "1";
				else if( searchable != null )
					searchable = "0";
					
				if( !user.matches("^[0-9]+$") )
				{
					Map<String,String> r = Initializer.db.selectOne("SELECT user_id FROM users WHERE user_name = '" + Security.escape(user) + "'");
					if( r == null || r.get("user_id") == null )
						throw new Exception("Unknown user");
					user = r.get("user_id");
				}
				
				String values = "user_id = user_id";
				
				if( newname != null )
					values += ", user_name = '" + Security.escape(newname) + "'";
				if( pass != null && pass.length() > 0 )
					values += ", user_password = '" + Security.escape(Security.getInstance().hash(pass)) + "'";
				if( firstname != null )
					values += ", user_firstname = '" + Security.escape(firstname) + "'";
				if( lastname != null )
					values += ", user_lastname = '" + Security.escape(lastname) + "'";
				if( mail != null )
					values += ", user_mail = '" + Security.escape(mail) + "'";
				if( lang != null )
					values += ", user_lang = '" + Security.escape(lang) + "'";
				if( newsletter != null )
					values += ", user_newsletter = '" + Security.escape(newsletter) + "'";
				if( address != null )
					values += ", user_address = '" + Security.escape(address) + "'";
				if( searchable != null )
					values += ", user_public = '" + Security.escape(searchable) + "'";
				if( version != null )
					values += ", user_version = '" + Security.escape(version) + "'";
				if( notification != null )
					values += ", user_notification = '" + Security.escape(notification) + "'";
					
				Initializer.db.update("UPDATE users SET " + values + " WHERE user_id = " + user);
				
				String values2 = "user_id = user_id";
				if( bankName != null )
					values2 += ", bank_name = '" + Security.escape(bankName) + "'";
				if( bankAddress != null )
					values2 += ", bank_address = '" + Security.escape(bankAddress) + "'";
				if( bankIban != null )
					values2 += ", bank_iban = '" + Security.escape(bankIban) + "'";
				if( bankBic != null )
					values2 += ", bank_bic = '" + Security.escape(bankBic) + "'";
				
				Initializer.db.update("UPDATE user_banking SET " + values2 + " WHERE user_id = " + user);
				
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Modifies the information related to a user";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "user_update" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 50;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.allowInUrl = true;
		user.description = "The user login or id";
		user.addAlias(new String[]{ "name", "user_name", "username", "login", "user", "id", "user_id", "uid" });
		update.addParameter(user);
		
		Parameter newname = new Parameter();
		newname.isOptional = true;
		newname.minLength = 3;
		newname.maxLength = 50;
		newname.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		newname.allowInUrl = true;
		newname.description = "The user new login";
		newname.addAlias(new String[]{ "newusername", "new_username", "new_login", "newlogin", "newname" });
		update.addParameter(newname);
		
		Parameter pass = new Parameter();
		pass.isOptional = true;
		pass.minLength = 2;
		pass.maxLength = 30;
		pass.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		pass.description = "The user password";
		pass.addAlias(new String[]{ "pass", "password", "user_password", "user_pass" });
		update.addParameter(pass);
		
		Parameter firstname = new Parameter();
		firstname.isOptional = true;
		firstname.minLength = 0;
		firstname.maxLength = 80;
		firstname.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		firstname.description = "The user's first name";
		firstname.addAlias(new String[]{ "firstname", "givenname", "first_name", "user_firstname", "user_givenname", "user_first_name", "user_given_name" });
		update.addParameter(firstname);
		
		Parameter lastname = new Parameter();
		lastname.isOptional = true;
		lastname.minLength = 0;
		lastname.maxLength = 80;
		lastname.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		lastname.description = "The user's last name";
		lastname.addAlias(new String[]{ "lastname", "sn", "user_lastname", "user_sn", "last_name", "user_last_name" });
		update.addParameter(lastname);
		
		Parameter mail = new Parameter();
		mail.isOptional = true;
		mail.minLength = 0;
		mail.maxLength = 150;
		mail.mustMatch = PatternBuilder.EMAIL;
		mail.description = "The user's email";
		mail.addAlias(new String[]{ "mail", "email", "user_email", "user_mail" });
		update.addParameter(mail);
		
		Parameter lang = new Parameter();
		lang.isOptional = true;
		lang.minLength = 2;
		lang.maxLength = 4;
		lang.mustMatch = PatternBuilder.getRegex(PatternBuilder.UPPER);
		lang.description = "The user's language";
		lang.addAlias(new String[]{ "language", "user_language", "lang", "user_lang" });
		update.addParameter(lang);
	
		Parameter address = new Parameter();
		address.isOptional = true;
		address.minLength = 5;
		address.maxLength = 1500;
		address.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		address.description = "The user's address";
		address.addAlias(new String[]{ "address", "postal", "billing_address", "user_address" });
		update.addParameter(address);
		
		Parameter newsletter = new Parameter();
		newsletter.isOptional = true;
		newsletter.minLength = 1;
		newsletter.maxLength = 10;
		newsletter.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		newsletter.description = "newsletter ok";
		newsletter.addAlias(new String[]{ "newsletter" });
		update.addParameter(newsletter);
		
		Parameter version = new Parameter();
		version.isOptional = true;
		version.minLength = 1;
		version.maxLength = 4;
		version.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		version.description = "The user's version";
		version.addAlias(new String[]{ "version", "user_version" });
		update.addParameter(version);
		
		Parameter searchable = new Parameter();
		searchable.isOptional = true;
		searchable.minLength = 1;
		searchable.maxLength = 10;
		searchable.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		searchable.description = "Is this user can be found?";
		searchable.addAlias(new String[]{ "public", "searchable" });
		update.addParameter(searchable);
		
		Parameter notification = new Parameter();
		notification.isOptional = true;
		notification.minLength = 1;
		notification.maxLength = 2;
		notification.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		notification.description = "User notification level";
		notification.addAlias(new String[]{ "notification", "notifications", "notif" });
		update.addParameter(notification);
		
		Parameter bankName = new Parameter();
		bankName.isOptional = true;
		bankName.minLength = 0;
		bankName.maxLength = 150;
		bankName.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		bankName.description = "The user's bank name";
		bankName.addAlias(new String[]{ "bank_name", "bankname", "bank" });
		update.addParameter(bankName);
		
		Parameter bankAddress = new Parameter();
		bankAddress.isOptional = true;
		bankAddress.minLength = 0;
		bankAddress.maxLength = 500;
		bankAddress.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		bankAddress.description = "The user's bank address";
		bankAddress.addAlias(new String[]{ "bank_address", "bankaddress" });
		update.addParameter(bankAddress);
		
		Parameter bankIban = new Parameter();
		bankIban.isOptional = true;
		bankIban.minLength = 0;
		bankIban.maxLength = 150;
		bankIban.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		bankIban.description = "The user's bank IBAN";
		bankIban.addAlias(new String[]{ "bank_iban", "bankiban", "iban" });
		update.addParameter(bankIban);
		
		Parameter bankBic = new Parameter();
		bankBic.isOptional = true;
		bankBic.minLength = 0;
		bankBic.maxLength = 50;
		bankBic.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		bankBic.description = "The user's bank BIC";
		bankBic.addAlias(new String[]{ "bank_bic", "bankbic", "bic" });
		update.addParameter(bankBic);
		
		index.addOwnHandler(update);
	}
	
	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				String user = getParameter("name").getValue();
				
				if( !user.matches("^[0-9]+$") )
				{
					Map<String,String> r = Initializer.db.selectOne("SELECT user_id FROM users WHERE user_name = '" + Security.escape(user) + "'");
					if( r == null || r.get("user_id") == null )
						throw new Exception("Unknown user");
					user = r.get("user_id");
				}
				
				// temporary during V1 => V2 transition, for testing purpose
				Initializer.db.delete("DELETE FROM confirm WHERE confirm_user = " + user);
				
				Initializer.db.delete("DELETE FROM users WHERE user_id = " + user);
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy" });
		delete.description = "Removes a user";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "user_delete" });
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.allowInUrl = true;
		user.description = "The user login or id";
		user.addAlias(new String[]{ "name", "user_name", "username", "login", "user", "id", "user_id", "uid" });
		delete.addParameter(user);
		
		index.addOwnHandler(delete);
	}
	
	private void initializeSelect(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> user = getParameter("name").getValues();
				Collection<String> firstname = getParameter("firstname").getValues();
				Collection<String> lastname = getParameter("lastname").getValues();
				Collection<String> mail = getParameter("mail").getValues();
				Collection<String> ip = getParameter("ip").getValues();
				Collection<String> lang = getParameter("user_lang").getValues();
				String keyword = getParameter("keyword").getValue();
				String from = getParameter("from").getValue();
				String to = getParameter("to").getValue();
				String count = getParameter("count").getValue();
				String limit = getParameter("limit").getValue();
				String group = getParameter("group").getValue();
				
				String where = "";
				if( user.size() > 0 )
				{
					where += " AND (1>1";
					for( String u : user )
					{
						if( u.matches("^[0-9]+$") )
							where += " OR user_id = " + Security.escape(u);
						else
							where += " OR user_name LIKE '%" + Security.escape(u) + "%'";
					}
					where += ")";
				}
				if( firstname.size() > 0 )
				{
					where += " AND (1>1";
					for( String f : firstname )
						where += " OR user_firstname LIKE '%" + Security.escape(f) + "%'";
					where += ")";
				}
				if( lastname.size() > 0 )
				{
					where += " AND (1>1";
					for( String l : lastname )
						where += " OR user_lastname LIKE '%" + Security.escape(l) + "%'";
					where += ")";
				}
				if( mail.size() > 0 )
				{
					where += " AND (1>1";
					for( String m : mail )
						where += " OR user_mail LIKE '%" + Security.escape(m) + "%'";
					where += ")";
				}
				if( ip.size() > 0 )
				{
					where += " AND (1>1";
					for( String i : ip )
						where += " OR user_ip LIKE '%" + Security.escape(i) + "%'";
					where += ")";
				}
				if( lang.size() > 0 && user.size() == 0 )
				{
					where += " AND (1>1";
					for( String l : lang )
						where += " OR user_lang = '" + Security.escape(l) + "'";
					where += ")";
				}
				if( from != null && from.length() > 0 )
					where += " AND user_date >= " + Security.escape(from);
				if( to != null && to.length() > 0 )
					where += " AND user_date <= " + Security.escape(to);
				
				if( count != null && count.matches("^(?i)(yes|true|1)$") )
					return Initializer.db.select("SELECT COUNT(user_id) as count FROM users WHERE 1=1 " + where);
				
				if( limit != null )
					limit = " ORDER BY user_date DESC LIMIT 0," + limit;
				else
					limit = " ORDER BY user_date DESC LIMIT 0,1000";
				
				if( keyword != null )
				{
					where = "user_name LIKE '%" + Security.escape(keyword) + "%' OR user_firstname LIKE '%" + Security.escape(keyword) + "%' OR user_lastname LIKE '%" 
					+ Security.escape(keyword) + "%' OR user_mail LIKE '%" + Security.escape(keyword) + "%' OR user_address LIKE '%" + Security.escape(keyword) + "%'";
					
					return Initializer.db.select("SELECT user_id, user_name, user_firstname, user_lastname, user_ip, user_mail, user_lang, user_date, user_status, user_newsletter, user_address, user_public, user_version, user_notification FROM users WHERE user_org = 0 AND ( " + where + ")" + limit);
				}
				
				if( group != null )
					return Initializer.db.select("SELECT COUNT(user_id) as count, " + group + " (FROM_UNIXTIME(user_date)) as " + group + " FROM users WHERE 1=1 " + where + " AND user_org = 0 GROUP BY " + group + " (FROM_UNIXTIME(user_date))");
			
				return Initializer.db.select("SELECT user_id, user_name, user_firstname, user_lastname, user_ip, user_mail, user_lang, user_date, user_status, user_newsletter, user_address, user_public, user_version, user_notification, confirm_id, confirm_date FROM users LEFT JOIN confirm ON(confirm_email = user_mail) WHERE 1=1 " + where + " AND user_org = 0 " + limit);
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves information about a user. Multiple search values for a single property will be OR'd and different criteria will be AND'd.";
		select.returnDescription = "The matching users [{'name', 'id', 'firstname, 'lastname', 'ip', 'mail', 'date', 'lang'},...]";
		select.addGrant(new String[] { "access", "user_select" });
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.allowInUrl = true;
		user.isMultipleValues = true;
		user.description = "The user login or id (will match *[user]* if not a number or an exact user id match if numeric)";
		user.addAlias(new String[]{ "name", "user_name", "username", "login", "id", "uid", "user_id", "user", "names", "user_names", "usernames", "logins", "users", "ids", "uids", "user_ids" });
		select.addParameter(user);

		Parameter keyword = new Parameter();
		keyword.isOptional = true;
		keyword.minLength = 3;
		keyword.maxLength = 50;
		keyword.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		keyword.isMultipleValues = true;
		keyword.description = "A keyword to search (excludes all other parameters!)";
		keyword.addAlias(new String[]{ "keyword", "key" });
		select.addParameter(keyword);
		
		Parameter firstname = new Parameter();
		firstname.isOptional = true;
		firstname.minLength = 3;
		firstname.maxLength = 50;
		firstname.mustMatch = PatternBuilder.getRegex(PatternBuilder.PHRASE);
		firstname.isMultipleValues = true;
		firstname.description = "The user's first name (will match *[firstname]*)";
		firstname.addAlias(new String[]{ "firstname", "givenname", "first_name", "user_firstname", "user_givenname", "user_first_name", "user_given_name" });
		select.addParameter(firstname);
		
		Parameter lastname = new Parameter();
		lastname.isOptional = true;
		lastname.minLength = 3;
		lastname.maxLength = 50;
		lastname.mustMatch = PatternBuilder.getRegex(PatternBuilder.PHRASE);
		lastname.isMultipleValues = true;
		lastname.description = "The user's last name (will match *[lastname]*)";
		lastname.addAlias(new String[]{ "lastname", "sn", "user_lastname", "user_sn", "last_name", "user_last_name" });
		select.addParameter(lastname);
		
		Parameter mail = new Parameter();
		mail.isOptional = true;
		mail.minLength = 3;
		mail.maxLength = 150;
		mail.mustMatch = "^[@_a-zA-Z0-9\\.\\+-]+$";
		mail.isMultipleValues = true;
		mail.description = "The user's email (will match *[mail]*)";
		mail.addAlias(new String[]{ "mail", "email", "address", "user_email", "user_mail", "user_address" });
		select.addParameter(mail);
		
		Parameter ip = new Parameter();
		ip.isOptional = true;
		ip.minLength = 3;
		ip.maxLength = 40;
		ip.mustMatch = "^(([0-9]{1,3}(\\.|$)){4})|(([a-fA-F0-9]{4}(:|$)){8})$";
		ip.isMultipleValues = true;
		ip.description = "The user's ip address (will match *[ip]*)";
		ip.addAlias(new String[]{ "ip", "user_ip", "origin", "user_origin" });
		select.addParameter(ip);
		
		Parameter lang = new Parameter();
		lang.isOptional = true;
		lang.minLength = 2;
		lang.maxLength = 4;
		lang.mustMatch = PatternBuilder.getRegex(PatternBuilder.UPPER);
		lang.isMultipleValues = true;
		lang.description = "The user's language (exact match)";
		lang.addAlias(new String[]{ "user_language", "user_lang" });
		select.addParameter(lang);
		
		Parameter from = new Parameter();
		from.isOptional = true;
		from.minLength = 1;
		from.maxLength = 11;
		from.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		from.description = "The user's registration date must be after this date (>= timestamp)";
		from.addAlias(new String[]{ "from", "date", "user_date", "date_from" });
		select.addParameter(from);
		
		Parameter to = new Parameter();
		to.isOptional = true;
		to.minLength = 1;
		to.maxLength = 11;
		to.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		to.description = "The user's registration date must be before this date (<= timestamp)";
		to.addAlias(new String[]{ "to", "date_to" });
		select.addParameter(to);

		Parameter limit = new Parameter();
		limit.isOptional = true;
		limit.minLength = 1;
		limit.maxLength = 11;
		limit.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		limit.description = "Limit results";
		limit.addAlias(new String[]{ "limit", "limit_to" });
		select.addParameter(limit);
		
		Parameter count = new Parameter();
		count.isOptional = true;
		count.minLength = 1;
		count.maxLength = 10;
		count.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		count.description = "Count users?";
		count.addAlias(new String[]{ "count" });
		select.addParameter(count);

		Parameter group = new Parameter();
		group.isOptional = true;
		group.minLength = 1;
		group.maxLength = 11;
		group.mustMatch = PatternBuilder.getRegex(PatternBuilder.UPPER);
		group.description = "Group by?";
		group.addAlias(new String[]{ "group", "group_by" });
		select.addParameter(group);
		
		index.addOwnHandler(select);
	}
}
