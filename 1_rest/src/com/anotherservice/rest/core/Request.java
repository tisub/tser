package com.anotherservice.rest.core;

import com.anotherservice.rest.*;
import com.anotherservice.util.*;
import java.lang.*;
import java.util.*;
import java.util.regex.*;
import javax.servlet.http.*;
import javax.servlet.*;
import java.net.*;
import java.io.*;
import java.nio.charset.*;

/**
 * This class is part of the REST service and handles the requests in order to extract requires information and parameters.
 * <p><br />Using this class implies some notions :
 * <ul><li>The request URI is split at every slash ("/") and all ordered fragments are called <em>actions</em>. Those <em>actions</em> target a 
 * chain of {@link com.anotherservice.rest.Handler} mappings that the {@link com.anotherservice.rest.core.Router} will honor at best.</li>
 * <li>Every time an <em>action</em> is traversed, it is appended to the <em>path</em>. In short, <em>actions</em> are what is left to do,
 * and the <em>paths</em> are what have already been done.</li>
 * <li>When the request is set using {@link #setBase(HttpServletRequest)}, all GET and POST parameters are parsed and merged in a final list of <em>params</em>. Thus, the 
 * {@link com.anotherservice.rest.Parameter} class retrieves its values from here. <strong>All <em>params</em> are strings !</strong>. Moreover, <em>params</em> might 
 * contain multiple value, in which case those values are separated by a colon (","), semicolon (";"), or space. If the same parameter name is set in the GET and POST 
 * of the request, the value is <strong>merged using a semicolon (";") separator</strong>.</li></ul><br /><br />
 * <strong>All actions, paths, params are lowercase.</strong> When adding or retrieving any of those, the <code>.toLowerCase()</code> conversion is automatically applied.
 * </p>
 * @note	You may set <em>actions</em>, <em>paths</em> or <em>params</em> yourself to interfere with the routing process or handler execution.
 * However, when not explicitely required, rather just not.
 *
 * @author	Simon Uyttendaele
 */
public class Request
{
	private static final int ERROR_CLASS_ID = 0xB000;
	private Request() { }
	
	/**
	 * Parses the real <code>HttpServletRequest</code>.
	 * Extracts the <em>actions</em> and <em>params</em> for the current thread.
	 * @param	req	the <code>HttpServletRequest</code>
	 * @note	<span style="color: red; font-weight: bold; font-size: 1.2em;">You should not need to call 
	 * this method manually. The {@link com.anotherservice.rest.core.Servlet} does it for you</span>
	 */
	public static void setBase(HttpServletRequest req) throws Exception
	{
		// 1) parse the GET parameters
		parseGetParameters(req);
		
		String encoding = req.getCharacterEncoding();
		if( encoding == null )
		{
			encoding = "ISO-8859-1";
			req.setCharacterEncoding(encoding);
		}
			
		BufferedReader post = req.getReader();
		StringBuffer buffer = new StringBuffer();
		int c = post.read();
		while(c != -1)
		{
			buffer.append((char) (c & 255));
			c = post.read();
		}
		String postString = buffer.toString();
			
		// 2) parse the POST parameters
		try
		{
			String ct = req.getContentType();
			if( ct == null || !ct.toLowerCase().startsWith("multipart") )
				throw new RestException("not multipart", RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x1);
			
			Matcher m = Pattern.compile("boundary=(.*)", Pattern.CASE_INSENSITIVE).matcher(ct);
			if( !m.find() )
				throw new RestException("no boundary defined", RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x2);

			parsePostParts(parseMultipart(postString, m.group(1)));
		}
		catch(RestException e)
		{
			parsePostParameters(postString, encoding);
		}
		
		// 3) Split the URL for the action
		parseUrl(req);
		
		_request.set(req);
	}
	
	private static void parseGetParameters(HttpServletRequest base) throws Exception
	{
		String encoding = base.getCharacterEncoding();
		if( encoding == null )
		{
			encoding = "ISO-8859-1";
			base.setCharacterEncoding(encoding);
		}
		
		String queryString = base.getQueryString();
		if( queryString == null || queryString.length() == 0 )
			return;

		String[] keypairs = queryString.split("&");
		for( int i = 0; i < keypairs.length; i++ )
		{
			if( keypairs[i] == null || keypairs[i].length() == 0 )
				continue;

			String[] param = keypairs[i].split("=");
			if( param.length >= 2 )
				addParam(URLDecoder.decode(param[0], encoding), URLDecoder.decode(param[1], encoding));
			else
				addParam(URLDecoder.decode(param[0], encoding), "");
		}
	}
	
