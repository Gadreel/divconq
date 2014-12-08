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
package divconq.struct.builder;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import divconq.util.Base64;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import divconq.lang.BigDateTime;
import divconq.lang.Memory;
import divconq.struct.CompositeStruct;
import divconq.struct.RecordStruct;
import divconq.struct.scalar.AnyStruct;
import divconq.struct.scalar.BigDateTimeStruct;
import divconq.struct.scalar.BigIntegerStruct;
import divconq.struct.scalar.BinaryStruct;
import divconq.struct.scalar.BooleanStruct;
import divconq.struct.scalar.DateTimeStruct;
import divconq.struct.scalar.DecimalStruct;
import divconq.struct.scalar.IntegerStruct;
import divconq.struct.scalar.NullStruct;
import divconq.struct.scalar.StringStruct;
import divconq.xml.XNode;

public class XmlStreamBuilder implements ICompositeBuilder {
	protected BuilderInfo cstate = null;
	protected List<BuilderInfo> bstate = new ArrayList<BuilderInfo>(); 
	protected boolean complete = false;
	protected PrintStream pw = null;
	protected boolean pretty = false;
	
	public XmlStreamBuilder(PrintStream pw) {
		this.pw = pw;
	}
	
	public XmlStreamBuilder(PrintStream pw, boolean pretty) {
		this.pw = pw;
		this.pretty = pretty;
	}
	
	@Override
	public BuilderState getState() {
		return (this.cstate != null) ? this.cstate.State : (this.complete) ? BuilderState.Complete : BuilderState.Ready;
	}
	
	@Override
	public ICompositeBuilder record(Object... props) throws BuilderStateException {
		this.startRecord();
		
		String name = null;
		
		for (Object o : props) {
			if (name != null) {
				this.field(name, o);				
				name = null;
			}
			else {
				if (o == null)
					throw new BuilderStateException("Null Field Name");
					
				name = o.toString();
			}
		}
		
		this.endRecord();
		
		return this;
	}
	
	@Override
	public ICompositeBuilder startRecord() throws BuilderStateException {
		// if in a list and need comma
		if ((this.cstate != null) && (this.cstate.State == BuilderState.InList) && this.cstate.CommaNeeded) {
			//this.write(", ");
			
			if (this.pretty) { 
				this.write("\n");
				this.indent();
			}
			
			this.cstate.CommaNeeded = false;
		}
		
		// indicate we are in a record
		this.cstate = new BuilderInfo(BuilderState.InRecord, (this.cstate != null) ? this.cstate.indent + 1 : 1);
		this.bstate.add(cstate);
		
		this.write("<Record>");
		
		if (this.pretty) { 
			this.write("\n");
			this.indent();
		}
		
		return this;
	}
	
	@Override
	public ICompositeBuilder endRecord() throws BuilderStateException {
		// cannot call end rec with being in a record or field
		if ((this.cstate == null) || (this.cstate.State == BuilderState.InList))
			throw new BuilderStateException("Cannot end record when in list");
		
		// if in a field, finish it
		if (this.cstate.State == BuilderState.InField)
			this.endField();
		
		// return to parent
		this.popState();
		
		if (this.pretty) { 
			this.write("\n");
			this.indent();
		}
		
		this.write("</Record>");
		
		// mark the value complete, let parent container know we need commas
		this.completeValue();
		
		return this;
	}

	// names may contain only alpha-numerics
	@Override
	public ICompositeBuilder field(String name, Object value) throws BuilderStateException {
		this.field(name);
		this.value(value);
		
		return this;
	}

	/*
	 * OK to call if in an unnamed field already (then adds name to the field)
	 * or to call if in a record straight up
	 */
	@Override
	public ICompositeBuilder field(String name) throws BuilderStateException {
		// fields cannot occur outside of records
		if ((this.cstate == null) || (this.cstate.State == BuilderState.InList))
			throw new BuilderStateException("Cannot add field when in list");
		
		// if in a named field, finish it
		if ((this.cstate.State == BuilderState.InField) && this.cstate.IsNamed)
			this.endField();
		
		// if not yet in a field mark as such
		if (this.cstate.State == BuilderState.InRecord)
			this.field();
		
		this.value(name);
		
		return this;
	}
	
	@Override
	public ICompositeBuilder field() throws BuilderStateException {
		// fields cannot occur outside of records
		if ((this.cstate == null) || (this.cstate.State == BuilderState.InList))
			throw new BuilderStateException("Cannot add field when in list");
		
		// if already in field then pop out of it
		if (this.cstate.State == BuilderState.InField)
			this.endField();
		
		// if pop leaves us hanging or not in record then bad
		if ((this.cstate == null) || (this.cstate.State != BuilderState.InRecord))
			throw new BuilderStateException("Cannot end field when not in record");
		
		// we should now be at record level, check for comma state
		if (this.cstate.CommaNeeded) {
			//this.write(", ");
		
			if (this.pretty) { 
				this.write("\n");
				this.indent();
			}
			
			this.cstate.CommaNeeded = false;
		}
		
		// note that we are in a field now, value not completed
		this.cstate = new BuilderInfo(BuilderState.InField, this.cstate.indent);
		this.bstate.add(cstate);
		
		return this;
	}
	
