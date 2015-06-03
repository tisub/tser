package com.busit.ftp.provider;

import com.anotherservice.util.*;
import com.anotherservice.io.*;
import java.util.*;
import com.busit.ftp.*;

public class Busit extends IOProvider
{
	public static String url = null;
	private String token = null;
	private String user = null;
	private static final int SPACE = 0;
	private static final int INSTANCE = 1;
	private static final int INTERFACE = 2;
	
	public Busit() { }
	
	private Any API(String url) throws Exception { return API(url, null); }
	private Any API(String url, Map<String, String> params) throws Exception
	{
		if( params == null )
			params = new HashMap<String, String>();
		if( token != null )
			params.put("auth", token);
		params.put("f", "json");
		
		String response = UrlReader.readUrl(this.url + url, params);
		Any a = Json.decode(response);
		if( a.containsKey("error") )
			throw new Exception(a.get("error").<String>value("message"));
		return a;
	}
	private Any API(String url, Map<String, String> params, byte[] data, String filename) throws Exception
	{
		if( params == null )
			params = new HashMap<String, String>();
		if( token != null )
			params.put("auth", token);
		params.put("f", "json");
		
		String response = UrlReader.readUrl(this.url + url, params, data, filename);
		Any a = Json.decode(response);
		if( a.containsKey("error") )
			throw new Exception(a.get("error").<String>value("message"));
		return a;
	}
	
	private String[] split(String file)
	{
		String[] p = file.replaceAll("/+", "/").replaceFirst("^/+", "").split("/");
		String[] f = new String[Math.max(3, p.length)];
		
		for( int i = 0; i < f.length; i++ )
			f[i] = (i < p.length ? p[i] : null);
		
		return f;
	}
	
	private String getSpace(String file)
	{
		String[] f = file.replaceFirst("^/+", "").split("/");
		if( f.length > 0 )
			return f[0];
		else
			return null;
	}
	
	private String getInstance(String file)
	{
		String[] f = file.replaceFirst("^/+", "").split("/");
		if( f.length > 1 )
			return f[1];
		else
			return null;
	}
	
	private String getInterface(String file)
	{
		String[] f = file.replaceFirst("^/+", "").split("/");
		if( f.length > 2 )
			return f[2];
		else
			return null;
	}
	
	public void reset()
	{
		token = null;
		user = null;
	}
	
	public void authenticate(String user, String pass) throws Exception
	{
		Map<String, String> params = new HashMap<String, String>();
		
		// is the pass a token ?
		if( pass.matches("^[0-9a-fA-F]{32}$") )
		{
			try
			{
				token = user + ":" + pass;
				this.user = user;
				API("/self/busit/space/select"); // check if user can at least list spaces
			}
			catch(Exception e)
			{
				reset();
				throw e;
			}
		}
		else
		{
			params.put("user", user);
			params.put("pass", pass);
			Any response = API("/system/token/select", params);

			if( response.containsKey("response") )
			{
				token = user + ":" + response.get("response").get(0).<String>value("token_value");
				this.user = user;
			}
			else
			{
				Logger.finest(response.toJson());
				throw new Exception("Unexpected response");
			}
		}
	}
	
	public void delete(String file) throws Exception
	{
		if( token == null )
			throw new IllegalStateException();

		String[] path = split(file);
		if( path[INTERFACE] != null && path[INTERFACE].length() > 0 )
		{
			Map<String, String> params = new HashMap<String, String>();
			//params.put("space", path[SPACE]); // not required because instance name is unique per user
			params.put("instance", path[INSTANCE]);
			params.put("interface", path[INTERFACE]);
			Any response = API("/self/busit/instance/interface/delete", params);
		}
		else if( path[INSTANCE] != null && path[INSTANCE].length() > 0 )
		{
			Map<String, String> params = new HashMap<String, String>();
			//params.put("space", path[SPACE]); // not required because instance name is unique per user
			params.put("instance", path[INSTANCE]);
			Any response = API("/self/busit/instance/delete", params);
		}
		else if( path[SPACE] != null && path[SPACE].length() > 0 )
		{
			Map<String, String> params = new HashMap<String, String>();
			params.put("space", path[SPACE]);
			Any response = API("/self/busit/space/delete", params);
		}
		else
			throw new IllegalStateException();
	}
	