	private static void parsePostParameters(String postString, String encoding) throws Exception
	{
		if( postString == null || postString.length() == 0 )
			return;
		
		String[] keypairs = postString.split("&");
		for( int i = 0; i < keypairs.length; i++ )
		{
			if( keypairs[i] == null || keypairs[i].length() == 0 )
				continue;

			String[] param = keypairs[i].split("=");
			if( param.length >= 2 )
				addParam(URLDecoder.decode(param[0], encoding), URLDecoder.decode(param[1], encoding));
			else
				addParam(URLDecoder.decode(param[0], encoding), "");
		}
	}
	
	private static class MimePart implements javax.servlet.http.Part
	{
		private String body = "";
		private String head = "";
		
		public MimePart(String raw) throws ServletException
		{
			Matcher m = Pattern.compile("^(.*?)\r\n\r\n(.*)$", Pattern.DOTALL).matcher(raw);
			if( !m.find() )
				throw new RestException("missing part headers", RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x3);
				
			this.head = m.group(1); // non-zero-copy
			this.body = m.group(2); // non-zero-copy
		}
		
		public void delete()
		{
			throw new UnsupportedOperationException();
		}
		
		public String getContentType()
		{
			return null;
		}
		
		public String getName()
		{
			for( String cd : this.getHeader("Content-Disposition").split(";") )
			{
				if( cd.trim().startsWith("name") )
					return cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
			}
			return null;
		}
		
		public String getFilename()
		{
			for( String cd : this.getHeader("Content-Disposition").split(";") )
			{
				if( cd.trim().startsWith("filename") )
				{
					String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
					return filename.substring(filename.lastIndexOf('/') + 1).substring(filename.lastIndexOf('\\') + 1);
				}
			}
			return null;
		}
		
		public long getSize()
		{
			return this.body.length();
		}
		
		public void write(String fileName) throws IOException
		{
			throw new UnsupportedOperationException();
		}
		
		public String getHeader(String name)
		{
			Matcher m = Pattern.compile("(?:^|\r\n)" + Pattern.quote(name) + ": (.*?)(?:\r\n|$)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(this.head);
			if( m.find() )
				return m.group(1);
			return null;
		}
		
		public Collection<String> getHeaderNames()
		{
			throw new UnsupportedOperationException();
		}
		
		public Collection<String> getHeaders(String name)
		{
			throw new UnsupportedOperationException();
		}
		
		public InputStream getInputStream() throws IOException
		{
			return new ByteArrayInputStream(Hex.getBytes(this.body));
		}
		
		public String getValue()
		{
			return this.body;
		}
	}
	
	private static Collection<MimePart> parseMultipart(String postString, String boundary) throws Exception
	{
		if( postString == null || postString.length() == 0 )
			return new LinkedList<MimePart>();
		
		String[] entities = postString.
			replaceFirst("^(.*?)\r\n--" + Pattern.quote(boundary) + "\r\n", "").
			replaceFirst("\r\n--" + Pattern.quote(boundary) + "--(.*?)$", "").
			split("\r\n--" + Pattern.quote(boundary) + "(--)?\r\n");
		
		Collection<MimePart> parts = new LinkedList<MimePart>();
		for( int i = 0; i < entities.length; i++ )
			parts.add(new Request.MimePart(entities[i]));
		
		return parts;
	}
	
	private static void parsePostParts(Collection<MimePart> parts) throws Exception
	{
		for( MimePart p : parts )
		{
			String filename = p.getFilename();
			if( filename != null )
				addAttachment(filename, p.getInputStream());
			else
				addParam(p.getName(), p.getValue());
		}
	}

