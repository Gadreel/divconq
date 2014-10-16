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
import w3.html.Input;
import w3.html.Label;
import divconq.lang.OperationContext;
import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;
import divconq.view.Attributes;
import divconq.view.Element;
import divconq.view.LiteralText;
import divconq.view.Node;
import divconq.view.Nodes;
import divconq.view.html.HtmlUtil;
import divconq.web.ViewInfo;
import divconq.xml.XElement;

public class TextField extends Div implements IFormInput {
	protected ValidationInfo vinfo = null;
	protected String record = null;
	protected String field = null;
	protected String inputid = null;
	protected String inputname = null;
	protected RecordStruct props = null;
	protected String[] auth = null;
	
	@Override
	public String getInputName() {
		return this.inputname;
	}
	
	@Override
	public String getInputId() {
		return this.inputid;
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
	
    public TextField() {
    	super();
	}
    
    public TextField(Object... args) {
    	super(args);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		TextField cp = new TextField();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

    @Override
    protected void doCopy(Node n) {
    	super.doCopy(n);
    	
    	TextField nn = (TextField)n;
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
		
		if (xel.hasAttribute("align"))
			attrs.add("align", xel.getRawAttribute("align"));
		
		Nodes children = new Nodes();
		
		if (xel.hasAttribute("Name")) {
			this.inputname = xel.getAttribute("Name");
			String label = xel.hasAttribute("Label") ? xel.getAttribute("Label") : this.inputname;
			String type = xel.hasAttribute("Type") ? xel.getAttribute("Type") : "text";
			String value = xel.hasAttribute("Value") ? xel.getAttribute("Value") : null;
			this.field = xel.hasAttribute("Field") ? xel.getAttribute("Field") : this.inputname;
			this.record = xel.getAttribute("Record", "Default");
			
			this.inputid = "txt" + this.inputname;
			
			this.vinfo = new ValidationInfo(xel);

			String ph = xel.getRawAttribute("Placeholder");
			String ro = "True".equals(xel.getRawAttribute("ReadOnly")) ? "true" : null;			
			
			children.add(new Label(new Attributes("for", this.inputid), new LiteralText(label + ":")));
			children.add(new Input(new Attributes("type", type, "id", this.inputid, "name", this.inputid, "data-mini", "true", 
					"value", value, "placeholder", ph, "readonly", ro, "data-record", this.record, "data-field", this.field), attrs));
			
			this.props = new RecordStruct(
					new FieldStruct("Type", "TextField"), 
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
			return "String";
		
		String t = rule.getFieldAsString("dcDataType");
		
		if (StringUtil.isNotEmpty(t))
			return t;
		
		return "String";
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
