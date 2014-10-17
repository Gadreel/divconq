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
package divconq.lang.chars;

import io.netty.buffer.ByteBuf;

public class Utf8Encoder {
	static public byte[] encode(CharSequence chars) {
		if (chars == null)
			return null;
		
		int seqidx = 0; 
		int leftOver = 0;
		int bufidx = 0;
		
		while (seqidx < chars.length()) {
			int ch = chars.charAt(seqidx);
			
			if (leftOver != 0) {
				if (ch >= '\uDC00' && ch <= '\uDFFF') {
					bufidx++;
					bufidx++;
					bufidx++;
					bufidx++;
				} 
				else {
					// We have a surrogate start followed by a
					// regular character.  Technically, this is
					// invalid, skip.
				}
				
				leftOver = 0x00;
			}
			
			if (ch < (int)0x80) {
				bufidx++;
			} 
			else if (ch < (int)0x800) {
				bufidx++;
				bufidx++;
			} 
			else if (ch < '\uD800' || ch > (int)'\uDFFF') {
				bufidx++;
				bufidx++;
				bufidx++;
			} 
			else if (ch <= '\uDBFF') {
				// This is a surrogate char, exit the inner loop.
				leftOver = ch;
				break;
			} 
			else {
				// We have a surrogate tail without 
				// leading surrogate. Do nothing
				leftOver = (int)0x00;
			}
			
			seqidx++;
		}
		
		byte[] buffer = new byte[bufidx]; 
		seqidx = 0; 
		leftOver = 0;
		bufidx = 0;
		
		while (seqidx < chars.length()) {
			int ch = chars.charAt(seqidx);
			
			if (leftOver != 0) {
				if (ch >= '\uDC00' && ch <= '\uDFFF') {
					// We have a correct surrogate pair.
					ch = 0x10000 + (int)ch - 0xDC00 + (((int) leftOver - 0xD800) << 10);

					buffer[bufidx] = (byte) (0xF0 | (ch >> 18));
					bufidx++;
					buffer[bufidx] = (byte) (0x80 | ((ch >> 12) & 0x3F));
					bufidx++;
					buffer[bufidx] = (byte) (0x80 | ((ch >> 6) & 0x3F));
					bufidx++;
					buffer[bufidx] = (byte) (0x80 | (ch & 0x3F));
					bufidx++;
				} 
				else {
					// We have a surrogate start followed by a
					// regular character.  Technically, this is
					// invalid, skip.
				}
				
				leftOver = 0x00;
			}
			if (ch < (int)0x80) {
				buffer[bufidx] = (byte)ch;
				bufidx++;
			} 
			else if (ch < (int)0x800) {
				buffer[bufidx] = (byte) (0xC0 | (ch >> 6));
				bufidx++;
				buffer[bufidx] = (byte) (0x80 | (ch & 0x3F));
				bufidx++;
			} 
			else if (ch < '\uD800' || ch > (int)'\uDFFF') {
				buffer[bufidx] = (byte) (0xE0 | (ch >> 12));
				bufidx++;
				buffer[bufidx] = (byte) (0x80 | ((ch >> 6) & 0x3F));
				bufidx++;
				buffer[bufidx] = (byte) (0x80 | (ch & 0x3F));
				bufidx++;
			} 
			else if (ch <= '\uDBFF') {
				// This is a surrogate char, exit the inner loop.
				leftOver = ch;
				break;
			} 
			else {
				// We have a surrogate tail without 
				// leading surrogate. Do nothing
				leftOver = (int)0x00;
			}
			
			seqidx++;
		}
		
		return buffer;
	}
	
