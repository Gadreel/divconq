package divconq.service.db;

import divconq.bus.IService;
import divconq.bus.Message;
import divconq.mod.ExtensionBase;
import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;
import divconq.work.TaskRun;

// TODO move to the right project
public class DesktopService extends ExtensionBase implements IService {

	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");

		//TaskContext tc = TaskContext.get();
		//UserContext uc = tc.getUserContext();

		//String did = uc.getDomainId();
		
		//LocalDataStore lds = Hub.instance.getLocalDataStore("default");
		
		//if (lds == null)
		//	return MessageUtil.errorTr(443);
		
		//System.out.println("Auth: " + feature + " - " + op);
		
		if ("Desktop".equals(feature)) {
			if ("LoadSelf".equals(op)) {
				/*
				String uid = uc.getUserId();
				
				RecordStruct urec = lds.getRecord(did, "dcUser", uid);
				
				if (urec != null) {
					return MessageUtil.successAlt(new RecordStruct(
							new FieldStruct("RootApp", urec.getFieldAsString("dcdRootApp"))
					));
				}
				*/
				
				// TODO access database
				request.setResult(new RecordStruct(
						new FieldStruct("RootApp", "/dcm/View/Cms/Index")
				));
				
				request.complete();
				return;
				
				//return MessageUtil.errorTr(444);
			}
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
}
