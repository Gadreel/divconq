/* ************************************************************************
#
#  DivConq
#
#  http://divconq.com/
#
#  Copyright:
#    Copyright 2014 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package divconq.util;

import java.security.InvalidKeyException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import divconq.lang.chars.Utf8Decoder;
import divconq.lang.chars.Utf8Encoder;
import divconq.log.Logger;
import divconq.xml.XElement;

/**
 * This is the default settings obfuscator, see ISettingsObfuscator for hints on 
 * how to build your own.  If a customer obfuscator is not provided then all
 * encrypted configuration settings use this implementation.
 * 
 *  This is good enough to keep the casual hacker at bay.  The encryption key is
 *  created by a combination of settings in config.xml and a hard coded default salt.
 *  So to break an encryption the hacker needs both the code and the config file.
 * 
 * @author Andy
 * 
 * TODO consider a bcrypt implementation http://www.mindrot.org/projects/jBCrypt/
 *
 */
public class BasicSettingsObfuscator implements ISettingsObfuscator {
	public static final byte[] DEFAULT_SALT = {
		(byte)201, (byte) 15, (byte)218, (byte)162, (byte) 33, (byte)104, (byte)194, (byte) 52,
		(byte)196, (byte)198, (byte) 98, (byte)139, (byte)128, (byte)220, (byte) 28, (byte)209,
		(byte) 41, (byte)  2, (byte) 78, (byte)  8, (byte)138, (byte)103, (byte)204, (byte)116,
		(byte)  2, (byte) 11, (byte)190, (byte)166, (byte) 59, (byte) 19, (byte)155, (byte) 34,
		(byte) 81, (byte) 74, (byte)  8, (byte)121, (byte)142, (byte) 52, (byte)  4, (byte)221,
		(byte)239, (byte)149, (byte) 25, (byte)179, (byte)205, (byte) 58, (byte) 67, (byte) 27,
		(byte) 48, (byte) 43, (byte) 10, (byte)109, (byte)242, (byte) 95, (byte) 20, (byte) 55,
		(byte) 79, (byte)225, (byte) 53, (byte)109, (byte)109, (byte) 81, (byte)194, (byte) 69
	};
	
	protected byte[] masterkey = null;
	protected SecretKeySpec aeskey = null;
	protected SecretKeySpec hmackey = null;
	
	@Override
	public void init(XElement config) {
		String salt1 = null;
		String salt2 = null;
		
		if (config != null) {
			salt1 = config.getAttribute("Id");
			salt2 = config.getAttribute("Feed");
		}
		
		byte[] skey = new byte[128];
		
		if (StringUtil.isEmpty(salt1)) 
			salt1 = "48656c6c6f";
		else if (salt1.length() > 128)
			salt1 = salt1.substring(salt1.length() - 128);
	
		byte[] bsalt1 = HexUtil.decodeHex(salt1);
		
		if (bsalt1 == null)
			bsalt1 = DEFAULT_SALT;
		
		ArrayUtil.blockCopy(bsalt1, 0, skey, 128 - bsalt1.length, bsalt1.length);
		
		if (bsalt1.length < 64) 
			ArrayUtil.blockCopy(DEFAULT_SALT, 0, skey, 64, 64 - bsalt1.length);
		
		if (StringUtil.isEmpty(salt2)) 
			salt2 = "576f726c64";
		else if (salt2.length() > 128)
			salt2 = salt2.substring(salt2.length() - 128);
		
		byte[] bsalt2 = HexUtil.decodeHex(salt2);
		
		if (bsalt2 == null)
			bsalt2 = DEFAULT_SALT;
		
		ArrayUtil.blockCopy(bsalt2, 0, skey, 0, bsalt2.length);
		
		if (bsalt2.length < 64) 
			ArrayUtil.blockCopy(DEFAULT_SALT, bsalt2.length, skey, bsalt2.length, 64 - bsalt2.length);
		
		this.masterkey = skey;
		
		byte[] akey = new byte[16];
		ArrayUtil.blockCopy(skey, bsalt2.length - 10, akey, 0, 16);
		
		// TODO confirm we are ending up with a 4096 bit encryption
		this.aeskey = new SecretKeySpec(akey, "AES");
		this.hmackey = new SecretKeySpec(skey, "hmacSHA512");
	}

	@Override
	public void configure(XElement config) {
		byte[] idbuff = new byte[64];
		byte[] feedbuff = new byte[64];
		
		SecureRandom rnd = new SecureRandom();
		rnd.nextBytes(idbuff);
		rnd.nextBytes(feedbuff);
		
		config.setAttribute("Id", HexUtil.bufferToHex(idbuff));
		config.setAttribute("Feed", HexUtil.bufferToHex(feedbuff));
	}

	@Override
	public String decryptHexToString(CharSequence v) {
		return this.decryptString(HexUtil.decodeHex(v));
	}

	@Override
	public String decryptString(byte[] v) {
		if (v == null)
			return null;
		
		try {
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.DECRYPT_MODE, this.aeskey);

			//System.out.println("a1: " + c.getAlgorithm());
			//System.out.println("a2: " + c.getBlockSize());
			
			return Utf8Decoder.decode(c.doFinal(v)).toString();			
		}
		catch(InvalidKeyException x) {
			Logger.warn("Invalid settings key: " + x, "Code", "202");
		}
		catch(Exception x) {
			Logger.info("Failed decryption: " + x, "Code", "203");
		}
		
		return null;
	}

	@Override
	public String encryptStringToHex(CharSequence v) {
    	return HexUtil.bufferToHex(this.encryptString(v));
	}

	@Override
	public byte[] encryptString(CharSequence v) {
		if (StringUtil.isEmpty(v))
			return null;
		
		try {
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.ENCRYPT_MODE, this.aeskey);
			return c.doFinal(Utf8Encoder.encode(v));				
		}
		catch(InvalidKeyException x) {
			Logger.warn("Invalid settings key: " + x, "Code", "204");
		}
		catch(Exception x) {
			Logger.info("Failed decryption: " + x, "Code", "205");
		}
		
		return null;
	}

	@Override
	public Cipher encryptCipher() {
		try {
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.ENCRYPT_MODE, this.aeskey);
			return c;				
		}
		catch(InvalidKeyException x) {
			Logger.warn("Invalid settings key: " + x, "Code", "204");
		}
		catch(Exception x) {
			Logger.info("Failed encryption init: " + x, "Code", "205");
		}
		
		return null;
	}
	
	@Override
	public Cipher decryptCipher() {
		try {
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.DECRYPT_MODE, this.aeskey);
			return c;				
		}
		catch(InvalidKeyException x) {
			Logger.warn("Invalid settings key: " + x, "Code", "204");
		}
		catch(Exception x) {
			Logger.info("Failed encryption init: " + x, "Code", "205");
		}
		
		return null;
	}

	@Override
	public String hashStringToHex(CharSequence v) {
    	return HexUtil.bufferToHex(this.hashString(v));
	}

	@Override
	public byte[] hashString(CharSequence v) {
		if (StringUtil.isEmpty(v))
			return null;
		
		try {
			Mac mac = Mac.getInstance("hmacSHA512");
			mac.init(this.hmackey);			
			return mac.doFinal(Utf8Encoder.encode(v));
		} 
		catch (Exception x) {
			Logger.info("Failed hash: " + x, "Code", "206");
		}	
		
		return null;
	}

	@Override
	public String getHashKey() {
		return HexUtil.bufferToHex(this.masterkey);
	}
}
