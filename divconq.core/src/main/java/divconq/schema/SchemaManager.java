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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import divconq.bus.Message;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.schema.DataType.DataKind;
import divconq.schema.ServiceSchema.Op;
import divconq.struct.CompositeStruct;
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
 * for for a given project and deployed as part of the conf directory.
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
	
	protected SchemaManager chain = null;
	
	public void setChain(SchemaManager v) {
		this.chain = v;
	}

	public boolean hasTable(String table) {
		boolean fnd = this.db.hasTable(table);
		
		if (fnd)
			return true;
		
		if (this.chain == null)
			return false;
		
		return this.chain.hasTable(table);
	}
	
	public List<DbField> getDbFields(String table) {
		List<DbField> t = this.db.getFields(table);
		
		if (t == null)
			t = new ArrayList<DbField>();
		
		if (this.chain == null)
			return t;
		
		List<DbField> t2 = this.chain.getDbFields(table);
		
		if (t2 != null)
			t.addAll(t2);
		
		return t;
	}
	
	public DbField getDbField(String table, String field) {
		DbField t = this.db.getField(table, field);
		
		if (t != null)
			return t;
		
		if (this.chain == null)
			return null;
		
		return this.chain.getDbField(table, field);
	}
	
	public DbProc getDbProc(String name) {
		DbProc t = this.db.getProc(name);
		
		if (t != null)
			return t;
		
		if (this.chain == null)
			return null;
		
		return this.chain.getDbProc(name);
	}
	
	public DbComposer getDbComposer(String name) {
		DbComposer t = this.db.getComposer(name);
		
		if (t != null)
			return t;
		
		if (this.chain == null)
			return null;
		
		return this.chain.getDbComposer(name);
	}
	
	// returns (copy) list of all triggers for all levels of the chain
	public List<DbTrigger> getDbTriggers(String table, String operation) {
		List<DbTrigger> t = this.db.getTriggers(table, operation);		
		
		if (t == null)
			t = new ArrayList<DbTrigger>();
		
		if (this.chain == null)
			return t;
		
		List<DbTrigger> t2 = this.chain.getDbTriggers(table, operation);
		
		if (t2 != null)
			t.addAll(t2);
		
		return t;
	}

	public Op getServiceOp(String service, String feature, String op) {
		Op t = this.service.getOp(service, feature, op);
		
		if (t != null)
			return t;
		
		if (this.chain == null)
			return null;
		
		return this.chain.getServiceOp(service, feature, op);
	}

	public Op getServiceOp(Message msg) {
		Op t = this.service.getOp(msg);
		
		if (t != null)
			return t;
		
		if (this.chain == null)
			return null;
		
		return this.chain.getServiceOp(msg);
	}

	public DataType getServiceRequest(Message msg) {
		DataType t = this.service.getRequestType(msg);
		
		if (t != null)
			return t;
		
		if (this.chain == null)
			return null;
		
		return this.chain.getServiceRequest(msg);
	}

	public DataType getServiceResponse(String service, String feature, String op) {
		DataType t = this.service.getResponseType(service, feature, op);
		
		if (t != null)
			return t;
		
		if (this.chain == null)
			return null;
		
		return this.chain.getServiceResponse(service, feature, op);
	}
	
	/**
	 * @param type schema name of type
	 * @return the schema data type
	 */
	public DataType getType(String type) { 
		DataType t = this.knownTypes.get(type);
		
		if (t != null)
			return t;
		
		if (this.chain == null)
			return null;
		
		return this.chain.getType(type);
	}

	// ----------
	
	public void removeService(String name) {
		this.service.remove(name);
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
			Op op = this.getServiceOp(msg);
			
			if (op == null)
				mr.errorTr(432);		
			else if (op.request == null)
				mr.errorTr(433);	
			else if (!msg.isVerifyRequest() && !tc.isAuthorized(op.securityTags)) {
				mr.errorTr(434);
				
				System.out.println("cannot call: " + msg);
			}
			else
				op.request.validate(msg);
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
		
		DataType dt = this.getServiceResponse(service, feature, op);
		
		if (dt == null)
			mr.errorTr(435);		
		else
			dt.validate(msg);
		
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
		OperationResult mr = new OperationResult();
		
		DbProc proc = this.getDbProc(name);
		
		if (proc == null)
			mr.errorTr(426);		
		else {
			// authorization is for the DB Service not here - the user is already elevated here
			//if (!tc.isAuthorized(proc.securityTags))

			if (proc.request == null) {
				if ((req != null) && !req.isEmpty())
					mr.errorTr(428);		
			}
			else
				proc.request.validate(req);
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
		OperationResult mr = new OperationResult();
		
		DbProc proc = this.getDbProc(name);
		
		if (proc == null)
			mr.errorTr(429);		
		else {
			if (proc.response == null) {
				if ((resp != null) && !resp.isEmpty())
					mr.errorTr(430);		
			}
			else
				proc.response.validate(resp);
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
		
		DataType dt = this.getType(type);
		
		if (dt == null)
			mr.errorTr(436);		
		else
			dt.validate(data);
		
		return mr;
	}
	
	/**
	 * Create a new record structure using a schema data type.
	 * 
	 * @param type type schema name of type
	 * @return initialized record structure
	 */
	public RecordStruct newRecord(String type) {
		DataType tp = this.getType(type);
		
		if ((tp == null) || (tp.kind != DataKind.Record))
			return null;
		
		return new RecordStruct(tp);
	}
	
	/**
	 * Schema files contain interdependencies, after loading the files call
	 * compile to resolve these interdependencies.
	 */
	public void compile() {
		// compiling not thread safe, do it once at start
		for (DataType dt : this.knownTypes.values()) 
			dt.compile();
		
		this.db.compile();
		
		this.service.compile();
	}

	/**
	 * Load a file containing schema into the master schema.
	 * 
	 * @param fl file to load 
	 * @return log of the load attempt
	 */
	public OperationResult loadSchema(Path fl) {
		OperationResult or = new OperationResult();
		
		if (fl == null) {
			or.error(108, "Unable to apply schema file, file null");
			return or;
		}
		
		if (Files.notExists(fl)) {
			or.error(109, "Missing schema file, expected: " + fl);
			return or;
		}
		
		FuncResult<XElement> xres3 = XmlReader.loadFile(fl, false);
		
		if (xres3.hasErrors()) {
			or.error(110, "Unable to apply schema file, missing xml");
			return or;
		}
		
		XElement schema = xres3.getResult();
				
		Schema s = new Schema(fl.toString(), this);
		
		s.loadSchema(schema);
		
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
	public DataType loadDataType(Schema schema, XElement dtel) {
		DataType dtype = new DataType(schema);
		
		dtype.load(dtel);
		
		if (StringUtil.isNotEmpty(dtype.id))
			this.knownTypes.put(dtype.id, dtype);
		
		return dtype;
	}

	public List<DataType> lookupOptionsType(String name) {
		List<DataType> ld = new ArrayList<DataType>();
		
		if (name.contains(":")) {
			String[] parts = name.split(":");
			
			DataType t1 = this.getType(parts[0]);
			
			t1.compile();
			
			if ((t1 == null) || (t1.fields == null)) {
				if (this.chain == null)
					return ld;
				
				return this.chain.lookupOptionsType(name);
			}
			
			Field f1 = t1.fields.get(parts[1]);
			
			if (f1 == null) {
				if (this.chain == null)
					return ld;
				
				return this.chain.lookupOptionsType(name);
			}
			
			return f1.options;
		}
		
		DataType d4 = this.getType(name);
		
		if (d4 != null)
			ld.add(d4);
		
		return ld;
	}

	public void loadDb(Schema schema, XElement xml) {
		this.db.load(schema, xml);
	}

	public void loadService(Schema schema, XElement xml) {
		this.service.load(schema, xml);
	}
}
