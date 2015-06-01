/* ************************************************************************
#
#  DivConq
#
#  http://divconq.com/
#
#  Copyright:
#    Copyright 2012 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package divconq.db.query;

import divconq.struct.RecordStruct;
import divconq.struct.Struct;

/**
 * A database field to select in a query.
 * Field may be formated and also may hold a display name.
 * 
 * Format, if present, may be:
 * 		Tr
 * 			Translate an enum for display
 * 
 * 		Num:pattern[:neg pattern]
 * 		Num:.    	use Decimal
 * 		Num:%		use Percent
 * 		Num:$		use Currency
 * 		
 * 		Time:dfmt:tfmt:chron:group		works with both DateTinme and BigDateTime	
 * 			dfmt = date format (full, long, medium, short, none) or pattern
 * 			tfmt = time format (full, long, medium, short, none) or pattern
 * 
 * 				y 			 = 1 digit years
 * 				yy			 = 2 digit years
 * 				yyy			 = normal years, obey grouping if present, no padding
 * 				yyyy		 = normal years, obey grouping if present, padding (min 4 digits)
 * 				yyyyy		 = normal years always with grouping
 *   
 *   			m			 = month in year
 *   			mm			 = month in year, 2 digit padded
 *   			mmm			 = month name narrow
 *   			mmmm		 = month name abbreviated
 *   			mmmmm		 = month name wide
 *   
 *   			d			 = day in month
 *   			dd			 = day in month, 2 digit padded
 *   			ddd			 = day in week narrow
 *   			dddd		 = day in week abbreviated
 *   			ddddd		 = day in week wide
 *   
 *   			g			 = era
 *   
 *   			H			 = 12 hour clock, no padding			
 *   			HH			 = 12 hour clock, 2 digit padding		
 *   
 *   			K			 = 12 hour clock marker (AM/PM)			
 *   	
 *   			FF			 = 24 hour clock (0-23), 2 digit padding		
 *   
 *   			M			 = minutes, no padding			
 *   			MM			 = minutes, 2 digit padding		
 *   
 *   			S			 = seconds, no padding			
 *   			SS			 = seconds, 2 digit padding		
 *   
 *   			LLL			 = millisecond, 3 digit padding		
 *   
 *   			Z			 = timezone notation (e.g. EDT)			
 *   			ZZ			 = timezone id
 * 
 * 			chron = chronology/timezone, where /timezone = ISO chronology
 * 			group = use the group symbol on all years (years with more than 4 digits automatically  
 * 					get grouping symbol, this helps if mixed with <= 4 digit years)
 * 
 * 		Script:[name]
 * 
 * 		Left:num
 * 		Right:num
 * 		LeftPad:num:char				pad if needed with char 
 * 		LeftAlign:num:char:char2		pad if needed with char, don't exceed num - trim left use char2 when trimmed
 * 		RightPad:num:char				pad if needed with char 
 * 		RightAlign:num:char:char2		pad if needed with char, don't exceed num - trim right use char2 when trimmed
 * 
 * @author Andy
 *
 */
public class SelectField implements ISelectField {
	protected RecordStruct column = new RecordStruct();
	
	public SelectField withField(String v) {
		this.column.setField("Field", v);		
		return this;
	}
	
	public SelectField withName(String v) {
		this.column.setField("Name", v);		
		return this;
	}
	
	public SelectField withFormat(String v) {
		this.column.setField("Format", v);		
		return this;
	}
	
	public SelectField withFull(boolean v) {
		this.column.setField("Full", v);		
		return this;
	}

	public SelectField withSubId(String v) {
		this.column.setField("SubId", v);		
		return this;
	}
	
	@Override
	public Struct getParams() {
		return this.column;
	}
	
	@Override
	public String toString() {
		return this.column.toString();
	}
}
