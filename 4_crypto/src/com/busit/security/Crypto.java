package com.busit.security;

import sun.security.x509.*;
import sun.security.rsa.*;
import sun.security.util.*;

import java.security.cert.*;
import java.security.interfaces.*;
import java.security.*;
import java.math.BigInteger;
import java.util.Date;
import java.io.*;
import com.anotherservice.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.regex.*;
import java.util.*;
import java.text.*;

public class Crypto
{
	public static String authorityInfoAccessURI = "http://api.busit.com/busit/identity/certificate?binary=1&id=";
	public static String CRLDistributionPointURI = "http://api.busit.com/busit/identity/crl";
	
	public static int defaultLeaseTime = 365 * 5;
	public static final String base64Pattern = "([a-zA-Z0-9\\+/=\\s]+)";
	public static final String encryptedPattern = "([a-zA-Z0-9\\+/=\\s\\-]+)";
	public static final int base64Split = 64;

	public static final String signatureStart =		"\n-----BEGIN SIGNATURE-----\n";
	public static final String signatureEnd = 		"\n-----END SIGNATURE-----\n";
	public static final String principalStart =		"\n-----BEGIN PRINCIPAL-----\n";
	public static final String principalEnd = 		"\n-----END PRINCIPAL-----\n";
	public static final String certificateStart =	"\n-----BEGIN CERTIFICATE-----\n";
	public static final String certificateEnd = 	"\n-----END CERTIFICATE-----\n";
	public static final String publicKeyStart =		"\n-----BEGIN PUBLIC KEY-----\n";
	public static final String publicKeyEnd = 		"\n-----END PUBLIC KEY-----\n";
	public static final String privateKeyStart =	"\n-----BEGIN PRIVATE KEY-----\n";
	public static final String privateKeyEnd = 		"\n-----END PRIVATE KEY-----\n";
	public static final String symmetricKeyStart =	"\n-----BEGIN SYMMETRIC KEY-----\n";
	public static final String symmetricKeyEnd = 	"\n-----END SYMMETRIC KEY-----\n";
	public static final String rsaEncryptionStart =	"\n-----BEGIN RSA ENCRYPTION-----\n";
	public static final String rsaEncryptionEnd = 	"\n-----END RSA ENCRYPTION-----\n";
	public static final String aesEncryptionStart =	"\n-----BEGIN AES ENCRYPTION-----\n";
	public static final String aesEncryptionEnd = 	"\n-----END AES ENCRYPTION-----\n";
	public static final String crlStart = 			"\n-----BEGIN X509 CRL-----\n";
	public static final String crlEnd = 			"\n-----END X509 CRL-----\n";
	
	public static final String messageStart =		"\n-----BEGIN MESSAGE-----\n";
	public static final String messageEnd = 		"\n-----END MESSAGE-----\n";
	public static final String contentStart =		"\n-----BEGIN CONTENT-----\n";
	public static final String contentEnd = 		"\n-----END CONTENT-----\n";
	public static final String attachmentStart = 	"\n-----BEGIN ATTACHMENT-----\n";
	public static final String attachmentEnd = 		"\n-----END ATTACHMENT-----\n";
	public static final String filenameStart = 		"\n-----BEGIN FILENAME-----\n";
	public static final String filenameEnd = 		"\n-----END FILENAME-----\n";
	
	public static final Pattern messagePattern = Pattern.compile(
					Crypto.messageStart.replaceAll("\\n", "\\\\s*") + 
					Crypto.base64Pattern + 
					Crypto.messageEnd.replaceAll("\\n", "\\\\s*"), 
					Pattern.DOTALL);
	public static final Pattern contentPattern = Pattern.compile(
					Crypto.contentStart.replaceAll("\\n", "\\\\s*") + 
					Crypto.base64Pattern + 
					Crypto.contentEnd.replaceAll("\\n", "\\\\s*"), 
					Pattern.DOTALL);
	public static final Pattern attachmentPattern = Pattern.compile(
					Crypto.attachmentStart.replaceAll("\\n", "\\\\s*") + 
					Crypto.filenameStart.replaceAll("\\n", "\\\\s*") + 
					Crypto.base64Pattern + 
					Crypto.filenameEnd.replaceAll("\\n", "\\\\s*") + 
					Crypto.base64Pattern + 
					Crypto.attachmentEnd.replaceAll("\\n", "\\\\s*"), 
					Pattern.DOTALL);
	
