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

import javax.crypto.Cipher;

import divconq.xml.XElement;

/**
 * For protection, certain settings can be obscured (encrypted) in the config file.
 * Applications wanting higher protection will want to use their own home-made
 * settings obfuscator - after all DivConq is open source so anyone reading this
 * source code will know how to decrypt your settings.  Unless you roll your own
 * obsuscator.
 * 
 *  The way to support this is to make your own class implementing ISettingObfuscator.
 *  Place the qualified class name in the "TimerClass" attribute of the "Clock"
 *  element in the config file:
 *  
 *  	<Clock TimerClass="divconq.util.BasicSettingsObfuscator" ... />
 *  
 *  If there is no TimerClass listed then the default - BasicSettingsObfuscator - is used.
 * 
 * The ISettingObfuscator implementation must be able to run in Configure and in other 
 * scenarios where Hub is not fully loaded.  Do not use struct classes, make references
 * to Hub.instance, access locale, scripting or any of the other "major" DivConq features
 * within the implementation.
 * 
 * @author Andy
 *
 */
public interface ISettingsObfuscator {
	/**
	 * Called when Hub or Configure loads the configuration file.  Load and initialize your
	 * secret keys here.  
	 * 
	 * @param config the Clock element
	 */
	void init(XElement config);
	
	/**
	 * Called when Configure first generates the configuration file.  Any shared secrets you
	 * may need to store should be stored here, in the element provided (or child element).  
	 * 
	 * @param config the Clock element
	 */
	void configure(XElement config);
	
	/**
	 * Take a hex string containing an encrypted setting and decrypt it.
	 * 
	 * @param v encrypted hex
	 * @return decrypted setting
	 */
	String decryptHexToString(CharSequence v);

	/**
	 * Take byte array containing an encrypted setting and decrypt it.
	 * 
	 * @param v encrypted byte array
	 * @return decrypted setting
	 */
	String decryptString(byte[] v);
	
	/**
	 * Take a setting and encrypt into a hex string
	 *  
	 * @param v setting value
	 * @return encrypted hex string
	 */
	String encryptStringToHex(CharSequence v);
	
	/**
	 * Take a setting and encrypt into a byte array
	 * 
	 * @param v setting value
	 * @return encrypted byte array
	 */
	byte[] encryptString(CharSequence v);
	
	Cipher decryptCipher();
	Cipher encryptCipher();
	
	/**
	 * Take a value and hash it into a hex string
	 * 
	 * @param v value to hash
	 * @return hash as hex string
	 */
	String hashStringToHex(CharSequence v);
	
	/**
	 * Take a value and hash it into a byte array
	 * 
	 * @param v value to hash
	 * @return hash as byte array
	 */
	byte[] hashString(CharSequence v);

	String getHashKey();
}
