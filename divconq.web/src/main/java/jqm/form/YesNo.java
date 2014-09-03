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

import w3.html.Div;
import w3.html.Label;
import w3.html.Option;
import divconq.lang.OperationContext;
import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;
import divconq.view.Attributes;
import divconq.view.Element;
import divconq.view.Node;
import divconq.view.Nodes;
import divconq.view.html.HtmlUtil;
import divconq.web.ViewInfo;
import divconq.xml.XElement;

public class YesNo extends Div implements IFormInput {
	protected ValidationInfo vinfo = null;
	protected String record = null;
	protected String field = null;
	protected String inputid = null;
	protected String inputname = null;
	protected RecordStruct props = null;
	protected String[] auth = null;
	
	@Override
	public String getInputId() {
		return this.inputid;
	}
	
	@Override
	public String getInputName() {
		return this.inputname;
	}
	
	@Override
	public String getRecord() {
		return this.record;
	}
	
	@Override
	public String getField() {
		return this.field;
	}
	
	@Override
	public RecordStruct getProps() {
		return this.props;
	}
	
	@Override
	public ValidationInfo getValidation() {
		return this.vinfo;
	}
	
    public YesNo() {
    	super();
	}
    
    public YesNo(Object... args) {
    	super(args);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		YesNo cp = new YesNo();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

    @Override
    protected void doCopy(Node n) {
    	super.doCopy(n);
    	
    	YesNo nn = (YesNo)n;
    	nn.vinfo = this.vinfo;
    	nn.record = this.record;
    	nn.field = this.field;
    	nn.inputname = this.inputname;
    	nn.inputid = this.inputid;
    	nn.props = this.props;
    	nn.auth = this.auth;
    }

	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		Nodes children = new Nodes();
		
		if (xel.hasAttribute("Name")) {
			this.inputname = xel.getAttribute("Name");
			String label = xel.hasAttribute("Label") ? xel.getAttribute("Label") : this.inputname;
			this.field = xel.hasAttribute("Field") ? xel.getAttribute("Field") : this.inputname;
			this.record = xel.getAttribute("Record", "Default");
			
			this.inputid = "cb" + this.inputname;
			
			this.vinfo = new ValidationInfo(xel);
			
			children.add(new Label(new Attributes("for", this.inputid), label + ":"));
			children.add(new w3.html.Select(new Attributes("id", this.inputid, "name", this.inputid, "data-role", "slider", 
						"data-mini", "true", "data-native-menu", "false",
						"data-record", this.record, "data-field", this.field
					),
					new Option(new Attributes("Value", "false"), "No"),
					new Option(new Attributes("Value", "true"), "Yes"),
					attrs
			));
			
			// for error message
			String errmsg = vinfo.getMessage();
			
			if (errmsg == null)
				errmsg = "Please choose one.";
			
			children.add(new Label(new Attributes("for", this.inputid, "class", "error", "style", "display: none;"), errmsg));
			
			this.props = new RecordStruct(
					new FieldStruct("Type", "YesNo"), 
					new FieldStruct("Id", this.inputid), 
					new FieldStruct("DataType", this.getDataType()), 
					new FieldStruct("Record", this.record),
					new FieldStruct("Field", this.field)
			);
		}
		
		if (xel.hasAttribute("Auth"))
			this.auth = xel.getAttribute("Auth").split(",");

        this.myArguments = new Object[] { new Attributes("data-role", "fieldcontain"), children };
		
		nodes.add(this);
	}
	
	public String getDataType() {
		if (this.vinfo == null)
			return "String";
		
		RecordStruct rule = this.vinfo.getRule();
		
		if (rule == null)
			return "Boolean";
		
		String t = rule.getFieldAsString("dcDataType");
		
		if (StringUtil.isNotEmpty(t))
			return t;
		
		return "Boolean";
	}
	
	@Override
	public void build(Object... args) {
		if ((this.auth != null) && !OperationContext.get().isAuthorized(this.auth))
			return;
		
		Form frm = Form.findForm(this);
		
		if (frm != null)
			frm.addFormInput(this);
		
		super.build(args);
	}
}