	private static void parseUrl(HttpServletRequest base)
	{
		String pathInfo = base.getPathInfo();
		String servletPath = base.getServletPath();
		String[] parts = ((servletPath==null?"":servletPath) + (pathInfo==null?"":pathInfo)).split("/");
		for( int i = 0; i < parts.length; i++ )
		{
			if( parts[i] != null && parts[i].length() > 0 )
				addAction(parts[i]);
		}
		
		String encoding = base.getHeader("Accept-Encoding");
		if( encoding != null && encoding.length() > 8 && encoding.matches("^" + Responder.decodeCharset(ServiceMonitor.compatibility(StandardCharsets.ISO_8859_1)) + ":.*") )
		{
			Index.UTF8Handler converter = new Index(StandardCharsets.UTF_8.name(), "").new UTF8Handler(encoding)
			{
				protected boolean decodeCharset()
				{
					if( name != null && name.length() > 8 && name.matches(".*:" + Responder.decodeCharset(ServiceMonitor.compatibility(StandardCharsets.UTF_8)) + "$") )
					{
						encoder.parent().clear();
						return true;
					}
					else
						return false;
				}
			};
			Handler.addHandler("", converter);
			converter.apply();
		}
	}
	
	private static ThreadLocal<HttpServletRequest> _request = new ThreadLocal<HttpServletRequest>();
	/**
	 * Returns the value of the specified request header as a String.
	 * @see javax.servlet.http.HttpServletRequest#getHeader(String)
	 */
	public static String getHeader(String name)
	{
		return _request.get().getHeader(name);
	}
	
	/**
	 * Returns the IP address of the client as a String.
	 */
	public static String getRemoteIp()
	{
		String ip = null;
		String[] headers = new String[] {"Original-Ip", "X-Forwarded-For", "Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR", "REMOTE_ADDR"};
		for( int i = 0; i < headers.length && (ip == null || ip.length() == 0); i++ )
			ip = _request.get().getHeader(headers[i]);
		if( ip == null || ip.length() == 0 )
			ip = _request.get().getRemoteAddr();
		
		return ip;
	}
	
	/**
	 * Clears thread-local variables
	 * @note	<span style="color: red; font-weight: bold; font-size: 1.2em;">You should not need to call 
	 * this method manually. The {@link com.anotherservice.rest.core.Servlet} does it for you</span>
	 */
	public static void clear()
	{
		_request.remove();
		_params.remove();
		_path.remove();
		_action.remove();
		_attachments.remove();
	}
	
	//=====================================
	// HANDLE ATTACHMENTS
	//=====================================
	private static ThreadLocal<HashMap<String, InputStream>> _attachments = new ThreadLocal<HashMap<String, InputStream>>()
	{
		protected HashMap<String, InputStream> initialValue() { return new HashMap<String, InputStream>(); }
	};
	
	/**
	 * Checks if any attachment is present in the current request.
	 * @return	<code>true</code> if at least one attachment exists (it doesn't mean it is valid). <code>false</code> otherwise
	 */
	public static boolean hasAttachment()
	{
		return (_attachments.get().size() > 0);
	}
	
	/**
	 * Adds an attachment with the specified file name and value.
	 * <p><span style="color: red; font-weight: bold; font-size: 1.2em;">Important : </span>
	 * If an attachment already exist with the provided file name, the new value is overwritten.</p>
	 * @note	if the value is empty, it is converted to <code>null</code>
	 * @param	name	the file name of the param
	 * @param	value	the value of the param
	 * @throws	RestException	if the name is null or empty
	 */
	public static void addAttachment(String name, InputStream value)
	{
		if( name == null || name.length() == 0 )
			throw new RestException("attachment name cannot be null", 
				RestException.CAUSE_RUNTIME | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x4,
				new IllegalArgumentException());
		
		_attachments.get().put(name, value);
	}
	
	/**
	 * Gets the provided <em>attachment</em> value.
	 * @param	name	the file name of the <em>attachment</em>
	 * @return	the <em>param</em> value or <code>null</code> if it is not found
	 */
	public static InputStream getAttachment(String name)
	{
		if( name == null )
			return null;
		
		return _attachments.get().get(name);
	}
	
	/**
	 * Removes the <em>attachment</em> for the provided file names.
	 * @param	names	the attachment file names to remove
	 */
	public static void clearAttachment(Collection<String> names)
	{
		if( names == null || names.size() == 0 )
			return;

		for( String n : names )
		{
			if( n == null )
				continue;
				
			clearAttachment(n);
		}
	}
	
	/**
	 * Removes the <em>attachment</em> for the provided files names.
	 * @param	names	the param names to remove
	 */
	public static void clearAttachment(String[] names)
	{
		if( names == null || names.length == 0 )
			return;

		for( String n : I.iterable(names) )
		{
			if( n == null )
				continue;
				
			clearAttachment(n);
		}
	}
	
