import com.busit.security.*;
import java.security.*;
import com.anotherservice.util.*;
import com.anotherservice.io.*;
import java.lang.*;
import java.util.*;
import java.io.ByteArrayInputStream;
import javax.security.auth.x500.X500Principal;

public class generate_everyone_keys
{
	public static void main(String[] args)
	{
		try
		{
			//KeyPair pair = Crypto.generateKeys();
			PublicKey pub = Crypto.parseCertificate(FileReader.readFile("5_busit/res/everyone.crt")).x509().getPublicKey();//pair.getPublic();
			PrivateKey priv = Crypto.parsePrivateKey(FileReader.readFile("5_busit/res/everyone.private"));//pair.getPrivate();
			Principal princ = new X500Principal("CN=Busit Unchecked,OU=Busit Unchecked,O=Busit");
			PrivateKey authorityPrivateKey = Crypto.parsePrivateKey(FileReader.readFile("5_busit/res/busit.private"));
			com.busit.security.Certificate certif = Crypto.parseCertificate(FileReader.readFile("5_busit/res/busit.crt"));
			
			com.busit.security.Certificate cert = Crypto.generateCertificate(princ, pub, certif.x509().getSubjectX500Principal(), authorityPrivateKey, certif.x509().getPublicKey(), -1);
			cert.setIssuer(certif);
			
			//FileWriter.writeFile("everyone.public", Crypto.toString(pub));
			//FileWriter.writeFile("everyone.private", Crypto.toString(priv));
			FileWriter.writeFile("everyone.crt", Crypto.toString(cert));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}