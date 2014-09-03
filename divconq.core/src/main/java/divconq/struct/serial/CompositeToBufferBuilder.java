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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import divconq.util.Base64;
import divconq.util.TimeUtil;

import divconq.lang.BigDateTime;
import divconq.lang.Memory;
import divconq.lang.chars.Special;
import divconq.lang.chars.Utf8Encoder;
import divconq.struct.CompositeStruct;
import divconq.struct.builder.BuilderInfo;
import divconq.struct.builder.BuilderState;
import divconq.struct.builder.BuilderStateException;
import divconq.struct.builder.ICompositeBuilder;
import divconq.struct.builder.ICompositeOutput;
import divconq.struct.builder.ObjectBuilder;
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

public class CompositeToBufferBuilder implements ICompositeBuilder {
	protected BuilderInfo cstate = null;
	protected List<BuilderInfo> bstate = new ArrayList<BuilderInfo>(); 
	protected boolean complete = false;
	protected ByteBuf buffer = null;
	
	public CompositeToBufferBuilder(ByteBuf buffer) {
		this.buffer = buffer;
	}
	
	@Override
	public BuilderState getState() {
		return (this.cstate != null) ? this.cstate.State : (this.complete) ? BuilderState.Complete : BuilderState.Ready;
	}
	
	@Override
	public void record(Object... props) throws BuilderStateException {
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
	}
	
	@Override
	public void startRecord() throws BuilderStateException {
		// indicate we are in a record
		this.cstate = new BuilderInfo(BuilderState.InRecord);
		this.bstate.add(cstate);
		
		this.write(Special.StartRec); 
	}
	
	@Override
	public void endRecord() throws BuilderStateException {
		// cannot call end rec with being in a record or field
		if ((this.cstate == null) || (this.cstate.State == BuilderState.InList))
			throw new BuilderStateException("Cannot end record when in list");
		
		// if in a field, finish it
		if (this.cstate.State == BuilderState.InField)
			this.endField();
		
		this.write(Special.EndRec); 
		
		// return to parent
		this.popState();
		
		// mark the value complete, let parent container know we need commas
		this.completeValue();
	}

	// names may contain only alpha-numerics
	@Override
	public void field(String name, Object value) throws BuilderStateException {
		this.field(name);
		this.value(value);
	}

	/*
	 * OK to call if in an unnamed field already (then adds name to the field)
	 * or to call if in a record straight up
	 */
	@Override
	public void field(String name) throws BuilderStateException {
		// fields cannot occur outside of records
		if ((this.cstate == null) || (this.cstate.State == BuilderState.InList))
			throw new BuilderStateException("Cannot add field while in list");
		
		// if in a named field, finish it
		if ((this.cstate.State == BuilderState.InField) && this.cstate.IsNamed)
			this.endField();
		
		// if not yet in a field mark as such
		if (this.cstate.State == BuilderState.InRecord)
			this.field();
		
		this.value(name);
	}
	
	@Override
	public void field() throws BuilderStateException {
		// fields cannot occur outside of records
		if ((this.cstate == null) || (this.cstate.State == BuilderState.InList))
			throw new BuilderStateException("Cannot add field when in list");
		
		// if already in field then pop out of it
		if (this.cstate.State == BuilderState.InField)
			this.endField();
		
		// if pop leaves us hanging or not in record then bad
		if ((this.cstate == null) || (this.cstate.State != BuilderState.InRecord))
			throw new BuilderStateException("Cannot end field when not in record");
			
		// note that we are in a field now, value not completed
		this.cstate = new BuilderInfo(BuilderState.InField);
		this.bstate.add(cstate);
		
		this.write(Special.Field);
	}
	
	private void endField() throws BuilderStateException {
		// cannot occur outside of field
		if ((this.cstate == null) || (this.cstate.State == BuilderState.InList) || (this.cstate.State == BuilderState.InRecord))
			throw new BuilderStateException("Cannot end field when not in a field");

		// return to the record state
		this.popState();
	}
	
	@Override
	public void list(Object... props) throws BuilderStateException {
		this.startList();
		
		for (Object o : props)
			this.value(o);
		
		this.endList();
	}
	
	@Override
	public void startList() throws BuilderStateException {
		// mark that we are in a list
		this.cstate = new BuilderInfo(BuilderState.InList);
		this.bstate.add(cstate);
		
		this.write(Special.StartList);
	}
	
	@Override
	public void endList() throws BuilderStateException {
		// must be in a list
		if ((this.cstate == null) || (this.cstate.State != BuilderState.InList))
			throw new BuilderStateException("Not in a list, cannot end list");
		
		// end list
		this.write(Special.EndList);

		// return to parent state
		this.popState();
		
		// mark the value complete, let parent container know we need commas
		this.completeValue();
	}
	
	@Override
	public boolean needFieldName() {
		if (this.cstate == null)
			return false;

		return ((this.cstate.State == BuilderState.InField) && !this.cstate.IsNamed);
	}
	
