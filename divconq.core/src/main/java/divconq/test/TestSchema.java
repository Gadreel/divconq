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
package divconq.test;

import divconq.hub.HubResources;
import divconq.lang.FuncResult;
import divconq.lang.OperationResult;
import divconq.log.DebugLevel;
import divconq.log.Logger;
import divconq.schema.SchemaManager;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;

public class TestSchema {
	public static void main(String[] args) {
		try {
			HubResources resources = new HubResources();
			resources.setDebugLevel(DebugLevel.Trace);
			OperationResult or = resources.init();
			
			if (or.hasErrors()) {
				Logger.error("Unable to continue, hub resources not properly initialized");
				return;
			}
			
			SchemaManager schemaman = resources.getSchema();

			// typed ResultMessage record
			RecordStruct msg1 = schemaman.newRecord("ResultMessage");
			
			System.out.println("----------------");
			System.out.println(msg1.validate().toString());
			
			msg1.setField("Level", "Info");
			msg1.setField("Code", 3);
			msg1.setField("Message", "Howday partner!");
			
			System.out.println("----------------");
			System.out.println(msg1.toString());
			
			System.out.println("----------------");
			System.out.println(msg1.validate().toString());
			
			//rec.setField("Level", "What!!");
			//rec.removeField("Code");
			
			//System.out.println("----------------");
			//System.out.println(rec.validate().toString());
			
			System.out.println("-------------------------------------------------------");

			// typed ResponseMessage record
			RecordStruct resp = schemaman.newRecord("ResponseMessage");
			
			System.out.println(resp.validate().toString());
			
			resp.setField("Service", "Reply");
			resp.setField("Result", 6);
			
			System.out.println("----------------");
			System.out.println(resp.toString());
			
			System.out.println("----------------");
			System.out.println(resp.validate().toString());
			
			System.out.println("-------------------------------------------------------");
			
			FuncResult<Struct> msgs = resp.getOrAllocateField("Messages"); 
			
			((ListStruct) msgs.getResult()).addItem(msg1);
			
			System.out.println("----------------");
			System.out.println(resp.toString());
			
			System.out.println("----------------");
			System.out.println(resp.validate().toString());
			
			System.out.println("-------------------------------------------------------");
			
			// untyped record attempting to merge with typed
			RecordStruct msg2 = new RecordStruct();
			
			msg2.setField("Level", "Exit");
			msg2.setField("Code", 5);
			msg2.setField("Message", "Goodday partner!");
			
			((ListStruct) msgs.getResult()).addItem(msg2);
			
			System.out.println("----------------");
			System.out.println(resp.toString());
			
			System.out.println("----------------");
			System.out.println(resp.validate().toString());
			
			System.out.println("-------------------------------------------------------");
		} 
		catch (Exception x) {
			System.out.println("Error in loop: " + x);
		}
	}
}
