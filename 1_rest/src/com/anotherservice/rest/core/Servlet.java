package com.anotherservice.rest.core;

import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.lang.*;
import com.anotherservice.util.*;
import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import java.util.regex.*;
import java.lang.reflect.*;
import java.security.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import javax.servlet.annotation.*;

/**
 * This is the main REST service <code>Servlet</code>.
 * It initializes the {@link com.anotherservice.rest.core.ServiceMonitor} at start, then it uses the {@link com.anotherservice.rest.core.Request},
 * {@link com.anotherservice.rest.core.Responder} and {@link com.anotherservice.rest.core.Router} to handle requests.
 *
 * @note	Servlet configuration parameters :<ul><li>logLevel : sets {@link com.anotherservice.util.Logger.Log#logLevel}</li>
 * <li>logStdout : if <code>true</code>, sets {@link com.anotherservice.util.Logger.Log#stream} to <code>System.out</code></li>
 * <li>servicesDirectory : sets {@link com.anotherservice.rest.core.ServiceMonitor#setDirectory(String)}</li>
 * <li>serviceInitializerSection : sets {@link com.anotherservice.rest.core.ServiceMonitor#setInitializerSection(String)}</li>
 * <li>serviceConfigFileName : sets {@link com.anotherservice.rest.core.ServiceMonitor#setConfigFileName(String)}</li></ul><br />
 * <span style="color: red; font-weight: bold; font-size: 1.2em;">Important : </span> all servlet configuration parameters 
 * are added to the {@link com.anotherservice.util.Config} under the key
 * <em>"com.anotherservice.rest.core"</em>.<br /> Thus, it may be an apropriate time for setting various REST service parameters for the
 * {@link com.anotherservice.rest.core.Router} and the {@link com.anotherservice.rest.core.Responder}.
 * @author  Simon Uyttendaele
 */
/*@MultipartConfig*/
public final class Servlet extends HttpServlet
{
	private static final int ERROR_CLASS_ID = 0x7000;
	
	private long rid = 0;
	private synchronized long getRid() { return rid++; }
	
	private ServiceMonitor monitor = new ServiceMonitor();
	
	// ========================================================================
	//
	// HttpServlet overrides 
	//
	// ========================================================================
	
	public void init() throws ServletException
	{
		try
		{
			// === Get the configuration from web.xml
			for( String key : I.iterable(this.getServletConfig().getInitParameterNames()) )
				Config.set("com.anotherservice.rest.core." + key, this.getServletConfig().getInitParameter(key));
			Config.set("com.anotherservice.rest.core.servletRoot", this.getServletContext().getContextPath());
			
			// in case we are at the root, strip the leading slash
			if( Config.get("com.anotherservice.rest.core.servletRoot").equals("/") )
				Config.set("com.anotherservice.rest.core.servletRoot", "");
			
			// === Initialize the Logger
			Logger.async(true);
			if( Config.gets("com.anotherservice.rest.core.logLevel") != null )
				Logger.instance().level = Level.parse(Config.gets("com.anotherservice.rest.core.logLevel")).intValue();
			if( Config.gets("com.anotherservice.rest.core.logStdout") != null && Config.gets("com.anotherservice.rest.core.logStdout").equalsIgnoreCase("true") )
				Logger.instance().stream = System.out;
			
			Logger.fine("Initializing servlet " + this.getClass().getCanonicalName() + "\n\t" + this.getServletInfo());
			Logger.config("Default charset " + java.nio.charset.Charset.defaultCharset().name());
			
			// === Initialize the ServiceMonitor
			monitor.setDirectory(this.getServletContext().getRealPath(Config.gets("com.anotherservice.rest.core.servicesDirectory")));
			monitor.setInitializerSection(Config.gets("com.anotherservice.rest.core.serviceInitializerSection"));
			monitor.setConfigFileName(Config.gets("com.anotherservice.rest.core.serviceConfigFileName"));
			monitor.start();
			
			Logger.info("Deployed");
		}
		catch(Exception e)
		{
			Logger.severe("Deployment error : " + e.getMessage());
			Logger.fine(e);
			throw new RestException("An error happened while parsing the services configuration file", 
				RestException.NOT_RECOVERABLE | RestException.CAUSE_CONFIG | ERROR_CLASS_ID | 0x1,
				new UnavailableException("An error happened while parsing the services configuration file"));
		}
	}
	
	public void destroy()
	{
		try { monitor.interrupt(); } 
		catch(Exception e)
		{
			Logger.warning("Service Monitor interrupted because of an error : " + e.getMessage());
			Logger.fine(e);
		}
		Logger.info("Undeployed");
	}
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		this.handleRequest(req, resp);
	}
	
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		this.handleRequest(req, resp);
	}
	
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		this.handleRequest(req, resp);
	}
	
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		this.handleRequest(req, resp);
	}
	
	public final String getServletInfo()
	{
		return "Author: Simon Uyttendaele";
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException
	{
		throw new RestException(Servlet.class.getCanonicalName() + " cannot be serialized", 
				RestException.NOT_RECOVERABLE | RestException.CAUSE_RUNTIME | ERROR_CLASS_ID | 0x2,
				new NotSerializableException());
	}

	// ========================================================================
	//
	// Handle the incomming requests
	//
	// ========================================================================
	
	/**
	 * This is a local temporary store location which is valid per request and then cleared.
	 * I.e. use it to share info between handlers.
	 */
	public static ThreadLocal<HashMap<String, Object>> tmp = new ThreadLocal<HashMap<String, Object>>()
	{
		protected HashMap<String, Object> initialValue() { return new HashMap<String, Object>(); }
	};
	
	private void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		long request_id = getRid();
		long start = System.nanoTime();
		try
		{
			Logger.finer("Handling incomming request #" + request_id);

			Request.setBase(req);
			Responder.setBase(resp);
			
			com.anotherservice.rest.security.Filter.getInstance().preFilter();
			
			Object result = null;
			try { result = Router.route(); }
			catch(Exception e) { result = e; }

			result = com.anotherservice.rest.security.Filter.getInstance().postFilter(result);
			Responder.send(result);
		}
		catch(Exception e)
		{
			try{ Responder.send(e); }
			catch(Exception ex)
			{
				Logger.severe("Last resort error handler : " + ex.getMessage() + " caused by " + e.getMessage());
				Logger.finer(ex);
				Logger.fine(e);
				resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}
		finally
		{
			Request.clear();
			Responder.clear();
			tmp.remove();
			Logger.fine("Request #" + request_id + " completed in " + (Math.round(((System.nanoTime() - start)/1000.0))/1000) + "ms");
		}
	}
}
