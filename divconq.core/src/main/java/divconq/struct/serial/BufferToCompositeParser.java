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
package divconq.struct.serial;

import io.netty.buffer.ByteBuf;

import java.math.BigDecimal;
import java.math.BigInteger;

import divconq.lang.chars.Special;
import divconq.lang.chars.Utf8Decoder;
import divconq.lang.Memory;
import divconq.lang.StringBuilder32;
import divconq.struct.builder.BuilderStateException;
import divconq.struct.builder.ICompositeBuilder;
import divconq.util.HexUtil;
import divconq.util.StringUtil;
import divconq.util.TimeUtil;

/**
 * Load a Json like structure from buffer.  Excepts data to be available one
 * buffer at a time.  Buffers can probably be very small, at least 64 bytes if not
 * less.
 * 
 * @author Andy
 *
 */
public class BufferToCompositeParser {
	protected ICompositeBuilder builder = null;
	protected Utf8Decoder decoder = new Utf8Decoder();
	protected boolean done = false;
	
	public ICompositeBuilder getBuilder() {
		return builder;
	}
	
	public boolean isDone() {
		return this.done;
	}
	
	public BufferToCompositeParser(ICompositeBuilder builder) {
		this.builder = builder;
	}
	
	/**
	 * @param input buffer to read and parse
	 */
	
	public void parseStruct(ByteBuf input) throws Exception {
		while (true) {
			StringBuilder32 str = this.decoder.processBytesUntilSpecial(input);			
		
			if (str == null)
				break;		// we need more buffer
			
			if (str.length() > 0) {
				if (this.builder.needFieldName())
					this.builder.value(str.toString());
				else
					this.toScalar(this.builder, str);
			}
			
			int special = this.decoder.getLastSpecialCharacter();
			
			// at the end (there should be no outstanding text
			if (special == Special.End.getCode()) {
				this.done = true;
				break;
			}
			
			if (special == Special.StartRec.getCode()) 
				this.builder.startRecord();
			else if (special == Special.EndRec.getCode()) 
				this.builder.endRecord();
			else if (special == Special.StartList.getCode()) 
				this.builder.startList();
			else if (special == Special.EndList.getCode()) 
				this.builder.endList();
			// is for delineation only does not do anything
			//else if (special == Special.Scalar.getCode()) 
			//	this.builder.scalar();
			else if (special == Special.Field.getCode())
				this.builder.field();
		}
	}
	
	protected void toScalar(ICompositeBuilder rb, CharSequence val) throws BuilderStateException {
		if (StringUtil.isEmpty(val)) {
			rb.value(null);
			return;
		}
		
		if ((val.charAt(0) == '`') || (val.charAt(0) == '*')) {
			// unescape the string
			StringBuilder sb = new StringBuilder();
			boolean escape = false;
			
			for (int i = 1; i < val.length(); i++) {
				char v = val.charAt(i);
				
				if (v == '\\') {
					if (escape) {
						sb.append('\\');
						escape = false;
					}
					else {
						escape = true;
					}
				}
				else if (v == 'n') {
					if (escape) {
						sb.append('\n');
						escape = false;
					}
					else {
						sb.append('n');
					}
				}
				else if (v == 't') {
					if (escape) {
						sb.append('\t');
						escape = false;
					}
					else {
						sb.append('t');
					}
				}
				//else if (escape && ((v == 'b') || (v == 'r') || (v == 'f') || (v == 'u') || Character.isDigit(v))) {
				else if (escape) {  // only t and n allowed
					throw new BuilderStateException("Invalid escape character: " + v);
				} 
				else {
					sb.append(v);
					escape = false;
				}
			}			
			
			if (val.charAt(0) == '*') 
				rb.rawJson(sb);
			else
				rb.value(sb);
			
			return;
		}
		
			/*
		if (val.charAt(0) == '*') {
			rb.rawJson(val.subSequence(1, val.length()));
			return;
		}
		*/
		
		if (val.charAt(0) == '(') {
			rb.value(new BigDecimal(val.subSequence(1, val.length()).toString()));
			return;
		}
		
		if (val.charAt(0) == ')') {
			try {
				rb.value(new Long(val.subSequence(1, val.length()).toString()));
				return;
			}
			catch (Exception x) {				
			}
			
			try {
				rb.value(new BigInteger(val.subSequence(1, val.length()).toString()));
			}
			catch (Exception x) {				
			}
			
			return;
		}
		
		if (val.charAt(0) == '@') {
			if (val.length() == 1)
				rb.value(null);
			else
				rb.value(TimeUtil.stampFmt.parseDateTime(val.subSequence(1, val.length()).toString()));
			
			return;
		}
		
		if (val.charAt(0) == '&') {
			if (val.length() == 1)
				rb.value(null);
			else
				rb.value(val.subSequence(1, val.length()));
			
			return;
		}
		
		// TODO is it just 1?
		if (val.charAt(0) == '!') {
			if (val.length() == 1)
				rb.value(null);
			else
				rb.value("1".equals(val.subSequence(1, val.length())) ? Boolean.TRUE : Boolean.FALSE);
			
			return;
		}
		
		if (val.charAt(0) == '%') {
			if (val.length() == 1)
				rb.value(null);
			else
				rb.value(new Memory(HexUtil.decodeHex(val.subSequence(1, val.length()))));
			
			return;
		}
		
		//if (val.charAt(0) == '@') {
		//	rb.value(TimeUtil.stampFmt.parseDateTime(val.subSequence(1, val.length()).toString()));
		//	return;
		//}
		
		if (val.charAt(0) == '$') {
			if (val.length() == 1)
				rb.value(null);
			else
				rb.value(TimeUtil.parseBigDateTime(val.subSequence(1, val.length()).toString()));
			
			return;
		}
		
		String val2 = val.toString().toLowerCase();
		
		if (val2.equals("null")) {
			rb.value(null);
			return;
		}
		
		if (val2.equals("true")) {
			rb.value(Boolean.TRUE);
			return;
		}
		
		if (val2.equals("false")) {
			rb.value(Boolean.FALSE);
			return;
		}
		
		try {
			rb.value(val2);
		}
		catch (Exception x) {				
		}
		
		return;
	}
}