	// =========================================
	// EXTRACT PARTS FROM MESSAGE
	// =========================================
	public static String extractRsaEncryptedPart(String text) throws Exception
	{
		// get the asymmetric-encrypted symmetric key
		Matcher m = Pattern.compile("(" + rsaEncryptionStart.replaceAll("\\n", "\\\\s*") + base64Pattern + rsaEncryptionEnd.replaceAll("\\n", "\\\\s*") + ")", Pattern.DOTALL).matcher(text);
		if( !m.find() )
			throw new Exception("Could not extract the rsa encrypted symmetric key from the message");
		return m.group(1);
	}
	
	public static String extractAesEncryptedPart(String text) throws Exception
	{
		// get the symmetric-encrypted signed-message
		Matcher m = Pattern.compile("(" + aesEncryptionStart.replaceAll("\\n", "\\\\s*") + base64Pattern + aesEncryptionEnd.replaceAll("\\n", "\\\\s*") + ")", Pattern.DOTALL).matcher(text);
		if( !m.find() )
			throw new Exception("Could not extract the aes encrypted signed message from the message");
		return m.group(1);
	}
	
	public static String extractMessagePart(String text) throws Exception
	{
		// get the message
		Matcher m = Pattern.compile("(" + messageStart.replaceAll("\\n", "\\\\s*") + base64Pattern + messageEnd.replaceAll("\\n", "\\\\s*") + ")", Pattern.DOTALL).matcher(text);
		if( !m.find() )
			throw new Exception("Could not extract the message content from the message");
		return m.group(1);
	}
	
	public static String excludeSignaturePart(String text) throws Exception
	{
		// exclude the signature component
		return text.replaceAll("(?s)(" + signatureStart + base64Pattern + signatureEnd + ")", "");
	}
	
	public static String extractSignaturePart(String text) throws Exception
	{
		// get the signature
		Matcher m = Pattern.compile("(" + signatureStart.replaceAll("\\n", "\\\\s*") + base64Pattern + signatureEnd.replaceAll("\\n", "\\\\s*") + ")", Pattern.DOTALL).matcher(text);
		if( !m.find() )
			throw new Exception("Could not extract the signature from the message");
		return m.group(1);
	}
	
	public static String extractPrincipalPart(String text) throws Exception
	{
		// get the origin principal from the decrypted message
		Matcher m = Pattern.compile("(" + principalStart.replaceAll("\\n", "\\\\s*") + base64Pattern + principalEnd.replaceAll("\\n", "\\\\s*") + ")", Pattern.DOTALL).matcher(text);
		if( !m.find() )
			throw new Exception("Could not extract the origin principal from the message");
		return m.group(1);
	}
	
	// =========================================
	// PARSE FROM STRING
	// =========================================
	public static PrivateKey parsePrivateKey(String key) throws Exception
	{
		Matcher m = Pattern.compile(privateKeyStart.replaceAll("\\n", "\\\\s*") + base64Pattern + privateKeyEnd.replaceAll("\\n", "\\\\s*"), Pattern.DOTALL).matcher(key);
		if( !m.find() )
			throw new Exception("Unrecognized private key format");
		key = m.group(1);
		
		return RSAPrivateCrtKeyImpl.newKey(Base64.decode(key));
	}
	
	public static PrivateKey parsePrivateKey(byte[] key) throws Exception
	{
		return RSAPrivateCrtKeyImpl.newKey(key);
	}
	
	public static PublicKey parsePublicKey(String key) throws Exception
	{
		Matcher m = Pattern.compile(publicKeyStart.replaceAll("\\n", "\\\\s*") + base64Pattern + publicKeyEnd.replaceAll("\\n", "\\\\s*"), Pattern.DOTALL).matcher(key);
		if( !m.find() )
			throw new Exception("Unrecognized public key format");
		key = m.group(1);
		
		return new RSAPublicKeyImpl(Base64.decode(key));
	}
	
	public static PublicKey parsePublicKey(byte[] key) throws Exception
	{
		return new RSAPublicKeyImpl(key);
	}
	
	public static SecretKey parseSecretKey(String key) throws Exception
	{
		Matcher m = Pattern.compile(symmetricKeyStart.replaceAll("\\n", "\\\\s*") + base64Pattern + symmetricKeyEnd.replaceAll("\\n", "\\\\s*"), Pattern.DOTALL).matcher(key);
		if( !m.find() )
			throw new Exception("Unrecognized symmetric key format : " + key);
		key = m.group(1);
		
		return new SecretKeySpec(Base64.decode(key), "AES");
	}
	
