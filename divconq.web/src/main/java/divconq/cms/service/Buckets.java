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
package divconq.cms.service;

import divconq.bus.Message;
import divconq.filestore.bucket.Bucket;
import divconq.hub.DomainInfo;
import divconq.lang.op.FuncCallback;
import divconq.lang.op.OperationContext;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.work.TaskRun;

public class Buckets {
	
	static public void handle(TaskRun request, String op, Message msg) {
		// in order to conserve efforts, check that we have a known operation first
		
		/*
		if (!"FileDetail".equals(op) && !"DeleteFile".equals(op) && !"DeleteFolder".equals(op) && !"AddFolder".equals(op)
				&& !"ListFiles".equals(op) && !"StartUpload".equals(op) && !"FinishUpload".equals(op) 
				&& !"StartDownload".equals(op) && !"FinishDownload".equals(op)) 
		{
			request.errorTr(441, "dcmCms", "Buckets", op);
			request.complete();	
			return;
		}
		*/
		
		RecordStruct rec = msg.getFieldAsRecord("Body");
		
		DomainInfo domain = OperationContext.get().getUserContext().getDomain();
		
		Bucket bucket = domain.getBucket(rec.getFieldAsString("Bucket"));
		
		if (bucket == null) {
			request.error("Missing bucket.");
			return;
		}
		
		if ("FileDetail".equals(op)) {
			bucket.handleFileDetail(rec, new FuncCallback<RecordStruct>() {			
				@Override
				public void callback() {
					request.setResult(this.getResult());
					request.complete();
				}
			});
			
			return;
		}
		
		if ("DeleteFile".equals(op) || "DeleteFolder".equals(op)) {
			bucket.handleDeleteFile(rec, new FuncCallback<RecordStruct>() {			
				@Override
				public void callback() {
					request.setResult(this.getResult());
					request.complete();
				}
			});
			
			return;
		}
		
		if ("AddFolder".equals(op)) {
			bucket.handleAddFolder(rec, new FuncCallback<RecordStruct>() {			
				@Override
				public void callback() {
					request.setResult(this.getResult());
					request.complete();
				}
			});
			
			return;
		}
		
		if ("ListFiles".equals(op)) {
			bucket.handleListFiles(rec, new FuncCallback<ListStruct>() {			
				@Override
				public void callback() {
					request.setResult(this.getResult());
					request.complete();
				}
			});
			
			return;
		}
		
		if ("Custom".equals(op)) {
			bucket.handleCustom(rec, new FuncCallback<RecordStruct>() {			
				@Override
				public void callback() {
					request.setResult(this.getResult());
					request.complete();
				}
			});
			
			return;
		}
		
		if ("StartUpload".equals(op)) {
			bucket.handleStartUpload(rec, new FuncCallback<RecordStruct>() {			
				@Override
				public void callback() {
					request.setResult(this.getResult());
					request.complete();
				}
			});
			
			return;
		}
		
		if ("FinishUpload".equals(op)) {
			bucket.handleFinishUpload(rec, new FuncCallback<RecordStruct>() {			
				@Override
				public void callback() {
					request.setResult(this.getResult());
					request.complete();
				}
			});
			
			return;
		}
		
		if ("StartDownload".equals(op)) {
			bucket.handleStartDownload(rec, new FuncCallback<RecordStruct>() {			
				@Override
				public void callback() {
					request.setResult(this.getResult());
					request.complete();
				}
			});
			
			return;
		}
		
		if ("FinishDownload".equals(op)) {
			bucket.handleFinishDownload(rec, new FuncCallback<RecordStruct>() {			
				@Override
				public void callback() {
					request.setResult(this.getResult());
					request.complete();
				}
			});
			
			return;
		}
		
		request.errorTr(441, "dcmCms", "Buckets", op);
		request.complete();	
	}
}
