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
package jqm.form;

import divconq.struct.RecordStruct;
import divconq.util.StringUtil;
import divconq.xml.XElement;

/**
 * <FormElement Validation="Required,RequiredCallback:cbfunc,MinLength:N,MaxLength:N,RangeLength:X:Y,Min:N,Max:N,Email,Url,Date,DateISO,Number,Digits,CreditCard,UsPhone,Custom:AAA" />
 * 
 * or with more options
 * 
 * <FormElement>
 * 		<Validation 
 * 			Options="[same as above]"
 * 			RequiredExpression="[expression]"
 * 			Message="NNN"
 * 			Pattern="[regex]"
 * 		 >
 *			<RequiredScript>
 *				[script]
 *			</RequiredScript> 			
 *			<ValueScript>
 *				[script]
 *			</ValueScript> 			
 * 		</Validation>
 * </FormElement>
 * 
 * 
 * @author andy
 *
 */
public class ValidationInfo {
	protected String requiredScript = null;
	protected String valueScript = null;
	protected String message = null;
	protected RecordStruct rule = new RecordStruct();

	public ValidationInfo(XElement xel) {
		if (xel.hasAttribute("DataType")) {
			String dt = xel.getAttribute("DataType");
			this.rule.setField("dcDataType", dt);
			
			// the hard coded approach - we should be able to do other ways to (validation scripts, dt of type record)
			if ("Json".equals(dt))
				this.rule.setField("dcJson", true);
		}
		
		this.parseOptions(xel.getAttribute("Validation"));
		
		XElement vel = xel.find("Validation");
		
		if (vel == null)
			return;
		
		this.parseOptions(vel.getAttribute("Options"));
		
		// TODO parse scripts too
	}

	public void parseOptions(String opts) {
		if (StringUtil.isEmpty(opts))
			return;
		
		String[] olist = opts.split(",");
		
		for (String r : olist) {
			String[] p = r.split(":");
			
			if ("Required".equals(p[0]))
				this.rule.setField("required", true);
			else if ("Digits".equals(p[0]))
				this.rule.setField("digits", true);
			else if ("Email".equals(p[0]))
				this.rule.setField("email", true);
			else if ("MaxLength".equals(p[0]))
				this.rule.setField("maxlength", p[1]);
			else if ("MinLength".equals(p[0]))
				this.rule.setField("minlength", p[1]);
			
			// TODO support others
		}
	}
	
	public String getMessage() {
		return this.message;
	}

	public String getValueFunction() {
		return this.valueScript;
	}

	public RecordStruct getRule() {
		return this.rule;
	}
}