	public static SecretKey parseSecretKey(byte[] key) throws Exception
	{
		return new SecretKeySpec(key, "AES");
	}
	
	public static com.busit.security.Certificate parseCertificate(String cert) throws Exception
	{
		return new com.busit.security.Certificate(cert);
	}
	
	public static byte[] parseMessage(String message) throws Exception
	{
		Matcher m = Pattern.compile(messageStart.replaceAll("\\n", "\\\\s*") + base64Pattern + messageEnd.replaceAll("\\n", "\\\\s*"), Pattern.DOTALL).matcher(message);
		if( !m.find() )
			throw new Exception("Unrecognized message format");
		message = m.group(1);
		
		return Base64.decode(message);
	}
	
	public static Principal parsePrincipal(String principal) throws Exception
	{
		Matcher m = Pattern.compile(principalStart.replaceAll("\\n", "\\\\s*") + base64Pattern + principalEnd.replaceAll("\\n", "\\\\s*"), Pattern.DOTALL).matcher(principal);
		if( !m.find() )
			return PrincipalUtil.parse(new String(Base64.decode(principal)));
		principal = m.group(1);
		
		return PrincipalUtil.parse(new String(Base64.decode(principal)));
	}
	
	// =========================================
	// ENCODE TO STRING
	// =========================================
	public static String toString(PrivateKey key) throws Exception
	{
		return privateKeyStart + new String(Base64.encode(key.getEncoded(), base64Split)) + privateKeyEnd;
	}
	
	public static String toString(PublicKey key) throws Exception
	{
		return publicKeyStart + new String(Base64.encode(key.getEncoded(), base64Split)) + publicKeyEnd;
	}
	
	public static String toString(SecretKey key) throws Exception
	{
		return symmetricKeyStart + new String(Base64.encode(key.getEncoded(), base64Split)) + symmetricKeyEnd;
	}
	
	public static String toString(com.busit.security.Certificate cert) throws Exception
	{
		return cert.toString();
	}
	
	public static String toString(X509CRL crl) throws Exception
	{
		return crlStart + new String(Base64.encode(crl.getEncoded(), base64Split)) + crlEnd;
	}
	
	public static String toString(byte[] message) throws Exception
	{
		return messageStart + new String(Base64.encode(message, base64Split)) + messageEnd;
	}
	
	public static String toString(Principal p) throws Exception
	{
		return principalStart + new String(Base64.encode(p.getName(), base64Split)) + principalEnd;
	}
	
	// =========================================
	// GENERATION
	// =========================================
	public static SecretKey generateSecretKey() throws Exception
	{
		KeyGenerator keygen = KeyGenerator.getInstance("AES");
        keygen.init(128);
        return keygen.generateKey();
	}
	
	public static KeyPair generateKeys() throws Exception
	{
		KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
		keygen.initialize(2048);
		return keygen.genKeyPair();
	}
	
	public static long generateExpireTimestamp(String cert) throws Exception
	{
		return generateExpireTimestamp(parseCertificate(cert));
	}
	
	public static long generateExpireTimestamp(com.busit.security.Certificate cert) throws Exception
	{
		return cert.x509().getNotAfter().getTime() / 1000;
	}
	
	public static Principal generatePrincipal(String name, String org) throws Exception
	{
		return PrincipalUtil.parse(
			"CN=" + name
				.replaceAll(",",	"\\\\,")
				.replaceAll("\\+",	"\\\\+")
				.replaceAll("\"",	"\\\\\"")
				.replaceAll("\\\\",	"\\\\\\\\")
				.replaceAll("<",	"\\\\<")
				.replaceAll(">",	"\\\\>")
				.replaceAll(";", 	"\\\\;") + 
			(org != null && org.length() > 0 ? ",OU=" + org
				.replaceAll(",",	"\\\\,")
				.replaceAll("\\+",	"\\\\+")
				.replaceAll("\"",	"\\\\\"")
				.replaceAll("\\\\",	"\\\\\\\\")
				.replaceAll("<",	"\\\\<")
				.replaceAll(">",	"\\\\>")
				.replaceAll(";", 	"\\\\;") : "") + 
			",O=" + PrincipalUtil.shortName(IdentityStore.getInstance().getAuthority().getSubjectPrincipal())
				.replaceAll(",",	"\\\\,")
				.replaceAll("\\+",	"\\\\+")
				.replaceAll("\"",	"\\\\\"")
				.replaceAll("\\\\",	"\\\\\\\\")
				.replaceAll("<",	"\\\\<")
				.replaceAll(">",	"\\\\>")
				.replaceAll(";", 	"\\\\;"));
	}
	
