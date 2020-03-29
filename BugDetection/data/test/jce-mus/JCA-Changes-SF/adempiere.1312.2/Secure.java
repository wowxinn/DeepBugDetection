package test;

import java.math.*;
import java.security.*;
import java.sql.Timestamp;
import java.util.logging.*;
import javax.crypto.*;
import javax.crypto.Cipher;

public class Secure
{
	String CLEARVALUE_START;
	String CLEARVALUE_END;
	String ENCRYPTEDVALUE_START;
	String ENCRYPTEDVALUE_END;

	public Secure()
	{
		initCipher();
	}	//	Secure

	/** Adempiere Cipher				*/
	private Cipher 			m_cipher = null;
	/** Adempiere Key				*/
	private SecretKey 		m_key = null;
	/** Message Digest				*/
	private MessageDigest	m_md = null;

	/**	Logger						*/
	private static Logger	log	= Logger.getLogger (Secure.class.getName());

	/**
	 *	Decryption.
	 * 	The methods must recognize clear text values
	 *  @param value encrypted value
	 *  @return decrypted String
	 */
	public String decrypt (String value)
	{
		if (value == null || value.length() == 0)
			return value;
		boolean isEncrypted = value.startsWith(ENCRYPTEDVALUE_START) && value.endsWith(ENCRYPTEDVALUE_END);
		if (isEncrypted)
			value = value.substring(ENCRYPTEDVALUE_START.length(), value.length()-ENCRYPTEDVALUE_END.length());
		//	Needs to be hex String
		byte[] data = convertHexString(value);
		if (data == null)	//	cannot decrypt
		{
			if (isEncrypted)
			{
				// log.info("Failed: " + value);
				log.info("Failed");
				return null;
			}
			//	assume not encrypted
			return value;
		}
		//	Init
		if (m_cipher == null)
			initCipher();

		//	Encrypt
		if (m_cipher != null && value != null && value.length() > 0)
		{
			try
			{
				AlgorithmParameters ap = m_cipher.getParameters();
				m_cipher.init(Cipher.DECRYPT_MODE, m_key, ap);
				byte[] out = m_cipher.doFinal(data);
				String retValue = new String(out);
				// globalqss - [ 1577737 ] Security Breach - show database password
				// log.log (Level.ALL, value + " => " + retValue);
				return retValue;
			}
			catch (Exception ex)
			{
				// log.info("Failed: " + value + " - " + ex.toString());
				log.info("Failed decrypting " + ex.toString());
			}
		}
		return null;
	}	//	decrypt
	//	encrypt
}   //  Secure
