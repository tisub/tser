import com.anotherservice.rest.*;
import com.anotherservice.rest.core.*;
import java.util.*;

public class test extends InitializerOnce
{
	public void initialize()
	{
		Action hello = new Action()
		{
			public Object execute() throws Exception
			{
				return "Hello World!";
			}
		};
		
		hello.addMapping("hello");
		hello.description = "Test the api return handling";
		hello.returnDescription = "\"Hello World!\"";
		Handler.addHandler("/test", hello);
		
		// ================================
		
		Action error = new Action()
		{
			public Object execute() throws Exception
			{
				throw new Exception("Test the api");
			}
		};
		
		error.addMapping("error");
		error.description = "Test the api error handling";
		error.returnDescription = "Exception";
		Handler.addHandler("/test", error);
		
		// ================================
		
		Action param = new Action()
		{
			public Object execute() throws Exception
			{
				Hashtable<String, String> r = new Hashtable<String, String>();
				for( Parameter p : getParameters() )
					r.put(p.getAlias(), p.getValue());
				return r;
			}
		};
		
		param.addMapping("param");
		param.description = "Test the api params handling";
		param.returnDescription = "Parameters with their values";
		Parameter p = new Parameter();
		p.isOptional = false;
		p.minLength = 3;
		p.maxLength = 10;
		p.description = "A test parameter";
		p.addAlias("test");
		param.addParameter(p);
		
		Handler.addHandler("/test", param);
		
		// ================================
		
		Action security = new Action()
		{
			public Object execute() throws Exception
			{
				return "void";
			}
		};
		
		security.addMapping("security");
		security.description = "Test the api security handling";
		security.returnDescription = "void";
		security.addGrant("grant");
		Handler.addHandler("/test", security);
		
		// ================================
		
		Action files = new Action()
		{
			public Object execute() throws Exception
			{
				if( !Request.hasAttachment() )
					throw new Exception("You should upload a file in multipart/form-data");
					
				String filename = Request.getAttachmentNames().iterator().next();
				Request.addAction(filename);
				Request.getAction();
				return Request.getAttachment(filename);
			}
		};
		
		files.addMapping("files");
		files.description = "Test the api file handling (upload and download)";
		files.returnDescription = "Download the uploaded file";
		Handler.addHandler("/test", files);
	}
}