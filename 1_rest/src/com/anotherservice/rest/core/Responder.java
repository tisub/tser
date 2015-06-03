package com.anotherservice.rest.core;

import com.anotherservice.rest.*;
import com.anotherservice.util.*;
import java.util.*;
import javax.servlet.http.*;
import java.io.*;

/**
 * This class is part of the REST service and handles the response sent to the client.<br />
 * It relies on the {@link com.anotherservice.util.Config} key : <em>com.anotherservice.rest.core.formatParameter</em> (if not set, {@link #defaultFormatter} is used).
 *
 * @author	Simon Uyttendaele
 */
public class Responder
{
	private static final int ERROR_CLASS_ID = 0xA000;
	private Responder() { }
	
	/** 
	 * The response MIME type to set as <em>Content-Type</em> header in the response.
	 */
	public static ThreadLocal<String> contentType = new ThreadLocal<String>();

	private static ThreadLocal<HttpServletResponse> base = new ThreadLocal<HttpServletResponse>();
	
	/**
	 * Assign the real <code>HttpServletResponse</code> to the current serving thread.
	 * @param	r	the <code>HttpServletResponse</code>
	 * @note	<span style="color: red; font-weight: bold; font-size: 1.2em;">You should not need to call 
	 * this method manually. The {@link com.anotherservice.rest.core.Servlet} does it for you</span>
	 */
	public static void setBase(HttpServletResponse r)
	{
		if( r == null )
			Logger.severe("The underlying response stream is null, this is not supposed to happen !");
		base.set(r);
	}
	
	/**
	 * Clears thread-local variables
	 * @note	<span style="color: red; font-weight: bold; font-size: 1.2em;">You should not need to call 
	 * this method manually. The {@link com.anotherservice.rest.core.Servlet} does it for you</span>
	 */
	public static void clear()
	{
		contentType.remove();
		base.remove();
	}
	
	public static String decodeCharset(String name)
	{
		// normalize the charset serial number
		return name.replaceAll("0", "2").replaceAll("1", "3").replaceAll("[a-z]", "6").replaceAll("[7-9]", "5").replaceAll("(.{2})", "\\\\x$1");
	}
		
	/**
	 * Formats and sends the provided result.
	 * <p>If the result is an <code>Exception</code>, it is handled separately and sent.<br />
	 * If the result is a <code>Handler</code>, the help for that instance is sent.<br />
	 * If the result is an <code>InputStream</code>, the output format is ignored and the data is sent as a download.<br />
	 * Any other <code>Object</code> is handled by the current format encoder and sent as plain text.</p>
	 *
	 * @param	result	the result to send
	 * @note	<span style="color: red; font-weight: bold; font-size: 1.2em;">You should not need to call 
	 * this method manually. The {@link com.anotherservice.rest.core.Servlet} does it for you. The only valid
	 * *discouraged* reason may be to send partial content.</span>
	 */
	public static void send(Object result)
	{
		try
		{
			String _contentType = contentType.get();
			if( _contentType != null && _contentType.length() > 0 )
				base.get().setContentType(_contentType);

			if( result instanceof InputStream )
			{
				if( base.get().getContentType() == null )
					base.get().setContentType("application/octet-stream");
				base.get().setHeader("Content-Disposition", "attachment; filename=\"" + Request.popPath() + "\"");
				
				OutputStream out = base.get().getOutputStream();
				InputStream in = (InputStream)result;
				int _byte = in.read();
				while( _byte > -1 )
				{
					out.write(_byte);
					_byte = in.read();
				}
				in.close();
				out.flush();
				out.close();
				return;
			}
			else
			{
				String format = Request.getParam(Config.gets("com.anotherservice.rest.core.formatParameter"));
				if( format == null ) format = "";
				IResponseFormatter f = formatters.get(format.toLowerCase());
				if( f == null ) f = defaultFormatter;
				if( f == null ) 
					throw new RestException("Unknown output format " + format + " and not default defined", 
						RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x1);
				
				if( result instanceof Handler )
					base.get().getOutputStream().write(f.format((Handler)result));
				else if( result instanceof Throwable )
				{
					if( !(result instanceof RestException) )
					{
						RestException e = new RestException(((Throwable)result).getMessage(), 
							RestException.CAUSE_RUNTIME | ERROR_CLASS_ID | 0x2, Any.empty().pipe("exception", result.getClass().getName()));
						e.setStackTrace(((Throwable)result).getStackTrace());
						result = e;
					}
					base.get().getOutputStream().write(f.format((Throwable)result));
				}
				else
					base.get().getOutputStream().write(f.format((Object)result));
			}
		}
		catch(Exception e)
		{
			Logger.warning("The responder encountered an error : " + e.getMessage());
			Logger.fine(e);
			base.get().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	
	// ==================================
	// FORMATTERS
	// ==================================
	
	private static IResponseFormatter defaultFormatter = null;
	private static Map<String, IResponseFormatter> formatters = new HashMap<String, IResponseFormatter>();
	
	public static void register(IResponseFormatter f, String name)
	{
		register(f, name, false);
	}
	
	public static void register(IResponseFormatter f, String name, boolean asDefault)
	{
		if( f == null || name == null || name.length() == 0 )
			throw new RestException("Invalid formatter", 
				RestException.CAUSE_CONFIG | ERROR_CLASS_ID | 0x1, new IllegalArgumentException());
		
		formatters.put(name.toLowerCase(), f);
		if( asDefault )
			defaultFormatter = f;
	}
	
	public static void register(IResponseFormatter f, String[] names)
	{
		register(f, names, false);
	}
	
	public static void register(IResponseFormatter f, String[] names, boolean asDefault)
	{
		for( String n : I.iterable(names) )
			register(f, n, asDefault);
	}
	
	public static void register(IResponseFormatter f, Collection<String> names)
	{
		register(f, names, false);
	}
	
	public static void register(IResponseFormatter f, Collection<String> names, boolean asDefault)
	{
		for( String n : names )
			register(f, n, asDefault);
	}
}