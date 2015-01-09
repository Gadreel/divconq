package divconq.service.db;

import divconq.bus.IService;
import divconq.bus.Message;
import divconq.db.IDatabaseManager;
import divconq.db.DataRequest;
import divconq.db.ObjectFinalResult;
import divconq.hub.Hub;
import divconq.mod.ExtensionBase;
import divconq.work.TaskRun;

public class DomainsService extends ExtensionBase implements IService {

	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");

		IDatabaseManager db = Hub.instance.getDatabase();
		
		if (db == null) {
			request.errorTr(443);
			request.complete();
			return;
		}
		
		if ("Manager".equals(feature)) {
			if ("LoadAll".equals(op)) {
				DataRequest req = new DataRequest("dcLoadDomains")
					.withRootDomain();	// use root for this request
				
				db.submit(req, new ObjectFinalResult(request));
				return;
			}			
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
}
