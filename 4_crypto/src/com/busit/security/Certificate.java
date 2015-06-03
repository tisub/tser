package com.busit.security;

import java.io.*;
import java.util.regex.*;
import com.anotherservice.util.*;

public class Certificate
{
	private Certificate issuer = null;
	private String encoded = null;
	private java.security.cert.X509Certificate cache = null;
	
	public Certificate(String data) throws Exception
	{
		// extract this certificate's data
		Matcher m = Pattern.compile(Crypto.certificateStart.replaceAll("\\n", "\\\\s*") + Crypto.base64Pattern + Crypto.certificateEnd.replaceAll("\\n", "\\\\s*"), Pattern.DOTALL).matcher(data);
		if( !m.find() )
			throw new Exception("Unrecognized certificate format");
		this.encoded = m.group(1);
		
		cache =  (java.security.cert.X509Certificate) java.security.cert.CertificateFactory
			.getInstance("X.509")
			.generateCertificate(new ByteArrayInputStream(Base64.decode(this.encoded)));

		// extract any possible issuer
		if( m.find() )
		{
			m.reset();
			this.issuer = new Certificate(m.replaceFirst(""));
		}
		
		// then check the issuer
		if( issuer != null )
			this.x509().verify(issuer.x509().getPublicKey());
	}
	
	public String toString()
	{
		return Crypto.certificateStart + this.encoded + Crypto.certificateEnd + (this.issuer != null ? "\n" + issuer.toString() : "");
	}
	
	public java.security.cert.X509Certificate x509()
	{
		return cache;
	}
	
	public Certificate getIssuer()
	{
		return this.issuer;
	}
	
	public void setIssuer(Certificate cert) throws Exception
	{
		// check that the issuer has signed this certificate
		this.x509().verify(cert.x509().getPublicKey());
		
		this.issuer = cert;
	}
	
	public static boolean isCertificate(String c)
	{
		Matcher m = Pattern.compile(Crypto.certificateStart.replaceAll("\\n", "\\\\s*") + Crypto.base64Pattern + Crypto.certificateEnd.replaceAll("\\n", "\\\\s*"), Pattern.DOTALL).matcher(c);
		return m.find();
	}
}