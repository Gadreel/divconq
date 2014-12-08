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

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import divconq.lang.Memory;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.CompositeParser;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;

public class ObjectBuilder implements ICompositeBuilder {
	protected BuilderInfo cstate = null;
	protected List<BuilderInfo> bstate = new ArrayList<BuilderInfo>(); 
	protected CompositeStruct root = null;		// TODO consider supporting any type of Struct
	
	public CompositeStruct getRoot() {
		return this.root;
	}
	
	@Override
	public BuilderState getState() {
		return (this.cstate != null) ? this.cstate.State : (this.root != null) ? BuilderState.Complete : BuilderState.Ready;
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
		// if in a list, check if we need a comma 
		if ((this.cstate != null) && (this.cstate.State == BuilderState.InList)) {			
			if (!this.cstate.ValueComplete)
				this.endItem(null);
			
			this.cstate.ValueComplete = false;
		}
		
		// indicate we are in a record
		this.cstate = new BuilderInfo(BuilderState.InRecord);
		this.cstate.CurrentE = new RecordStruct();
		
		if (this.root == null)
			this.root = this.cstate.CurrentE;
		
		this.bstate.add(cstate);
		
		return this;
	}
	
	@Override
	public ICompositeBuilder endRecord() throws BuilderStateException {
		// cannot call end rec with being in a record or field
		if (this.cstate == null)			
			throw new BuilderStateException("Cannot end record, structure not started");
		
		if (this.cstate.State == BuilderState.InList)
			throw new BuilderStateException("Cannot end record, in a list");
		
		// if in a field, finish it
		if (this.cstate.State == BuilderState.InField)
			this.endField(null);

		CompositeStruct child = this.cstate.CurrentE;
		
		// return to parent
		this.popState();
		
		// mark the value complete, let parent container know we need commas
		this.completeValue(child);
		
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
		if (this.cstate == null)			
			throw new BuilderStateException("Cannot end record, structure not started");
		
		if (this.cstate.State == BuilderState.InList)
			throw new BuilderStateException("Cannot end record, in a list");
		
		// if in a named field, finish it
		if ((this.cstate.State == BuilderState.InField) && this.cstate.IsNamed)
			this.endField(null);
		
		// if not yet in a field mark as such
		if (this.cstate.State == BuilderState.InRecord)
			this.field();
		
		this.value(name);
		
		return this;
	}
	
	@Override
	public ICompositeBuilder field() throws BuilderStateException {
		// fields cannot occur outside of records
		if (this.cstate == null)			
			throw new BuilderStateException("Cannot end record, structure not started");
		
		if (this.cstate.State == BuilderState.InList)
			throw new BuilderStateException("Cannot end record, in a list");
		
		// if already in field then pop out of it
		if (this.cstate.State == BuilderState.InField)
			this.endField(null);
		
		// if pop leaves us hanging or not in record then bad
		if ((this.cstate == null) || (this.cstate.State != BuilderState.InRecord))
			throw new BuilderStateException("Cannot end field when not in a record");
			
		// note that we are in a field now, value not completed
		this.cstate = new BuilderInfo(BuilderState.InField);
		this.bstate.add(cstate);
		
		return this;
	}
	
	private void endField(CompositeStruct child) throws BuilderStateException {
		// cannot occur outside of field
		if ((this.cstate == null) || (this.cstate.State == BuilderState.InList) || (this.cstate.State == BuilderState.InRecord))
			throw new BuilderStateException("End field cannot occur ourside of a field.");
		
		// field name cannot be empty
		if (StringUtil.isEmpty(this.cstate.CurrentField))
			throw new BuilderStateException("Field cannot end, field name cannot be empty");
		
		BuilderInfo fi = this.cstate;

		// return to the record state
		this.popState();
		
		// add the field to the record
		((RecordStruct)this.cstate.CurrentE).setField(fi.CurrentField, (child != null) ? child : fi.CurrentValue);
		this.cstate.CurrentValue = null;
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
		// if in a list, check if we need a comma 
		if ((this.cstate != null) && (this.cstate.State == BuilderState.InList)) {			
			if (!this.cstate.ValueComplete)
				this.endItem(null);
			
			this.cstate.ValueComplete = false;
		}
		
		// mark that we are in a list
		this.cstate = new BuilderInfo(BuilderState.InList);
		this.cstate.CurrentE = new ListStruct();
		
		if (this.root == null)
			this.root = this.cstate.CurrentE;
		
		this.bstate.add(cstate);
		
		// start out complete (an empty list is complete)
		this.cstate.ValueComplete = true;
		
		return this;
	}
	
