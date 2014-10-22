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
package divconq.struct;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import divconq.hub.Hub;
import divconq.lang.BigDateTime;
import divconq.lang.Memory;
import divconq.lang.OperationResult;
import divconq.schema.DataType;
import divconq.script.StackEntry;
import divconq.struct.builder.BuilderStateException;
import divconq.struct.builder.ICompositeBuilder;
import divconq.struct.scalar.IntegerStruct;
import divconq.struct.scalar.NullStruct;
import divconq.util.ClassicIterableAdapter;
import divconq.util.IAsyncIterable;
import divconq.util.StringUtil;
import divconq.xml.XElement;

/**
 * DivConq uses a specialized type system that provides type consistency across services 
 * (including web services), database fields and stored procedures, as well as scripting.
 * 
 * All scalars (including primitives) and composites (collections) are wrapped by some
 * subclass of Struct.  List/array collections are expressed by this class.  
 * This class is analogous to an Array in JSON but may contain type information as well, 
 * similar to Yaml.
 * 
 *  TODO link to blog entries.
 * 
 * @author Andy
 *
 */
public class ListStruct extends CompositeStruct implements IItemCollection {
	protected List<Struct> items = new CopyOnWriteArrayList<Struct>();		// TODO can we make a more efficient list (one that allows modifications but won't crash an iterator)

	@Override
	public DataType getType() {
		if (this.explicitType != null)
			return super.getType();

		// implied only, not explicit
		return Hub.instance.getSchema().getType("AnyList");
	}
	
	/**
	 * Provide data type info (schema for fields) and a list of initial items
	 * 
	 * @param type field schema
	 * @param items initial values
	 */
	public ListStruct(DataType type, Object... items) {
		super(type);
		this.addItem(items);
	}
	
	public ListStruct(DataType type, Collection<? extends Object> items) {
		super(type);
		this.addCollection(items);
	}
	
	/**
	 * Optionally provide a list of initial items
	 * 
	 * @param items initial values
	 */
	public ListStruct(Object... items) {
		this.addItem(items);
	}
	
	public ListStruct(Collection<? extends Object> items) {
		this.addCollection(items);
	}
	
	/* (non-Javadoc)
	 * @see divconq.struct.CompositeStruct#select(divconq.struct.PathPart[])
	 */
	@Override
	public Struct select(PathPart... path) {
		if (path.length == 0)
			return this;
		
		PathPart part = path[0];
		OperationResult log = part.getLog();
		
		// not allowed
		if (log == null)
			return NullStruct.instance;
		
		String fld = part.getField();
		
		if ("Length".equals(fld))
			return new IntegerStruct(this.items.size());
		
		if (fld != null) {
			log.warnTr(501, this);
			return NullStruct.instance;
		}
		
		int idx = part.getIndex();
		
		if (idx >= this.items.size()) {
			log.warnTr(502, part.getIndex());
			return NullStruct.instance;
		}
		
		Struct o = this.items.get(idx);
		
		if (path.length == 1) 
			return o;			
		
		if (o instanceof CompositeStruct) 
			return ((CompositeStruct)o).select(Arrays.copyOfRange(path, 1, path.length));		
		
		log.warnTr(503, o);
		return NullStruct.instance;
	}
	
	public Stream<Struct> structStream() {
		return this.items.stream();
	}
	
	public Stream<RecordStruct> recordStream() {
		return this.items.stream().map(p -> (RecordStruct)p);
	}
	
	public Stream<String> stringStream() {
		return this.items.stream().map(p -> Struct.objectToString(p));
	}
	
	public Stream<Long> integerStream() {
		return this.items.stream().map(p -> Struct.objectToInteger(p));
	}
	
	/* (non-Javadoc)
	 * @see divconq.struct.Struct#isBlank()
	 */
	@Override
	public boolean isEmpty() {
		return (this.items.size() == 0);
	}
	
	/* (non-Javadoc)
	 * @see divconq.struct.builder.ICompositeOutput#toBuilder(divconq.struct.builder.ICompositeBuilder)
	 */
	@Override
	public void toBuilder(ICompositeBuilder builder) throws BuilderStateException {
		builder.startList();
		
		for (Object o : this.items) 
			builder.value(o);
		
		builder.endList();
	}

