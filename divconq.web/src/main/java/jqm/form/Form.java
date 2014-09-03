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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jqm.Document;
import jqm.Page;

import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;
import divconq.view.Attributes;
import divconq.view.Element;
import divconq.view.ICodeTag;
import divconq.view.MixedElement;
import divconq.view.Node;
import divconq.view.Nodes;
import divconq.view.html.HtmlUtil;
import divconq.web.ViewInfo;
import divconq.xml.XElement;

/**
 * Assumes a jQuery form that is not to be POSTed by handled by ajax.  We add the validation stuff 
 * through here.
 * 
 * @author andy
 *
 */
public class Form extends MixedElement implements ICodeTag {
	protected String name = null;
	protected XElement src = null;
	//protected String onsave = null;
	//protected String recorder = null;
	
	// these are temp, only during build - no need to copy
	protected String savebtn = null;
	protected List<IFormInput> inputs = new ArrayList<IFormInput>();
	protected Map<String, String> funcs = new HashMap<String, String>();
	
    public Form() {
    	super();
	}
    
    public Form(Object... args) {
    	super(args);
	}
    
	@Override
	public Node deepCopy(Element parent) {
		Form cp = new Form();
		cp.setParent(parent);
		this.doCopy(cp);
		return cp;
	}

    @Override
    protected void doCopy(Node n) {
    	super.doCopy(n);
    	
    	Form nn = (Form)n;
    	nn.name = this.name;
    	nn.src = this.src;
    }

	@Override
	public void parseElement(ViewInfo view, Nodes nodes, XElement xel) {
		this.src = xel;
		
		Attributes attrs = HtmlUtil.initAttrs(xel);
		
		this.name = xel.getAttribute("Name");

		attrs.add("id", "frm" + this.name);
				
        this.myArguments = new Object[] { attrs, view.getDomain().parseXml("HtmlOutput", view, xel) };
		
		nodes.add(this);
	}

	public void addSaveButton(String id) {
		this.savebtn = id;
	}
	
	public void addFormInput(IFormInput input) {
		this.inputs.add(input);
	}

	public void setFormButtons(Map<String, String> funcs) {
		this.funcs = funcs;
	}
	
