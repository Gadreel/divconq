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
package divconq.tasks.sql;

import divconq.bus.IService;
import divconq.bus.Message;
import divconq.hub.Hub;
import divconq.lang.FuncResult;
import divconq.mod.ExtensionBase;
import divconq.sql.SqlManager.SqlDatabase;
import divconq.sql.SqlSelect;
import divconq.sql.SqlSelectString;
import divconq.sql.SqlSelectStringList;
import divconq.struct.ListStruct;
import divconq.work.TaskRun;

public class DomainsService extends ExtensionBase implements IService {

	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
		
		if ("Manager".equals(feature)) {
			if ("LoadAll".equals(op)) {
				SqlDatabase db = Hub.instance.getSQLDatabase();
				
				if (db == null) {
					request.errorTr(443);
					request.complete();
					return;
		        }

				// TODO GROUP_CONCAT only works with MariaDB/MySQL -- should work with H2 -- fix for others
        		String nsql = "(SELECT GROUP_CONCAT(DISTINCT dcName) FROM dcDomainNames dn WHERE dn.dcDomainId = d.Id)";
				
				FuncResult<ListStruct> rsres = db.executeQuery(
						new SqlSelect[] { 
								new SqlSelectString("Id"), 
								new SqlSelectString("dcTitle", "Title", null), 
								new SqlSelectString("dcObscureClass", "ObscureClass", null), 
								new SqlSelectString("dcObscureSeed", "ObscureSeed", null), 
								new SqlSelectStringList(nsql, "Names", null) 
						},
						"dcDomain d",  
						"Active = 1", 
						null, 
						"Id"
				);
			
				request.copyMessages(rsres);
				
				ListStruct rs = rsres.getResult();
				
				request.setResult(rs);
				request.complete();
				return;
			}			
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
}