	public static Principal generatePrincipal(String cert) throws Exception
	{
		return generatePrincipal(parseCertificate(cert));
	}
	
	public static Principal generatePrincipal(com.busit.security.Certificate cert) throws Exception
	{
		return cert.x509().getIssuerX500Principal();
	}
	
	public static String generateCertificate(Principal owner, String publicKey, Principal issuer, String privateKey, String issuerPublicKey) throws Exception
	{
		return toString(generateCertificate(owner, parsePublicKey(publicKey), issuer, parsePrivateKey(privateKey), parsePublicKey(issuerPublicKey), defaultLeaseTime));
	}
	
	public static com.busit.security.Certificate generateCertificate(Principal owner, PublicKey publicKey, com.busit.security.Certificate issuer, PrivateKey privateKey, int lease) throws Exception
	{
		Principal issuerChain = issuer.x509().getIssuerX500Principal();
		return generateCertificate(owner, publicKey, issuerChain, privateKey, issuer.x509().getPublicKey(), lease);
	}
	
	public static com.busit.security.Certificate generateCertificate(Principal owner, PublicKey publicKey, Identity issuer) throws Exception
	{
		return generateCertificate(owner, publicKey, issuer.getSubjectPrincipal(), issuer.getPrivateKey(), issuer.getPublicKey(), defaultLeaseTime);
	}
	
	public static com.busit.security.Certificate generateCertificate(Principal owner, PublicKey publicKey, Principal issuer, PrivateKey privateKey, PublicKey issuerPublicKey, int lease) throws Exception
	{
		return generateCertificate(owner, publicKey, issuer, privateKey, issuerPublicKey, new Date(), 
			(lease <= 0 ? new SimpleDateFormat("yyyyMMddHHmmss").parse("20491231235959") : new Date(new Date().getTime() + lease * 86400000l))
		);
	}
	
	public static com.busit.security.Certificate generateCertificate(Principal owner, PublicKey publicKey, Principal issuer, 
		PrivateKey privateKey, PublicKey issuerPublicKey, Date from, Date to) throws Exception
	{
		AlgorithmId algo = new AlgorithmId(AlgorithmId.sha256WithRSAEncryption_oid);
		KeyIdentifier id = new KeyIdentifier(publicKey);
		 
		X509CertInfo info = new X509CertInfo();
		info.set(X509CertInfo.VALIDITY, new CertificateValidity(from, to));
		
		// TODO : find a way to have a unique auto_increment number for "Certificate Serial Number"
		// however, it is only used for CRL to revoke the cert. Certs can only be revoked because of security breach,
		// then it means the private key is compromised, but this cert or another have the same private key
		// so instead it should use the "subject key identifier" to revoke the KEY (not the cert) !
		// --> for now we use the same : cert id = key id
		//info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(new BigInteger(64, new SecureRandom())));
		info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(new BigInteger(id.getIdentifier()))); 
		
