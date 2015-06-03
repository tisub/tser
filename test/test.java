import com.anotherservice.util.*;

public class test
{
	public static void main(String[] args)
	{
		try
		{
			test.check("a\u00e8"); // ascii
			test.check("a\u00c3\u00a8"); // doubled utf8
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void check(String txt) throws Exception
	{
		System.out.println(txt);
		System.out.println(Json.encode(txt));
		System.out.println(Json.decode(Json.encode(txt)));
		
		System.out.println(Hex.toHexString(txt));
		System.out.println(Hex.toHexString(Hex.toString(Hex.getBytes(txt))));
		
		System.out.println(Base64.encode(txt));
		System.out.println(Hex.toString(Base64.decode(Base64.encode(txt))));
		System.out.println("=========");
	}
}