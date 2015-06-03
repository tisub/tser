package com.busit.rest;

import com.anotherservice.db.*;
import com.anotherservice.util.*;
import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import java.io.InputStream;
import java.util.logging.Level;

public class Initializer extends InitializerOnce
{
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
			
			// set the logger (same as in com.anotherservice.rest.core.Servlet)
			try
			{
				if( Config.gets("com.busit.rest.logLevel") != null )
					Logger.instance().level = Level.parse(Config.gets("com.busit.rest.logLevel")).intValue();
				if( Config.gets("com.busit.rest.logAsync") != null && Config.gets("com.busit.rest.logAsync").matches("(?i)^(yes|1|true)$") )
					Logger.instance().async(true);
			} catch(Exception ex) { Logger.severe(ex); }
			
			// set the filter
			String filterClass = Config.gets("com.busit.rest.filter");
			if( filterClass != null && filterClass.length() > 0 )
				com.anotherservice.rest.security.Filter.setInstance((IFilter) Class.forName(filterClass).newInstance());
			
			// initialize sub-components
			try { new CA().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new Delegate().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			
			try { new Identity().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			
			try { new Organization().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new OrganizationMember().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new Relation().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new Impersonate().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			
			try { new Category().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new Status().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			
			try { new Tag().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new TagObject().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			
			try { new Connector().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new ConnectorError().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new ConnectorStats().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new ConnectorConfig().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new ConnectorInterface().initialize(); } catch(Exception ex) { Logger.severe(ex); }

			try { new Instance().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new InstanceError().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new InstanceStats().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new InstanceConfig().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new InstanceInterface().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new InstanceInterfaceCron().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new InstanceInterfacePublic().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new InstanceInterfaceShared().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			
			try { new Space().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new Flow().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new FlowStats().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new Template().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new Link().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			
			try { new Message().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new Credit().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new Plan().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new Voucher().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			
			try { new Bill().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new News().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			
			try { new Cloud().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			
			try { new UI().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new KnownType().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			try { new Search().initialize(); } catch(Exception ex) { Logger.severe(ex); }
			
			// temporary for data migration
			try { new Migration().initialize(); } catch(Exception ex) { Logger.severe(ex); }
		}
		catch(Exception e)
		{
			Logger.severe(e);
		}
	}
}