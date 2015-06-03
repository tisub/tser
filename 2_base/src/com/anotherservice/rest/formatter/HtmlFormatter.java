package com.anotherservice.rest.formatter;

import com.anotherservice.rest.core.*;
import com.anotherservice.rest.*;
import com.anotherservice.util.*;
import com.anotherservice.db.*;
import java.util.*;

public class HtmlFormatter implements IResponseFormatter
{
	/**
	 * This is the HTML template used when the output format is HTML.
	 * It <strong>should</strong> contain one sequence <code>{body}</code> that will be replaced by the actual response.
	 */
	public static String htmlTemplate = "<!DOCTYPE html>" + 
			"\n<html>" + 
			"\n<head>" + 
			"\n<title>API Help</title>" + 
			"\n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />" + 
			"\n<style>" + 
			"\n	li { list-style-type: circle; }" + 
			"\n	ul li > ul { border : 1px dashed #CCCCCC; padding: 10px; margin: 0px; margin-left: 25px; }" + 
			"\n	ul li > ul li { margin-left: 20px; }" + 
			"\n	ul li > ul li > ul { border: 0px none white; }" + 
			"\n	h3 { margin: 0px; display: inline; font-size: 1em; font-weight: normal; font-style: italic; color: #668899; }" + 
			"\n	h2 { margin: 0px; display: inline; font-size: 16px; color: #668899; }" + 
			"\n	h1 { margin: 10px; font-size: 20px; border-top: 2px solid #99BBCC; border-bottom: 2px solid #99BBCC; background-color: #EEF5FF; padding: 8px; color: #668899; }" + 
			"\n	h1 a, h2 a, a { text-decoration: underline; color: #668899; }" + 
			"\n	h3.label { width: 120px; display: inline-block; }" + 
			"\n	span.check { color: red; margin-left: 30px; }" + 
			"\n	span.required { font-weight: bold; color: #FF7575; }" + 
			"\n	span.urlizable { font-weight: bold; color: #7979FF; }" + 
			"\n	span.multiple { font-weight: bold; color: #27DE55; }" + 
			"\n	span.optional { font-weight: bold; color: #DFE32D; }" + 
			"\n</style>" + 
			"\n</head>" + 
			"\n<body>\n{body}\n</body>\n</html>";

	public byte[] format(Object o)
	{
		return Hex.getBytes(template(htmlize(o)));
	}
	
	public byte[] format(Throwable t)
	{
		if( t == null )
			throw new IllegalArgumentException("t cannot be null");
		
		Hashtable<String, Object> error = new Hashtable<String, Object>();
		error.put("message", (t.getMessage() == null ? "null" : t.getMessage()));
		
		if( t instanceof RestException )
		{
			error.put("code", ((RestException)t).getCode());
			error.put("data", ((RestException)t).getData().toJson());
		}
		
		if( Logger.instance().level <= 500 /* Level.FINE */ )
		{
			StackTrace.collapse(t);
			StackTrace.removeLastUntil(t, Handler.class.getCanonicalName()); // do not show tomcat and routing stuff
			StackTrace.keepLastUntil(t, Database.class.getCanonicalName()); // do not show internal database driver stuff
			
			ArrayList<Hashtable<String, Object>> trace = new ArrayList<Hashtable<String, Object>>();
			for( StackTraceElement s : t.getStackTrace() )
			{
				Hashtable<String, Object> line = new Hashtable<String, Object>();
				line.put("file", (s.getFileName() == null ? "unknown" : s.getFileName()));
				line.put("line", s.getLineNumber());
				line.put("class", (s.getClassName() == null ? "unknown" : s.getClassName()));
				line.put("method", (s.getMethodName() == null ? "unknown" : s.getMethodName()));
				trace.add(line);
			}
			error.put("trace", trace);
		}

		return format(error);
	}
	