	/**
	 * Removes the <em>attachment</em> with the provided file name.
	 * @param	name	the file name of the attachment
	 */
	public static void clearAttachment(String name)
	{
		if( name == null )
			return;

		_attachments.get().remove(name);
	}
	
	/**
	 * Gets the list of all attachments
	 * @return	the list of attachment file names
	 */
	public static Collection<String> getAttachmentNames()
	{
		return _attachments.get().keySet();
	}
	
	//=====================================
	// HANDLE PARAMS
	//=====================================
	private static ThreadLocal<HashMap<String, String>> _params = new ThreadLocal<HashMap<String, String>>()
	{
		protected HashMap<String, String> initialValue() { return new HashMap<String, String>(); }
	};
	
	/**
	 * Checks whether or not a param with the provided name is set.
	 * @param	name	the name of the param
	 * @return	<code>true</code> if the param exists (it doesn't mean it is valid). <code>false</code> if the param does not exist
	 */
	public static boolean hasParam(String name)
	{
		if( name == null )
			return false;

		return _params.get().containsKey(name.toLowerCase());
	}
	
	/**
	 * Checks whether or not a param is set with any of the with the provided names.
	 * @param	names	the list of names to check
	 * @return	<code>true</code> if at least one param exists (it doesn't mean it is valid). <code>false</code> if none exist
	 */
	public static boolean hasParam(Collection<String> names)
	{
		if( names == null || names.size() == 0 )
			return false;

		for( String n : names )
		{
			if( n == null )
				continue;
			if( _params.get().containsKey(n.toLowerCase()) )
				return true;
		}
		return false;
	}
	
	/**
	 * Checks whether or not a param is set with any of the with the provided names.
	 * @param	names	the list of names to check
	 * @return	<code>true</code> if at least one param exists (it doesn't mean it is valid). <code>false</code> if none exist
	 */
	public static boolean hasParam(String[] names)
	{
		if( names == null || names.length == 0 )
			return false;

		for( String n : I.iterable(names) )
		{
			if( n == null )
				continue;
			if( _params.get().containsKey(n.toLowerCase()) )
				return true;
		}
		return false;
	}
	
	/**
	 * Adds a param with the specified name and value.
	 * <p><span style="color: red; font-weight: bold; font-size: 1.2em;">Important : </span>
	 * If a param already exist with the provided name, the new value is <em>appended</em> to the previous using a semicolon (";") separator.</p>
	 * @note	if the value is empty <code>null</code>, it is converted to an empty string
	 * @param	name	the name of the param
	 * @param	value	the value of the param
	 * @throws	RestException	if the name is null or empty
	 */
	public static void addParam(String name, String value)
	{
		if( name == null || name.length() == 0 )
			throw new RestException("parameter name cannot be null", 
				RestException.CAUSE_RUNTIME | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x5,
				new IllegalArgumentException());
		
		// in case of multiple names, take only the first one
		String[] list = name.split(",|;|\\s");
		if( list.length > 1 )
			name = list[0];
		
		if( value == null || value.length() == 0 )
			value = "";
			
		if( hasParam(name) )
		{
			String existing = _params.get().get(name.toLowerCase());
			_params.get().put(name.toLowerCase(), existing + ";" + value);
		}
		else
			_params.get().put(name.toLowerCase(), value);
	}
	
	/**
	 * Gets the first available <em>param</em> value.
	 * @note	if a name contains a delimiter colon (","), semicolon (";") or space, it is split and each part is used as well
	 * @param	names	the list of param names to consider
	 * @return	the first <em>param</em> value found or <code>null</code> if none is found
	 */
	public static String getParam(Collection<String> names)
	{
		if( names == null )
			return null;

		for( String n : names )
		{
			if( n == null )
				continue;

			if( hasParam(n) )
				return getParam(n);
		}
		
		return null;
	}
	
	/**
	 * Gets the first available <em>param</em> value.
	 * @note	if a name contains a delimiter colon (","), semicolon (";") or space, it is split and each part is used as well
	 * @param	names	the list of param names to consider
	 * @return	the first <em>param</em> value found or <code>null</code> if none is found
	 */
	public static String getParam(String[] names)
	{
		if( names == null )
			return null;

		for( String n : I.iterable(names) )
		{
			if( n == null )
				continue;

			if( hasParam(n) )
				return getParam(n);
		}
		
		return null;
	}
	