    @Override
	public void build(Object... args) {
    	// clear temps
    	this.savebtn = null;
    	this.inputs.clear();
    	
	    super.build("form", args);
	    
    	Page pg = Page.findPage(this);
    	
    	if (pg == null)
    		return;
	    
	    Document doc = Document.findDocument(pg);
    	
    	if (doc == null)
    		return;

    	// collect record loading/saving order
    	ListStruct srecorder = new ListStruct();
    	
		String recorder = this.src.getAttribute("RecordOrder");	// a comma list
    	
    	if (StringUtil.isNotEmpty(recorder))
    		srecorder.addItem((Object[])recorder.split(","));
    	else {
    		Set<String> os = new HashSet<String>();
    		
        	for (int i = 0; i < this.inputs.size(); i++) {
        		String rn = this.inputs.get(i).getRecord();
        		
        		if (os.contains(rn))
        			continue;
        		
        		os.add(rn);
        		srecorder.addItem(rn);        	
        	}
    	}
    	
    	// collect form inputs
    	RecordStruct sinputs = new RecordStruct();

    	for (int i = 0; i < this.inputs.size(); i++) {
    		IFormInput input = this.inputs.get(i); 
	    	
    		String name = input.getInputName();
    		RecordStruct props = input.getProps();
    		
    		if ((name != null) && (props != null))
    			sinputs.setField(name, props);
    	}    	

    	// validation rules and messages
    	RecordStruct vrules = new RecordStruct();
    	RecordStruct vmsgs = new RecordStruct();
    	
    	for (int i = 0; i < this.inputs.size(); i++) {
    		IFormInput input = this.inputs.get(i); 
    		RecordStruct rule = input.getValidation().getRule();
    		
    		if (rule != null) 
    			vrules.setField(input.getInputId(), rule);
    		
    		String msg = input.getValidation().getMessage();
    		
    		if (msg != null) 
    			vmsgs.setField(input.getInputId(), msg);
    	}    	

    	// form buttons
    	RecordStruct fbtns = new RecordStruct();
    		
    	for (Entry<String, String> func : this.funcs.entrySet()) 
   			fbtns.setField(func.getKey(), func.getValue());
    	
    	// collect form options
    	RecordStruct frm = new RecordStruct(
    			new FieldStruct("FormName", this.name),
    			new FieldStruct("PageName", pg.getPageName()),
    			new FieldStruct("RecordOrder", srecorder),
    			new FieldStruct("Inputs", sinputs),
    			new FieldStruct("ValidationRules", vrules),
				new FieldStruct("ValidationMessages", vmsgs),
				new FieldStruct("AlwaysNew", "True".equals(this.src.getAttribute("AlwaysNew"))),
				new FieldStruct("BeforeLoad", this.src.hasAttribute("OnBeforeLoad") ? this.src.getAttribute("OnBeforeLoad") : null),
				new FieldStruct("LoadRecord", this.src.hasAttribute("OnLoadRecord") ? this.src.getAttribute("OnLoadRecord") : null),
				new FieldStruct("AfterLoadRecord", this.src.hasAttribute("OnAfterLoadRecord") ? this.src.getAttribute("OnAfterLoadRecord") : null),
				new FieldStruct("AfterLoad", this.src.hasAttribute("OnAfterLoad") ? this.src.getAttribute("OnAfterLoad") : null),
				new FieldStruct("SaveRecord", this.src.hasAttribute("OnSaveRecord") ? this.src.getAttribute("OnSaveRecord") : null),
				new FieldStruct("AfterSaveRecord", this.src.hasAttribute("OnAfterSaveRecord") ? this.src.getAttribute("OnAfterSaveRecord") : null),
				new FieldStruct("AfterSave", this.src.hasAttribute("OnAfterSave") ? this.src.getAttribute("OnAfterSave") : null),
				new FieldStruct("SaveButton", StringUtil.isNotEmpty(this.savebtn) ? this.savebtn : null),
				new FieldStruct("FormButtons", fbtns)
    	);
    	    	
    	pg.addForm(frm);
    	
    	// other scripts
    	
    	//ContentPlaceholder ph = this.getContext().getHolder("Scripts");
    	
    	// TODO bind to pageinit of the page, not of the document - need to get page id
    	/*
    	StringBuilder32 sb = new StringBuilder32();

		sb.appendLine("\t $('#frm" + this.name + "').bind('formInit.dcForm', function() {");
    	
    	// add validator messages
    	
    	for (int i = 0; i < this.inputs.size(); i++) {
    		IFormInput input = this.inputs.get(i); 
    		String func = input.getValidation().getValueFunction();
    		String msg = input.getValidation().getMessage();
    		
    		if (func != null)
    	    	sb.appendLine("\t\t $.validator.addMethod('inline" + this.name + i 
    	    			+ "', function(value) { " + func + " }, '" 
    	    			+ StringUtil.escapeSingleQuotes(msg) + "');");
    	}    	
    	
    	sb.appendLine();
    	
    	// add rules and messages
    	
    	sb.appendLine("\t\t $('#frm" + this.name + "').validate({");
    	
    	// rules
    	sb.appendLine("\t\t\t rules: {");

    	boolean fnd = false;
    	
    	for (int i = 0; i < this.inputs.size(); i++) {
    		IFormInput input = this.inputs.get(i); 
    		String rule = input.getValidation().getRule();
    		
    		if (rule != null) {
    			if (fnd)
    				sb.appendLine(", ");
    			
    	    	sb.append("\t\t\t\t " + input.getInputId() + ": " + rule);
    	    	
    	    	fnd = true;
    		}
    	}    	
		
		if (fnd)
			sb.appendLine();
    	
    	sb.appendLine("\t\t\t },");
    	
    	// messages
    	sb.appendLine("\t\t\t messages: {");

    	fnd = false;
    	
    	for (int i = 0; i < this.inputs.size(); i++) {
    		IFormInput input = this.inputs.get(i); 
    		String msg = input.getValidation().getMessage();
    		
    		if (msg != null) {
    			if (fnd)
    				sb.appendLine(", ");
    			
    	    	sb.append("\t\t\t\t " + input.getInputId() + ": " + msg);
    	    	
    	    	fnd = true;
    		}
    	}    	
		
		if (fnd)
			sb.appendLine();
    	
    	sb.appendLine("\t\t\t },");
    	
    	// invalid handler
    	sb.appendLine("\t\t\t invalidHandler: function() {");
    	sb.appendLine("\t\t\t\t $(document).dcDocument('showWarnPopup', 'Missing or invalid inputs, please correct.');");
    	sb.appendLine("\t\t\t },");
		
		// submit handler
    	sb.appendLine("\t\t\t submitHandler: function(form) {");

    	if (StringUtil.isNotEmpty(this.savebtn))
        	sb.appendLine("\t\t\t\t $('#frm" + this.name + "').dcForm('save');");
    	
		sb.appendLine("\t\t\t }");
		
		// validation end
    	sb.appendLine("\t\t });");
    	
    	sb.appendLine();

    	// add event shims
    	
    	if (this.src.hasAttribute("OnBeforeLoad"))
    		sb.appendLine("\t\t $('#frm" + this.name + "').bind('beforeLoad.dcForm', " + this.src.getAttribute("OnBeforeLoad") + ");");
    	
    	if (this.src.hasAttribute("OnLoadRecord"))
    		sb.appendLine("\t\t $('#frm" + this.name + "').bind('loadRecord.dcForm', " + this.src.getAttribute("OnLoadRecord") + ");");
    	
    	if (this.src.hasAttribute("OnSaveRecord"))
    		sb.appendLine("\t\t $('#frm" + this.name + "').bind('saveRecord.dcForm', " + this.src.getAttribute("OnSaveRecord") + ");");
    	
    	if (this.src.hasAttribute("OnAfterSaveRecord"))
    		sb.appendLine("\t\t $('#frm" + this.name + "').bind('afterSaveRecord.dcForm', " + this.src.getAttribute("OnAfterSaveRecord") + ");");    	
    	
    	if (this.src.hasAttribute("OnAfterSave"))
    		sb.appendLine("\t\t $('#frm" + this.name + "').bind('afterSave.dcForm', " + this.src.getAttribute("OnAfterSave") + ");");    	
    	
    	// add form button code
    	
    	if (StringUtil.isNotEmpty(this.savebtn)) {
        	sb.appendLine("\t\t $('#" + this.savebtn + "').click(function() { $('#frm" + this.name + "').validate().form(); });");
	    	sb.appendLine();
	    }
    	
    	for (Entry<String, String> func : this.funcs.entrySet()) {
        	sb.appendLine("\t\t $('#" + func.getKey() + "').click(" + func.getValue() + ");");
	    	sb.appendLine();
    	}
    	
    	sb.appendLine("\t });");

    	doc.addBindingScript(sb.toString());
    	*/
    	
    	
    	/*
    	sb.appendLine();
    	sb.appendLine("$(document).bind('pageinit', function(){");
    	
    	// form loader supports
    	sb.appendLine("\t $('#frm" + this.name + "').dcForm({");

		String recorder = this.src.getAttribute("RecordOrder");	// a comma list
    	
    	if (StringUtil.isNotEmpty(recorder))
        	sb.appendLine("\t\t RecordOrder: '" + recorder + "', ");
    	else {
    		Set<String> recs = new HashSet<String>();
    		
        	for (int i = 0; i < this.inputs.size(); i++) {
        		IFormInput input = this.inputs.get(i);
        		recs.add(input.getRecord());
        	}    		
        	
        	sb.appendLine("\t\t RecordOrder: '" + StringUtils.join(recs, ",") + "', ");
    	}
    	
    	// track inputs
    	sb.appendLine("\t\t Inputs: {");

    	boolean fnd = false;
    	
    	for (int i = 0; i < this.inputs.size(); i++) {
    		IFormInput input = this.inputs.get(i); 
	    	
    		String name = input.getInputName();
    		String props = input.getProps();
    		
    		if ((name != null) && (props != null)) {
    			if (fnd)
    				sb.appendLine(", ");
    			
    	    	sb.append("\t\t\t\t " + name + ": " + props);
    	    	
    	    	fnd = true;
    		}
    	}    	
    	
    	if (fnd)
    		sb.appendLine();
    	
    	sb.appendLine("\t\t },");
    	
    	// TODO add other features?
    	
    	sb.appendLine("\t\t FormName: '" + this.name + "', ");
    	sb.appendLine("\t\t PageName: '" + Page.findPage(this).getPageName() + "'");
		
		// form loader end
    	sb.appendLine("\t });");
    	
    	
    	
    	----------------------------
    	
    	
    	
    	// add validator messages
    	
    	// 'NULL' is accepted because it should be treated as a null internally
    	sb.appendLine("\t $.validator.addMethod('dcDataType', function(value, element, param) {\n\t\t if (this.optional(element)) return true;\n\t\t "
    			+ "if (value == 'NULL') return true;\n\t\t "
    			+ "return (dc.schema.Manager.validate(value, param).Code == 0); \n\t}, 'Invalid format.');");
    	sb.appendLine("\t $.validator.addMethod('dcJson', function(value, element, param) {\n\t\t if (this.optional(element)) return true;\n\t\t "
    			+ "try { JSON.parse(value); return true; } catch (x) { } return false; \n\t}, 'Invalid JSON.');");
    	sb.appendLine();
    	
    	for (int i = 0; i < this.inputs.size(); i++) {
    		IFormInput input = this.inputs.get(i); 
    		String func = input.getValidation().getValueFunction();
    		String msg = input.getValidation().getMessage();
    		
    		if (func != null)
    	    	sb.appendLine("\t $.validator.addMethod('inline" + this.name + i 
    	    			+ "', function(value) { " + func + " }, '" 
    	    			+ StringUtil.escapeSingleQuotes(msg) + "');");
    	}    	
    	
    	sb.appendLine();
    	
    	// add rules and messages
    	
    	sb.appendLine("\t $('#frm" + this.name + "').validate({");
    	
    	// rules
    	sb.appendLine("\t\t rules: {");

    	fnd = false;
    	
    	for (int i = 0; i < this.inputs.size(); i++) {
    		IFormInput input = this.inputs.get(i); 
    		String rule = input.getValidation().getRule();
    		
    		if (rule != null) {
    			if (fnd)
    				sb.appendLine(", ");
    			
    	    	sb.append("\t\t\t " + input.getInputId() + ": " + rule);
    	    	
    	    	fnd = true;
    		}
    	}    	
		
		if (fnd)
			sb.appendLine();
    	
    	sb.appendLine("\t\t },");
    	
    	// messages
    	sb.appendLine("\t\t messages: {");

    	fnd = false;
    	
    	for (int i = 0; i < this.inputs.size(); i++) {
    		IFormInput input = this.inputs.get(i); 
    		String msg = input.getValidation().getMessage();
    		
    		if (msg != null) {
    			if (fnd)
    				sb.appendLine(", ");
    			
    	    	sb.append("\t\t\t " + input.getInputId() + ": " + msg);
    	    	
    	    	fnd = true;
    		}
    	}    	
		
		if (fnd)
			sb.appendLine();
    	
    	sb.appendLine("\t\t },");
    	
    	// invalid handler
    	sb.appendLine("\t\t invalidHandler: function() {");
    	sb.appendLine("\t\t\t $(document).dcDocument('showWarnPopup', 'Missing or invalid inputs, please correct.');");
    	sb.appendLine("\t\t },");
		
		// submit handler
    	sb.appendLine("\t\t submitHandler: function(form) {");

    	if (StringUtil.isNotEmpty(this.savebtn))
        	sb.appendLine("\t\t\t $('#frm" + this.name + "').dcForm('save');");
    	
		sb.appendLine("\t\t }");
		
		// validation end
    	sb.appendLine("\t });");
    	
    	sb.appendLine();

    	// add event shims
    	
    	if (this.src.hasAttribute("OnBeforeLoad"))
    		sb.appendLine("\t $('#frm" + this.name + "').bind('beforeLoad.dcForm', " + this.src.getAttribute("OnBeforeLoad") + ");");
    	
    	if (this.src.hasAttribute("OnLoadRecord"))
    		sb.appendLine("\t $('#frm" + this.name + "').bind('loadRecord.dcForm', " + this.src.getAttribute("OnLoadRecord") + ");");
    	
    	if (this.src.hasAttribute("OnSaveRecord"))
    		sb.appendLine("\t $('#frm" + this.name + "').bind('saveRecord.dcForm', " + this.src.getAttribute("OnSaveRecord") + ");");
    	
    	if (this.src.hasAttribute("OnAfterSaveRecord"))
    		sb.appendLine("\t $('#frm" + this.name + "').bind('afterSaveRecord.dcForm', " + this.src.getAttribute("OnAfterSaveRecord") + ");");    	
    	
    	if (this.src.hasAttribute("OnAfterSave"))
    		sb.appendLine("\t $('#frm" + this.name + "').bind('afterSave.dcForm', " + this.src.getAttribute("OnAfterSave") + ");");    	
    	
    	// add form button code
    	
    	if (StringUtil.isNotEmpty(this.savebtn)) {
        	sb.appendLine("\t $('#" + this.savebtn + "').click(function() { $('#frm" + this.name + "').validate().form(); });");
	    	sb.appendLine();
	    }
    	
    	for (Entry<String, String> func : this.funcs.entrySet()) {
        	sb.appendLine("\t $('#" + func.getKey() + "').click(" + func.getValue() + ");");
	    	sb.appendLine();
    	}
    	
    	sb.appendLine("});");
    	
    	sb.appendLine();
    	sb.appendLine("$(document).bind('mobileinit', function(){");

    	sb.appendLine("\t var cntdwn = new dc.lang.CountDownCallback(2, function() { $('#frm" + this.name + "').dcForm('load'); });");

    	sb.appendLine("\t $('#" + pg.getPageId() + "').live('pageinit', function() {\n\t\t cntdwn.dec();\n\t });");
		sb.appendLine("\t $(document).bind('init.dcDocument', function() {\n\t\t cntdwn.dec();\n\t });");
    	sb.appendLine("\t $.mobile.ajaxEnabled = false;");
    	
    	sb.appendLine("});");
    	*/

    	//ph.addChildren(new Script(new LiteralText(sb.toString())));
	}
    
    static public Form findForm(Element el) {
    	while (el != null) {
    		if (el instanceof Form)
    			return (Form)el;
    		
    		el = el.getParent();
    	}
    	
    	return null;
    }
}
