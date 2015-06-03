package com.busit.rest;

import com.anotherservice.db.*;
import com.anotherservice.rest.*;
import com.anotherservice.io.*;
import com.anotherservice.util.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.rest.core.*;
import com.busit.security.*;
import java.util.*;
import java.util.regex.*;
import java.io.InputStream;

public class Connector extends InitializerOnce
{
	private static final int ERROR_CLASS_ID = 0x20000;
	
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "connector", "connectors" });
		index.description = "Manages connectors";
		Handler.addHandler("/busit/", index);
		
		initializeInsert(index);
		initializeUpdate(index);
		initializeDelete(index);
		initializeSelect(index);
		initializeSelectIndex(index);
		initializeTrends(index);
		initializeUpload(index);
		initializeDownload(index);
		initializeSetRate(index);
		initializeGetRate(index);
		initializeDeleteFile(index);
		
		Self.selfize("/busit/connector/insert");
		Self.selfize("/busit/connector/update");
		Self.selfize("/busit/connector/delete");
		Self.selfize("/busit/connector/select");
		Self.selfize("/busit/connector/upload");
		Self.selfize("/busit/connector/download");
		Self.selfize("/busit/connector/setrate");
		Self.selfize("/busit/connector/getrate");
		Self.selfize("/busit/connector/deletefile");
		
		initializeImport(index);
		Self.selfize("/busit/connector/import");
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				String name = getParameter("name").getValue();
				String category = getParameter("category").getValue();
				String user = getParameter("user").getValue();
				
				if( name.matches("^[0-9]+$") || name.matches("(^[\\._\\-\\s]|[\\._\\-\\s]$)") )
					throw new Exception("The connector name may not be numeric and may not start or end with special characters");
				if( Pattern.compile("(:?^[\\s_\\-\\.])|[\\s_\\-\\.]{2,}|(:?[\\s_\\-\\.]$)").matcher(name).find() )
					throw new Exception("The connector name may not start, end or contain consecutive special characters");
				
				if( !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
				
				if( category == null )
					category = "1";
				
				Long uid = Database.getInstance().insert("INSERT INTO connectors (connector_name, connector_category, connector_user, connector_date) VALUES ('" + Security.escape(name) + "', '" + Security.escape(category) + "', " + user + ", UNIX_TIMESTAMP())");
				
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("name", name);
				result.put("id", uid);
				return result;
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Creates a new connector";
		insert.returnDescription = "The newly created connector {'name', 'id'}";
		insert.addGrant(new String[] { "access", "connector_insert" });
		
		Parameter name = new Parameter();
		name.isOptional = false;
		name.minLength = 3;
		name.maxLength = 30;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		name.allowInUrl = true;
		name.description = "The connector name";
		name.addAlias(new String[]{ "name", "connector_name" });
		insert.addParameter(name);

		Parameter category = new Parameter();
		category.isOptional = true;
		category.minLength = 1;
		category.maxLength = 11;
		category.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		category.description = "The connector category";
		category.addAlias(new String[]{ "category", "connector_category" });
		insert.addParameter(category);
		
		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
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
				String name = getParameter("name").getValue(); // CAUTION : we cannot change the initial connector name. The name here is the TRANSLATION of the name !
				String description = getParameter("description").getValue();
				String category = getParameter("category").getValue();
				String useprice = getParameter("useprice").getValue();
				String usetax = getParameter("usetax").getValue();
				String buyprice = getParameter("buyprice").getValue();
				String buytax = getParameter("buytax").getValue();
				String help = getParameter("help").getValue();
				String promo = getParameter("promo").getValue();
				String configurl = getParameter("configurl").getValue();
				String panelurl = getParameter("panelurl").getValue();
				String language = getParameter("language").getValue();
				String userstatus = getParameter("userstatus").getValue();
				String status = getParameter("status").getValue();
				String programming = getParameter("programming").getValue();
				Collection<String> tag = getParameter("tag").getValues();
				String tagmethod = getParameter("tagmethod").getValue();
				String direction = getParameter("direction").getValue();
				String buyurl = getParameter("buyurl").getValue();
				String docurl = getParameter("docurl").getValue();
				String user = getParameter("user").getValue();
				
				if( name != null )
				{
					if( name.matches("^[0-9]+$") || name.matches("(^[\\._\\-\\s]|[\\._\\-\\s]$)") )
						throw new Exception("The connector name may not be numeric and may not start or end with special characters");
					if( Pattern.compile("(:?^[\\s_\\-\\.])|[\\s_\\-\\.]{2,}|(:?[\\s_\\-\\.]$)").matcher(name).find() )
						throw new Exception("The connector name may not start, end or contain consecutive special characters");
				}
				
				if( user != null )
				{
					if( !IdentityChecker.getInstance().isUserConnector(user, id) )
						throw new Exception("The current user is not an administrator of the provided connector");
				}
				
				if( tagmethod == null )
					tagmethod = "add";
				
				if( tag.size() > 0 )
				{
					for( String t : tag )
					{
						if( !t.matches("^[0-9]+$") )
						{
							Map<String, String> result = Database.getInstance().selectOne("SELECT tag_id FROM tags WHERE tag_name = '" + Security.escape(t) + "'");
							if( result != null && result.get("tag_id") != null )
								t = result.get("tag_id");
							else
							{
								Long uid = Database.getInstance().insert("INSERT INTO tags (tag_name) VALUES ('" + Security.escape(t) + "')");
								t = String.valueOf(uid);
							}
						}
						
						if( tagmethod.equals("add") )
							Database.getInstance().insert("INSERT INTO connector_tag (tag_id, connector_id) VALUES (" + t + ", " + Security.escape(id) + ")");
						else if( tagmethod.equals("delete") )
							Database.getInstance().insert("DELETE FROM connector_tag WHERE tag_id = " + t + " AND connector_id = " + Security.escape(id));
					}
				}				
				
				if( category != null || useprice != null || usetax != null || buyprice != null || buytax != null || userstatus != null || status != null || programming != null )
				{
					String set = "";
					if( category != null )
						set += "connector_category = '" + Security.escape(category) + "', ";
					if( useprice != null )
						set += "connector_use_price = '" + Security.escape(useprice) + "', ";
					if( buyprice != null )
						set += "connector_buy_price = '" + Security.escape(buyprice) + "', ";
					if( direction != null )
						set += "connector_direction = '" + Security.escape(direction) + "', ";
					if( userstatus != null )
						set += "connector_user_status = '" + Security.escape(userstatus) + "', ";
					if( status != null && Security.getInstance().hasGrant(Config.gets("com.busit.rest.admin.grant")) )
						set += "connector_intern_status = '" + Security.escape(status) + "', ";
					if( programming != null )
						set += "connector_language = '" + Security.escape(programming) + "', ";
					if( usetax != null && Security.getInstance().hasGrant(Config.gets("com.busit.rest.admin.grant")) )
						set += "connector_use_tax = '" + Security.escape(usetax) + "', ";
					if( buytax != null && Security.getInstance().hasGrant(Config.gets("com.busit.rest.admin.grant")) )
						set += "connector_buy_tax = '" + Security.escape(buytax) + "', ";
						
					Database.getInstance().update("UPDATE connectors SET " + set + " connector_id = " + Security.escape(id) + 
						" WHERE connector_id = " + Security.escape(id));
				}

				// update language
				if( name != null || description != null || help != null || promo != null || configurl != null || panelurl != null )
				{
					if( language == null )
						language = "EN";
					
					// get existing translation ID
					String tid = null;
					Any data = null;
					Map<String, String> result = Database.getInstance().selectOne("SELECT t.translation_id, t.translation_text " +
						"FROM connector_translation ct " + 
						"LEFT JOIN translations t ON(ct.translation_id = t.translation_id) " + 
						"WHERE ct.connector_id = " + id + " AND ct.translation_language = '" + language + "'");
						
					if( result != null || result.get("translation_id") != null )
					{
						tid = result.get("translation_id");
						data = Json.decode(result.get("translation_text"));
					}
					
					if( data == null || data.isEmpty() )
					{
						data = Any.empty(); 
						data.put("connector_name", null); data.put("connector_description", null); 
						data.put("connector_help", null); data.put("connector_promo", null); 
						data.put("connector_config_url", null); data.put("connector_panel_url", null);
					}
					
					if( name != null )
						data.put("connector_name", name);
					if( description != null )
						data.put("connector_description", description);
					if( help != null )
						data.put("connector_help", help);
					if( promo != null )
						data.put("connector_promo", promo);
					if( configurl != null )
						data.put("connector_config_url", configurl);
					if( panelurl != null )
						data.put("connector_panel_url", panelurl);
					if( buyurl != null )
						data.put("connector_buy_url", buyurl);
					if( docurl != null )
						data.put("connector_doc_url", docurl);
						
					if( tid != null )
					{
						// update existing
						Database.getInstance().update("UPDATE translations SET translation_text = '" + Security.escape(Json.encode(data)) + "' WHERE translation_id = " + tid);
					}
					else
					{
						// insert new
						tid = Database.getInstance().insert("INSERT INTO translations (translation_text) VALUES ('" + Security.escape(Json.encode(data)) + "')").toString();
						
						Database.getInstance().insert("INSERT INTO connector_translation (connector_id, translation_id, translation_language) " + 
							"VALUES (" + id + ", " + tid + ", '" + language + "')");
					}
				}
				
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Modify a connector";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "connector_update" });
	
		Parameter id = new Parameter();
		id.isOptional = false;
		id.minLength = 1;
		id.maxLength = 30;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.allowInUrl = true;
		id.description = "The connector id";
		id.addAlias(new String[]{ "connector", "id", "connector_id", "cid" });
		update.addParameter(id);
		
		Parameter name = new Parameter();
		name.isOptional = true;
		name.minLength = 3;
		name.maxLength = 32;
		name.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		name.description = "The connector name";
		name.addAlias(new String[]{ "name", "connector_name" });
		update.addParameter(name);
		
		Parameter description = new Parameter();
		description.isOptional = true;
		description.minLength = 1;
		description.maxLength = 2000;
		description.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		description.description = "The connector description";
		description.addAlias(new String[]{ "description", "connector_description" });
		update.addParameter(description);

		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		update.addParameter(user);
		
		Parameter category = new Parameter();
		category.isOptional = true;
		category.minLength = 1;
		category.maxLength = 11;
		category.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		category.description = "The connector category";
		category.addAlias(new String[]{ "category", "category_id", "connector_category" });
		update.addParameter(category);
		
		Parameter useprice = new Parameter();
		useprice.isOptional = true;
		useprice.minLength = 1;
		useprice.maxLength = 11;
		useprice.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		useprice.description = "The connector use price (developer tax)";
		useprice.addAlias(new String[]{ "useprice", "use_price" });
		update.addParameter(useprice);
		
		Parameter buyprice = new Parameter();
		buyprice.isOptional = true;
		buyprice.minLength = 1;
		buyprice.maxLength = 11;
		buyprice.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		buyprice.description = "The connector buy price (developer tax)";
		buyprice.addAlias(new String[]{ "buyprice", "buy_price" });
		update.addParameter(buyprice);
		
		Parameter usetax = new Parameter();
		usetax.isOptional = true;
		usetax.minLength = 1;
		usetax.maxLength = 11;
		usetax.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		usetax.description = "The connector use additionnal tax (busit tax)";
		usetax.addAlias(new String[]{ "usetax", "use_tax" });
		update.addParameter(usetax);
		
		Parameter buytax = new Parameter();
		buytax.isOptional = true;
		buytax.minLength = 1;
		buytax.maxLength = 11;
		buytax.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		buytax.description = "The connector buy additionnal tax (busit tax)";
		buytax.addAlias(new String[]{ "buytax", "buy_tax" });
		update.addParameter(buytax);

		Parameter help = new Parameter();
		help.isOptional = true;
		help.minLength = 1;
		help.maxLength = 2000;
		help.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		help.description = "The connector help";
		help.addAlias(new String[]{ "help", "connector_help" });
		update.addParameter(help);
		
		Parameter promo = new Parameter();
		promo.isOptional = true;
		promo.minLength = 1;
		promo.maxLength = 200;
		promo.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		promo.description = "Short promotionnal description";
		promo.addAlias(new String[]{ "promo", "connector_promo" });
		update.addParameter(promo);
		
		Parameter configurl = new Parameter();
		configurl.isOptional = true;
		configurl.minLength = 1;
		configurl.maxLength = 150;
		configurl.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		configurl.description = "The connector configuration url";
		configurl.addAlias(new String[]{ "configurl", "config_url", "connector_config_url" });
		update.addParameter(configurl);

		Parameter panelurl = new Parameter();
		panelurl.isOptional = true;
		panelurl.minLength = 1;
		panelurl.maxLength = 150;
		panelurl.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		panelurl.description = "The connector panel url";
		panelurl.addAlias(new String[]{ "panelurl", "panel_url", "connector_panel_url" });
		update.addParameter(panelurl);
		
		Parameter buyurl = new Parameter();
		buyurl.isOptional = true;
		buyurl.minLength = 1;
		buyurl.maxLength = 150;
		buyurl.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		buyurl.description = "The connector buy url";
		buyurl.addAlias(new String[]{ "buyurl", "buy_url", "connector_buy_url" });
		update.addParameter(buyurl);
		
		Parameter docurl = new Parameter();
		docurl.isOptional = true;
		docurl.minLength = 1;
		docurl.maxLength = 150;
		docurl.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		docurl.description = "The connector doc url";
		docurl.addAlias(new String[]{ "docurl", "doc_url", "connector_doc_url" });
		update.addParameter(docurl);
		
		Parameter language = new Parameter();
		language.isOptional = true;
		language.minLength = 2;
		language.maxLength = 2;
		language.mustMatch = PatternBuilder.getRegex(PatternBuilder.UPPER);
		language.description = "Translation language currently updated.";
		language.addAlias(new String[]{ "lang", "translation_language", "language" });
		update.addParameter(language);

		Parameter userstatus = new Parameter();
		userstatus.isOptional = true;
		userstatus.minLength = 1;
		userstatus.maxLength = 1;
		userstatus.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		userstatus.description = "Connector status for the developer.";
		userstatus.addAlias(new String[]{ "userstatus", "connector_user_status", "user_status" });
		update.addParameter(userstatus);
	
		Parameter status = new Parameter();
		status.isOptional = true;
		status.minLength = 1;
		status.maxLength = 1;
		status.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		status.description = "Connector status for the Busit team.";
		status.addAlias(new String[]{ "status", "busit_status" });
		update.addParameter(status);
	
		Parameter tag = new Parameter();
		tag.isOptional = true;
		tag.minLength = 1;
		tag.maxLength = 30;
		tag.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		tag.isMultipleValues = true;
		tag.description = "The tag name(s) or id(s)";
		tag.addAlias(new String[]{ "tag", "tag_name", "tid", "tag_id", "tag_names", "tags", "tids", "tag_ids" });
		update.addParameter(tag);

		Parameter tagmethod = new Parameter();
		tagmethod.isOptional = true;
		tagmethod.minLength = 1;
		tagmethod.maxLength = 7;
		tagmethod.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		tagmethod.description = "Tag method (add or delete)";
		tagmethod.addAlias(new String[]{ "tagmethod", "tag_method", "method" });
		update.addParameter(tagmethod);
		
		Parameter programming = new Parameter();
		programming.isOptional = true;
		programming.minLength = 1;
		programming.maxLength = 70;
		programming.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		programming.description = "Connector programming language";
		programming.addAlias(new String[]{ "programming", "connector_programming" });
		update.addParameter(programming);
		
		Parameter direction = new Parameter();
		direction.isOptional = true;
		direction.minLength = 1;
		direction.maxLength = 1;
		direction.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		direction.description = "Connector direction.";
		direction.addAlias(new String[]{ "direction", "connector_direction" });
		update.addParameter(direction);
		
		index.addOwnHandler(update);
	}
	
	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> connector = getParameter("name").getValues();

				String where = "1>1";
				for( String c : connector )
				{
					if( !IdentityChecker.getInstance().isUserConnector(null, c) )
						throw new Exception("The current user is not an administrator of the provided connector");
					
					if( !c.matches("^[0-9]+$") )
						where += " OR c.connector_name = '" + Security.escape(c) + "'";
					else
						where += " OR c.connector_id = " + c;
				}
				
				Database.getInstance().delete("DELETE c, t FROM connectors c " + 
					"LEFT JOIN connector_translation ct ON(c.connector_id = ct.connector_id) " +
					"LEFT JOIN translations t ON(t.translation_id = ct.translation_id) " + 
					"WHERE " + where);
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy" });
		delete.description = "Removes a connector";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "connector_delete" });
		
		Parameter connector = new Parameter();
		connector.isOptional = false;
		connector.minLength = 1;
		connector.maxLength = 30;
		connector.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		connector.allowInUrl = true;
		connector.isMultipleValues = true;
		connector.description = "The connector name(s) or id(s)";
		connector.addAlias(new String[]{ "name", "connector_name", "id", "connector_id", "cid", "names", "connector_names", "connector_names", "ids", "connector_ids", "connector_ids", "cids" });
		delete.addParameter(connector);

		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
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
				Collection<String> connector = getParameter("connector").getValues();
				String category = getParameter("category").getValue();
				String keyword = getParameter("keyword").getValue();
				String start = getParameter("start").getValue();
				String limit = getParameter("limit").getValue();
				String status = getParameter("status").getValue();
				String ustatus = getParameter("ustatus").getValue();
				String language = getParameter("language").getValue();
				String language2 = getParameter("language").getValue();
				String order = getParameter("order").getValue();
				String user = getParameter("user").getValue();
				String count = getParameter("count").getValue();
				String extended = getParameter("extended").getValue();
				Collection<String> tag = getParameter("tag").getValues();
				String interface_tag = getParameter("interface_tag").getValue();
				
				String where = "";
				
				if( user != null )
				{
					if( !user.matches("^[0-9]+$") )
						user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
					where += " AND c.connector_user = " + user + "";				
				}
				
				if( connector.size() > 0 )
				{
					where += " AND (1>1";
					for( String c : connector )
					{
						if( c.matches("^[0-9]+$") )
							where += " OR c.connector_id = " + Security.escape(c);
						else
							where += " OR c.connector_name = '" + Security.escape(c) + "'";
					}
					where += ")";
				}
				
				if( tag.size() > 0 )
				{
					where += " AND (1>1";
					for( String t : tag )
					{
						if( t.matches("^[0-9]+$") )
							where += " OR tg.tag_id = " + Security.escape(t);
						else
							where += " OR tg.tag_name = '" + Security.escape(t) + "'";
					}
					where += ")";
				}
				
				if( user != null && connector.size() > 0 )
					for( String c : connector )
						if( !IdentityChecker.getInstance().isUserConnector(user, c) )
							throw new Exception("The current user does not own the provided connector");
				
				if( category != null )
					where += " AND c.connector_category = " + category;
					
				if( status != null && status.equals("all") )
					where += " AND c.connector_intern_status >= 0";
				else if( status != null )
					where += " AND c.connector_intern_status = " + status;
				else
					where += " AND c.connector_intern_status > 1";
					
				if( ustatus != null && ustatus.equals("all") )
					where += " AND c.connector_user_status >= 0";
				else if( ustatus != null )
					where += " AND c.connector_user_status = " + ustatus;
				else
					where += " AND c.connector_user_status > 1";

				if( start != null && limit != null )
					limit = "LIMIT " + start + ", " + limit;
				else
					limit = "LIMIT 0, 10";
				
				if( order != null )
					order = Security.escape(order);
				else
					order = "c.connector_id";

				Boolean filter = false;
				if( interface_tag != null )
				{
					interface_tag = " AND ci.interface_tag = '" + interface_tag + "'";
					filter = true;
				}
				else
					interface_tag = "";
								
				// return count only, keyword does not matter
				if( count != null && count.matches("^(?i)(yes|true|1)$") )
					return Database.getInstance().select("SELECT COUNT(c.connector_id) as count FROM connectors c WHERE 1=1 " + where + " ORDER BY " + order + " DESC " + limit);
				
				// return connector data only, keyword does not matter
				double vat = Double.parseDouble(Config.gets("com.busit.rest.vat.percent"));
					
				if( language == null )
				{
					List<Map<String, String>> data = Database.getInstance().select("SELECT c.connector_id, c.connector_user, c.connector_name, c.connector_date, c.connector_instances, c.connector_direction, " + 
						"c.connector_category, c.connector_use_price, c.connector_use_tax, c.connector_buy_price, c.connector_buy_tax, c.connector_user_status, " +
						"c.connector_intern_status, c.connector_rating, " + 
						"c.connector_language, u.user_name, u.user_firstname, u.user_lastname, " + 
						"COUNT(DISTINCT i.instance_user) as users, " + 
						"GROUP_CONCAT(DISTINCT tg.tag_id) as tag_id, GROUP_CONCAT(DISTINCT tg.tag_name) as tag_name " +
						"FROM connectors c " + 
						"LEFT JOIN users u ON(c.connector_user = u.user_id) " + 
						"LEFT JOIN connector_tag cta ON(c.connector_id = cta.connector_id) " + 
						"LEFT JOIN tags tg ON(cta.tag_id = tg.tag_id) " + 
						"LEFT JOIN instances i ON(i.instance_connector = c.connector_id) " +
						"WHERE 1=1 " + where + " GROUP BY c.connector_id ORDER BY " + order + " DESC " + limit);
					
					List<Map<String, Object>> results = new Vector<Map<String, Object>>();
					for( Map<String, String> d : data )
					{
						Map<String, Object> c = new HashMap<String, Object>();
						c.putAll(d);
							
						Map<String, String> users = new HashMap<String, String>();
						users.put("count", (String)d.get("users"));
						c.remove("users");
						c.put("users", users);
						
						c.put("connector_use_price_ttc", "" + (Double.parseDouble(d.get("connector_use_price")) + Math.ceil(vat / 100.0 * Double.parseDouble(d.get("connector_use_price"))) + Double.parseDouble(d.get("connector_use_tax"))));
						c.put("connector_buy_price_ttc", "" + (Double.parseDouble(d.get("connector_buy_price")) + Math.ceil(vat / 100.0 * Double.parseDouble(d.get("connector_buy_price"))) + Double.parseDouble(d.get("connector_buy_tax"))));
						
						results.add(c);
					}
					
					return results;
				}
				//else if( keyword == null || keyword.length() < 3 )
				//	throw new Exception("Search keyword is mandatory");
				
				if( keyword != null && keyword.length() > 0 )
					where += " AND t.translation_text LIKE '%" + Security.escape(keyword) + "%'";

				for( int attempt = 0; attempt < 3; attempt++ )
				{
					// FIRST attempt : with provided language
					if( attempt == 0 )
						language = " AND ct.translation_language = '" + language + "'";
					// SECOND attempt : with EN
					else if( attempt == 1 )
						language = " AND ct.translation_language = 'EN'";
					// THIRD attempt : any (including null)
					else
						language = "";
					
					String sql = "SELECT c.connector_id, c.connector_user, c.connector_direction, c.connector_name as connector_internal_name, c.connector_date, c.connector_instances, " + 
						"c.connector_category, c.connector_use_price, c.connector_use_tax, c.connector_buy_price, c.connector_buy_tax, c.connector_user_status, c.connector_intern_status, c.connector_rating, " + 
						"c.connector_language, u.user_name, u.user_firstname, u.user_lastname, t.translation_id, t.translation_text, ct.translation_language, " +
						"AVG(ur.rating_value) AS value, COUNT(DISTINCT ur.user_id) AS count, " + 
						"COUNT(DISTINCT CASE WHEN ur.rating_value = 1 THEN ur.user_id END) as count1, " +
						"COUNT(DISTINCT CASE WHEN ur.rating_value = 2 THEN ur.user_id END) as count2, " +
						"COUNT(DISTINCT CASE WHEN ur.rating_value = 3 THEN ur.user_id END) as count3, " +
						"COUNT(DISTINCT CASE WHEN ur.rating_value = 4 THEN ur.user_id END) as count4, " +
						"COUNT(DISTINCT CASE WHEN ur.rating_value = 5 THEN ur.user_id END) as count5, " +
						"COUNT(DISTINCT i.instance_user) as users, " + 
						"GROUP_CONCAT(DISTINCT f.file_id) as file_id, GROUP_CONCAT(DISTINCT f.file_date) as file_date, " +
						"GROUP_CONCAT(DISTINCT tg.tag_id) as tag_id, GROUP_CONCAT(DISTINCT tg.tag_name) as tag_name " +
						"FROM connectors c " + 
						"LEFT JOIN users u ON(c.connector_user = u.user_id) " + 
						"LEFT JOIN connector_tag cta ON(c.connector_id = cta.connector_id) " + 
						"LEFT JOIN tags tg ON(cta.tag_id = tg.tag_id) " + 
						"LEFT JOIN connector_translation ct ON(ct.connector_id = c.connector_id) " + 
						"LEFT JOIN user_rating ur ON(ur.connector_id = c.connector_id) " +
						"LEFT JOIN translations t ON(t.translation_id = ct.translation_id) " + 
						"LEFT JOIN instances i ON(i.instance_connector = c.connector_id) " +
						"LEFT JOIN connector_file f ON(f.connector_id = c.connector_id) " +
						"WHERE 1=1 " + where + language + " GROUP BY c.connector_id ORDER BY " + order + " DESC " + limit;
					Vector<Map<String, String>> data = Database.getInstance().select(sql);

					if( data != null && data.size() > 0 )
					{
						// if the language is null, try with another language (except at the last attempt (=2))
						if( attempt < 2 && data.get(0).get("translation_id") == null )
							continue;
						
						// format results
						List<Map<String, Object>> results = new Vector<Map<String, Object>>();
						for( Map<String, String> r : data )
						{
							Map<String, Object> c = new HashMap<String, Object>();
							c.putAll(r);
							
							c.put("connector_use_price_ttc", "" + (Double.parseDouble(r.get("connector_use_price")) + Math.ceil(vat / 100.0 * Double.parseDouble(r.get("connector_use_price"))) + Double.parseDouble(r.get("connector_use_tax"))));
							c.put("connector_buy_price_ttc", "" + (Double.parseDouble(r.get("connector_buy_price")) + Math.ceil(vat / 100.0 * Double.parseDouble(r.get("connector_buy_price"))) + Double.parseDouble(r.get("connector_buy_tax"))));
							
							if( r.get("translation_id") != null && r.get("translation_text") != null )
								c.putAll(Json.decode(r.get("translation_text")).map());
							c.remove("translation_text");
							
							List<Map<String, String>> files = new Vector<Map<String, String>>();
							if( c.get("file_id") != null )
							{
								String[] file_id = ((String)c.get("file_id")).split(",");
								c.remove("file_id");
								String[] file_date = ((String)c.get("file_date")).split(",");
								c.remove("file_date");
								
								for( int i = 0; i < file_id.length; i++ )
								{
									Map<String, String> f = new HashMap<String, String>();
									f.put("file_id", file_id[i]);
									f.put("file_date", file_date[i]);
									files.add(f);
								}
							}
							c.put("files", files);
							
							List<Map<String, String>> tags = new Vector<Map<String, String>>();
							if( c.get("tag_id") != null )
							{
								String[] tag_id = ((String)c.get("tag_id")).split(",");
								c.remove("tag_id");
								String[] tag_name = ((String)c.get("tag_name")).split(",");
								c.remove("tag_name");
								
								for( int i = 0; i < tag_id.length; i++ )
								{
									Map<String, String> t = new HashMap<String, String>();
									t.put("tag_id", tag_id[i]);
									t.put("tag_name", tag_name[i]);
									tags.add(t);
								}
							}
							c.put("tags", tags);
							
							Map<String, String> rating = new HashMap<String, String>();
							rating.put("value", (String)c.get("value"));
							c.remove("value");
							rating.put("count", (String)c.get("count"));
							c.remove("count");
							rating.put("count1", (String)c.get("count1"));
							c.remove("count1");
							rating.put("count2", (String)c.get("count2"));
							c.remove("count2");
							rating.put("count3", (String)c.get("count3"));
							c.remove("count3");
							rating.put("count4", (String)c.get("count4"));
							c.remove("count4");
							rating.put("count5", (String)c.get("count5"));
							c.remove("count5");
							c.put("rating", rating);
					
							Map<String, String> users = new HashMap<String, String>();
							users.put("count", (String)c.get("users"));
							c.remove("users");
							c.put("users", users);
							
							Map<String, String> interfacesTable = new HashMap<String, String>();
							
							Any t2 = Any.wrap(Database.getInstance().select("SELECT ci.interface_key, ci.interface_tag, ci.interface_type, ci.interface_dynamic, ci.interface_cron, ci.interface_cron_timer, ci.interface_hidden, ci.interface_wkt, " + 
									"t.translation_id, t.translation_text " + 
									"FROM connector_interface ci " + 
									"LEFT JOIN interface_translation ct ON(ct.connector_id = ci.connector_id AND ct.interface_key = ci.interface_key) " + 
									"LEFT JOIN translations t ON(t.translation_id = ct.translation_id) " + 
									"WHERE ci.connector_id = " + r.get("connector_id") + interface_tag + language));
	
							if( t2 != null && t2.size() > 0 && !t2.get(0).get("translation_id").isNull() )
							{
								// format results
								Any interfaces = Any.empty();
								for( Any r2 : t2 )
								{
									Any text = Json.decode(r2.<String>value("translation_text"));
									
									if( text.isEmpty() )
									{
										text.put("name", null); text.put("description", null); 
										text.put("help", null); text.put("input", null);
									}
								
									r2.remove("translation_text");
									r2.remove("translation_id");
									text.putAll(r2);
									interfaces.put(text.<String>value("interface_key"), text);
								}
					
								interfacesTable = (Map<String, String>)interfaces.unwrap();
								c.put("interfaces", interfacesTable);
							}								
													
							Any t3 = Any.wrap(Database.getInstance().select("SELECT cc.config_key, cc.config_value, cc.config_hidden, t.translation_id, t.translation_text " + 
								"FROM connector_config cc " + 
								"LEFT JOIN config_translation ct ON(ct.connector_id = cc.connector_id AND ct.config_key = cc.config_key) " + 
								"LEFT JOIN translations t ON(t.translation_id = ct.translation_id) " + 
								"WHERE cc.connector_id = " + r.get("connector_id") + language));
					
							if( t3 != null && t3.size() > 0 && !t3.get(0).get("translation_id").isNull() )
							{
								// format results
								Any configs = Any.empty();
								for( Any conf : t3 )
								{
									if( !conf.get("translation_id").isNull() && !conf.get("translation_text").isNull() )
										conf.putAll(Json.decode(conf.<String>value("translation_text")));
									conf.remove("translation_text");
							
									// caution : decrypt the config value
									if( !conf.get("config_value").isNull() )
										conf.put("config_value", CryptoSimple.unserialize(conf.<String>value("config_value")));
								}

								c.put("configs", t3);
							}

							if( filter == true )
							{
								if( c.get("interfaces") != null )
									results.add(c);
							}
							else
								results.add(c);							
						}

						return results;
					}
				}
				
				// THIS SHOULD NEVER HAPPEN...
				// ==== OPTION 1 : return something anyways
				//Request.clearParam(new String[]{ "lang", "translation_language", "language" });
				//return execute();
				// ==== OPTION 2 : return an empty list
				return new Vector<Map<String, Object>>();
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves information about a connector. Multiple search values for a single property will be OR'd and different criteria will be AND'd.";
		select.returnDescription = "The matching connector [{'name', 'id'},...]";
		select.addGrant(new String[] { "access", "connector_select" });
		
		Parameter connector = new Parameter();
		connector.isOptional = true;
		connector.minLength = 1;
		connector.maxLength = 30;
		connector.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		connector.allowInUrl = true;
		connector.isMultipleValues = true;
		connector.description = "The connector name or id (will match *[connector]* if not a number or an exact connector id match if numeric)";
		connector.addAlias(new String[]{ "name", "connector_name", "id", "cid", "connector_id", "connector", "names", "connector_names", "connectors", "ids", "cids", "connector_ids" });
		select.addParameter(connector);

		Parameter category = new Parameter();
		category.isOptional = true;
		category.minLength = 1;
		category.maxLength = 11;
		category.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		category.description = "The connector category";
		category.addAlias(new String[]{ "category", "category_id", "connector_category" });
		select.addParameter(category);
		
		Parameter keyword = new Parameter();
		keyword.isOptional = true;
		keyword.minLength = 2;
		keyword.maxLength = 200;
		keyword.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		keyword.description = "Search with a keyword (on name & description).";
		keyword.addAlias(new String[]{ "search", "keyword", "key" });
		select.addParameter(keyword);
		
		Parameter start = new Parameter();
		start.isOptional = true;
		start.minLength = 1;
		start.maxLength = 11;
		start.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		start.description = "Start parameter for limits (SQL like: LIMIT start,limit).";
		start.addAlias(new String[]{ "start", "limit_start" });
		select.addParameter(start);

		Parameter limit = new Parameter();
		limit.isOptional = true;
		limit.minLength = 1;
		limit.maxLength = 11;
		limit.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		limit.description = "Start parameter for limits (SQL like: LIMIT start,limit).";
		limit.addAlias(new String[]{ "limit" });
		select.addParameter(limit);		

		Parameter status = new Parameter();
		status.isOptional = true;
		status.minLength = 1;
		status.maxLength = 3;
		status.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER | PatternBuilder.LOWER);
		status.description = "Filter status.";
		status.addAlias(new String[]{ "status", "connector_status" });
		select.addParameter(status);		

		Parameter ustatus = new Parameter();
		ustatus.isOptional = true;
		ustatus.minLength = 1;
		ustatus.maxLength = 3;
		ustatus.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER | PatternBuilder.LOWER);
		ustatus.description = "Filter status.";
		ustatus.addAlias(new String[]{ "ustatus", "connector_ustatus" });
		select.addParameter(ustatus);	
		
		Parameter language = new Parameter();
		language.isOptional = true;
		language.minLength = 2;
		language.maxLength = 3;
		language.mustMatch = PatternBuilder.getRegex(PatternBuilder.UPPER);
		language.description = "Translation language.";
		language.addAlias(new String[]{ "lang", "translation_language", "language" });
		select.addParameter(language);

		Parameter order = new Parameter();
		order.isOptional = true;
		order.minLength = 1;
		order.maxLength = 30;
		order.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		order.description = "Order by?";
		order.addAlias(new String[]{ "order", "order_by" });
		select.addParameter(order);
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		select.addParameter(user);
		
		Parameter count = new Parameter();
		count.isOptional = true;
		count.minLength = 1;
		count.maxLength = 10;
		count.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		count.description = "Count connectors?";
		count.addAlias(new String[]{ "count" });
		select.addParameter(count);
		
		Parameter extended = new Parameter();
		extended.isOptional = true;
		extended.minLength = 1;
		extended.maxLength = 10;
		extended.mustMatch = "^(?i)(yes|true|1|no|false|0)$";
		extended.description = "Count connectors?";
		extended.addAlias(new String[]{ "extended" });
		select.addParameter(extended);
		
		Parameter tag = new Parameter();
		tag.isOptional = true;
		tag.minLength = 1;
		tag.maxLength = 30;
		tag.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		tag.isMultipleValues = true;
		tag.description = "The tag name(s) or id(s)";
		tag.addAlias(new String[]{ "tag", "tag_name", "tid", "tag_id", "tag_names", "tags", "tids", "tag_ids" });
		select.addParameter(tag);
		
		Parameter interface_tag = new Parameter();
		interface_tag.isOptional = true;
		interface_tag.minLength = 1;
		interface_tag.maxLength = 50;
		interface_tag.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER | PatternBuilder.LOWER);
		interface_tag.description = "Filter tag on interface.";
		interface_tag.addAlias(new String[]{ "interface_tag", "interface" });
		select.addParameter(interface_tag);	
		
		index.addOwnHandler(select);		
	}
	
	private void initializeSelectIndex(Index index)
	{
		Action selectIndex = new Action()
		{
			public Object execute() throws Exception
			{
				String keyword = getParameter("keyword").getValue();
				String language = getParameter("language").getValue();
				
				String where = " ";
				if( keyword != null )
					where += " AND t.translation_text LIKE '%" + Security.escape(keyword) + "%'";
				
				Vector<Map<String, String>> data = Database.getInstance().select("SELECT c.connector_id, t.translation_id, t.translation_text, ct.translation_language " +
				"FROM connectors c " + 
				"LEFT JOIN connector_translation ct ON(ct.connector_id = c.connector_id) " + 
				"LEFT JOIN translations t ON(t.translation_id = ct.translation_id) " + 
				"WHERE c.connector_intern_status > 1 AND c.connector_user_status > 1 AND ct.translation_language = '" + language + "' " + where + " GROUP BY c.connector_id");
				
				if( data != null && data.size() > 0 && data.get(0).get("translation_id") != null )
				{
					// format results
					List<Map<String, Object>> results = new Vector<Map<String, Object>>();
					for( Map<String, String> r : data )
					{
						Map<String, Object> c = new HashMap<String, Object>();
						c.putAll(r);
						
						if( r.get("translation_id") != null && r.get("translation_text") != null )
							c.putAll(Json.decode(r.get("translation_text")).map());
						c.remove("translation_text");
						
						results.add(c);
					}
						
					return results;
				}
				
				// THIS SHOULD NEVER HAPPEN...
				// remove the language and recurse
				Request.clearParam(new String[]{ "lang", "translation_language", "language" });
				return execute();
				//return new Vector<Map<String, Object>>();
			}
		};
		
		selectIndex.addMapping(new String[] { "selectindex", "index" });
		selectIndex.description = "Retrieves simple information index";
		selectIndex.returnDescription = "The matching connector [{'name', 'id'},...]";
		selectIndex.addGrant(new String[] { "access", "connector_select" });
		
		Parameter keyword = new Parameter();
		keyword.isOptional = true;
		keyword.minLength = 3;
		keyword.maxLength = 200;
		keyword.mustMatch = PatternBuilder.getRegex(PatternBuilder.PHRASE);
		keyword.description = "Search with a keyword (on name & description).";
		keyword.addAlias(new String[]{ "search", "keyword", "key" });
		selectIndex.addParameter(keyword);
		
		Parameter language = new Parameter();
		language.isOptional = false;
		language.minLength = 2;
		language.maxLength = 3;
		language.mustMatch = PatternBuilder.getRegex(PatternBuilder.UPPER);
		language.description = "Translation language.";
		language.addAlias(new String[]{ "lang", "translation_language", "language" });
		selectIndex.addParameter(language);
		
		index.addOwnHandler(selectIndex);		
	}

	private void initializeUpload(Index index)
	{
		Action upload = new Action()
		{
			public Object execute() throws Exception
			{
				String connector = getParameter("connector").getValue();
				String user = getParameter("user").getValue();
				
				if( !Request.hasAttachment() )
					throw new Exception("Missing attachment");
				
				Collection<String> names = Request.getAttachmentNames();
				if( names.size() > 1 )
					throw new Exception("Only one attachment is allowed");
				
				InputStream attachment = null;
				for( String name : names )
				{
					if( !name.endsWith(".jar") && !name.endsWith(".php") )
						throw new Exception("The attachment must be a JAR or PHP file");
					attachment = Request.getAttachment(name);
				}

				if( !connector.matches("^[0-9]+$") )
					connector = "(SELECT connector_id FROM connectors WHERE connector_name = '" + Security.escape(connector) + "')";
				
				if( !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
				
				Database.getInstance().insert("INSERT INTO connector_file (connector_id, file_date, file_content) " + 
					"SELECT c.connector_id, UNIX_TIMESTAMP(), '" + new Base64InputStream(attachment, false).toString() + "' FROM connectors c " + 
					"LEFT JOIN users u ON(u.user_id = c.connector_user) " +
					"WHERE u.user_id = " + user + " AND c.connector_id = " + connector);
					
				return "OK";
			}
		};
		
		upload.addMapping(new String[] { "upload" });
		upload.description = "Upload the connector code. The code should be a valid JAR file uploaded as attachment";
		upload.returnDescription = "Ok";
		upload.addGrant(new String[] { "access", "connector_update" });
		
		Parameter connector = new Parameter();
		connector.isOptional = false;
		connector.minLength = 1;
		connector.maxLength = 30;
		connector.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		connector.allowInUrl = true;
		connector.description = "The connector name or id";
		connector.addAlias(new String[]{ "connector", "connector_name", "connector_id", "cid" });
		upload.addParameter(connector);

		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		upload.addParameter(user);
		
		index.addOwnHandler(upload);
	}
	
	private void initializeDownload(Index index)
	{
		Action download = new Action()
		{
			public Object execute() throws Exception
			{
				String connector = getParameter("connector").getValue();
				String user = getParameter("user").getValue();
				String active = getParameter("active").getValue();
				String file = getParameter("file").getValue();
				String encoded = getParameter("encoded").getValue();

				if( !connector.matches("^[0-9]+$") )
					connector = "(SELECT connector_id FROM connectors WHERE connector_name = '" + Security.escape(connector) + "')";
				
				String where = "f.connector_id = " + connector;
				if( active != null && active.matches("^(1|yes|true)$") )
					where += " AND c.connector_user_status > 1 AND c.connector_intern_status > 1 ";
				
				if( file != null )
					where += " AND f.file_id = " + file;
				
				Map<String, String> result;
				if( user == null )
				{
					result = Database.getInstance().selectOne("SELECT f.file_id, f.file_content, c.connector_name FROM connector_file f " +
						"LEFT JOIN connectors c ON(c.connector_id = f.connector_id) " + 
						"WHERE " + where +
						" ORDER BY f.file_date DESC LIMIT 1");
				}
				else
				{
					if( !user.matches("^[0-9]+$") )
						user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
					where += " AND u.user_id = " + user;
					
					result = Database.getInstance().selectOne("SELECT f.file_id, f.file_content, c.connector_name FROM connector_file f " +
						"LEFT JOIN connectors c ON(c.connector_id = f.connector_id) " + 
						"LEFT JOIN users u ON(u.user_id = c.connector_user) " +
						"WHERE " + where +
						" ORDER BY f.file_date DESC LIMIT 1");
				}
					
				if( result == null || result.get("file_id") == null )
					throw new Exception("The target connector/user does not have associated code");
				
				if( encoded != null && encoded.matches("^(1|yes|true)$") )
				{
					Hashtable<String, String> response = new Hashtable<String, String>();
					response.put("name", result.get("connector_name"));
					response.put("code", result.get("file_content"));
					return response;
				}
				else
					return new Base64InputStream(result.get("file_content"), true);
			}
		};
		
		download.addMapping(new String[] { "download" });
		download.description = "Download the connector code";
		download.returnDescription = "The binary code";
		download.addGrant(new String[] { "access", "connector_select" });
		
		Parameter connector = new Parameter();
		connector.isOptional = false;
		connector.minLength = 1;
		connector.maxLength = 30;
		connector.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		connector.allowInUrl = true;
		connector.description = "The connector name or id";
		connector.addAlias(new String[]{ "connector", "connector_name", "id", "connector_id", "cid" });
		download.addParameter(connector);

		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		download.addParameter(user);

		Parameter file = new Parameter();
		file.isOptional = true;
		file.minLength = 1;
		file.maxLength = 30;
		file.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		file.allowInUrl = true;
		file.description = "The file id";
		file.addAlias(new String[]{ "file", "file_id", "fid" });
		download.addParameter(file);
		
		Parameter encoded = new Parameter();
		encoded.isOptional = true;
		encoded.minLength = 1;
		encoded.maxLength = 5;
		encoded.mustMatch = "^(1|0|true|false|yes|no)$";
		encoded.description = "Whether or not to base64 encode the response";
		encoded.addAlias(new String[]{ "encode", "encoded", "base64" });
		download.addParameter(encoded);
		
		Parameter active = new Parameter();
		active.isOptional = true;
		active.minLength = 1;
		active.maxLength = 5;
		active.mustMatch = "^(1|0|true|false|yes|no)$";
		active.description = "Whether or not the connector should be validated for use (default false)";
		active.addAlias(new String[]{ "active", "valid", "online" });
		download.addParameter(active);
		
		index.addOwnHandler(download);
	}
	
	private void initializeSetRate(Index index)
	{
		Action setrate = new Action()
		{
			public Object execute() throws Exception
			{
				String id = getParameter("id").getValue();
				String rating = getParameter("rating").getValue();
				String user = getParameter("user").getValue();
			
				if( !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
					
				Map<String, String> check = Database.getInstance().selectOne("SELECT rating_value FROM user_rating WHERE connector_id = " + Security.escape(id) + " AND user_id = " + user);
				
				if( check != null && check.get("rating_value") != null )
					Database.getInstance().update("UPDATE user_rating SET rating_value = " + Security.escape(rating) + " WHERE connector_id = " + Security.escape(id) + " AND user_id = " + user);
				else
					Database.getInstance().insert("INSERT INTO user_rating (user_id, connector_id, rating_value) VALUES (" + user + ", '" + Security.escape(id) + "', '" + Security.escape(rating) + "')");
			
				Map<String, String> avg = Database.getInstance().selectOne("SELECT AVG(rating_value) AS value FROM user_rating WHERE connector_id = " + Security.escape(id));
				Database.getInstance().update("UPDATE connectors SET connector_rating = '" + avg.get("value") + "' WHERE connector_id = " + Security.escape(id));
			
				return "OK";
			}
		};
		
		setrate.addMapping(new String[] { "setrate" });
		setrate.description = "Rate a connector";
		setrate.returnDescription = "OK";
		setrate.addGrant(new String[] { "access", "instance_insert" });
	
		Parameter id = new Parameter();
		id.isOptional = false;
		id.minLength = 1;
		id.maxLength = 30;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.allowInUrl = true;
		id.description = "The connector id";
		id.addAlias(new String[]{ "connector", "id", "connector_id", "cid" });
		setrate.addParameter(id);
		
		Parameter rating = new Parameter();
		rating.isOptional = false;
		rating.minLength = 1;
		rating.maxLength = 1;
		rating.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		rating.description = "The connector rating";
		rating.addAlias(new String[]{ "rating", "connector_rating" });
		setrate.addParameter(rating);

		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		setrate.addParameter(user);
		
		index.addOwnHandler(setrate);
	}
	
	private void initializeGetRate(Index index)
	{
		Action getrate = new Action()
		{
			public Object execute() throws Exception
			{
				String id = getParameter("id").getValue();
				String user = getParameter("user").getValue();
			
				if( !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
					
				Map<String, String> result = Database.getInstance().selectOne("SELECT rating_value FROM user_rating WHERE connector_id = " + Security.escape(id) + " AND user_id = " + user);
			
				return result;
			}
		};
		
		getrate.addMapping(new String[] { "getrate" });
		getrate.description = "Get the rating of a connector";
		getrate.returnDescription = "The rating {'rating_value'}";
		getrate.addGrant(new String[] { "access", "instance_insert" });
	
		Parameter id = new Parameter();
		id.isOptional = false;
		id.minLength = 1;
		id.maxLength = 30;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.allowInUrl = true;
		id.description = "The connector id";
		id.addAlias(new String[]{ "connector", "id", "connector_id", "cid" });
		getrate.addParameter(id);

		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		getrate.addParameter(user);
		
		index.addOwnHandler(getrate);
	}
	
	private void initializeTrends(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				String connector = getParameter("connector").getValue();
				
				if( connector.matches("^[0-9]+$") )
					connector = "c.connector_id = " + connector;
				else
					connector = "c.connector_name = '" + Security.escape(connector) + "'";
				
				Any result = Any.empty();
				result.put("from", Database.getInstance().select(
					"SELECT cl.connector_name, cl.connector_id, COUNT(cl.connector_id) as hits " + 
					"FROM connectors c " + 
					"LEFT JOIN instances i ON(i.instance_connector = c.connector_id) " +
					"LEFT JOIN links l ON(l.instance_from = i.instance_id) " +
					"LEFT JOIN instances il ON(il.instance_id = l.instance_to) " + 
					"LEFT JOIN connectors cl ON(cl.connector_id = il.instance_connector) " +
					"WHERE cl.connector_id IS NOT NULL AND " + connector + " GROUP BY cl.connector_id ORDER BY hits DESC LIMIT 20"));
				result.put("to", Database.getInstance().select(
					"SELECT cl.connector_name, cl.connector_id, COUNT(cl.connector_id) as hits " + 
					"FROM connectors c " + 
					"LEFT JOIN instances i ON(i.instance_connector = c.connector_id) " +
					"LEFT JOIN links l ON(l.instance_to = i.instance_id) " +
					"LEFT JOIN instances il ON(il.instance_id = l.instance_from) " + 
					"LEFT JOIN connectors cl ON(cl.connector_id = il.instance_connector) " +
					"WHERE cl.connector_id IS NOT NULL AND " + connector + " GROUP BY cl.connector_id ORDER BY hits DESC LIMIT 20"));
				return result;
			}
		};
		
		select.addMapping(new String[] { "usage", "top", "trend", "trending" });
		select.description = "Returns the top 20 most connected connectors to the specified one.";
		select.returnDescription = "The top linked connectors {'from':[{'name', 'id', 'hits'},...], 'to':[...]}";
		select.addGrant(new String[] { "access", "connector_select" });
		
		Parameter connector = new Parameter();
		connector.isOptional = false;
		connector.minLength = 1;
		connector.maxLength = 30;
		connector.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		connector.allowInUrl = true;
		connector.description = "The connector name or id";
		connector.addAlias(new String[]{ "name", "connector_name", "id", "cid", "connector_id", "connector" });
		select.addParameter(connector);

		index.addOwnHandler(select);
	}
	
	private void initializeDeleteFile(Index index)
	{
		Action deletefile = new Action()
		{
			public Object execute() throws Exception
			{
				String id = getParameter("id").getValue();
				String file = getParameter("file").getValue();
				String user = getParameter("user").getValue();

				if( user != null )
				{
					if( !IdentityChecker.getInstance().isUserConnector(user, id) )
						throw new Exception("The current user is not an administrator of the provided connector");
				}
				
				Database.getInstance().delete("DELETE FROM connector_file WHERE connector_id = " + Security.escape(id) + " AND file_id = " + Security.escape(file) );
				
				return "OK";
			}
		};
		
		deletefile.addMapping(new String[] { "deletefile", "deletecode" });
		deletefile.description = "Delete a connector code";
		deletefile.returnDescription = "OK";
		deletefile.addGrant(new String[] { "access", "connector_update" });
		
		Parameter id = new Parameter();
		id.isOptional = false;
		id.minLength = 1;
		id.maxLength = 30;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		id.allowInUrl = true;
		id.description = "The connector id";
		id.addAlias(new String[]{ "connector", "id", "connector_id", "cid" });
		deletefile.addParameter(id);

		Parameter file = new Parameter();
		file.isOptional = false;
		file.minLength = 1;
		file.maxLength = 30;
		file.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		file.allowInUrl = true;
		file.description = "The file id";
		file.addAlias(new String[]{ "file", "file_id", "fid" });
		deletefile.addParameter(file);
		
		Parameter user = new Parameter();
		user.isOptional = true;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		deletefile.addParameter(user);
		
		index.addOwnHandler(deletefile);
	}

	private void initializeImport(Index index)
	{
		Action upload = new Action()
		{
			public Object execute() throws Exception
			{
				String connector = getParameter("connector").getValue();
				String user = getParameter("user").getValue();
				
				if( !Request.hasAttachment() )
					throw new Exception("Missing attachment");
				
				Collection<String> names = Request.getAttachmentNames();
				if( names.size() != 2 )
					throw new Exception("Only one code and one config attachment are allowed");
				
				InputStream config = null;
				InputStream code = null;
				for( String name : names )
				{
					if( name.endsWith(".jar") || name.endsWith(".php") )
						code = Request.getAttachment(name);
					else if( name.endsWith(".json") )
						config = Request.getAttachment(name);
					else
						throw new Exception("Unsupported file type");
				}

				if( !connector.matches("^[0-9]+$") )
					connector = "(SELECT connector_id FROM connectors WHERE connector_name = '" + Security.escape(connector) + "')";
				
				if( !user.matches("^[0-9]+$") )
					user = "(SELECT user_id FROM users where user_name = '" + Security.escape(user) + "')";
				
				Any a = validateJsonConfig(Hex.toString(StreamReader.readAll(config)));
				
				// 1) if there are previous pending (thus not available) releases and no one uses them, then delete
				
				// 2) create the connector
				
				// 3) insert interfaces and configs
				
				// 4) import code
					
				return "OK";
			}
		};
		
		upload.addMapping(new String[] { "import" });
		upload.description = "Import a connector code and config";
		upload.returnDescription = "Ok";
		upload.addGrant(new String[] { "access", "connector_update", "connector_insert" });
		
		Parameter connector = new Parameter();
		connector.isOptional = false;
		connector.minLength = 1;
		connector.maxLength = 30;
		connector.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		connector.allowInUrl = true;
		connector.description = "The connector name or id";
		connector.addAlias(new String[]{ "connector", "connector_name", "connector_id", "cid" });
		upload.addParameter(connector);

		Parameter user = new Parameter();
		user.isOptional = false;
		user.minLength = 1;
		user.maxLength = 30;
		user.mustMatch = PatternBuilder.getRegex(PatternBuilder.LOWER | PatternBuilder.NUMBER | PatternBuilder.PUNCT);
		user.description = "The user login or id.";
		user.addAlias(new String[]{ "user", "user_name", "username", "login", "user_id", "uid" });
		upload.addParameter(user);
		
		index.addOwnHandler(upload);
	}
	
	private Any validateJsonConfig(String jsonString) throws Exception
	{
		Any json = Json.decode(jsonString, true); // all string (true = "true", null = "null")
		
		Any fail = Any.empty();
	
		// check sections
		if( !json.containsKey("general") ) 		fail.add("Missing 'general' section in json file");
		if( !json.containsKey("interfaces") ) 	fail.add("Missing 'interfaces' section in json file");
		if( !json.containsKey("configs") ) 		fail.add("Missing 'configs' section in json file");
		if( !json.containsKey("translation") )	fail.add("Missing 'translation' section in json file");
		if( fail.size() > 0 ) throw new RestException("Invalid json file", RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x1, fail);
		
		// check general
		if( !json.isMap("general") ) 			fail.add("Section 'general' must be an object");
		if( fail.size() > 0 ) throw new RestException("Invalid json file", RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x1, fail);
		Any general = json.get("general");
		if( !general.containsKey("language") ) 	fail.add("Missing 'language' entry in 'general' section");
		if( !general.containsKey("category") ) 	fail.add("Missing 'category' entry in 'general' section");
		if( !general.containsKey("buy_price") ) fail.add("Missing 'buy_price' entry in 'general' section");
		if( !general.containsKey("use_price") ) fail.add("Missing 'use_price' entry in 'general' section");
		if( !general.containsKey("small_logo") )fail.add("Missing 'small_logo' entry in 'general' section");
		if( !general.containsKey("big_logo") ) 	fail.add("Missing 'big_logo' entry in 'general' section");
		if( !general.<String>value("language").equals("PHP") )
			fail.add("Entry 'language' in 'general' section must be set to \"PHP\"");
		if( !general.<String>value("category").matches("^(Business|Communication|Entertainment|Health|Home automation|Industry|News|Productivity|Social|Storage|Technology|Utilities)$") ) 
			fail.add("Entry 'category' in 'general' section must be one of 'Business', 'Communication', 'Entertainment', 'Health', 'Home automation', 'Industry', 'News', 'Productivity', 'Social', 'Storage', 'Technology', 'Utilities'");
		if( !general.<String>value("buy_price").matches("^[0-9]+$") || Integer.parseInt(general.<String>value("buy_price")) > 100000 )
			fail.add("Entry 'buy_price' in 'general' section must be numeric and must be between 0 and 100000");
		if( !general.<String>value("use_price").matches("^[0-9]+$") || Integer.parseInt(general.<String>value("use_price")) > 100000 )
			fail.add("Entry 'use_price' in 'general' section must be numeric and must be between 0 and 100000");
		if( fail.size() > 0 ) throw new RestException("Invalid json file", RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x1, fail);
		
		// check interfaces
		if( !json.isList("interfaces") ) 		fail.add("Section 'interfaces' must be an array");
		if( fail.size() > 0 ) throw new RestException("Invalid json file", RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x1, fail);
		List<String> i_keys = new LinkedList<String>();
		for( Any i : json.get("interfaces") )
		{
			if( !i.isMap() ) {					fail.add("Entry in 'interfaces' must be an object"); continue; }
			if( !i.containsKey("key") ) 		fail.add("Missing interface 'key' entry in 'interfaces' section");
			if( !i.containsKey("direction") ) 	fail.add("Missing 'direction' entry for interface " + i.<String>value("key"));
			if( !i.containsKey("pattern") ) 	fail.add("Missing 'pattern' entry for interface " + i.<String>value("key"));
			if( !i.containsKey("custom") ) 		fail.add("Missing 'custom' entry for interface " + i.<String>value("key"));
			if( !i.containsKey("automatic") ) 	fail.add("Missing 'automatic' entry for interface " + i.<String>value("key"));
			if( !i.containsKey("timer") ) 		fail.add("Missing 'timer' entry for interface " + i.<String>value("key"));
			if( !i.containsKey("hidden") ) 		fail.add("Missing 'hidden' entry for interface " + i.<String>value("key"));
			if( !i.containsKey("rule") ) 		fail.add("Missing 'rule' entry for interface " + i.<String>value("key"));
			if( i_keys.contains(i.<String>value("key")) )
				fail.add("Non unique interface key interface " + i.<String>value("key"));
			i_keys.add(i.<String>value("key"));
			if( i.<String>value("key").matches("^[0-9]+$") )
				fail.add("Entry 'key' cannot be numeric for interface " + i.<String>value("key"));
			if( !i.<String>value("direction").matches("^(input|output)$") )
				fail.add("Entry 'direction' must be one of 'input', 'output' for interface " + i.<String>value("key"));
			if( !i.<String>value("pattern").matches("^(producer|consumer|transformer)$") )
				fail.add("Entry 'pattern' must be one of 'producer', 'consumer', 'transformer' for interface " + i.<String>value("key"));
			if( i.<String>value("direction").equals("input") && i.<String>value("pattern").equals("producer") )
				fail.add("Pattern 'producer' is not compatible with direction 'input' for interface " + i.<String>value("key"));
			if( i.<String>value("direction").equals("output") && i.<String>value("pattern").equals("consumer") )
				fail.add("Pattern 'consumer' is not compatible with direction 'output' for interface " + i.<String>value("key"));
			if( !i.<String>value("custom").matches("^(true|false)$") )
				fail.add("Entry 'custom' must be a boolean (true/false) for interface " + i.<String>value("key"));
			if( !i.<String>value("automatic").matches("^(true|false)$") )
				fail.add("Entry 'automatic' must be a boolean (true/false) for interface " + i.<String>value("key"));
			if( i.<String>value("pattern").equals("transformer") && i.<String>value("automatic").equals("true") )
				fail.add("Pattern 'transformer' is not compatible with automatic triggering for interface " + i.<String>value("key"));
			if( !i.<String>value("hidden").matches("^(true|false)$") )
				fail.add("Entry 'hidden' must be a boolean (true/false) for interface " + i.<String>value("key"));
			if( !i.<String>value("timer").equals("null") && !i.<String>value("timer").matches("^(([0-9]{1,4}|E)(\\-|$){5}$") ) 	
				fail.add("Entry 'timer' must be null or a valid time period (see documentation) for interface " + i.<String>value("key"));
			if( !i.<String>value("rule").equals("null") && !i.<String>value("rule").matches("!^(domain|email|ip|phone|url|number|text|upper|lower|/.*/g?m?g?i?g?m?g?)$!") )
				fail.add("Entry 'rule' must be a valid rule (see documentation) for interface " + i.<String>value("key"));
			if( i.<String>value("rule").matches("!^/.*/g?m?g?i?g?m?g?$!") )
			{
				try { Pattern.compile(i.<String>value("rule").replaceAll("!/g?m?g?(i)?g?m?g?$!", "/$1")); }
				catch(PatternSyntaxException pse) { fail.add("Entry 'rule' contains invalid regex pattern for interface " + i.<String>value("key")); }
			}
		}
		if( fail.size() > 0 ) throw new RestException("Invalid json file", RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x1, fail);
		
		// check configs
		if( !json.isList("configs") ) 			fail.add("Section 'configs' must be an array");
		if( fail.size() > 0 ) throw new RestException("Invalid json file", RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x1, fail);
		List<String> c_keys = new LinkedList<String>();
		for( Any c : json.get("configs") )
		{
			if( !c.isMap() ) { 					fail.add("Entry in 'configs' must be an object"); continue; }
			if( !c.containsKey("key") ) 		fail.add("Missing 'key' entry in 'configs' section");
			if( !c.containsKey("hidden") ) 		fail.add("Missing 'hidden' entry for config " + c.<String>value("key"));
			if( !c.containsKey("bindable") ) 	fail.add("Missing 'bindable' entry for config " + c.<String>value("key"));
			if( !c.containsKey("default") ) 	fail.add("Missing 'default' entry for config " + c.<String>value("key"));
			if( !c.containsKey("rule") ) 		fail.add("Missing 'rule' entry for config " + c.<String>value("key"));
			if( c_keys.contains(c.<String>value("key")) )
				fail.add("Non unique config key config " + c.<String>value("key"));
			c_keys.add(c.<String>value("key"));
			if( c.<String>value("key").matches("^[0-9]+$") )
				fail.add("Entry 'key' cannot be numeric for config " + c.<String>value("key"));
			if( !c.<String>value("hidden").matches("^(true|false)$") )
				fail.add("Entry 'hidden' must be a boolean (true/false) for config " + c.<String>value("key"));
			if( !c.<String>value("bindable").matches("^(true|false)$") )
				fail.add("Entry 'bindable' must be a boolean (true/false) for config " + c.<String>value("key"));
			if( !c.<String>value("rule").equals("null") && !c.<String>value("rule").matches("!^(domain|email|ip|phone|url|number|text|upper|lower|/.*/g?m?g?i?g?m?g?)$!") )
				fail.add("Entry 'rule' must be a valid rule (see documentation) for config " + c.<String>value("key"));
			if( c.<String>value("rule").matches("!^/.*/g?m?g?i?g?m?g?$!") )
			{
				try { Pattern.compile(c.<String>value("rule").replaceAll("!/g?m?g?(i)?g?m?g?$!", "/$1")); }
				catch(PatternSyntaxException pse) { fail.add("Entry 'rule' contains invalid regex pattern for config " + c.<String>value("key")); }
			}
		}
		if( fail.size() > 0 ) throw new RestException("Invalid json file", RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x1, fail);
		
		// check translations
		if( !json.isMap("translation") ) 		fail.add("Section 'translation' must be an object");
		if( fail.size() > 0 ) throw new RestException("Invalid json file", RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x1, fail);
		if( !json.get("translation").containsKey("en") ) 
			fail.add("English ('en') translation is mandatory in section 'translation'");
		for( Map.Entry<String, Any> tr : json.get("translation").entrySet() )
		{
			String l = tr.getKey();
			Any t = tr.getValue();
			
			if( !t.isMap() ) { 					fail.add("Entry '" + l + "' in 'translation' must be an object"); continue; }
			if( !l.matches("^[a-z]{2}$") )		fail.add("Invalid translation language (see documentation) : " + l);
			if( !t.containsKey("general") )		fail.add("Missing 'general' section for translation in '" + l + "'");
			if( !t.containsKey("interfaces") )	fail.add("Missing 'interfaces' section for translation in '" + l + "'");
			if( !t.containsKey("configs") )		fail.add("Missing 'configs' section for translation in '" + l + "'");
			if( fail.size() > 0 ) throw new RestException("Invalid json file", RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x1, fail);
		
			// check general translation
			general = t.get("general");
			if( !general.isMap() ) {						fail.add("Entry 'general' for translation in '" + l + "' must be an object"); continue; }
			if( !general.containsKey("name") )				fail.add("Missing 'name' entry for 'general' translation in '" + l + "'");
			if( !general.containsKey("short_description") )	fail.add("Missing 'short_description' entry for 'general' translation in '" + l + "'");
			if( !general.containsKey("long_description") )	fail.add("Missing 'long_description' entry for 'general' translation in '" + l + "'");
			if( !general.containsKey("prevention") )		fail.add("Missing 'prevention' entry for 'general' translation in '" + l + "'");
			if( !general.containsKey("config_url") )		fail.add("Missing 'config_url' entry for 'general' translation in '" + l + "'");
			if( !general.containsKey("panel_url") )			fail.add("Missing 'panel_url' entry for 'general' translation in '" + l + "'");
			if( !general.containsKey("product_url") )		fail.add("Missing 'product_url' entry for 'general' translation in '" + l + "'");
			if( general.<String>value("name").equals("null") || general.<String>value("name").length() == 0 )
				fail.add("The 'name' cannot be null or empty for 'general' translation in '" + l + "'");
			if( general.<String>value("short_description").equals("null") || general.<String>value("short_description").length() == 0 )
				fail.add("The 'short_description' cannot be null or empty for 'general' translation in '" + l + "'");
			if( general.<String>value("long_description").equals("null") || general.<String>value("long_description").length() == 0 )
				fail.add("The 'long_description' cannot be null or empty for 'general' translation in '" + l + "'");
			if( !general.<String>value("config_url").equals("null") && !general.<String>value("config_url").matches("^https?://.*$") )
				fail.add("The 'config_url' must be null or a valid url for 'general' translation in '" + l + "'");
			if( !general.<String>value("panel_url").equals("null") && !general.<String>value("panel_url").matches("^https?://.*$") )
				fail.add("The 'panel_url' must be null or a valid url for 'general' translation in '" + l + "'");
			if( !general.<String>value("product_url").equals("null") && !!general.<String>value("product_url").matches("^https?://.*$") )
				fail.add("The 'product_url' must be null or a valid url for 'general' translation in '" + l + "'");
			if( fail.size() > 0 ) throw new RestException("Invalid json file", RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x1, fail);

			// check interfaces translation
			if( !t.get("interfaces").isMap() ) { 	fail.add("Entry 'interfaces' for translation in '" + l + "' must be an object"); continue; }
			for( String key : i_keys )
				if( !t.get("interfaces").containsKey(key) )
					fail.add("Missing translation for interface '" + key + "' in '" + l + "'");
			for( Map.Entry<String, Any> ki : t.get("interfaces").entrySet() )
			{
				String key = ki.getKey();
				Any i = ki.getValue();
				
				if( !i.isMap() ) { 					fail.add("Entry '" + key + "' for translation of interfaces in '" + l + "' must be an object"); continue; }
				if( !i_keys.contains(key) ) {	 	fail.add("Translation for interface '" + key + "' in '" + l + "' is useless because there is no such interface"); continue; }
				if( !i.containsKey("name") )		fail.add("Missing 'name' entry for translation of interface '" + key + "' in '" + l + "'");
				if( !i.containsKey("description") )	fail.add("Missing 'description' entry for translation of interface '" + key + "' in '" + l + "'");
				if( !i.containsKey("rule") )		fail.add("Missing 'rule' entry for translation of interface '" + key + "' in '" + l + "'");
				if( i.<String>value("name").equals("null") || i.<String>value("name").length() == 0 )
					fail.add("The 'name' cannot be null or empty for translation of interface '" + key + "' in '" + l + "'");
				if( i.<String>value("description").equals("null") || i.<String>value("description").length() == 0 )
					fail.add("The 'description' cannot be null or empty for translation of interface '" + key + "' in '" + l + "'");
				for( Any i2 : json.get("interfaces") ) if( i2.<String>value("key").equals(key) && !i2.<String>value("rule").equals("null") && (i.<String>value("rule").equals("null") || i.<String>value("rule").length() == 0) )
					fail.add("The 'rule' cannot be null or empty for translation of interface '" + key + "' in '" + l + "'");
			}
			if( fail.size() > 0 ) throw new RestException("Invalid json file", RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x1, fail);

			// check configs translation
			if( !t.get("configs").isMap() ) { 		fail.add("Entry 'configs' for translation in '" + l + "' must be an object"); continue; }
			for( String key : c_keys )
				if( !t.get("configs").containsKey(key) )
					fail.add("Missing translation for config '" + key + "' in '" + l + "'");
			for( Map.Entry<String, Any> kc : t.get("configs").entrySet() )
			{
				String key = kc.getKey();
				Any c = kc.getValue();
				
				if( !c.isMap() ) { 					fail.add("Entry '" + key + "' for translation of configs in '" + l + "' must be an object"); continue; }
				if( !c_keys.contains(key) ) { 		fail.add("Translation for config '" + key + "' in '" + l + "' is useless because there is no such config"); continue; }
				if( !c.containsKey("name") )		fail.add("Missing 'name' entry for translation of config '" + key + "' in '" + l + "'");
				if( !c.containsKey("description") )	fail.add("Missing 'description' entry for translation of config '" + key + "' in '" + l + "'");
				if( !c.containsKey("rule") )		fail.add("Missing 'rule' entry for translation of config '" + key + "' in '" + l + "'");
				if( c.<String>value("name").equals("null") || c.<String>value("name").length() == 0 )
					fail.add("The 'name' cannot be null or empty for translation of config '" + key + "' in '" + l + "'");
				if( c.<String>value("description").equals("null") || c.<String>value("description").length() == 0 )
					fail.add("The 'description' cannot be null or empty for translation of config '" + key + "' in '" + l + "'");
				for( Any c2 : json.get("configs") ) if( c2.<String>value("key").equals(key) && !c2.<String>value("rule").equals("null") && (c.<String>value("rule").equals("null") || c.<String>value("rule").length() == 0) )
					fail.add("The 'rule' cannot be null or empty for translation of config '" + key + "' in '" + l + "'");
			}
			if( fail.size() > 0 ) throw new RestException("Invalid json file", RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x1, fail);
		}
		if( fail.size() > 0 ) throw new RestException("Invalid json file", RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x1, fail);
		
		return json;
	}
}
