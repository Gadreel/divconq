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

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import divconq.hub.Hub;
import divconq.hub.HubResources;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.log.DebugLevel;
import divconq.log.Logger;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.scalar.IntegerStruct;
import divconq.struct.scalar.StringStruct;

public class Schema1 {
	/* 0 = Sunday */
	static public int dayOfWeek(int y, int m, int d) {
		int[] t = { 0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4 };
		
		y -= (m < 3) ? 1 : 0;
		
		return (y + y/4 - y/100 + y/400 + t[m-1] + d) % 7;
	}
	
	public static void main(String[] args) {
		try {
			DateTime zyear3 = new DateTime(2012, 3, 15, 0, 0, 0, DateTimeZone.UTC);
			
			System.out.println("1: " + zyear3.plusMonths(1));
			System.out.println("5: " + zyear3.plusMonths(5));
			System.out.println("8: " + zyear3.plusMonths(8));
			System.out.println("9: " + zyear3.plusMonths(9));
			System.out.println("10: " + zyear3.plusMonths(10));
			System.out.println("11: " + zyear3.plusMonths(11));
			System.out.println("12: " + zyear3.plusMonths(12));
			System.out.println("13: " + zyear3.plusMonths(13));
			System.out.println("20: " + zyear3.plusMonths(20));
			System.out.println("21: " + zyear3.plusMonths(21));
			System.out.println("22: " + zyear3.plusMonths(22));
			System.out.println("23: " + zyear3.plusMonths(23));
			System.out.println("24: " + zyear3.plusMonths(24));
			System.out.println("30: " + zyear3.plusMonths(30));
			System.out.println("31: " + zyear3.plusMonths(31));
			System.out.println("32: " + zyear3.plusMonths(32));
			System.out.println("33: " + zyear3.plusMonths(33));
			System.out.println("34: " + zyear3.plusMonths(34));
			System.out.println("40: " + zyear3.plusMonths(40));
			System.out.println("41: " + zyear3.plusMonths(41));
			System.out.println("42: " + zyear3.plusMonths(42));
			System.out.println("43: " + zyear3.plusMonths(43));
			System.out.println("200: " + zyear3.plusMonths(200));
			System.out.println("500: " + zyear3.plusMonths(500));
			System.out.println("1000: " + zyear3.plusMonths(1000));
			System.out.println("2000: " + zyear3.plusMonths(2000));
			System.out.println("5000: " + zyear3.plusMonths(5000));
			System.out.println("7000: " + zyear3.plusMonths(7000));
			
			/*
			System.out.println("1: " + Schema1.dayOfWeek(0, 1, 1));
			System.out.println("1: " + Schema1.dayOfWeek(1, 1, 1));
			System.out.println("2: " + Schema1.dayOfWeek(500, 1, 1));
			System.out.println("3: " + Schema1.dayOfWeek(900, 1, 1));
			System.out.println("4: " + Schema1.dayOfWeek(1800, 1, 1));
			System.out.println("5: " + Schema1.dayOfWeek(1900, 1, 1));
			System.out.println("6: " + Schema1.dayOfWeek(2000, 1, 1));
			
			
			DateTime zyeara = new DateTime(0, 1, 1, 0, 0, 0, DateTimeZone.UTC);
			DateTime zyeara1 = new DateTime(1, 1, 1, 0, 0, 0, DateTimeZone.UTC);
			DateTime zyearb = new DateTime(500, 1, 1, 0, 0, 0, DateTimeZone.UTC);
			DateTime zyearc = new DateTime(900, 1, 1, 0, 0, 0, DateTimeZone.UTC);
			DateTime zyear1 = new DateTime(1800, 1, 1, 0, 0, 0, DateTimeZone.UTC);
			DateTime zyear2 = new DateTime(1900, 1, 1, 0, 0, 0, DateTimeZone.UTC);
			DateTime zyear3 = new DateTime(2000, 1, 1, 0, 0, 0, DateTimeZone.UTC);
			
			System.out.println("1: " + zyeara.getDayOfWeek());
			System.out.println("1: " + zyeara1.getDayOfWeek());
			System.out.println("2: " + zyearb.getDayOfWeek());
			System.out.println("3: " + zyearc.getDayOfWeek());
			System.out.println("4: " + zyear1.getDayOfWeek());
			System.out.println("5: " + zyear2.getDayOfWeek());
			System.out.println("6: " + zyear3.getDayOfWeek());
			*/
			
			/*
			 DateTimeZone America_Los_Angeles = new DateTimeZoneBuilder()
			     .addCutover(-2147483648, 'w', 1, 1, 0, false, 0)
			     .setStandardOffset(-28378000)
			     .setFixedSavings("LMT", 0)
			     .addCutover(1883, 'w', 11, 18, 0, false, 43200000)
			     .setStandardOffset(-28800000)
			     .addRecurringSavings("PDT", 3600000, 1918, 1919, 'w',  3, -1, 7, false, 7200000)
			     .addRecurringSavings("PST",       0, 1918, 1919, 'w', 10, -1, 7, false, 7200000)
			     .addRecurringSavings("PWT", 3600000, 1942, 1942, 'w',  2,  9, 0, false, 7200000)
			     .addRecurringSavings("PPT", 3600000, 1945, 1945, 'u',  8, 14, 0, false, 82800000)
			     .addRecurringSavings("PST",       0, 1945, 1945, 'w',  9, 30, 0, false, 7200000)
			     .addRecurringSavings("PDT", 3600000, 1948, 1948, 'w',  3, 14, 0, false, 7200000)
			     .addRecurringSavings("PST",       0, 1949, 1949, 'w',  1,  1, 0, false, 7200000)
			     .addRecurringSavings("PDT", 3600000, 1950, 1966, 'w',  4, -1, 7, false, 7200000)
			     .addRecurringSavings("PST",       0, 1950, 1961, 'w',  9, -1, 7, false, 7200000)
			     .addRecurringSavings("PST",       0, 1962, 1966, 'w', 10, -1, 7, false, 7200000)
			     .addRecurringSavings("PST",       0, 1967, 2147483647, 'w', 10, -1, 7, false, 7200000)
			     .addRecurringSavings("PDT", 3600000, 1967, 1973, 'w', 4, -1,  7, false, 7200000)
			     .addRecurringSavings("PDT", 3600000, 1974, 1974, 'w', 1,  6,  0, false, 7200000)
			     .addRecurringSavings("PDT", 3600000, 1975, 1975, 'w', 2, 23,  0, false, 7200000)
			     .addRecurringSavings("PDT", 3600000, 1976, 1986, 'w', 4, -1,  7, false, 7200000)
			     .addRecurringSavings("PDT", 3600000, 1987, 2147483647, 'w', 4, 1, 7, true, 7200000)
			     .toDateTimeZone("America/Los_Angeles", true);			
			
			int year = 2012;
			
			//for (String id : DateTimeZone.getAvailableIDs()) {
				DateTimeZone z = DateTimeZone.forID("America/New_York");
				
				DateTime zyear = new DateTime(year, 1, 1, 0, 0, 0, z);
				
				System.out.println("Zone: " + z.getID());
				
				System.out.println("   " + zyear);
				
				//while (zyear.getYear() == year) {
					long lz = z.nextTransition(zyear.getMillis());				
					zyear = new DateTime(lz, z);
					
					System.out.println("   " + zyear);
				//}
			//}
			*/
			
			System.exit(0);
			
			OperationContext.useHubContext();
			
			HubResources resources = new HubResources();
			resources.setDebugLevel(DebugLevel.Info);
			OperationResult or = resources.init();
			
			if (or.hasErrors()) {
				Logger.error("Unable to continue, hub resources not properly initialized");
				return;
			}
			
			Hub.instance.start(resources);

			{
				System.out.println("Implicit Example");
				
				RecordStruct ex1 = new RecordStruct();
				ex1.setField("Code", 5);
				ex1.setField("Message", "Problem with coolant system.");
				
				System.out.println("Code: " + ex1.getFieldAsString("Code"));
				long code = ex1.getFieldAsInteger("Code");
				
				System.out.println("Code Is Priority Level: " + (code < 3));
			}
			
			System.out.println("----------------");

			{
				System.out.println("Explicit Example");
				
				RecordStruct ex1 = new RecordStruct();
				ex1.setField("Code", new IntegerStruct(5));
				ex1.setField("Message", new StringStruct("Problem with coolant system."));
				
				System.out.println("Code: " + ex1.getFieldAsString("Code"));
				long code = ex1.getFieldAsInteger("Code");
				
				System.out.println("Code Is Priority Level: " + (code < 3));
			}
			
			System.out.println("----------------");

			{
				System.out.println("Alternative Type Example");
				
				RecordStruct ex1 = new RecordStruct();
				ex1.setField("Code", "5");
				ex1.setField("Message", "Problem with coolant system.");
				
				System.out.println("Code: " + ex1.getFieldAsString("Code"));
				long code = ex1.getFieldAsInteger("Code");
				
				System.out.println("Code Is Priority Level: " + (code < 3));
			}
			
			System.out.println("----------------");

			{
				System.out.println("List Example");
				
				ListStruct ex2 = new ListStruct();
				ex2.addItem(0);
				ex2.addItem(20);
				ex2.addItem(50);
				ex2.addItem(90);
				ex2.addItem(140);
				
				for (int i = 0; i < ex2.getSize(); i++) {
					long code = ex2.getItemAsInteger(i);
					System.out.println("Code " + code + " is priority level: " + (code < 73));
				}
			}
			
			System.out.println("----------------");

			{
				System.out.println("Record List Example");
				
				ListStruct ex3 = new ListStruct();
				
				RecordStruct m1 = new RecordStruct();
				m1.setField("Code", 5);
				m1.setField("Message", "Problem with coolant system.");
				
				ex3.addItem(m1);
				
				RecordStruct m2 = new RecordStruct();
				m2.setField("Code", 53);
				m2.setField("Message", "Fan blade nicked.");
				
				ex3.addItem(m2);
				
				for (int i = 0; i < ex3.getSize(); i++) {
					RecordStruct msg = ex3.getItemAsRecord(i);
					System.out.println("Message #" + i);
					System.out.println("   Code: " + msg.getFieldAsString("Code"));
					System.out.println("   Text: " + msg.getFieldAsString("Message"));
				}
			}
			
			System.out.println("----------------");

			{
				System.out.println("Record List Example 2");
				
				ListStruct ex3 = new ListStruct(
						new RecordStruct(
								new FieldStruct("Code", 5),
								new FieldStruct("Message", "Problem with coolant system.")
						),
						new RecordStruct(
								new FieldStruct("Code", 53),
								new FieldStruct("Message", "Fan blade nicked.")
						)
				);
				
				for (int i = 0; i < ex3.getSize(); i++) {
					RecordStruct msg = ex3.getItemAsRecord(i);
					System.out.println("Message #" + i);
					System.out.println("   Code: " + msg.getFieldAsString("Code"));
					System.out.println("   Text: " + msg.getFieldAsString("Message"));
				}
			}
			
			System.out.println("----------------");

			{
				System.out.println("Schema Example 1-A");
				
				RecordStruct ex1 = new RecordStruct();
				ex1.setField("Code", 5);
				ex1.setField("Message", "Problem with coolant system.");
				
				System.out.println(ex1.validate("Schema1Ex1").toString());
			}
			
			System.out.println("----------------");

			{
				System.out.println("Schema Example 1-B");
				
				RecordStruct ex1 = new RecordStruct();
				ex1.setField("Code", "5");
				ex1.setField("Message", "Problem with coolant system.");
				
				System.out.println(ex1.validate("Schema1Ex1").toString());
			}
			
			System.out.println("----------------");

			{
				System.out.println("Schema Example 1-C");
				
				RecordStruct ex1 = new RecordStruct();
				ex1.setField("Code", "abc");
				ex1.setField("Message", "Problem with coolant system.");
				
				System.out.println(ex1.validate("Schema1Ex1").toString());
			}
			
			System.out.println("----------------");

			{
				System.out.println("Schema Example 2-A");
				
				RecordStruct ex1 = new RecordStruct();
				ex1.setField("Message", "Problem with coolant system.");
				
				System.out.println(ex1.validate("Schema1Ex1").toString());
			}
			
			System.out.println("----------------");

			{
				System.out.println("Schema Example 2-B");
				
				RecordStruct ex1 = new RecordStruct();
				ex1.setField("Message", "Problem with coolant system.");
				
				System.out.println(ex1.validate("Schema1Ex2").toString());
			}
			
			System.out.println("----------------");

			{
				System.out.println("Schema Example 2-C");
				
				RecordStruct ex1 = new RecordStruct();
				ex1.setField("Code", 5);
				ex1.setField("Message", "Problem with coolant system.");
				
				System.out.println(ex1.validate("Schema1Ex2").toString());
			}
			
			System.out.println("----------------");

			{
				System.out.println("Schema Example 3A");
				
				ListStruct ex3 = new ListStruct(
						new RecordStruct(
								new FieldStruct("Code", 5),
								new FieldStruct("Message", "Problem with coolant system.")
						),
						new RecordStruct(
								new FieldStruct("Message", "Fan belt cracked.")
						),
						new RecordStruct(
								new FieldStruct("Code", 53),
								new FieldStruct("Message", "Fan blade nicked.")
						),
						new RecordStruct(
								new FieldStruct("Code", "abc"),
								new FieldStruct("Message", "Fan bearing worn.")
						)
				);
				
				System.out.println(ex3.validate("Schema1Ex3").toString());
			}
			
			System.out.println("----------------");

			{
				System.out.println("Schema Example 3B");
				
				ListStruct ex3 = new ListStruct(
						new RecordStruct(
								new FieldStruct("Code", 5),
								new FieldStruct("Message", "Problem with coolant system.")
						),
						new RecordStruct(
								new FieldStruct("Code", 52),
								new FieldStruct("Message", "Fan belt cracked.")
						),
						new RecordStruct(
								new FieldStruct("Code", 53),
								new FieldStruct("Message", "Fan blade nicked.")
						),
						new RecordStruct(
								new FieldStruct("Code", 54),
								new FieldStruct("Message", "Fan bearing worn.")
						)
				);
				
				System.out.println(ex3.validate("Schema1Ex3").toString());
			}
			
			System.out.println("----------------");

			{
				System.out.println("Schema Example 7A");
				
				RecordStruct ex1 = new RecordStruct();
				ex1.setField("Code", 5);
				ex1.setField("Message", "Problem with coolant system.");
				
				System.out.println(ex1.validate("Schema1Ex7").toString());
			}
			
			System.out.println("----------------");

			{
				System.out.println("Schema Example 7B");
				
				RecordStruct ex1 = new RecordStruct();
				ex1.setField("Code", 200);
				ex1.setField("Message", "Problem with coolant system.");
				
				System.out.println(ex1.validate("Schema1Ex7").toString());
			}
			
			System.out.println("----------------");

			{
				System.out.println("Schema Example 7C");
				
				RecordStruct ex1 = new RecordStruct();
				ex1.setField("Code", 201);
				ex1.setField("Message", "Problem with coolant system.");
				
				System.out.println(ex1.validate("Schema1Ex7").toString());
			}
			
			//System.out.println("----------------");
			//System.out.println(ex1.validate("Schema1Ex1").toString());
			
			/*
			// typed ResultMessage record
			RecordStruct msg1 = dcschema.newRecord("ResultMessage");
			
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
			RecordStruct resp = dcschema.newRecord("ResponseMessage");
			
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
			*/
		} 
		catch (Exception x) {
			System.out.println("Error in test: " + x);
		}
		
		Hub.instance.stop();
	}
}
