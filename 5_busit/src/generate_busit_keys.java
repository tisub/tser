import com.busit.security.*;
import java.security.*;
import com.anotherservice.util.*;
import com.anotherservice.io.*;
import java.lang.*;
import java.util.*;
import java.io.ByteArrayInputStream;
import javax.security.auth.x500.X500Principal;
import java.security.cert.X509CRL;

public class generate_busit_keys
{
	public static void main(String[] args)
	{
		try
		{
			//Crypto.authorityInfoAccessURI = "https://api.busit.com/busit/identity/certificate?binary=1&id=";
			//Crypto.CRLDistributionPointURI = "https://crl.busit.com/global.crl";
			//Crypto.CRLIssuerDN = "CN=Bus IT,O=Bus IT";
			
			//KeyPair pair = Crypto.generateKeys();
			PublicKey pub = Crypto.parseCertificate(FileReader.readFile("5_busit/res/busit.crt")).x509().getPublicKey();//pair.getPublic();
			PrivateKey priv = Crypto.parsePrivateKey(FileReader.readFile("5_busit/res/busit.private"));//pair.getPrivate();
			
			Principal princ = new X500Principal("CN=Busit,O=Busit");
			com.busit.security.Certificate cert = Crypto.generateCertificate(princ, pub, princ, priv, pub, -1);
			//X509CRL crl = Crypto.generateCRL(new Hashtable<com.busit.security.Certificate, Long>(), princ, pub, priv);
			
			//FileWriter.writeFile("busit.public", Crypto.toString(pub));
			//FileWriter.writeFile("busit.private", Crypto.toString(priv));
			FileWriter.writeFile("busit.crt", Crypto.toString(cert));
			//FileWriter.writeFile("global.crl", Crypto.toString(crl));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}