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

import java.util.Arrays;
import java.util.List;

import divconq.hub.Hub;
import divconq.lang.FuncResult;
import divconq.lang.StringBuilder32;
import divconq.script.ExecuteState;
import divconq.script.Instruction;
import divconq.script.StackEntry;
import divconq.sql.SqlManager.SqlDatabase;
import divconq.sql.SqlNull;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class SqlUpdate extends Instruction {
	@Override
	public void run(final StackEntry stack) {
		//stack.log().info("Doing a SQL UPDATE on thread " + Thread.currentThread().getName());
		
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
		sb1.append("UPDATE " + table + " SET ");
		
		List<XElement> fields = codeEl.selectAll("Field");
		XElement el2 = codeEl.selectFirst("Where");
		
		if (el2 == null) {
			stack.log().error("Missing WHERE in SQL UPDATE");
			stack.setState(ExecuteState.Done);
			stack.resume();
			return;
		}
		
		boolean firstfld = true;
    	int pos = 0;
    	
		Object[] vals = new Object[fields.size() + ((el2 == null) ? 0 : 1)];
	
		for (XElement el : fields) {
			boolean skipnull = stack.boolFromElement(el, "SkipNull", false);
			
	        Object val = SqlInsert.convertValueToInternal(stack, el);

			if (skipnull && (val instanceof SqlNull)) 
				continue;
			
			if (firstfld)
				firstfld = false;
			else 
				sb1.append(',');
			
			sb1.append(stack.stringFromElement(el, "Name") + " = ?");
			
			vals[pos] = val;
			pos++;
		}
		
		vals[pos] = SqlInsert.convertValueToInternal(stack, el2);
		pos++;
		
		// if too big, resize array
		if (pos < vals.length) 
			vals = Arrays.copyOfRange(vals, 0, pos);
		
		// no fields updated - don't error, might just mean there was no match after skip checks
		if (firstfld) {
			stack.setState(ExecuteState.Done);
			stack.resume();
			return;
		}
		
		String sql = sb1.toString() + " WHERE " + stack.stringFromElement(el2, "Name") + " = ?";
		
		SqlDatabase db = Hub.instance.getSQLDatabase(dbname);
		
		if (db == null) {
			stack.log().errorTr(185, dbname);
			stack.setState(ExecuteState.Done);
			stack.resume();
			return;
		}
        
		FuncResult<Integer> rsres = db.executeUpdate(sql, vals);
		
		stack.log().copyMessages(rsres);
		
		if (rsres.getResult() != 1) 
			stack.log().error("UPDATE failed, expected 1 row result count");		
		
		stack.setState(ExecuteState.Done);
		stack.resume();
	}
	
	@Override
	public void cancel(StackEntry stack) {
		// do nothing, this isn't cancellable
	}
}
