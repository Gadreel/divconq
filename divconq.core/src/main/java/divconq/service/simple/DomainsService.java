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
package divconq.service.simple;

import divconq.bus.IService;
import divconq.bus.Message;
import divconq.mod.ExtensionBase;
import divconq.mod.ExtensionLoader;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.work.TaskRun;
import divconq.xml.XElement;

public class DomainsService extends ExtensionBase implements IService {

	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
		
		if ("Manager".equals(feature)) {
			if ("LoadAll".equals(op)) {
				ListStruct names = new ListStruct("root", "localhost");
				
				ExtensionLoader el = this.getLoader(); 
				
				if (el != null) {
					XElement config = el.getSettings();
					
					if (config != null)
						for (XElement del : config.selectAll("Domain"))
							names.addItem(del.getAttribute("Name"));
				}
				
				request.setResult(new ListStruct(
						new RecordStruct(
								new FieldStruct("Id", "00000_000000000000001"),
								new FieldStruct("Title", "root"),
								new FieldStruct("Names", names)
						)
				));
				
				request.complete();
				return;
			}			
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
	
	@Override
	public String serviceName() {
		// TODO if through Loader then get name there (super)
		return "dcDomains";
	}
}
