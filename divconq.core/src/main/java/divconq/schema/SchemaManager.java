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
package divconq.schema;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import divconq.bus.Message;
import divconq.lang.FuncResult;
import divconq.lang.OperationContext;
import divconq.lang.OperationResult;
import divconq.schema.DataType.DataKind;
import divconq.schema.ServiceSchema.Op;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

/**
 * DivConq uses a specialized type system that provides type consistency across services 
 * (including web services), database fields and stored procedures, as well as scripting.
 * 
 * All scalars (including primitives) and composites (collections) are wrapped by some
 * subclass of Struct.  List/array collections are expressed by this class.  
 * This class is analogous to an Array in JSON but may contain type information as well, 
 * similar to Yaml.
 * 
 * There are schema files (written in Xml and stored in the Packages repository) that define
 * all the known data types, including complex data types.  These schema files get compiled
 * for for a given project and delpoyed as part of the conf directory.
 * 
 * This class oversees the management of all the known data types as well as database
 * tables, stored procedures and services (including web services). 
 */
public class SchemaManager {
	// composite schema of database
	protected DatabaseSchema db = new DatabaseSchema(this);
	
	// composite schema of database
	protected ServiceSchema service = new ServiceSchema(this);
	
	// types with ids
	protected HashMap<String, DataType> knownTypes = new HashMap<String, DataType>();
	
	/**
	 * @return schema pertaining to the dcDb (stored procs, tables, etc)
	 */
	public DatabaseSchema getDb() {
		return this.db;
	}
	
	/**
	 * @return schema pertaining to services 
	 */
	public ServiceSchema getService() {
		return this.service;
	}
		
	/**
	 * @return map of all known data types
	 */
	public Map<String, DataType> knownTypes() {
		return this.knownTypes;
	}
	
	/**
	 * Take a given message and treat it is a service request - see that it is valid.
	 * 
	 * @param msg service request
	 * @return log of validation attempt
	 */
	public OperationResult validateRequest(Message msg){
		OperationResult mr = new OperationResult();
		
		OperationContext tc = OperationContext.get();
		
		if (tc == null) {
			mr.errorTr(431);		
		}
		else {
			Op op = this.service.getOp(msg);
			
			if (op == null)
				mr.errorTr(432);		
			else if (op.request == null)
				mr.errorTr(433);	
			else if (!msg.isVerifyRequest() && !tc.isAuthorized(op.securityTags)) {
				mr.errorTr(434);
				
				System.out.println("cannot call: " + msg);
			}
			else
				op.request.validate(msg, mr);
		}
		
		return mr;
	}
	
	/**
	 * Take a given message and treat it is a service response - see that it is valid.
	 * 
	 * @param msg service response
	 * @param original original request
	 * @return log of validation attempt
	 */
	public OperationResult validateResponse(Message msg, Message original){
		return this.validateResponse(msg, original.getFieldAsString("Service"), original.getFieldAsString("Feature"), original.getFieldAsString("Op"));
	}
	
	/**
	 * Take a given message and treat it is a service response - see that it is valid.
	 * 
	 * @param msg service response
	 * @param service name
	 * @param feature name
	 * @param op name
	 * @return log of validation attempt
	 */
	public OperationResult validateResponse(Message msg, String service, String feature, String op){
		OperationResult mr = new OperationResult();
		
		DataType dt = this.service.getResponseType(service, feature, op);
		
		if (dt == null)
			mr.errorTr(435);		
		else
			dt.validate(msg, mr);
		
		return mr;
	}
	
	/**
	 * For a given stored procedure, check that the parameters comprise a valid request.
	 * 
	 * @param name of the procedure
	 * @param req procedure parameters
	 * @return log of validation attempt
	 */
	public OperationResult validateProcRequest(String name, CompositeStruct req){
		OperationContext tc = OperationContext.get();
		OperationResult mr = new OperationResult();
		
		if (tc == null) {
			mr.errorTr(425);
		}
		else {
			DbProc proc = this.db.getProc(name);
			
			if (proc == null)
				mr.errorTr(426);		
			else {
				if (!tc.isAuthorized(proc.securityTags))
					mr.errorTr(427);		
				else {
					if (proc.request == null) {
						if ((req != null) && !req.isEmpty())
							mr.errorTr(428);		
					}
					else
						proc.request.validate(req, mr);
				}
			}
		}
		
		return mr;
	}
	
	/**
	 * For a given stored procedure, check that the response is a valid structure.
	 * 
	 * @param name of stored procedure
	 * @param resp structure returned
	 * @return log of validation attempt
	 */
	public OperationResult validateProcResponse(String name, CompositeStruct resp){
		DbProc proc = this.db.getProc(name);
		OperationResult mr = new OperationResult();
		
		if (proc == null)
			mr.errorTr(429);		
		else {
			if (proc.response == null) {
				if ((resp != null) && !resp.isEmpty())
					mr.errorTr(430);		
			}
			else
				proc.response.validate(resp, mr);
		}
		
		return mr;
	}
	
