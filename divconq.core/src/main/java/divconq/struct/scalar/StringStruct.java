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
package divconq.struct.scalar;

import java.text.DecimalFormat;

import org.joda.time.format.DateTimeFormat;

import divconq.hub.Hub;
import divconq.schema.DataType;
import divconq.schema.RootType;
import divconq.script.StackEntry;
import divconq.struct.ScalarStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class StringStruct extends ScalarStruct {
	protected String value = null;

	@Override
	public DataType getType() {
		if (this.explicitType != null)
			return super.getType();
		
		return Hub.instance.getSchema().getType("String");
	}

	public StringStruct() {		
	}

	public StringStruct(Object v) {
		this.adaptValue(v);
	}
	
	@Override
	public Object getGenericValue() {
		return this.value;
	}
	
	@Override
	public void adaptValue(Object v) {
		this.value = Struct.objectToString(v);
	}

	public String getValue() {
		return this.value; 
	}
	
	public void setValue(String v) { 
		this.value = v; 
	}
	
	@Override
	public boolean isEmpty() {
		return StringUtil.isEmpty(this.value);
	}
	
	@Override
	public boolean isNull() {
		return (this.value == null);
	}
	
	@Override
	public void operation(StackEntry stack, XElement code) {
		if ("Lower".equals(code.getName())) {
			if (StringUtil.isNotEmpty(this.value))
				this.value = this.value.toLowerCase();
			
			stack.resume();
			return;
		}
		else if ("Upper".equals(code.getName())) {
			if (StringUtil.isNotEmpty(this.value))
				this.value = this.value.toUpperCase();
			
			stack.resume();
			return;
		}
		else if ("Set".equals(code.getName())) {
			Struct sref = code.hasAttribute("Value")
					? stack.refFromElement(code, "Value")
					: stack.resolveValue(code.getText());
			
			this.adaptValue(sref);			
			
			stack.resume();
			return;
		}
		else if ("Format".equals(code.getName())) {
			Struct sref = code.hasAttribute("Value")
					? stack.refFromElement(code, "Value")
					: stack.resolveValue(code.getText());

			String pat = stack.stringFromElement(code, "Pattern");
					
			try {
				if (sref instanceof DateTimeStruct)
					this.value = DateTimeFormat.forPattern(pat).print(((DateTimeStruct)sref).getValue());			
				else if (sref instanceof DateStruct)
					this.value = DateTimeFormat.forPattern(pat).print(((DateStruct)sref).getValue());			
				else if (sref instanceof DecimalStruct)
					this.value = new DecimalFormat(pat).format(((DecimalStruct)sref).getValue());			
				else if (sref instanceof IntegerStruct || sref instanceof BigIntegerStruct)		
					this.value = String.format(pat, ((ScalarStruct)sref).getGenericValue());			
			}
			catch (Exception x) {
				stack.log().error("Error doing " + code.getName() + ": " + x);
			}
			
			stack.resume();
			return;
		}
		else if ("Append".equals(code.getName())) {
			String me = (this.value == null) ? "" : this.value;
			String it = "";
			
			Struct sref = code.hasAttribute("Value")
					? stack.refFromElement(code, "Value")
					: stack.resolveValue(code.getText());

			String its = Struct.objectToString(sref);
			
			if (its != null)
				it = its;
			
			this.value = me + it;			
			
			stack.resume();
			return;
		}
		else if ("Replace".equals(code.getName())) {
			if (StringUtil.isNotEmpty(this.value)) {
				String from = stack.stringFromElement(code, "Old"); 
				String to = stack.stringFromElement(code, "New"); 
				String pattern = stack.stringFromElement(code, "Pattern"); 
				
				if (StringUtil.isEmpty(pattern))
					this.value = this.value.replace(from, to);
				else 
					this.value = this.value.replaceAll(pattern, to);
			}
			
			
			stack.resume();
			return;
		}
		else if ("Substring".equals(code.getName())) {
			if (StringUtil.isNotEmpty(this.value)) {
				int from = (int) stack.intFromElement(code, "From", 0); 
				int to = (int) stack.intFromElement(code, "To", 0); 
				int length = (int) stack.intFromElement(code, "Length", 0); 
						
				if (to > 0) 
					this.value = this.value.substring(from, to);
				else if (length > 0) 
					this.value = this.value.substring(from, from + length);
				else
					this.value = this.value.substring(from);
			}
						
			stack.resume();
			return;
		}
		else if ("FillCode".equals(code.getName())) {
			int length = (int) stack.intFromElement(code, "Length", 12); 
			this.value = StringUtil.buildSecurityCode(length);
						
			stack.resume();
			return;
		}
		else if ("Trim".equals(code.getName())) {
			if (StringUtil.isNotEmpty(this.value)) 
				this.value = StringUtil.stripWhitespace(this.value.trim());
						
			stack.resume();
			return;
		}
		else if ("TrimStart".equals(code.getName())) {
			if (StringUtil.isNotEmpty(this.value)) 
				this.value = StringUtil.stripLeadingWhitespace(this.value);
						
			stack.resume();
			return;
		}
		else if ("TrimEnd".equals(code.getName())) {
			if (StringUtil.isNotEmpty(this.value)) 
				this.value = StringUtil.stripTrailingWhitespace(this.value);
						
			stack.resume();
			return;
		}
		else if ("LeftPad".equals(code.getName())) {
			if (StringUtil.isEmpty(this.value)) 
				this.value = "";
			
			int size = (int) stack.intFromElement(code, "Size", 1);
			String ch = code.hasAttribute("With") ? stack.stringFromElement(code, "With") : " ";
				
			this.value = StringUtil.leftPad(this.value, size, ch);
						
			stack.resume();
			return;
		}
		else if ("RightPad".equals(code.getName())) {
			if (StringUtil.isEmpty(this.value)) 
				this.value = "";
			
			int size = (int) stack.intFromElement(code, "Size", 1);
			String ch = code.hasAttribute("With") ? stack.stringFromElement(code, "With") : " ";
				
			this.value = StringUtil.rightPad(this.value, size, ch);
						
			stack.resume();
			return;
		}
		
		/*
		// TODO also implement
		 *  <Join List="$var" With="delim" />
		// <Piece Delim="str" Index="num" />
		// <Align Size="num" Pad="left|right" PadChar="c" />
		 */
		
		super.operation(stack, code);
	}
	
    @Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	StringStruct nn = (StringStruct)n;
    	nn.value = this.value;
    }
    
	@Override
	public Struct deepCopy() {
		StringStruct cp = new StringStruct();
		this.doCopy(cp);
		return cp;
	}

	@Override
	public boolean equals(Object obj) {
		return (StringStruct.comparison(this, obj) == 0);
	}

	@Override
	public int compare(Object y) {
		return StringStruct.comparison(this, y);
	}

	@Override
	public int hashCode() {
		return (this.value == null) ? 0 : this.value.hashCode();
	}
	
	@Override
	public String toString() {
		return (this.value == null) ? "null" : this.value.toString();
	}

	@Override
	public Object toInternalValue(RootType t) {
		return this.value;
	}

	public static int comparison(Object x, Object y)
	{
		String xv = Struct.objectToString(x);
		String yv = Struct.objectToString(y);

		if ((yv == null) && (xv == null))
			return 0;

		if (yv == null)
			return 1;

		if (xv == null)
			return -1;

		return xv.compareTo(yv);
	}
	
	@Override
	public boolean checkLogic(StackEntry stack, XElement source) {
		boolean isok = true;
		boolean condFound = false;
		
		if (this.value != null) {
			boolean caseinsensitive = stack.boolFromElement(source, "CaseInsensitive", false);
			
			if (!condFound && source.hasAttribute("Contains")) {
				String other = stack.stringFromElement(source, "Contains");
	            isok = caseinsensitive ? this.value.toLowerCase().contains(other.toLowerCase()) : this.value.contains(other);
	            condFound = true;
	        }
			
			if (!condFound && source.hasAttribute("StartsWith")) {
				String other = stack.stringFromElement(source, "StartsWith");
	            isok = caseinsensitive ? this.value.toLowerCase().startsWith(other.toLowerCase()) : this.value.startsWith(other);
	            condFound = true;
	        }
			
			if (!condFound && source.hasAttribute("EndsWith")) {
				String other = stack.stringFromElement(source, "EndsWith");
	            isok = caseinsensitive ? this.value.toLowerCase().endsWith(other.toLowerCase()) : this.value.endsWith(other);
	            condFound = true;
	        }
		}
		
		if (!condFound) 
			isok = Struct.objectToBooleanOrFalse(this.value);
		
		return isok;
	}
}