	private void endField() throws BuilderStateException {
		// cannot occur outside of field
		if ((this.cstate == null) || (this.cstate.State == BuilderState.InList) || (this.cstate.State == BuilderState.InRecord))
			throw new BuilderStateException("Cannot end field when not in field");
		
		// end the field
		if (!this.cstate.ValueComplete)
			this.write("<Scalar />");
		
		this.write("</Field>");

		// return to the record state
		this.popState();
		
		// we should now be at record level, mark comma state
		this.cstate.CommaNeeded = true;
	}
	
	@Override
	public ICompositeBuilder list(Object... props) throws BuilderStateException {
		this.startList();
		
		for (Object o : props)
			this.value(o);
		
		this.endList();
		
		return this;
	}
	
	@Override
	public ICompositeBuilder startList() throws BuilderStateException {
		// if in a list and need comma
		if ((this.cstate != null) && ((this.cstate.State == BuilderState.InList) || (this.cstate.State == BuilderState.InField)) && this.cstate.CommaNeeded) {
			//this.write(", ");
			
			if (this.pretty) { 
				this.write("\n");
				this.indent();
			}
			
			this.cstate.CommaNeeded = false;
		}
		
		// mark that we are in a list
		this.cstate = new BuilderInfo(BuilderState.InList, (this.cstate != null) ? this.cstate.indent + 1 : 1);
		this.bstate.add(cstate);
		
		// start out complete (an empty list is complete)
		this.cstate.ValueComplete = true;
		
		this.write("<List>");
		
		if (this.pretty) { 
			this.write("\n");
			this.indent();
		}
		
		return this;
	}
	
	@Override
	public ICompositeBuilder endList() throws BuilderStateException {
		// must be in a list
		if ((this.cstate == null) || (this.cstate.State != BuilderState.InList))
			throw new BuilderStateException("Cannot end list when not in list");
		
		if (!this.cstate.ValueComplete)
			this.endItem();

		// return to parent state
		this.popState();
		
		if (this.pretty) { 
			this.write("\n");
			this.indent();
		}
		
		// end list
		this.write("</List>");
		
		// mark the value complete, let parent container know we need commas
		this.completeValue();
		
		return this;
	}
	
	private void indent() {
		if (this.cstate == null)
			return;
		
		for (int i = 0; i < this.cstate.indent; i++)
			this.writeChar('\t');
	}

	private void endItem() throws BuilderStateException {
		// cannot occur outside of field
		if ((this.cstate == null) || (this.cstate.State != BuilderState.InList))
			throw new BuilderStateException("Cannot end item when not in list");
		
		// end the item
		if (!this.cstate.ValueComplete) {
			this.write("<Scalar />");
			
			// tell list that the lastest entry is complete
			this.cstate.ValueComplete = true;
		}
		
		// note need for comma in parent
		this.cstate.CommaNeeded = true;
	}
	
	@Override
	public boolean needFieldName() {
		if (this.cstate == null)
			return false;

		return ((this.cstate.State == BuilderState.InField) && !this.cstate.IsNamed);
	}
	