	public byte[] format(Handler h)
	{
		String body = "\n<ul>" + 
			"\n	<li><h2>Alias :</h2> " + implode(", ", h.getMappings()) + "</li>" + 
			"\n	<li><h2>Description :</h2> " + h.getDescription() + "</li>" + 
			"\n	<li><h2>Required grants :</h2> " + implode(", ", h.getGrants()) + "</li>";

		String pathLink = Config.get("com.anotherservice.rest.core.servletRoot") + Request.getCurrentPath() + "/";
		if( pathLink.equals("//") ) pathLink = "/";
		if( h instanceof Index )
		{
			body += "\n	<li><h2>Entries :</h2>\n		<ul>";
			for( Handler e : ((Index)h).getOwnHandlers() )
				body += "\n			<li><h2><a href=\"" + pathLink + e + "/help?f=html\">" + e + "</a></h2> (alias : " + implode(", ", e.getMappings()) + ")</li>";
			body += "\n		</ul>\n	</li>";
		}
		else if( h instanceof Action )
		{
			body += "\n	<li><h2>Parameters :</h2>\n		<ul>";
			
			Collection<Parameter> params = ((Action)h).getParameters();
			for( Parameter p : params )
			{
				body += "\n			<li>" + p.getAlias() + " : " + p.description + " ";
				if( p.isOptional )
					body += "<span class=\"optional\">optional</span> ";
				else
					body += "<span class=\"required\">required</span> ";
				if( p.isMultipleValues )
					body += "<span class=\"multiple\">multiple</span> ";
				if( p.allowInUrl )
					body += "<span class=\"urlizable\">urlizable</span> ";
				body += "\n			<ul>\n				<li><h3>alias :</h3> " + implode(", ", p.getAliases()) + "</li>";
				
				if( p.minLength > 0 )
					body += "\n				<li><h3>min length : </h3> " + p.minLength + "</li>";
				if( p.maxLength > 0 )
					body += "\n				<li><h3>max length : </h3> " + p.maxLength + "</li>";
				if( p.mustMatch != null )
					body += "\n				<li><h3>must match : </h3> " + p.mustMatch + "</li>";
				
				body += "\n		</ul>\n			</li>";
			}
		
			body += "\n		</ul>\n	</li>" + 
					"\n	<li><h2>Returns :</h2> " + ((Action)h).returnDescription + "</li>" + 
					"\n	<li>" + 
					"\n		<h2>Launch this action :</h2>" + 
					"\n<script type=\"text/javascript\">" + 
					"\n	var rules = {" + 
					"\n		'_': null";
			
			for( Parameter p : params )
			{
				if( p.mustMatch != null && p.mustMatch.length() > 0 )
					body += ",\n		'" + p.getAlias() + "': {'match': new RegExp(\"" + 
							p.mustMatch.replaceFirst("\\(\\?s\\)", "").replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\"").replaceAll("\\\\x\\{(..)\\}", "\\\\x$1") + 
							"\"), 'min':" + p.minLength + ", 'max':" + p.maxLength + "}";
				else
					body += ",\n			'" + p.getAlias() + "': {'match': null, 'min':" + p.minLength + ", 'max':" + p.maxLength + "}";
			}
			
			body += "\n	};" +
					"\n	function checkInput(name)" + 
					"\n	{" + 
					"\n		var input = document.getElementById('input_'+name);" + 
					"\n		var elem = document.getElementById('check_'+name);" + 
					"\n		if( rules[name] )" + 
					"\n		{" + 
					"\n			if( (rules[name]['min'] > 0 && input.value.length < rules[name]['min']) || (rules[name]['max'] > 0 && input.value.length > rules[name]['max']) )" +
					"\n				elem.innerHTML = 'Value length does not comply';" +
					"\n			else if( rules[name]['match'] )" + 
					"\n			{" + 
					"\n				try {" + 
					"\n					if( rules[name]['match'].test(input.value) )" + 
					"\n						elem.innerHTML = 'Simple check OK';" + 
					"\n					else" + 
					"\n						elem.innerHTML = 'Check did not pass';" + 
					"\n				} catch(e) { " + 
					"\n					elem.innerHTML = 'Regexp too complex to check';" +
					"\n				}" +
					"\n			}" + 
					"\n		}" + 
					"\n		else" + 
					"\n		{" + 
					"\n			elem.innerHTML = 'No check on this field';" +
					"\n		}" + 
					"\n	}" + 
					"\n</script>" + 
					"\n		<form action=\"" + Config.get("com.anotherservice.rest.core.servletRoot") + Request.getCurrentPath() + "?f=html\" method=\"post\" style=\"margin-left: 50px;\">";
			
			for( Parameter p : params )
			{
				body += "\n			<h3 class=\"label\">" + p.getAlias() + "</h3>" + 
						"\n			<input type=\"text\" id=\"input_" + p.getAlias() + "\" name=\"" + p.getAlias() + "\" " +
						"onkeyup=\"checkInput('" + p.getAlias() + "');\" /><span class=\"check\" id=\"check_" + p.getAlias() + "\"></span><br />";
			}

			body += "\n			<h3 class=\"label\">credentials</h3>" + 
					"\n			<input type=\"text\" id=\"input_auth\" name=\"auth\" /><br />" + 
					"\n			<input type=\"submit\" value=\"Submit\" />" + 
					"\n		</form>" + 
					"\n	</li>";
		}
		