	/**
	 * Attempt to add items to the list, but if there is a schema the items
	 * must match the schema.
	 * 
	 * @param items to add
	 * @return log of the result of the call (check hasErrors)
	 */
	public OperationResult addItem(Object... items) {
		OperationResult or = new OperationResult();
		
		for (Object o : items) {
			Object value = o;
			Struct svalue = null;
			
			if (value instanceof ICompositeBuilder)
				value = ((ICompositeBuilder)value).toLocal();
			
			if (this.explicitType != null) {
				Struct sv = this.explicitType.wrapItem(value, or);
				
				if (sv != null)
					svalue = sv;
			}
			
			if (svalue == null) 
				svalue = Struct.objectToStruct(value); 
			
			this.items.add(svalue);
		}
		
		return or;
	}
	
	public OperationResult addCollection(Collection<? extends Object> coll) {
		OperationResult or = new OperationResult();
		
		for (Object o : coll)
			or.copyMessages(this.addItem(o));		// extra slow, enhance TOTO
		
		return or;
	}
	
	public OperationResult addCollection(ListStruct coll) {
		OperationResult or = new OperationResult();
		
		for (Struct o : coll.getItems())
			or.copyMessages(this.addItem(o));		// extra slow, enhance TOTO
		
		return or;
	}
	
	/**
	 * 
	 * @return collection of all the items the list holds
	 */
	@Override
	public Iterable<Struct> getItems() {
		return this.items;
	}
	
	@Override
	public IAsyncIterable<Struct> getItemsAsync() {
		return new ClassicIterableAdapter<Struct>(this.items);
	}
	
	/**
	 * 
	 * @param idx position in list of the item desired (0 based)
	 * @return the struct for that field
	 */
	public Struct getItem(int idx) {
		if ((idx >= this.items.size()) || (idx < 0))
			return null;
		
		return this.items.get(idx);
	}
	
	/**
	 * 
	 * @param idx position in list of the item desired (0 based)
	 * @return true if an item is at that position
	 */
	public boolean hasItem(int idx) {
		if (idx >= this.items.size())
			return false;
		
		return true;
	}
	
	/**
	 * Unlike getItem, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param idx position in list of the item desired (0 based)
	 * @return field's "inner" value as Integer (DivConq thinks of integers as 64bit)
	 */
	public Long getItemAsInteger(int idx) {
		return Struct.objectToInteger(this.getItem(idx));
	}
	
	/**
	 * Unlike getItem, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param idx position in list of the item desired (0 based)
	 * @return field's "inner" value as BigInteger 
	 */
	public BigInteger getItemAsBigInteger(int idx) {
		return Struct.objectToBigInteger(this.getItem(idx));
	}
	
	/**
	 * Unlike getItem, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param idx position in list of the item desired (0 based)
	 * @return field's "inner" value as BigDecimal
	 */
	public BigDecimal getItemAsDecimal(int idx) {
		return Struct.objectToDecimal(this.getItem(idx));
	}
	
	/**
	 * Unlike getItem, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param idx position in list of the item desired (0 based)
	 * @return field's "inner" value as Boolean
	 */
	public Boolean getItemAsBoolean(int idx) {
		return Struct.objectToBoolean(this.getItem(idx));
	}
	
	/**
	 * Unlike getItem, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param idx position in list of the item desired (0 based)
	 * @return field's "inner" value as Datetime
	 */
	public DateTime getItemAsDateTime(int idx) {
		return Struct.objectToDateTime(this.getItem(idx));
	}
	
	/**
	 * Unlike getItem, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param idx position in list of the item desired (0 based)
	 * @return field's "inner" value as Datetime
	 */
	public BigDateTime getItemAsBigDateTime(int idx) {
		return Struct.objectToBigDateTime(this.getItem(idx));
	}
	
	/**
	 * Unlike getItem, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param idx position in list of the item desired (0 based)
	 * @return field's "inner" value as Date
	 */
	public LocalDate getItemAsDate(int idx) {
		return Struct.objectToDate(this.getItem(idx));
	}
	
	/**
	 * Unlike getItem, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param idx position in list of the item desired (0 based)
	 * @return field's "inner" value as Time
	 */
	public LocalTime getItemAsTime(int idx) {
		return Struct.objectToTime(this.getItem(idx));
	}
	
	/**
	 * Unlike getItem, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param idx position in list of the item desired (0 based)
	 * @return field's "inner" value as String
	 */
	public String getItemAsString(int idx) {
		return Struct.objectToString(this.getItem(idx));
	}
	
	/**
	 * Unlike getItem, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param idx position in list of the item desired (0 based)
	 * @return field's "inner" value as Binary
	 */
	public Memory getItemAsBinary(int idx) {
		return Struct.objectToBinary(this.getItem(idx));
	}
	