	/**
	 * For a given structure, validate that it conforms to a given schema type
	 * 
	 * @param data structure to validate
	 * @param type schema name of type
	 * @return log of validation attempt
	 */
	public OperationResult validateType(Struct data, String type){
		OperationResult mr = new OperationResult();
		
		DataType dt = this.knownTypes.get(type);
		
		if (dt == null)
			mr.errorTr(436);		
		else
			dt.validate(data, mr);
		
		return mr;
	}
	
	// TODO maybe just dump right to builder/output stream instead - save extra work 
	public RecordStruct toJsonDef() {
		RecordStruct def = new RecordStruct();
		
		ListStruct known = new ListStruct();
		def.setField("DataTypes", known);
		
		for (DataType dt : this.knownTypes.values()) 
			known.addItem(dt.toJsonDef(10));
		
		return def;
	}
	
	/**
	 * @param type schema name of type
	 * @return the schema data type
	 */
	public DataType getType(String type) { 
		return this.knownTypes.get(type);
	}
	
	/**
	 * Create a new record structure using a schema data type.
	 * 
	 * @param type type schema name of type
	 * @return initialized record structure
	 */
	public RecordStruct newRecord(String type) {
		DataType tp = this.knownTypes.get(type);
		
		if ((tp == null) || (tp.kind != DataKind.Record))
			return null;
		
		return new RecordStruct(tp);
	}
	
	/**
	 * Schema files contain interdependencies, after loading the files call
	 * compile to resolve these interdependencies.
	 * 
	 * @return log of compilation activity 
	 */
	public OperationResult compile() {
		OperationResult mr = new OperationResult(OperationContext.getHubContext());
		
		// compiling not thread safe, do it once at start
		for (DataType dt : this.knownTypes.values()) 
			dt.compile(mr);
		
		this.db.compile(mr);
		
		this.service.compile(mr);
		
		return mr;
	}

	/**
	 * Load a file containing schema into the master schema.
	 * 
	 * @param fl file to load 
	 * @return log of the load attempt
	 */
	public OperationResult loadSchema(File fl) {
		OperationResult or = new OperationResult(OperationContext.getHubContext());
		
		if (fl == null) {
			or.error(108, "Unable to apply schema file, file null");
			return or;
		}
		
		if (!fl.exists()) {
			or.error(109, "Missing schema file, expected: " + fl.getAbsolutePath());
			return or;
		}
		
		FuncResult<XElement> xres3 = XmlReader.loadFile(fl, false);
		
		if (xres3.hasErrors()) {
			or.copyMessages(xres3);
			or.error(110, "Unable to apply schema file, missing xml");
			return or;
		}
		
		XElement schema = xres3.getResult();
				
		Schema s = new Schema();
		s.manager = this;
		
		s.loadSchema(or, schema);
		
		return or;
	}

	/**
	 * Load a file containing schema into the master schema.
	 * 
	 * @param fl file to load 
	 * @return log of the load attempt
	 */
	public OperationResult loadSchema(InputStream fl) {
		OperationResult or = new OperationResult(OperationContext.getHubContext());
		
		if (fl == null) {
			or.error(108, "Unable to apply schema file, file null");
			return or;
		}
		
		FuncResult<XElement> xres3 = XmlReader.parse(fl, false);
		
		if (xres3.hasErrors()) {
			or.copyMessages(xres3);
			or.error(110, "Unable to apply schema file, missing xml");
			return or;
		}
		
		XElement schema = xres3.getResult();
				
		Schema s = new Schema();
		s.manager = this;
		
		s.loadSchema(or, schema);
		
		return or;
	}

	/**
	 * Load type definition from an Xml element
	 *  
	 * @param or log of the load
	 * @param schema to associate type with 
	 * @param dtel xml source of the definition
	 * @return the schema data type
	 */
	public DataType loadDataType(OperationResult or, Schema schema, XElement dtel) {
		DataType dtype = new DataType(schema);
		
		dtype.load(or, dtel);
		
		if (StringUtil.isNotEmpty(dtype.id))
			this.knownTypes.put(dtype.id, dtype);
		
		return dtype;
	}

	/*
	 * TODO 
	 * 
	 * @param name
	 * @param mr
	 * @return
	 */
	public List<DataType> lookupOptionsType(String name, OperationResult mr) {
		List<DataType> ld = new ArrayList<DataType>();
		
		if (name.contains(":")) {
			String[] parts = name.split(":");
			
			DataType t1 = this.knownTypes.get(parts[0]);
			
			if (t1 == null) 
				return ld;
			
			t1.compile(mr);
						
			if (t1.fields == null)
				return ld;
			
			Field f1 = t1.fields.get(parts[1]);
			
			if (f1 == null) 
				return ld;
			
			return f1.options;
		}
		
		DataType d4 = this.knownTypes.get(name);
		
		if (d4 != null)
			ld.add(d4);
		
		return ld;
	}
}
