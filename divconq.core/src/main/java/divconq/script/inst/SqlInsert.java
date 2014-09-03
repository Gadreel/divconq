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
package divconq.script.inst;

import java.math.BigDecimal;
import java.util.List;

import org.joda.time.DateTime;

import divconq.hub.Hub;
import divconq.lang.FuncResult;
import divconq.lang.StringBuilder32;
import divconq.script.ExecuteState;
import divconq.script.Instruction;
import divconq.script.StackEntry;
import divconq.sql.SqlManager.SqlDatabase;
import divconq.sql.SqlNull;
import divconq.sql.SqlType;
import divconq.struct.Struct;
import divconq.struct.scalar.NullStruct;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class SqlInsert extends Instruction {
	@Override
	public void run(StackEntry stack) {
		//stack.log().info("Doing a SQL INSERT on thread " + Thread.currentThread().getName());
		
		String dbname = stack.stringFromSource("Database", "default");
		String table = stack.stringFromSource("Table");

		if (StringUtil.isEmpty(dbname) || StringUtil.isEmpty(table)) {
			stack.log().error("Missing table for insert");
			stack.setState(ExecuteState.Done);
			stack.resume();
			return;
		}
		
		XElement codeEl = stack.getInstruction().getXml();
		
		StringBuilder32 sb1 = new StringBuilder32();
		sb1.append("INSERT INTO " + table + " (");
		
		StringBuilder32 sb2 = new StringBuilder32();
		sb2.append(" VALUES (");
		
		List<XElement> fields = codeEl.selectAll("Field");
		boolean firstfld = true;
		Object[] vals = new Object[fields.size()];
    	int pos = 0;
		
		for (XElement el : fields) {
			if (firstfld)
				firstfld = false;
			else {
				sb1.append(',');
				sb2.append(',');
			}
			
			sb1.append(stack.stringFromElement(el, "Name"));
			sb2.append('?');
			
        	vals[pos] = SqlInsert.convertValueToInternal(stack, el);
			pos++;
		}
		
		sb1.append(')');
		sb2.append(')');
		
		// no fields found
		if (firstfld) {
			stack.setState(ExecuteState.Done);
			stack.resume();
			return;
		}
		
		String sql = sb1.toString() + sb2.toString();
		
		SqlDatabase db = Hub.instance.getSQLDatabase(dbname);
		
		if (db == null) {
			stack.log().errorTr(185, dbname);
			stack.setState(ExecuteState.Done);
			stack.resume();
			return;
		}
        
		FuncResult<Integer> rsres = db.executeInsert(sql, vals);
		
		stack.log().copyMessages(rsres);
		
		if (rsres.getResult() != 1) 
			stack.log().error("INSERT failed, expected 1 row result count");		
		
		stack.setState(ExecuteState.Done);
		stack.resume();
	}
	
	@Override
	public void cancel(StackEntry stack) {
		// do nothing, this isn't cancellable
	}
	
	static public Object convertValueToInternal(StackEntry stack, XElement el) {
		String ftype = stack.stringFromElement(el, "Type");
        Struct dt = stack.refFromElement(el, "Value");
        
        SqlType stype = SqlType.valueOf(ftype);
        
		if (stype == SqlType.Int) {
        	Long ret = Struct.objectToInteger(dt);
        	return (ret == null) ? SqlNull.Int : ret;
		}
		
		if (stype == SqlType.Long) {
        	Long ret = Struct.objectToInteger(dt);
        	return (ret == null) ? SqlNull.Long : ret;
		}
		
		if (stype == SqlType.Double) {
        	BigDecimal ret = Struct.objectToDecimal(dt);
        	return (ret == null) ? SqlNull.Double : ret;
		}
		
		if (stype == SqlType.BigDecimal) {
        	BigDecimal ret = Struct.objectToDecimal(dt);
        	return (ret == null) ? SqlNull.BigDecimal : ret;
		}

		if (stype == SqlType.DateTime) {
        	DateTime ret = Struct.objectToDateTime(dt);
        	return (ret == null) ? SqlNull.DateTime : ret;
		}
		
		if (stype == SqlType.Text) {
			if (dt == NullStruct.instance)
        		return SqlNull.Text;
			
        	String ret = Struct.objectToString(dt);
        	
        	if (ret == null)
        		return SqlNull.Text;

			if ("True".equals(stack.stringFromElement(el, "Encrypt")))
				ret = Hub.instance.getClock().getObfuscator().encryptStringToHex(ret);
			
    		return ret;
		}

		if (dt == NullStruct.instance)
    		return SqlNull.VarChar;
		
    	String ret = Struct.objectToString(dt);
    	
    	if (ret == null)
    		return SqlNull.VarChar;
    	
		if ("True".equals(stack.stringFromElement(el, "Encrypt")))
			ret = Hub.instance.getClock().getObfuscator().encryptStringToHex(ret);
			
    	return ret;
	}
}