	/**
	 * Unlike getItem, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param idx position in list of the item desired (0 based)
	 * @return field's "inner" value as CompositeStruct
	 */
	public CompositeStruct getItemAsComposite(int idx) {
		return Struct.objectToComposite(this.getItem(idx));
	}
	
	/**
	 * Unlike getItem, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param idx position in list of the item desired (0 based)
	 * @return field's "inner" value as RecordStruct
	 */
	public RecordStruct getItemAsRecord(int idx) {
		return Struct.objectToRecord(this.getItem(idx));
	}
	
	/**
	 * Unlike getItem, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param idx position in list of the item desired (0 based)
	 * @return field's "inner" value as ListStruct
	 */
	public ListStruct getItemAsList(int idx) {
		return Struct.objectToList(this.getItem(idx));
	}
	
	/**
	 * Unlike getItem, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param idx position in list of the item desired (0 based)
	 * @return field's "inner" value as Struct
	 */
	public Struct getItemAsStruct(int idx) {
		return Struct.objectToStruct(this.getItem(idx));
	}
	
	/**
	 * Unlike getItem, this returns the value (inner) rather than struct wrapping 
	 * the value.
	 * 
	 * @param idx position in list of the item desired (0 based)
	 * @return field's "inner" value as Xml (will parse if value is string)
	 */
	public XElement getItemAsXml(int idx) {
		return Struct.objectToXml(this.getItem(idx));
	}
	
	/**
	 * @param idx position in list of the item desired (0 based)
	 * @return true if item does not exist or if item is string and its value is empty 
	 */
	public boolean isItemEmpty(int idx) {
		if (idx >= this.items.size())
			return true;
		
		Object o = this.items.get(idx);
		
		if (o == null)
			return true;
		
		if (o instanceof CharSequence)
			return o.toString().isEmpty();
		
		return false;
	}
	
	/**
	 * @return number of items in this list
	 */
	public int getSize() {
		return this.items.size();
	}

	/**
	 * @param idx position in list of the item to remove from list
	 */
	public void removeItem(int idx) {		
		if (idx >= this.items.size())
			return;
		
		this.items.remove(idx);
	}

	/*
	 * @param idx position in list of the item to remove from list
	 */
	public void removeItem(Struct itm) {
		// TODO dispose
		//Struct old = this.items.get(itm);
		
		//if (old != null)
		//	old.dispose();
		
		this.items.remove(itm);
	}

    @Override
    protected void doCopy(Struct n) {
    	super.doCopy(n);
    	
    	ListStruct nn = (ListStruct)n;
    	
   		nn.addCollection(this.items);
    }
    
	@Override
	public Struct deepCopy() {
		ListStruct cp = new ListStruct();
		this.doCopy(cp);
		return cp;
	}

	/**
	 * 
	 * @return schema for the primary/default data type of the list items
	 */
	public DataType getChildType() {
		if (this.explicitType != null) 
			return this.explicitType.getPrimaryItemType();
		
		return null;
	}

	/* (non-Javadoc)
	 * @see divconq.struct.CompositeStruct#clear()
	 */
	@Override
	public void clear() {
		this.items.clear();
	}
	
	@Override
	public void operation(StackEntry stack, XElement code) {
		if ("Set".equals(code.getName())) {
			this.clear();
			
			String json = stack.resolveValueToString(code.getText());
			
			if (StringUtil.isNotEmpty(json)) {
				ListStruct pjson = (ListStruct) CompositeParser.parseJson(" [ " + json + " ] ").getResult();

				for (Struct s : pjson.getItems())
					this.items.add(s);
			}
			
			// TODO else check for Xml or Yaml
			
			stack.resume();
			return;
		}
		else if ("AddItem".equals(code.getName())) {
			Struct sref = stack.refFromElement(code, "Value"); 
			this.addItem(sref);
			stack.resume();
			return;
		}
		else if ("RemoveItem".equals(code.getName())) {
			long idx = stack.intFromElement(code, "Index", -1); 
			
			if (idx > -1)
				this.removeItem((int) idx);
			
			stack.resume();
			return;
		}
		else if ("Clear".equals(code.getName())) {
			this.clear();
			
			stack.resume();
			return;
		}
		
		super.operation(stack, code);
	}

	public List<String> toStringList() {
		List<String> nlist = new ArrayList<>();
		
		for (Struct s : this.items) 
			if (s != null)
				nlist.add(s.toString());
		
		return nlist;
	}

	public boolean contains(Struct v) {
		return this.items.contains(v);
	}
}
