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
	 *	Encryption.
	 *  @param value clear value
	 *  @return encrypted String
	 */
	public String encrypt (String value)
	{
		String clearText = value;
		if (clearText == null)
			clearText = "";
		//	Init
		if (m_cipher == null)
			initCipher();
		//	Encrypt
		if (m_cipher != null)
		{
			try
			{
				m_cipher.init(Cipher.ENCRYPT_MODE, m_key);
				byte[] encBytes = m_cipher.doFinal(clearText.getBytes());
				String encString = convertToHexString(encBytes);
				// globalqss - [ 1577737 ] Security Breach - show database password
				// log.log (Level.ALL, value + " => " + encString);
				return encString;
			}
			catch (Exception ex)
			{
				// log.log(Level.INFO, value, ex);
				log.log(Level.INFO, "Problem encrypting string", ex);
			}
		}
		//	Fallback
		return CLEARVALUE_START + value + CLEARVALUE_END;
	}	//	encrypt
	//	encrypt
}   //  Secure