	public List<Info> list(String dir) throws Exception
	{
		if( token == null )
			throw new IllegalStateException();

		LinkedList<Info> infos = new LinkedList<Info>();

		String[] path = split(dir);
		if( path[INTERFACE] != null && path[INTERFACE].length() > 0 )
		{
			return infos;
		}
		else if( path[INSTANCE] != null && path[INSTANCE].length() > 0  )
		{
			Map<String, String> params = new HashMap<String, String>();
			params.put("space", path[SPACE]);
			params.put("instance", path[INSTANCE]);
			Any response = API("/self/busit/instance/interface/select", params);
			if( response.containsKey("response") )
			{
				Any instances = response.get("response");
				for( Any i : instances )
				{
					if( i.<String>value("interface_type").equals("1") ) // include only inputs
						infos.add(new Info(0, new Date().getTime(), true, i.<String>value("interface_name")));
				}
			}
			
			return infos;
		}
		else if( path[SPACE] != null && path[SPACE].length() > 0  )
		{
			Map<String, String> params = new HashMap<String, String>();
			params.put("space", path[SPACE]);
			Any response = API("/self/busit/instance/select", params);
			if( response.containsKey("response") )
			{
				Any instances = response.get("response");
				for( Any i : instances )
					infos.add(new Info(0, new Date().getTime(), true, i.<String>value("instance_name")));
			}
			
			return infos;
		}
		else
		{
			Any response = API("/self/busit/space/select");
			if( response.containsKey("response") )
			{
				Any spaces = response.get("response");
				for( Any s : spaces )
					infos.add(new Info(0, new Date().getTime(), true, s.<String>value("space_name")));
			}
			
			return infos;
		}
	}
	
	public void upload(String file, byte[] data) throws Exception
	{
		if( token == null )
			throw new IllegalStateException();

		String[] path = split(file);
		if( path.length < INTERFACE+1 )
			throw new IllegalStateException();
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("destination", user + "+" + path[INSTANCE] + "+" + path[INTERFACE]);
		params.put("message", path[INTERFACE+1]);

		Logger.finer("Uploading file [" + path[INTERFACE+1] + "] for user [" + this.user + "]");
		API("/busit/message/send", params, data, path[INTERFACE+1]);
	}
	
	public byte[] download(String file) throws Exception
	{
		if( token == null )
			throw new IllegalStateException();

		throw new IllegalStateException();
	}
	
	public void rename(String from, String to) throws Exception
	{
		if( token == null )
			throw new IllegalStateException();

		String[] path = split(from);
		String[] path2 = split(to);
		if( path[INTERFACE] != null && path[INTERFACE].length() > 0 )
		{
			Map<String, String> params = new HashMap<String, String>();
			//params.put("space", path[SPACE]); // not required because instance name is unique per user
			params.put("instance", path[INSTANCE]);
			params.put("interface", path[INTERFACE]);
			params.put("name", path2[INTERFACE]);
			Any response = API("/self/busit/instance/interface/update", params);
		}
		else if( path[INSTANCE] != null && path[INSTANCE].length() > 0 )
		{
			Map<String, String> params = new HashMap<String, String>();
			//params.put("space", path[SPACE]); // not required because instance name is unique per user
			params.put("instance", path[INSTANCE]);
			params.put("name", path2[INSTANCE]);
			Any response = API("/self/busit/instance/update", params);
		}
		else if( path[SPACE] != null && path[SPACE].length() > 0 )
		{
			Map<String, String> params = new HashMap<String, String>();
			params.put("space", path[SPACE]);
			params.put("name", path2[SPACE]);
			Any response = API("/self/busit/space/update", params);
		}
		else
			throw new IllegalStateException();
	}
	
	public Info info(String file) throws Exception
	{
		if( token == null )
			throw new IllegalStateException();

		String[] path = split(file);
		String name = "";
		if( path[INTERFACE] != null && path[INTERFACE].length() > 0 )
			name = path[INTERFACE];
		else if( path[INSTANCE] != null && path[INSTANCE].length() > 0 )
			name = path[INSTANCE];
		else if( path[SPACE] != null && path[SPACE].length() > 0 )
			name = path[SPACE];

		return new Info(0, new Date().getTime(), true, file);
	}
	
	public boolean isFile(String file)
	{
		if( token == null )
			throw new IllegalStateException();

		return false;
	}
	
	public boolean isDir(String dir)
	{
		if( token == null )
			throw new IllegalStateException();
		
		
		String[] path = dir.replaceFirst("^/+", "").split("/");
		if( path.length > INTERFACE+1 )
			return false;

		return true;
	}
}