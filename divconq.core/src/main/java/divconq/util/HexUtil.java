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

import divconq.lang.Memory;

public class HexUtil {
	/**
	 * Take a chunk of memory and format as a hex string
	 * (set the memory position before calling, use 0 if all memory desired)
	 * 
	 * @param buffer bytes to format
	 * @return formatted hex string
	 */
	public static String bufferToHex(Memory buffer) {
		if (buffer == null)
			return null;
		
		return HexUtil.bufferToHex(buffer, buffer.getLength());
	}
	
	/**
	 * Take a chunk of memory and format as a hex string
	 * (set the memory position before calling)
	 * 
	 * @param buffer bytes to format
	 * @param length how many bytes past start to format
	 * @return formatted hex string
	 */
	public static String bufferToHex(Memory buffer, int length) {
		if (buffer == null)
			return null;
		
		StringBuilder hexString = new StringBuilder(length << 1);
		char highNibble;
		char lowNibble;

		for (int i = 0; i < length; i++) {
			int cb = buffer.readByte();
			
			highNibble = HexUtil.HexChars[(cb & 0xF0) >> 4];
	  	  	lowNibble  = HexUtil.HexChars[cb & 0x0F];

	  	  	hexString.append(highNibble);
	  	  	hexString.append(lowNibble);
		}
		
		return hexString.toString();
	}
	
	/**
	 * Take an array of bytes and format as a hex string
	 * 
	 * @param buffer bytes to format
	 * @return formatted hex string
	 */
	public static String bufferToHex(byte[] buffer) {
		if (buffer == null)
			return null;
		
		return HexUtil.bufferToHex(buffer, 0, buffer.length);
	}
	
	/**
	 * Take a range of bytes and format as a hex string
	 * 
	 * @param buffer bytes to format
	 * @param startOffset position in buffer to start
	 * @param length how many bytes past start to format
	 * @return formatted hex string
	 */
	public static String bufferToHex(byte[] buffer, int startOffset, int length) {
		if (buffer == null)
			return null;
		
		StringBuilder hexString = new StringBuilder(length << 1);
		int  endOffset = startOffset + length;
		char highNibble;
		char lowNibble;

		for (int i = startOffset; i < endOffset; i++) {
			highNibble = HexUtil.HexChars[(buffer[i] & 0xF0) >> 4];
	  	  	lowNibble  = HexUtil.HexChars[buffer[i] & 0x0F];

	  	  	hexString.append(highNibble);
	  	  	hexString.append(lowNibble);
		}
		
		return hexString.toString();
	}
	
	public static String charToHex(int chr) {
		StringBuilder hexString = new StringBuilder(2);
		char highNibble;
		char lowNibble;

		highNibble = HexUtil.HexChars[(chr & 0xF0) >> 4];
  	  	lowNibble  = HexUtil.HexChars[chr & 0x0F];

  	  	hexString.append(highNibble);
  	  	hexString.append(lowNibble);
		
		return hexString.toString();
	}
    
	public static byte[] decodeHexClean(String data) {
		return HexUtil.decodeHex(data.replace(":", "").replace("-", ""));
    }
    
	/**
	 * convert hex characters into corresponding byte array
	 * 
	 * @param data hex characters
	 * @return bytes from hex
	 */
    public static byte[] decodeHex(CharSequence data) {
    	if (data == null)
    		return null;
    	
        int len = data.length();

        if ((len & 0x01) != 0) 
        	return null;

        byte[] out = new byte[len >> 1];

        // two characters form the hex value.
        for (int i = 0, j = 0; j < len; i++) {
        	int d1 = Character.digit(data.charAt(j++), 16);
        	int d2 = Character.digit(data.charAt(j++), 16);
        	
        	if ((d1 == -1) || (d2 == -1))
        		return null;
        	
            int f = (d1 << 4) | d2;

            out[i] = (byte) (f & 0xFF);
        }

        return out;
    }	
		
    public static final char HexChars[] = { '0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f' };
}
