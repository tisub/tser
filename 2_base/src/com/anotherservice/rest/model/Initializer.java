package com.anotherservice.rest.model;

import com.anotherservice.db.*;
import com.anotherservice.util.*;
import com.anotherservice.rest.*;
import com.anotherservice.rest.core.*;
import java.io.InputStream;
import java.util.*;
import com.anotherservice.rest.formatter.*;

public class Initializer extends InitializerOnce
{
	static Database db = null;
	static String path = null;
	
	public void initialize()
	{
		try
		{
			// load the config
			InputStream config = getResourceAsStream("config.xml");
			if( config != null )
			{
				Config.load(config);
				config.close();
			}
			
			// set the formatters
			Responder.register(new HtmlFormatter(), new String[]{"html", "h"});
			Responder.register(new JsonFormatter(), new String[]{"json", "pjson", "jsonp", "j", "jp"}, true);
			Responder.register(new XmlFormatter(), new String[]{"xml", "x"});
			
			path = Config.gets("com.anotherservice.rest.model.path");
			if( path == null ) path = "";
			if( path.length() > 0 && path.charAt(0) != '/' ) path = "/" + path;
			
			// connect to the database
			String db_host = Config.gets("com.anotherservice.rest.model.db_host");
			String db_port = Config.gets("com.anotherservice.rest.model.db_port");
			String db_name = Config.gets("com.anotherservice.rest.model.db_name");
			String db_user = Config.gets("com.anotherservice.rest.model.db_user");
			String db_pass = Config.gets("com.anotherservice.rest.model.db_pass");
			String db_max = Config.gets("com.anotherservice.rest.model.db_max");
			String db_min = Config.gets("com.anotherservice.rest.model.db_min");
			String db_shared = Config.gets("com.anotherservice.rest.model.db_shared");
			
			if( db_host != null )
			{
				Class.forName("com.mysql.jdbc.Driver");
				db = new PooledDatabase(new Mysql(db_host, db_port, db_name, db_user, db_pass));
				
				if( db_max != null )
					((PooledDatabase)db).maxActiveConnections = Integer.parseInt(db_max);
				if( db_min != null )
					((PooledDatabase)db).minActiveConnections = Integer.parseInt(db_min);
				
				if( db_shared != null && db_shared.matches("^(?i)(yes|true|1)$") )
					Database.setInstance(db);
			}
			
			// set the security
			String use_security = Config.gets("com.anotherservice.rest.model.use_security");
			if( use_security != null && use_security.matches("^(?i)(yes|true|1)$") )
			{
				com.anotherservice.rest.security.Security.setInstance(new SecurityImpl());
			}
			
			// add a pass through to the schema but protect it
			Index index = new Index();
			index.addMapping(new String[] { "schema" });
			index.description = "Generated database actions";
			index.addGrant(new String[]{ "access", "generated_schema" });
			Handler.addHandler(Initializer.path + "/", index);
		
			Schema s = Schema.discover(Database.getInstance(), Config.gets("com.anotherservice.rest.model.db_name"));
			Schema.setInstance(s);
			Generator.generate(s, index);
			
			// initialize sub-components
			new User().initialize();
			
			new Token().initialize();
			
			new Group().initialize();
			new GroupUser().initialize();
			
			new Grant().initialize();
			new GrantUser().initialize();
			new GrantGroup().initialize();
			new GrantToken().initialize();
			
			new Quota().initialize();
			new QuotaUser().initialize();
			
			new Confirm().initialize();
			
			// self at last in order to reference previous actions
			new Self().initialize();
		}
		catch(Exception e)
		{
			Logger.severe(e);
		}
	}
}