	@Override
	public ICompositeBuilder value(Object value) throws BuilderStateException {
		// cannot occur outside of field or list
		if ((this.cstate == null) || ((this.cstate.State != BuilderState.InField) && (this.cstate.State != BuilderState.InList)))
			throw new BuilderStateException("Cannot add value unless in field or in list");

		if ((this.cstate.State == BuilderState.InField) && !this.cstate.IsNamed) {
			String name = value.toString();
			
			if (!RecordStruct.validateFieldName(name))
				throw new BuilderStateException("Invalid field name");
				
			this.write("<Field Name=\"" + name + "\">");
			
			this.cstate.IsNamed = true;
			
			return this;
		}
		
		// if in a list, check if we need a comma 
		if (this.cstate.State == BuilderState.InList) {			
			if (!this.cstate.ValueComplete)
				this.endItem();
			
			this.cstate.ValueComplete = false;
			
			// we should now be at list level, check for comma state
			if (this.cstate.CommaNeeded) {
				//this.write(", ");
				
				if (this.pretty) { 
					this.write("\n");
					this.indent();
				}
				
				this.cstate.CommaNeeded = false;
			}
		}
		
		// TODO cleanup - handle data more like JsonBuilder
		
		// TODO handle other object types - reader, etc
		if (value instanceof AnyStruct)
			value = ((AnyStruct)value).getValue();
		
		if (value == null) 
			this.write("<Scalar />");
		else if (value instanceof NullStruct)
			this.write("<Scalar />");
		else if (value instanceof BooleanStruct)
			this.write(((BooleanStruct)value).getValue() ? "<Scalar Value=\"True\" />" : "<Scalar Value=\"False\" />");
		else if (value instanceof IntegerStruct)
			this.write("<Scalar Value=\"" + ((IntegerStruct)value).getValue() + "\" />");
		else if (value instanceof BigIntegerStruct)
			this.write("<Scalar Value=\"" + ((BigIntegerStruct)value).getValue() + "\" />");
		else if (value instanceof DecimalStruct)
			this.write("<Scalar Value=\"" + ((DecimalStruct)value).getValue() + "\" />");
		else if (value instanceof DateTimeStruct)
			this.write("<Scalar Value=\"" + ((DateTimeStruct)value).toString() + "\" />");
		else if (value instanceof BigDateTimeStruct)
			this.write("<Scalar Value=\"" + ((DateTimeStruct)value).getValue() + "\" />");
		else if (value instanceof BinaryStruct) {
			String output = Base64.encodeToString(((BinaryStruct)value).getValue().toArray(), false);
			
			if ((output.length() < 65) && !output.contains("\t") && !output.contains("\n")) {		
				this.write("<Scalar Value=\"");
				this.write(XNode.quote(output));		// TODO more efficient
				this.write("\" />");
			}
			else {
				this.write("<Scalar>");
				this.write(output);
				this.write("</Scalar>");
			}
		}
		else if (value instanceof StringStruct) {
			String output = ((StringStruct)value).toString();
			
			if ((output.length() < 65) && !output.contains("\t") && !output.contains("\n")) {		
				this.write("<Scalar Value=\"");
				this.write(XNode.quote(output));		// TODO more efficient
				this.write("\" />");
			}
			else {
				this.write("<Scalar>");
				this.write(output);
				this.write("</Scalar>");
			}
		}
		else if (value instanceof Boolean)
			this.write((Boolean)value ? "<Scalar Value=\"True\" />" : "<Scalar Value=\"False\" />");
		else if (value instanceof Number) 
			this.write("<Scalar Value=\"" + value.toString() + "\" />");
		else if (value instanceof DateTime)
			this.write("<Scalar Value=\"" + ((DateTime)value).toDateTime(DateTimeZone.UTC) + "\" />");
		else if (value instanceof BigDateTime)
			this.write("<Scalar Value=\"" + value + "\" />");
		else if (value instanceof CompositeStruct)
			((CompositeStruct)value).toBuilder(this);
		else if (value instanceof ICompositeOutput)
			this.write(value.toString());		// TODO no, not really - need to work with object (e.g. raw json) this is place holder until we use new parser
		//else if (value instanceof ByteBuffer) 
			//this.write("\"" + Base64.encodeBase64String(((ByteBuffer)value).array()) + "\"");		// TODO more efficient
		//else if (value instanceof byte[]) 
			//this.write("\"" + Base64.encodeBase64String((byte[])value) + "\"");		// TODO more efficient
		//else if (value instanceof Elastic)
			//((Elastic)value).toBuilder(this); 
		else {
			String output = value.toString();
			
			if ((output.length() < 65) && !output.contains("\t") && !output.contains("\n")) {		
				this.write("<Scalar Value=\"");
				this.write(XNode.quote(output));		// TODO more efficient
				this.write("\" />");
			}
			else {
				this.write("<Scalar>");
				this.write(output);
				this.write("</Scalar>");
			}
		}
		
		// mark the value complete, let parent container know we need commas
		this.completeValue();
		
		return this;
	}
	
	@Override
	public ICompositeBuilder rawJson(Object value) throws BuilderStateException {
		// cannot occur outside of field or list
		if ((this.cstate == null) || (this.cstate.State == BuilderState.InRecord))
			throw new BuilderStateException("Cannot add JSON when not in field or in list");

		if ((this.cstate.State == BuilderState.InField) && !this.cstate.IsNamed) 
			throw new BuilderStateException("Cannot use JSON for name of field");
		
		if (value instanceof Memory)
			((Memory)value).copyToStream(this.pw);
		else
			// TODO handle other object types - reader, etc
			this.write(value.toString());		// TODO more efficient
		
		// mark the value complete, let parent container know we need commas
		this.completeValue();
		
		return this;
	}
	
	private void popState() throws BuilderStateException {
		if (this.cstate == null)
			throw new BuilderStateException("Cannot pop state when no state is present");
		
		this.bstate.remove(this.bstate.size() - 1);
		
		if (this.bstate.size() == 0) {
			this.cstate = null;
			this.complete = true;
		}
		else
			this.cstate = this.bstate.get(this.bstate.size() - 1);
	}
	
	private void completeValue() throws BuilderStateException {
		// if parent, mark it a having a complete value
		if (this.cstate != null) {
			this.cstate.ValueComplete = true;
			
			if (this.cstate.State == BuilderState.InField)
				this.endField();
			else
				this.endItem();
		}
	}
	
	@Override
	public Memory toMemory() {
		// incompatible concepts
		return null;
	}
	
	@Override
	public CompositeStruct toLocal() {
		// incompatible concepts
		return null;
	}

	public void write(String v) {
		this.pw.append(v);
	}

	public void writeChar(char v) {
		this.pw.append(v);
	}	
}
