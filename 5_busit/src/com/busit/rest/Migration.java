package com.busit.rest;

import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.rest.core.*;
import com.anotherservice.db.*;
import com.anotherservice.util.*;
import com.busit.security.*;
import java.util.*;
import java.lang.*;
import java.math.*;
import sun.security.x509.*;
import sun.security.rsa.*;
import sun.security.util.*;

import java.security.cert.*;
import java.security.interfaces.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class Migration extends InitializerOnce
{
	public void initialize()
	{	
		Index index = new Index();
		index.addMapping(new String[] { "migration" });
		Handler.addHandler("/", index);
		
		Action migrate = new Action("apply", "", "")
		{
			public String migrate() throws Exception
			{
				List<Map<String, String>> rows = Database.getInstance().select("SELECT u.user_id, u.user_name, from_unixtime(u.user_date) as inscription from users u left join identities i on(i.identity_user = u.user_id) where i.identity_id is null order by inscription desc");
				
				for( Map<String, String> row : rows)
				{
					Request.clearParam("user");
					Request.clearParam("name");
					Request.addParam("user", row.get("user_id"));
					Request.addParam("name", row.get("user_name"));
					
					Router.forward("/busit/identity/insert");
				}
				
				return "";
			}
			
			public Object execute() throws Exception
			{
				String pass = getParameter("pass").getValue();
				
				if( pass == null || !pass.equals("simon123") )
					return "ABORTED";
					
				String msg = migrate();
				return "OK " + msg;
			}
		};
		
		Parameter pass = new Parameter();
		pass.isOptional = false;
		pass.minLength = 1;
		pass.maxLength = 200;
		pass.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		pass.addAlias(new String[]{ "pass" });
		migrate.addParameter(pass);
		
		index.addOwnHandler(migrate);
	}
}