	@Override
	public void value(Object value) throws BuilderStateException {
		// cannot occur outside of field or list
		if ((this.cstate == null) || (this.cstate.State == BuilderState.InRecord))
			throw new BuilderStateException("Cannot add a value unless in a field or in a list");

		if ((this.cstate.State == BuilderState.InField) && !this.cstate.IsNamed) {
			// TODO check that name is valid
			this.write(value.toString());			
			this.cstate.IsNamed = true;
			return;
		}

		// TODO handle other object types - reader, etc
		if (value instanceof AnyStruct)
			value = ((AnyStruct)value).getValue();
		
		// if object can handle it's own ouput then go ahead and use that 
		if (value instanceof ICompositeOutput) {
			((ICompositeOutput)value).toBuilder(this);
			this.completeValue();
			return;
		}

		// if not a composite then it is a scalar
		this.write(Special.Scalar);
		
		if (value instanceof BooleanStruct)
			value = ((BooleanStruct)value).getValue();
		else if (value instanceof IntegerStruct)
			value = ((IntegerStruct)value).getValue();
		else if (value instanceof BigIntegerStruct)
			value = ((BigIntegerStruct)value).getValue();
		else if (value instanceof DecimalStruct)
			value = ((DecimalStruct)value).getValue();
		else if (value instanceof DateTimeStruct)
			value = ((DateTimeStruct)value).getValue();
		else if (value instanceof BigDateTimeStruct)
			value = ((BigDateTimeStruct)value).getValue();
		else if (value instanceof BinaryStruct) 
			value = ((BinaryStruct)value).getValue();
		else if (value instanceof StringStruct) 
			value = ((StringStruct)value).getValue();	
		
		if ((value == null) || (value instanceof NullStruct))
			this.write("null");
		else if (value instanceof Boolean)
			this.write(value.toString());
		else if ((value instanceof BigDecimal) || (value instanceof Double) || (value instanceof Float)) 
			this.write("(" + value.toString());
		else if (value instanceof Number) 
			this.write(")" + value.toString());
		else if (value instanceof DateTime)
			this.write("@" + TimeUtil.stampFmt.print((DateTime)value));
		else if (value instanceof BigDateTime)
			this.write("$" + ((BigDateTime)value).toString());
		else if (value instanceof ByteBuffer) 
			this.write("%" + Base64.encodeToString(((ByteBuffer)value).array(), false));
		else if (value instanceof ByteBuf) 
			this.write("%" + Base64.encodeToString(((ByteBuf)value).array(), false));
		else if (value instanceof Memory) 
			this.write("%" + Base64.encodeToString(((Memory)value).toArray(), false));
		else if (value instanceof byte[]) 
			this.write("%" + Base64.encodeToString(((byte[])value), false));
		else 
			this.writeEscape("`" + value.toString());		// TODO more efficient with memory/builder/etc
	}
	
	@Override
	// TODO not supported currently, need some sort of escaping/framing
	public void rawJson(Object value) throws BuilderStateException {
		// cannot occur outside of field or list
		if ((this.cstate == null) || (this.cstate.State == BuilderState.InRecord))
			throw new BuilderStateException("Cannot add JSON when not in field or in list");

		if ((this.cstate.State == BuilderState.InField) && !this.cstate.IsNamed) 
			throw new BuilderStateException("Cannot use JSON for field name");
		
		// TODO handle other object types - reader, etc
		//this.write(value.toString());		// TODO more efficient
		throw new BuilderStateException("Cannot use JSON with this builder at this time");
		
		// mark the value complete, let parent container know we need commas
		//this.completeValue();
	}
	
	public void writeEscape(String str) {
		str = str.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");		
		this.write(str);
	}
	
	public void writeEscape(char ch) {
		  switch (ch) {
		    case '\\':
		    	this.write("\\\\");
		    	break;
			case '\n':
				this.write("\\n");
				break;
			case '\t':
				this.write("\\t");
			    break;
		    default:
		    	this.writeChar(ch);
		  }			
	}
	
	private void completeValue() throws BuilderStateException {
		// if parent, mark it a having a complete value
		if (this.cstate != null) {
			if (this.cstate.State == BuilderState.InField)		// be sure to pop the state
				this.endField();
		}
	}
	
	private void popState() throws BuilderStateException {
		if (this.cstate == null)
			throw new BuilderStateException("Cannot pop state when state is null");
		
		this.bstate.remove(this.bstate.size() - 1);
		
		if (this.bstate.size() == 0) {
			this.cstate = null;
			this.complete = true;
		}
		else
			this.cstate = this.bstate.get(this.bstate.size() - 1);
	}
	
	/**
     * Write a single character into Memory as UTF-8.  Increment position accordingly.
     * 
	 * @param ch character to write
	 */
	public void writeChar(int ch) {
		this.buffer.writeBytes(Utf8Encoder.encode(ch));
	}
	
	/**
     * Write a special character into Memory as UTF-8.  Increment position accordingly.
     * 
	 * @param ch character to write
	 */
	public void write(Special ch) {
		this.buffer.writeBytes(Utf8Encoder.encode(ch.getCode()));
	}
	
	/**
     * Write a string into Memory as UTF-8.  Increment position accordingly.
     * 
	 * @param str string to write
	 */
	public void write(CharSequence str) {
		// TODO this could be more efficient - encode <= 64 bytes at a time and add
		this.buffer.writeBytes(Utf8Encoder.encode(str));
	}
	
	@Override
	public Memory toMemory() {
		return new Memory(this.buffer.array());
	}
	
	@Override
	public CompositeStruct toLocal() {
		this.buffer.readerIndex(0);
		
		ObjectBuilder obj = new ObjectBuilder();
		BufferToCompositeParser parser = new BufferToCompositeParser(obj);
		
		try {
			parser.parseStruct(this.buffer);
		} 
		catch (Exception x) {
		}
		
		return obj.getRoot();
	}

}