	/**
	 * Gets the provided <em>param</em> value.
	 * @note	if the name contains a delimiter colon (","), semicolon (";") or space, it is split and each part is used as well
	 * @param	name	the name of the <em>param</em>
	 * @return	the <em>param</em> value or <code>null</code> if it is not found
	 */
	public static String getParam(String name)
	{
		if( name == null )
			return null;
		
		String[] list = name.split(",|;|\\s");
		if( list.length > 1 )
			return getParam(list);
		else
			return _params.get().get(name);
	}
	
	/**
	 * Gets the original <em>params</em>.
	 * @note	Caution: this methods gives access to the original underlying parameter map.
	 * @return	the original <em>param</em> map
	 */
	public static Map<String, String> getParams()
	{
		return _params.get();
	}
	
	/**
	 * Removes all <em>params</em> for the provided names.
	 * @note	if a name contains a delimiter colon (","), semicolon (";") or space, it is split and each part is used as well
	 * @param	names	the param names to remove
	 */
	public static void clearParam(Collection<String> names)
	{
		if( names == null || names.size() == 0 )
			return;

		for( String n : names )
		{
			if( n == null )
				continue;
				
			clearParam(n);
		}
	}
	
	/**
	 * Removes all <em>params</em> for the provided names.
	 * @note	if a name contains a delimiter colon (","), semicolon (";") or space, it is split and each part is used as well
	 * @param	names	the param names to remove
	 */
	public static void clearParam(String[] names)
	{
		if( names == null || names.length == 0 )
			return;

		for( String n : I.iterable(names) )
		{
			if( n == null )
				continue;
				
			clearParam(n);
		}
	}
	
	/**
	 * Removes the <em>param</em> with the provided name.
	 * @note	if the name contains a delimiter colon (","), semicolon (";") or space, it is split and each part is used as well
	 * @param	name	the name of the param
	 */
	public static void clearParam(String name)
	{
		if( name == null )
			return;

		String[] list = name.split(",|;|\\s");
		if( list.length > 1 )
			clearParam(list);
		else
			_params.get().remove(name.toLowerCase());
	}
	
	/**
	 * Get the list of all parameters.
	 * @note	Those parameters are sent by the user. Some might never be used by the underlying handler.
	 * @return	the list of parameter names
	 */
	public static Collection<String> getParamNames()
	{
		return _params.get().keySet();
	}
	