		body += "\n</ul>";
		
		return Hex.getBytes(template(body));
	}
	
	private String implode(String glue, Collection<String> pieces)
	{
		String result = "";
		int i = 0;
		for( String p : pieces )
		{
			if( i > 0 )
				result += glue;
			result += p;
			i++;
		}
		
		return result;
	}
	
	private String implode(String glue, String[] pieces)
	{
		String result = "";
		int i = 0;
		for( String p : I.iterable(pieces) )
		{
			if( i > 0 )
				result += glue;
			result += p;
			i++;
		}
		
		return result;
	}
	
	private String htmlize(Object object)
	{
		String html = "";
		
		if( object instanceof Any )
			object = (Object)((Any)object).value();
		
		if( object instanceof Map )
		{
			html += "<ul>";
			for( Object key : ((Map)object).keySet() )
				html += "<li><strong>" + key + "</strong>: " + htmlize(((Map)object).get(key)) + "</li>";
			html += "</ul>";
		}
		else if( object instanceof Iterable )
		{
			html += "<ol>";
			for( Object o : ((Iterable)object) )
				html += "<li>" + htmlize(o) + "</li>";
			html += "</ol>";
		}
		else if( object != null && object.getClass().isArray() )
		{
			if( object instanceof char[] )
				html += Hex.toString((char[])object);
			else if( object instanceof byte[] )
				html += Hex.toString((byte[])object);
			else
			{
				html += "<ol>";
				for( Object o : I.iterable((Object[])object) )
					html += "<li>" + htmlize(o) + "</li>";
				html += "</ol>";
			}
		}
		else if( object != null )
			html += object.toString();
		
		return html;
	}
	
	private String template(String body)
	{
		String helpPath = Config.gets("com.anotherservice.rest.core.helpActionPath");
		if( helpPath == null )
			helpPath = "help";

		String b = "<h1><a href=\"" + Config.get("com.anotherservice.rest.core.servletRoot") + "/" + helpPath + "?f=html\">API Help</a>";
		
		Collection<String> p = Request.getPathParts();
		String path = Config.get("com.anotherservice.rest.core.servletRoot") + "/";
		for( String s : p )
		{
			if( s.equalsIgnoreCase(helpPath) )
				continue;
			else
			{
				path += s + "/";
				b += " :: <a href=\"" + path + helpPath + "?f=html\">" + s + "</a>";
			}
		}
		
		b += " :: help</h1>\n" + body;
		
		return htmlTemplate.replace("{body}", b);
	}
}