	static public byte[] encode(int ch) {
		if (ch == -1) 
			return null;
		
		byte[] buffer = null; 
		
		if (ch < (int)0x80) {
			buffer = new byte[1]; 
			buffer[0] = (byte)ch;
		} 
		else if (ch < (int)0x800) {
			buffer = new byte[2]; 
			buffer[0] = (byte) (0xC0 | (ch >> 6));
			buffer[1] = (byte) (0x80 | (ch & 0x3F));
		} 
		else if (ch < '\uD800' || ch > (int)'\uDFFF') {
			buffer = new byte[3]; 
			buffer[0] = (byte) (0xE0 | (ch >> 12));
			buffer[1] = (byte) (0x80 | ((ch >> 6) & 0x3F));
			buffer[2] = (byte) (0x80 | (ch & 0x3F));
		} 
		
		return buffer;
	}
	
	static public int size(CharSequence chars) {
		if (chars == null)
			return 0;
		
		int seqidx = 0; 
		int leftOver = 0;
		int bufidx = 0;
		
		while (seqidx < chars.length()) {
			int ch = chars.charAt(seqidx);
			
			if (leftOver != 0) {
				if (ch >= '\uDC00' && ch <= '\uDFFF') {
					bufidx++;
					bufidx++;
					bufidx++;
					bufidx++;
				} 
				else {
					// We have a surrogate start followed by a
					// regular character.  Technically, this is
					// invalid, skip.
				}
				
				leftOver = 0x00;
			}
			
			if (ch < (int)0x80) {
				bufidx++;
			} 
			else if (ch < (int)0x800) {
				bufidx++;
				bufidx++;
			} 
			else if (ch < '\uD800' || ch > (int)'\uDFFF') {
				bufidx++;
				bufidx++;
				bufidx++;
			} 
			else if (ch <= '\uDBFF') {
				// This is a surrogate char, exit the inner loop.
				leftOver = ch;
				break;
			} 
			else {
				// We have a surrogate tail without 
				// leading surrogate. Do nothing
				leftOver = (int)0x00;
			}
			
			seqidx++;
		}
		
		return bufidx; 
	}
	
	/**
	 * Blindly assumes content will fit in buffer, *you* have to be sure it will...
	 * 
	 * @param chars
	 * @param buffer
	 */
	static public void encode(CharSequence chars, ByteBuf buffer) {
		if ((chars == null) || (buffer == null))
			return;
		
		int seqidx = 0; 
		int leftOver = 0;
		
		while (seqidx < chars.length()) {
			int ch = chars.charAt(seqidx);
			
			if (leftOver != 0) {
				if (ch >= '\uDC00' && ch <= '\uDFFF') {
					// We have a correct surrogate pair.
					ch = 0x10000 + (int)ch - 0xDC00 + (((int) leftOver - 0xD800) << 10);

					buffer.writeByte(0xF0 | (ch >> 18));
					buffer.writeByte(0x80 | ((ch >> 12) & 0x3F));
					buffer.writeByte(0x80 | ((ch >> 6) & 0x3F));
					buffer.writeByte(0x80 | (ch & 0x3F));
				} 
				else {
					// We have a surrogate start followed by a
					// regular character.  Technically, this is
					// invalid, skip.
				}
				
				leftOver = 0x00;
			}
			if (ch < (int)0x80) {
				buffer.writeByte(ch);
			} 
			else if (ch < (int)0x800) {
				buffer.writeByte(0xC0 | (ch >> 6));
				buffer.writeByte(0x80 | (ch & 0x3F));
			} 
			else if (ch < '\uD800' || ch > (int)'\uDFFF') {
				buffer.writeByte(0xE0 | (ch >> 12));
				buffer.writeByte(0x80 | ((ch >> 6) & 0x3F));
				buffer.writeByte(0x80 | (ch & 0x3F));
			} 
			else if (ch <= '\uDBFF') {
				// This is a surrogate char, exit the inner loop.
				leftOver = ch;
				break;
			} 
			else {
				// We have a surrogate tail without 
				// leading surrogate. Do nothing
				leftOver = (int)0x00;
			}
			
			seqidx++;
		}
	}
}