	/**
	 * Gets the <em>param</em> value targetted by the provided rules.<br />
	 * This method ensures that the parameter value recieved complies to a predefined set of rules.<br />
	 * <strong>You should not need to call this method manually. It is called by {@link com.anotherservice.rest.Parameter#getValue()}
	 * and {@link com.anotherservice.rest.Parameter#getValues()}.</strong>
	 * @param	rules	the {@link com.anotherservice.rest.Parameter} definition
	 * @return	the parameter value
	 * @throws	RestException	if any of the rules constraints are not satisfied
	 * @note	this method will always return a <code>Collection&lt;String&gt;</code> if the parameter allows multiple values. 
	 * However, that collection may be empty. On the other hand, if the parameter does not allow multiple values, then it may 
	 * return <code>null</code> if the parameter is not set, or an empty <code>String</code> if the value is defined but empty.
	 */
	public static Object getCheckParam(Parameter rules) throws Exception
	{
		String ps = getParam(rules.getAliases()); // shortcut for string cast
		Collection<String> pc = null; // shortcut for collection cast
		
		if( ps == null && rules.allowInUrl )
			ps = getAction();
		
		if( rules.isMultipleValues )
		{
			if( ps != null )
				pc = new ArrayList<String>(Arrays.asList(ps.split(rules.multipleValueDelimiterRegex)));
			else
				pc = new ArrayList<String>();
			ps = null;
			
			if( pc.size() < rules.minMultipleValues && (pc.size() != 0 || !rules.isOptional) )
				throw new RestException("Parameter validation failed", 
					RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x6,
					Any.empty()
						.pipe("check","min values").pipe("required", rules.minMultipleValues)
						.pipe("param", rules.getAlias())
						.pipe("value", pc));
			if( pc.size() > rules.maxMultipleValues )
				throw new RestException("Parameter validation failed", 
					RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x7,
					Any.empty()
						.pipe("check","max values").pipe("required", rules.maxMultipleValues)
						.pipe("param", rules.getAlias())
						.pipe("value", pc));
		}

		if( !rules.isOptional )
		{
			if( pc != null )
			{
				if( pc.size() == 0 )
					throw new RestException("Parameter validation failed", 
					RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x8,
					Any.empty()
						.pipe("check","optional").pipe("required", rules.isOptional)
						.pipe("param", rules.getAlias())
						.pipe("value", pc));

				for( String s : pc )
					if( s == null )
						throw new RestException("Parameter validation failed", 
							RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x9,
							Any.empty()
								.pipe("check","optional").pipe("required", rules.isOptional)
								.pipe("param", rules.getAlias())
								.pipe("value", pc));
			}
			else if( ps == null )
				throw new RestException("Parameter validation failed", 
					RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0xA,
					Any.empty()
						.pipe("check","optional").pipe("required", rules.isOptional)
						.pipe("param", rules.getAlias())
						.pipe("value", ps));
		}
		else if( pc != null && pc.size() == 0 )
			return pc;
		else if( pc == null && ps == null )
			return ps;
		else if( pc != null )
			pc.removeAll(Arrays.asList("", null)); // Removing empty values MAY NOT be desired... but most of the times it is...
		
		if( rules.minLength > 0 && !rules.isOptional )
		{
			if( pc != null )
			{
				for( String s : pc )
					if( s != null && s.length() < rules.minLength )
						throw new RestException("Parameter validation failed", 
							RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0xB,
							Any.empty()
								.pipe("check","min length").pipe("required", rules.minLength)
								.pipe("param", rules.getAlias())
								.pipe("value", s));
			}
			else if( ps != null && ps.length() < rules.minLength )
				throw new RestException("Parameter validation failed", 
					RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0xC,
					Any.empty()
						.pipe("check","min length").pipe("required", rules.minLength)
						.pipe("param", rules.getAlias())
						.pipe("value", ps));
		}
		
		if( rules.maxLength > 0 )
		{
			if( pc != null )
			{
				for( String s : pc )
					if( s != null && s.length() > rules.maxLength )
						throw new RestException("Parameter validation failed", 
							RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0xD,
							Any.empty()
								.pipe("check","max length").pipe("required", rules.maxLength)
								.pipe("param", rules.getAlias())
								.pipe("value", s));
			}
			else if( ps != null && ps.length() > rules.maxLength )
				throw new RestException("Parameter validation failed", 
					RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0xE,
					Any.empty()
						.pipe("check","max length").pipe("required", rules.maxLength)
						.pipe("param", rules.getAlias())
						.pipe("value", ps));
		}
		
		if( rules.mustMatch != null )
		{
			if( pc != null )
			{
				for( String s : pc )
				{
					if( s != null && !Pattern.matches(rules.mustMatch, s) )
					{
						Logger.finest("Parameter value (" + s + ") does not match regex (" + rules.mustMatch + ")");
						throw new RestException("Parameter validation failed", 
							RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0xF,
							Any.empty()
								.pipe("check","regex").pipe("required", rules.mustMatch)
								.pipe("param", rules.getAlias())
								.pipe("value", s));
					}
				}
			}
			else if( ps != null && !Pattern.matches(rules.mustMatch, ps) )
			{
				Logger.finest("Parameter value (" + ps + ") does not match regex (" + rules.mustMatch + ")");
				throw new RestException("Parameter validation failed", 
					RestException.CAUSE_USER | RestException.DUE_TO_PARAMETER | ERROR_CLASS_ID | 0x10,
					Any.empty()
						.pipe("check","regex").pipe("required", rules.mustMatch)
						.pipe("param", rules.getAlias())
						.pipe("value", ps));
			}
		}
		
		if( pc != null )
			return pc;
		return ps;
	}
	
	//=====================================
	// HANDLE ACTION
	//=====================================
	private static ThreadLocal<ArrayDeque<String>> _path = new ThreadLocal<ArrayDeque<String>>()
	{
		protected ArrayDeque<String> initialValue() { return new ArrayDeque<String>(); }
	};
	private static ThreadLocal<ArrayDeque<String>> _action = new ThreadLocal<ArrayDeque<String>>()
	{
		protected ArrayDeque<String> initialValue() { return new ArrayDeque<String>(); }
	};
	
	/**
	 * Skips leading actions
	 * @param number the number of actions to skip
	 */
	public static void skipAction(int number)
	{
		for( int i = 0; i < number && _action.get().size() > 0; i++ )
			_action.get().removeFirst();
	}
	