	@Override
	public ICompositeBuilder endList() throws BuilderStateException {
		// must be in a list
		if ((this.cstate == null) || (this.cstate.State != BuilderState.InList))
			throw new BuilderStateException("Cannot end list when not in a list");
		
		if (!this.cstate.ValueComplete)
			this.endItem(null);

		CompositeStruct child = this.cstate.CurrentE;
		
		// return to parent state
		this.popState();
		
		// mark the value complete, let parent container know we need commas
		this.completeValue(child);
		
		return this;
	}
		
	private void endItem(CompositeStruct child) throws BuilderStateException {
		// cannot occur outside of field
		if ((this.cstate == null) || (this.cstate.State != BuilderState.InList))
			throw new BuilderStateException("Cannot end a list item when not in a list.");
		
		// end the item
		if (!this.cstate.ValueComplete) {
			((ListStruct)this.cstate.CurrentE).addItem((child != null) ? child : this.cstate.CurrentValue);
			
			// tell list that the latest entry is complete
			this.cstate.CurrentValue = null;
			this.cstate.ValueComplete = true;
		}
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
			throw new BuilderStateException("Value can only be called within a field or a list");

		if ((this.cstate.State == BuilderState.InField) && !this.cstate.IsNamed) {
			String name = value.toString();
			
			if (!RecordStruct.validateFieldName(name))
				throw new BuilderStateException("Invalid field name");
				
			this.cstate.CurrentField = name;
			this.cstate.IsNamed = true;
			return this;
		}
		
		// if in a list, check if we need a comma 
		if (this.cstate.State == BuilderState.InList) {			
			if (!this.cstate.ValueComplete)
				this.endItem(null);
			
			this.cstate.ValueComplete = false;
		}
		
		// TODO handle other object types - reader, etc
		
		if (value == null) 
			this.cstate.CurrentValue = null;
		else if (value instanceof Boolean)
			this.cstate.CurrentValue = value;
		else if (value instanceof Number) 
			this.cstate.CurrentValue = value;
		else if (value instanceof DateTime) 
			this.cstate.CurrentValue = value;
		else if (value instanceof Struct) 
			this.cstate.CurrentValue = value;
		else if (value instanceof ICompositeOutput) {
			((ICompositeOutput)value).toBuilder(this);	
			return this;		// don't mark complete, stuff in value should handle that
		}
		//else if (value instanceof ByteBuffer) 
			//this.write("\"" + Base64.encodeBase64String(((ByteBuffer)value).array()) + "\"");		// TODO more efficient
		//else if (value instanceof byte[]) 
			//this.write("\"" + Base64.encodeBase64String((byte[])value) + "\"");		// TODO more efficient
		//else if (value instanceof Elastic)
			//((Elastic)value).toBuilder(this);
		else {
			this.cstate.CurrentValue = value.toString();		
		}
		
		// mark the value complete, let parent container know we need commas
		this.completeValue(null);
		
		return this;
	}
	
	@Override
	public ICompositeBuilder rawJson(Object value) throws BuilderStateException {
		// cannot occur outside of field or list
		if ((this.cstate == null) || (this.cstate.State == BuilderState.InRecord))
			throw new BuilderStateException("Raw JSON can only occur within a field or a list");

		if ((this.cstate.State == BuilderState.InField) && !this.cstate.IsNamed) 
			throw new BuilderStateException("Raw JSON cannot be used for a field name");

		if (value instanceof CharSequence)		
			this.cstate.CurrentValue = CompositeParser.parseJson((CharSequence)value).getResult();
		else if (value instanceof Memory)		
			this.cstate.CurrentValue = CompositeParser.parseJson((Memory)value).getResult();
		else if (value instanceof RawJson)		// TODO this may not work, test it
			((RawJson)value).toBuilder(this);
		
		// mark the value complete, let parent container know we need commas
		this.completeValue(null);
		
		return this;
	}
	
	private void popState() throws BuilderStateException {
		if (this.cstate == null)
			throw new BuilderStateException("Cannot pop state, structure not started.");
		
		this.bstate.remove(this.bstate.size() - 1);
		
		if (this.bstate.size() == 0)
			this.cstate = null;
		else
			this.cstate = this.bstate.get(this.bstate.size() - 1);
	}
	
	private void completeValue(CompositeStruct child) throws BuilderStateException {
		// if parent, mark it a having a complete value
		if (this.cstate != null) {
			if (this.cstate.State == BuilderState.InField)
				this.endField(child);
			else
				this.endItem(child);
		}
	}
	
	@Override
	public Memory toMemory() throws BuilderStateException {
		if (this.root != null)		
			return this.root.toMemory();
		
		return null;
	}
	
	@Override
	public CompositeStruct toLocal() {
		return this.root;
	}
	
	@Override
	public String toString() {
		try {
			return this.toMemory().toString();
		}
		catch(Exception x) {			
		}
		
		return null;
	}
}
