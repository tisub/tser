package com.busit.security;

import java.security.Principal;
import com.anotherservice.util.*;
import java.util.regex.*;
import javax.security.auth.x500.X500Principal;

public class PrincipalUtil
{
	public static Principal parse(String p)
	{
		return nameToPrincipal(p);
	}
	
	public static boolean equals(Principal a, Principal b)
	{
		if( a == null || b == null )
			return false;
		
		return a.getName().equalsIgnoreCase(b.getName());
	}
	
	public static boolean equals(Principal a, String b)
	{
		if( a == null || b == null )
			return false;
		
		return a.getName().equalsIgnoreCase(b);
	}
	
	public static boolean equals(String a, Principal b)
	{
		if( a == null || b == null )
			return false;
		
		return b.getName().equalsIgnoreCase(a);
	}
	
	public static boolean isPrincipal(String p)
	{
		String CN = extractFromDN("CN", p);
		String O = extractFromDN("O", p);
		return (CN != null && CN.length() > 0 && O != null && O.length() > 0);
	}
	
	public static String shortName(Principal p)
	{
		if( p == null ) return null;

		// only the authority is not '@'
		if( equals(p, IdentityStore.getInstance().getAuthority().getSubjectPrincipal()) )
			return extractFromDN("CN", p.getName());
		
		String org = orgName(p);
		return extractFromDN("CN", p.getName()) + "@" + (org == null ? remoteName(p) : org);
	}
	
	public static String shortName(String p)
	{
		return shortName(parse(p));
	}
	
	public static String shortName(Identity i)
	{
		if( i == null ) return null;
		return shortName(i.getSubjectPrincipal());
	}
	
	public static boolean isLocal(Principal p)
	{
		String local = shortName(IdentityStore.getInstance().getAuthority().getSubjectPrincipal());
		String remote = remoteName(p);
		return (remote != null && remote.equals(local));
	}
	
	public static boolean isRemote(Principal p)
	{
		return !isLocal(p);
	}
	
	public static String remoteName(Principal p)
	{
		if( p == null ) return null;
		return extractFromDN("O", p.getName());
	}
	
	public static String remoteName(String p)
	{
		return remoteName(parse(p));
	}
	
	public static boolean isOrg(Principal p)
	{
		String org = orgName(p);
		return (org != null && org.length() > 0);
	}
	
	public static String orgName(Principal p)
	{
		return extractFromDN("OU", p.getName());
	}
	
	public static String extractFromDN(String key, String DN)
	{
		if( key == null || (!key.equals("CN") && !key.equals("L") && !key.equals("ST") && !key.equals("O") && !key.equals("OU") && !key.equals("C") && !key.equals("STREET")) )
			throw new IllegalArgumentException("Key is not valid");

/*
	http://www.ietf.org/rfc/rfc2253.txt
	
   2.4.  Converting an AttributeValue from ASN.1 to a String
   
   [...]
   
   If the UTF-8 string does not have any of the following characters
   which need escaping, then that string can be used as the string
   representation of the value.

    o   a space or "#" character occurring at the beginning of the
        string

    o   a space character occurring at the end of the string

    o   one of the characters ",", "+", """, "\", "<", ">" or ";"

   Implementations MAY escape other characters.

   If a character to be escaped is one of the list shown above, then it
   is prefixed by a backslash ('\' ASCII 92).

   Otherwise the character to be escaped is replaced by a backslash and
   two hex digits, which form a single byte in the code of the
   character.
*/

		// 1) we replace all escaped non-hex characters by their hex-representation
		// \, = \2C 
		// \+ = \2B 
		// \" = \22 
		// \\ = \5C 
		// \< = \3C 
		// \> = \3E 
		// \; = \3B
		DN = DN.replaceAll("\\\\,", "\\\\2C")
			.replaceAll("\\\\\\+",	"\\\\2B")
			.replaceAll("\\\\\"",	"\\\\22")
			.replaceAll("\\\\\\\\",	"\\\\5C")
			.replaceAll("\\\\<",	"\\\\3C")
			.replaceAll("\\\\>",	"\\\\3E")
			.replaceAll("\\\\;", 	"\\\\3B");
		
		// 2) we extract the CN
		Matcher m = Pattern.compile("(?:^|,|;)\\s*" + key + "\\s*=\\s*(.*?)\\s*(?:$|,|;)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(DN);
		if( !m.find() )
			return null;
		DN = m.group(1);
		
		// 3) we replace all excaped hex characters by their value
		m = Pattern.compile("\\\\([0-9a-fA-F]{2})").matcher(DN);
		boolean result = m.find();
		if( !m.find() )
			return DN;
		
		StringBuffer sb = new StringBuffer();
		do { m.appendReplacement(sb, String.valueOf(Hex.toChar(m.group(1)))); }
		while( m.find() );
		m.appendTail(sb);
		return sb.toString();
	}

	public static Principal nameToPrincipal(String name)
	{
		if( name == null || name.length() == 0 )
			return null;
		if( isPrincipal(name) )
			return new X500Principal(name);
		
		String authorityName = shortName(IdentityStore.getInstance().getAuthority().getSubjectPrincipal());
		name = name.replaceFirst("@" + authorityName, "");
		
		return new X500Principal("CN=" + name.replaceFirst("@", ",OU=") + ",O=" + authorityName);
	}
}