		info.set(X509CertInfo.SUBJECT, new CertificateSubjectName(new X500Name(owner.getName())));
		info.set(X509CertInfo.ISSUER, new CertificateIssuerName(new X500Name(issuer.getName())));
		info.set(X509CertInfo.KEY, new CertificateX509Key(publicKey));
		info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
		info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));
		info.set(X509CertInfo.ALGORITHM_ID + "." + CertificateAlgorithmId.ALGORITHM, algo);
		
		CertificateExtensions ext = new CertificateExtensions();
		boolean isCA = to.equals(new SimpleDateFormat("yyyyMMddHHmmss").parse("20491231235959"));
		
		// =================
		// Basic Constraints
		// =================
		if( isCA )
			ext.set(BasicConstraintsExtension.NAME, new BasicConstraintsExtension(true, true, -1));
		
		// ============================
		// Authority Information Access
		// ============================
		if( !isCA )
		{
			List<AccessDescription> ad = new ArrayList<AccessDescription>();
			ad.add(new AccessDescription(new ObjectIdentifier("1.3.6.1.5.5.7.48.2"), new GeneralName(new URIName(authorityInfoAccessURI + java.net.URLEncoder.encode(issuer.toString(), "UTF-8")))));
			ext.set(AuthorityInfoAccessExtension.NAME, new AuthorityInfoAccessExtension(ad));
		}
		
		// =======================
		// CRL Distribution Points
		// =======================
		if( !isCA )
		{
			List<DistributionPoint> dp = new ArrayList<DistributionPoint>();
			GeneralNames gn_dpn = new GeneralNames();
			gn_dpn.add(new GeneralName(new URIName(CRLDistributionPointURI)));
			GeneralNames gn_ci = new GeneralNames();
			gn_ci.add(new GeneralName(new X500Name(IdentityStore.getInstance().getAuthority().getSubjectPrincipal().getName())));
			dp.add(new DistributionPoint(gn_dpn, null, gn_ci));
			ext.set(CRLDistributionPointsExtension.NAME, new CRLDistributionPointsExtension(dp));
		}
		
		// ======================
		// Subject Key Identifier
		// ======================
		ext.set(SubjectKeyIdentifierExtension.NAME, new SubjectKeyIdentifierExtension(id.getIdentifier()));
		
		// ========================
		// Authority Key Identifier
		// ========================
		if( !isCA )
		{
			GeneralNames gn_aci = new GeneralNames();
			gn_aci.add(new GeneralName(new X500Name(issuer.toString())));
			KeyIdentifier iid = new KeyIdentifier(issuerPublicKey);
			SerialNumber acsn = new SerialNumber(new BigInteger(iid.getIdentifier()));
			ext.set(AuthorityKeyIdentifierExtension.NAME, new AuthorityKeyIdentifierExtension(iid, gn_aci, acsn));
		}
		
		// ====================
		// Certificate Policies
		// ====================
		// set the policy to "anyPolicy" with special OID 2.5.29.32.0
		List<PolicyInformation> pi = new ArrayList<PolicyInformation>();
		pi.add(new PolicyInformation(new CertificatePolicyId(new ObjectIdentifier("2.5.29.32.0")), new LinkedHashSet<PolicyQualifierInfo>()));
		ext.set(CertificatePoliciesExtension.NAME, new CertificatePoliciesExtension(pi));
		
		info.set(X509CertInfo.EXTENSIONS, ext);
		
		X509CertImpl cert = new X509CertImpl(info);
		cert.sign(privateKey, "SHA256withRSA");
		return new com.busit.security.Certificate(certificateStart + new String(Base64.encode(cert.getEncoded(), base64Split)) + certificateEnd);
	}
	
	public static X509CRL generateCRL(Map<com.busit.security.Certificate, Long> revoked) throws Exception
	{
		return generateCRL(revoked, IdentityStore.getInstance().getAuthority());
	}
	
	public static X509CRL generateCRL(Map<com.busit.security.Certificate, Long> revoked, Identity issuer) throws Exception
	{
		return generateCRL(revoked, issuer.getSubjectPrincipal(), issuer.getPublicKey(), issuer.getPrivateKey());
	}
	
	public static X509CRL generateCRL(Map<com.busit.security.Certificate, Long> revoked, Principal issuer, PublicKey publicKey, PrivateKey privateKey) throws Exception
	{
		Date now = new Date();
		Date next = new Date(now.getTime() + (24 * 60 * 60 * 1000L));
		
		CRLExtensions ext = new CRLExtensions();
		
		// ========================
		// Authority Key Identifier
		// ========================
		GeneralNames gn_aci = new GeneralNames();
		gn_aci.add(new GeneralName(new X500Name(issuer.toString())));
		KeyIdentifier iid = new KeyIdentifier(publicKey);
		SerialNumber acsn = new SerialNumber(new BigInteger(iid.getIdentifier()));
		ext.set(AuthorityKeyIdentifierExtension.NAME, new AuthorityKeyIdentifierExtension(iid, gn_aci, acsn));
		
		// ==========
		// CRL Number
		// ==========
		ext.set(CRLNumberExtension.NAME, new CRLNumberExtension(BigInteger.valueOf(now.getTime())));
		
		// ============================
		// Authority Information Access
		// ============================
		List<AccessDescription> ad = new ArrayList<AccessDescription>();
		ad.add(new AccessDescription(new ObjectIdentifier("1.3.6.1.5.5.7.48.2"), new GeneralName(new URIName(authorityInfoAccessURI + java.net.URLEncoder.encode(issuer.toString(), "UTF-8")))));
		ext.set(AuthorityInfoAccessExtension.NAME, new AuthorityInfoAccessExtension(ad));
		
		// ====================
		// Revoked Certificates
		// ====================
		X509CRLEntry[] list = new X509CRLEntry[revoked.size()];
		int i = 0;
		for( com.busit.security.Certificate c : revoked.keySet() )
			list[i++] = new X509CRLEntryImpl(new BigInteger(new KeyIdentifier(c.x509().getPublicKey()).getIdentifier()), new Date(revoked.get(c)));
		
		X509CRLImpl crl = new X509CRLImpl(new X500Name(issuer.toString()), now, next, list, ext);
		crl.sign(privateKey, "SHA256withRSA");
		
		return crl;
	}
	
	// =========================================
	// SIGN
	// =========================================
	public static String sign(String privateKey, String text) throws Exception
	{
		return sign(parsePrivateKey(privateKey), Hex.getBytes(text));
	}
	
	public static String sign(String privateKey, byte[] text) throws Exception
	{
		return sign(parsePrivateKey(privateKey), text);
	}
	
	public static String sign(PrivateKey key, String text) throws Exception
	{
		return sign(key, Hex.getBytes(text));
	}
	
	public static String sign(PrivateKey key, byte[] text) throws Exception
	{
		Signature s = Signature.getInstance("SHA256withRSA");
		s.initSign(key);
		s.update(text);
		return signatureStart + new String(Base64.encode(s.sign(), base64Split)) + signatureEnd;
	}
	
	public static boolean verify(String publicKey, String text, String signature) throws Exception
	{
		return verify(parsePublicKey(publicKey), Hex.getBytes(text), signature);
	}
	
	public static boolean verify(String publicKey, byte[] text, String signature) throws Exception
	{
		return verify(parsePublicKey(publicKey), text, signature);
	}
	
	public static boolean verify(PublicKey key, String text, String signature) throws Exception
	{
		return verify(key, Hex.getBytes(text), signature);
	}
	
	public static boolean verify(PublicKey key, byte[] text, String signature) throws Exception
	{
		Matcher m = Pattern.compile(signatureStart.replaceAll("\\n", "\\\\s*") + base64Pattern + signatureEnd.replaceAll("\\n", "\\\\s*"), Pattern.DOTALL).matcher(signature);
		if( !m.find() )
			throw new Exception("Unrecognized signature format");
		signature = m.group(1);
		
		Signature s = Signature.getInstance("SHA256withRSA");
		s.initVerify(key);
		s.update(text);
		return s.verify(Base64.decode(signature));
	}
	
	// =========================================
	// ASSYMMETRIC EN/DECRYPT
	// =========================================
	public static String encryptAsymmetric(String publicKey, String text) throws Exception
	{
		return encryptAsymmetric(parsePublicKey(publicKey), Hex.getBytes(text));
	}
	
	public static String encryptAsymmetric(String publicKey, byte[] text) throws Exception
	{
		return encryptAsymmetric(parsePublicKey(publicKey), text);
	}
	
	public static String encryptAsymmetric(PublicKey key, String text) throws Exception
	{
		return encryptAsymmetric(key, Hex.getBytes(text));
	}
	
	public static String encryptAsymmetric(PublicKey key, byte[] text) throws Exception
	{
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, key);

		return rsaEncryptionStart + new String(Base64.encode(cipher.doFinal(text), base64Split)) + rsaEncryptionEnd;
	}
	
	public static String decryptAsymmetric(String privateKey, String text) throws Exception
	{
		return decryptAsymmetric(parsePrivateKey(privateKey), text);
	}
	
	public static String decryptAsymmetric(PrivateKey key, String text) throws Exception
	{
		Matcher m = Pattern.compile(rsaEncryptionStart.replaceAll("\\n", "\\\\s*") + base64Pattern + rsaEncryptionEnd.replaceAll("\\n", "\\\\s*"), Pattern.DOTALL).matcher(text);
		if( !m.find() )
			throw new Exception("Unrecognized rsa encryption format");
		text = m.group(1);
		
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.DECRYPT_MODE, key);

		return new String(cipher.doFinal(Base64.decode(text)));
	}
	
	// =========================================
	// SYMMETRIC EN/DECRYPT
	// =========================================
	public static String encryptSymmetric(String symmetricKey, String text) throws Exception
	{
		return encryptSymmetric(parseSecretKey(symmetricKey), Hex.getBytes(text));
	}
	
	public static String encryptSymmetric(String symmetricKey, byte[] text) throws Exception
	{
		return encryptSymmetric(parseSecretKey(symmetricKey), text);
	}
	
	public static String encryptSymmetric(SecretKey key, String text) throws Exception
	{
		return encryptSymmetric(key, Hex.getBytes(text));
	}
	
	public static String encryptSymmetric(SecretKey key, byte[] text) throws Exception
	{
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		// cannot use our key, so we have to convert it :'(
		
		cipher.init(Cipher.ENCRYPT_MODE, key);

		return aesEncryptionStart + new String(Base64.encode(cipher.doFinal(text), base64Split)) + aesEncryptionEnd;
	}
	
	public static String decryptSymmetric(String symmetricKey, String text) throws Exception
	{
		return decryptSymmetric(parseSecretKey(symmetricKey), text);
	}
	
	public static String decryptSymmetric(SecretKey key, String text) throws Exception
	{
		Matcher m = Pattern.compile(aesEncryptionStart.replaceAll("\\n", "\\\\s*") + base64Pattern + aesEncryptionEnd.replaceAll("\\n", "\\\\s*"), Pattern.DOTALL).matcher(text);
		if( !m.find() )
			throw new Exception("Unrecognized aes encryption format");
		text = m.group(1);
		
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, key);

		return new String(cipher.doFinal(Base64.decode(text)));
	}
	
	// =========================================
	// SIGN+SYMMETRIC+ASYMMETRIC EN/DECRYPT
	// =========================================
	public static String encrypt(Identity origin, Identity destination, String text) throws Exception
	{
		return encrypt(destination.getPublicKey(), destination.getSubjectPrincipal(), origin.getPrivateKey(), origin.getSubjectPrincipal(), Hex.getBytes(text));
	}
	
	public static String encrypt(Identity origin, Identity destination, byte[] text) throws Exception
	{
		return encrypt(destination.getPublicKey(), destination.getSubjectPrincipal(), origin.getPrivateKey(), origin.getSubjectPrincipal(), text);
	}
	
	public static String encrypt(String recipientPublicKey, String recipientPrincipal, String originPrivateKey, String originPrincipal, String text) throws Exception
	{
		return encrypt(parsePublicKey(recipientPublicKey), parsePrincipal(recipientPrincipal), parsePrivateKey(originPrivateKey), parsePrincipal(originPrincipal), Hex.getBytes(text));
	}
	
	public static String encrypt(PublicKey recipientKey, Principal recipientPrincipal, PrivateKey originKey, Principal originPrincipal, byte[] text) throws Exception
	{
		/*
		RFC 4880 (2.1)
   Both digital signature and confidentiality services may be applied to
   the same message.  First, a signature is generated for the message
   and attached to the message.  Then the message plus signature is
   encrypted using a symmetric session key.  Finally, the session key is
   encrypted using public-key encryption and prefixed to the encrypted
   block.
   
		NOTE : we deviate from RFC in out implementation but we preserve the concept
		*/

		// 1) sign the message with the origin's private key
		String signature = sign(originKey, text);
		
		// 2) append the signature to the message (and convert message to base64)
		// in order to identify WHO is the origin, we include the principal also
		// CAUTION : the principal sould match the SENDER's private key (originKey)
		String signedText = toString(text) + signature + toString(originPrincipal);
		
		// 3) generate symmetric key
		SecretKey s = generateSecretKey();
		
		// 4) encode signed-message with symmetric key
		String encryptedSignedText = encryptSymmetric(s, signedText);
		
		// 5) crypt symmetric key with the recipient's public key
		String encryptedKey = encryptAsymmetric(recipientKey, toString(s));
		
		// 6) in order to identify WHO is the recipient, we include the principal also
		// CAUTION : the principal sould match the RECIPIENT's public key (recipientKey)
		encryptedKey += toString(recipientPrincipal);
		
		// 7) prepend the asymmetric-encrypted symmetric key to the symmetric-encrypted signed-message
		return encryptedKey + encryptedSignedText;
	}
	
	public static byte[] decrypt(Identity destination, String text) throws Exception
	{
		return decrypt(destination.getPrivateKey(), text);
	}
	
	public static byte[] decrypt(String recipientPrivateKey, String text) throws Exception
	{
		return decrypt(parsePrivateKey(recipientPrivateKey), text);
	}
	
	public static byte[] decrypt(PrivateKey recipientKey, String text) throws Exception
	{
		// 1) get the asymmetric-encrypted symmetric key
		String rsaSecretKey = extractRsaEncryptedPart(text);
		
		// 2) get the symmetric-encrypted signed-message
		String aesSignedMessage = extractAesEncryptedPart(text);
		
		// 3) decrypt the symmetric key using the recipient's private key
		SecretKey symmetricKey = parseSecretKey(decryptAsymmetric(recipientKey, rsaSecretKey));
		
		// 4) decrypt the signed-message with the symmetric key
		String signedMessage = decryptSymmetric(symmetricKey, aesSignedMessage);
		
		// 5) get the message
		byte[] message = parseMessage(extractMessagePart(signedMessage));
		
		// 6) get the signature
		String signature = extractSignaturePart(signedMessage);
		
		// 7) get the origin principal from the decrypted message
		Principal originPrincipal = parsePrincipal(extractPrincipalPart(signedMessage));
		
		// 8) retrieve the origin public key using the principal
		PublicKey originKey = IdentityStore.getInstance().getIdentity(originPrincipal).getPublicKey();
		
		// 9) verify the signature of the message using the origin's public key
		if( !verify(originKey, message, signature) )
			throw new Exception("Signature mismatch. The original message may have been altered.");
		
		// 10) return the message
		return message;
	}
	
	// =========================================
	// STEP BY STEP DECRYPT
	// =========================================
	// decrypt the message and return the raw signed message (base64 message + origin principal + signature)
	public static String decrypt_1to4(PrivateKey recipientKey, String text) throws Exception
	{
		// 1) get the asymmetric-encrypted symmetric key
		String rsaSecretKey = extractRsaEncryptedPart(text);
		
		// 2) get the symmetric-encrypted signed-message
		String aesSignedMessage = extractAesEncryptedPart(text);
		
		// 3) decrypt the symmetric key using the recipient's private key
		SecretKey symmetricKey = parseSecretKey(decryptAsymmetric(recipientKey, rsaSecretKey));
		
		// 4) decrypt the signed-message with the symmetric key
		String signedMessage = decryptSymmetric(symmetricKey, aesSignedMessage);
		
		return signedMessage;
	}
	
	// verify the signature and return the original author principal
	public static Principal decrypt_5to9(String signedMessage) throws Exception
	{
		// 5) get the message
		byte[] message = parseMessage(extractMessagePart(signedMessage));
		
		// 6) get the signature
		String signature = extractSignaturePart(signedMessage);
		
		// 7) get the origin principal from the decrypted message
		Principal originPrincipal = parsePrincipal(extractPrincipalPart(signedMessage));
		
		// 8) retrieve the origin public key using the principal
		PublicKey originKey = IdentityStore.getInstance().getIdentity(originPrincipal).getPublicKey();
		
		// 9) verify the signature of the message using the origin's public key
		if( !verify(originKey, message, signature) )
			throw new Exception("Signature mismatch. The original message may have been altered.");
			
		return originPrincipal;
	}
	
	// =========================================
	// STEP BY STEP ENCRYPT
	// =========================================
	public static String encrypt_1to2(PrivateKey originKey, Principal originPrincipal, byte[] text) throws Exception
	{
		// 1) sign the message with the origin's private key
		String signature = sign(originKey, text);
		
		// 2) append the signature to the message (and convert message to base64)
		// in order to identify WHO is the origin, we include the principal also
		// CAUTION : the principal sould match the SENDER's private key (originKey)
		String signedText = toString(text) + signature + toString(originPrincipal);
		
		return signedText;
	}
	
	public static String encrypt_3to7(PublicKey recipientKey, Principal recipientPrincipal, String signedText) throws Exception
	{
		// 3) generate symmetric key
		SecretKey s = generateSecretKey();
		
		// 4) encode signed-message with symmetric key
		String encryptedSignedText = encryptSymmetric(s, signedText);
		
		// 5) crypt symmetric key with the recipient's public key
		String encryptedKey = encryptAsymmetric(recipientKey, toString(s));
		
		// 6) in order to identify WHO is the recipient, we include the principal also
		// CAUTION : the principal sould match the RECIPIENT's public key (recipientKey)
		encryptedKey += toString(recipientPrincipal);
		
		// 7) prepend the asymmetric-encrypted symmetric key to the symmetric-encrypted signed-message
		return encryptedKey + encryptedSignedText;
	}
}