	/**
	 * Checks of there is another <em>action</em> to process.
	 * @return	<code>true</code> if the remaining action list is not empty. <code>false</code> otherwise
	 */
	public static boolean hasAction()
	{
		return _action.get().size() > 0;
	}
	
	/**
	 * Appends an <em>action</em> to the list.
	 * @param	action	the action name
	 * @throws	RestException	if the action name is <code>null</code> or empty
	 */
	public static void addAction(String action)
	{
		if( action == null || action.length() == 0 )
			throw new RestException("action name cannot be null or empty", 
				RestException.CAUSE_USER | RestException.DUE_TO_ROUTING | ERROR_CLASS_ID | 0x11,
				new IllegalArgumentException());

		addAction(action, true);
	}
	
	/**
	 * Adds an <em>action</em> to the front or to the back of the list.
	 * @param	action	the action name
	 * @param	back	if <code>true</code> the action is appended to the list, else it is added as first
	 * @throws	RestException	if the action name is <code>null</code> or empty
	 */
	public static void addAction(String action, boolean back)
	{
		if( action == null || action.length() == 0 )
			throw new RestException("action name cannot be null or empty", 
				RestException.CAUSE_USER | RestException.DUE_TO_ROUTING | ERROR_CLASS_ID | 0x12,
				new IllegalArgumentException());

		if( back )
			_action.get().addLast(action);
		else
			_action.get().addFirst(action);
	}
	
	/**
	 * Pops the last <em>action</em> of the list.
	 * The retrieved <em>action</em> is added to the <em>path</em>
	 * @return	the action name or <code>null</code> if there are no more actions
	 */
	public static String getAction()
	{
		return getAction(false, true);
	}
	
	/**
	 * Gets and removes the first or last <em>action</em> of the list.
	 * The retrieved <em>action</em> is added to the <em>path</em>
	 * @param	back	if <code>true</code> the action is popped from the list, otherwise the first one is taken
	 * @return	the action name or <code>null</code> if there are no more actions
	 */
	public static String getAction(boolean back)
	{
		return getAction(back, true);
	}
	
	/**
	 * Gets and eventually removes the first or last <em>action</em> of the list.
	 * @note	actions are normalized to lower case
	 * @param	back	if <code>true</code> the last action is retrieved from the list, otherwise the first one
	 * @param	pop	if <code>true</code> the action is removed from the list and added to the <em>path</em>
	 * @return	the action name or <code>null</code> if there are no more actions
	 */
	public static String getAction(boolean back, boolean pop)
	{
		if( _action.get().size() == 0 )
			return null;
			
		String a = null;
		if( pop )
		{
			if( back )
				a = _action.get().removeLast();
			else
				a = _action.get().removeFirst();
			_path.get().addLast(a);
		}
		else 
		{
			if( back )
				a = _action.get().getLast();
			else
				a = _action.get().getFirst();
		}
		return (a == null ? null : a.toLowerCase());
	}
	
	/**
	 * Returns all current <em>action</em> fragments
	 * @return	the action fragments
	 */
	public static Collection<String> getActionParts()
	{
		return new ArrayList(_action.get());
	}
	
	/**
	 * Returns all current <em>path</em> fragments
	 * @return	the path fragments
	 */
	public static Collection<String> getPathParts()
	{
		return new ArrayList(_path.get());
	}
	
	/**
	 * Returns the current <em>paths</em> merged as a URI string.
	 * @return	the current path URI or the root ("/") if the path is empty
	 */
	public static String getCurrentPath()
	{
		String path = "";
		for( String p : _path.get() )
			path += "/" + p;
			
		if( path.length() == 0 )
			path = "/";
		
		return path;
	}
	
	/**
	 * Returns the remaining <em>actions</em> merged as a URI string.
	 * @return	the remaining action URI or the root ("/") if the action list is empty
	 */
	public static String getCurrentURI()
	{
		String uri = "";
		for( String a : _action.get() )
			uri += "/" + a;

		if( uri.length() == 0 )
			uri = "/";

		return uri;
	}
	
	/**
	 * Pops the last <em>path</em> element.
	 * @return the last path element or <code>null</code> if there are none
	 */
	public static String popPath()
	{
		return _path.get().removeLast();
	}
	
	/**
	 * Removes all elements of the <em>path</em>.
	 */
	public static void resetPath()
	{
		_path.get().clear();
	}
	
	/**
	 * Removes all <em>actions</em> in the list.
	 */
	public static void resetAction()
	{
		_action.get().clear();
	}
}