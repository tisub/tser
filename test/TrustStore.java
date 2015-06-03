import javax.net.ssl.*;
import java.security.*;
import java.security.cert.*;
import java.io.*;
import java.net.*;

public class TrustStore implements X509TrustManager, X509KeyManager
{
	public static void main(String[] args)
	{
		System.out.println("TrustStore");
	}
	
	public TrustStore()
	{
	}
	
	// ==============================
	// X509KeyManager
	// Instances of this interface manage which X509 certificate-based key pairs are used to authenticate the local side of a secure socket.
	// During secure socket negotiations, implentations call methods in this interface to:
	// 		determine the set of aliases that are available for negotiations based on the criteria presented,
	// 		select the best alias based on the criteria presented, and
	// 		obtain the corresponding key material for given aliases. 
	// ==============================
	
	/**
	 * Choose an alias to authenticate the client side of a secure socket given the public key type 
	 * and the list of certificate issuer authorities recognized by the peer (if any).
	 * Parameters:
	 * 		keyType - the key algorithm type name(s), ordered with the most-preferred key type first.
	 * 		issuers - the list of acceptable CA issuer subject names or null if it does not matter which issuers are used.
	 * 		socket - the socket to be used for this connection. This parameter can be null, which indicates that implementations are free to select an alias applicable to any socket.
	 * Returns:
	 * 		the alias name for the desired key, or null if there are no matches.
	 */
	public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket)
	{
		return null;
	}
	
	/**
	 * Choose an alias to authenticate the server side of a secure socket given the public key type and the 
	 * list of certificate issuer authorities recognized by the peer (if any).
	 * Parameters:
	 * 		keyType - the key algorithm type name.
	 * 		issuers - the list of acceptable CA issuer subject names or null if it does not matter which issuers are used.
	 * 		socket - the socket to be used for this connection. This parameter can be null, which indicates that implementations are free to select an alias applicable to any socket.
	 * Returns:
	 * 		the alias name for the desired key, or null if there are no matches.
	 */
	public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket)
	{
		return null;
	}
	
	/**
	 * Returns the certificate chain associated with the given alias.
	 * Parameters:
	 * 		alias - the alias name
	 * Returns:
	 * 		the certificate chain (ordered with the user's certificate first and the root certificate authority last), or null if the alias can't be found.
	 */
	public X509Certificate[] getCertificateChain(String alias)
	{
		return null;
	}
	
	/**
	 * Get the matching aliases for authenticating the client side of a secure socket given 
	 * the public key type and the list of certificate issuer authorities recognized by the peer (if any).
	 * Parameters:
	 * 		keyType - the key algorithm type name
	 * 		issuers - the list of acceptable CA issuer subject names, or null if it does not matter which issuers are used.
	 * Returns:
	 * 		an array of the matching alias names, or null if there were no matches.
	 */
	public String[] getClientAliases(String keyType, Principal[] issuers)
	{
		return null;
	}
	
	/**
	 * Returns the key associated with the given alias.
	 * Parameters:
	 * 		alias - the alias name
	 * Returns:
	 * 		the requested key, or null if the alias can't be found.
	 */
	public PrivateKey getPrivateKey(String alias)
	{
		return null;
	}
	
	/**
	 * Get the matching aliases for authenticating the server side of a secure socket given the public key type 
	 * and the list of certificate issuer authorities recognized by the peer (if any).
	 * Parameters:
	 * 		keyType - the key algorithm type name
	 * 		issuers - the list of acceptable CA issuer subject names or null if it does not matter which issuers are used.
	 * Returns:
	 * 		an array of the matching alias names, or null if there were no matches.
	 */
	public String[] getServerAliases(String keyType, Principal[] issuers)
	{
		return null;
	}
	
	// ==============================
	// X509TrustManager
	// Instance of this interface manage which X509 certificates may be used to authenticate the remote side of a secure socket. 
	// Decisions may be based on trusted certificate authorities, certificate revocation lists, online status checking or other means.
	// ==============================
	
	/**
	 * Given the partial or complete certificate chain provided by the peer, build a certificate path to a trusted root and 
	 * return if it can be validated and is trusted for client SSL authentication based on the authentication type.
	 * The authentication type is determined by the actual certificate used. For instance, if RSAPublicKey is used, the 
	 * authType should be "RSA". Checking is case-sensitive.
	 * Parameters:
	 * 		chain - the peer certificate chain
	 * 		authType - the authentication type based on the client certificate
	 * Throws:
	 * 		IllegalArgumentException - if null or zero-length chain is passed in for the chain parameter or if null or zero-length string is passed in for the authType parameter
	 * 		CertificateException - if the certificate chain is not trusted by this TrustManager.
	 */
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
	{
	}
	
	/**
	 * Given the partial or complete certificate chain provided by the peer, build a certificate path to a trusted root and 
	 * return if it can be validated and is trusted for server SSL authentication based on the authentication type.
	 * The authentication type is the key exchange algorithm portion of the cipher suites represented as a String, such 
	 * as "RSA", "DHE_DSS". Note: for some exportable cipher suites, the key exchange algorithm is determined at run time 
	 * during the handshake. For instance, for TLS_RSA_EXPORT_WITH_RC4_40_MD5, the authType should be RSA_EXPORT when an 
	 * ephemeral RSA key is used for the key exchange, and RSA when the key from the server certificate is used. Checking is case-sensitive.
	 * Parameters:
	 * 		chain - the peer certificate chain
	 * 		authType - the key exchange algorithm used
	 * Throws:
	 * 		IllegalArgumentException - if null or zero-length chain is passed in for the chain parameter or if null or zero-length string is passed in for the authType parameter
	 * 		CertificateException - if the certificate chain is not trusted by this TrustManager.
	 */
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
	{
	}
	
	/**
	 * Return an array of certificate authority certificates which are trusted for authenticating peers.
	 * Returns:
	 * 		a non-null (possibly empty) array of acceptable CA issuer certificates.
	 */
	public X509Certificate[] getAcceptedIssuers()
	{
		return null;
	}
}