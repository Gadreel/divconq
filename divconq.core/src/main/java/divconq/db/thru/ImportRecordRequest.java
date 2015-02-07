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
package divconq.db.thru;

import divconq.db.update.DbRecordRequest;
import divconq.db.update.DynamicListField;
import divconq.db.update.DynamicScalarField;
import divconq.db.update.ListField;
import divconq.db.update.ScalarField;
import divconq.lang.op.OperationContext;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;

/**
 * Update a record in dcDatabase, see dcUpdateRecord schema.
 * Limit to 1MB of values (total size)
 * 
 * @author Andy
 *
 * TODO review 
 */
public class ImportRecordRequest extends DbRecordRequest {
	
	/**
	 * import data must have an Id 
	 * 
	 * @param table name
	 * @param record to import see Export for format
	 */
	public ImportRecordRequest(String table, RecordStruct record) {
		super("dcUpdateRecord");
		
		this.withTable(table);
		
		this.id = record.getFieldAsRecord("Id").getFieldAsString("Data");
		
		for (divconq.schema.DbField sfld : OperationContext.get().getSchema().getDbFields(table)) {
			if (record.hasField(sfld.name)) {
				if (sfld.list && sfld.dynamic) {
					ListStruct items = record.getFieldAsList(sfld.name);
					
					for (Struct itm : items.getItems()) 
						if (itm != null)
							this.withFields(DynamicListField.buildImport(sfld.name, (RecordStruct)itm));
				}
				else if (sfld.list) {
					ListStruct items = record.getFieldAsList(sfld.name);
					
					for (Struct itm : items.getItems()) 
						if (itm != null)
							this.withFields(ListField.buildImport(sfld.name, (RecordStruct)itm));
				}
				else if (sfld.dynamic) 
					this.withFields(DynamicScalarField.buildImport(sfld.name, record.getFieldAsRecord(sfld.name)));
				else 
					this.withFields(ScalarField.buildImport(sfld.name, record.getFieldAsRecord(sfld.name)));
			}
		}
	}
}
