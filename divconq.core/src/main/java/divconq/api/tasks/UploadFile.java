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
package divconq.api.tasks;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import divconq.api.ApiSession;
import divconq.api.ServiceResult;
import divconq.bus.Message;
import divconq.hub.Hub;
import divconq.lang.FuncResult;
import divconq.lang.WrappedFuncCallback;
import divconq.lang.WrappedOperationCallback;
import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;
import divconq.util.HashUtil;
import divconq.util.StringUtil;
import divconq.work.ISmartWork;
import divconq.work.TaskRun;

public class UploadFile implements ISmartWork {
	public enum UploadState {
		REQUEST_UPLOAD,
		SEND_STREAM,
		RECORD_OUTCOME,
		FREE_LOCAL_CHANNEL,
		UPLOAD_DONE;
	}
	
	protected ApiSession session = null;
	protected TransferContext xferctx = null;
	protected UploadState uploadstate = UploadState.REQUEST_UPLOAD;

	@Override
	public void run(TaskRun run) {
		if (this.xferctx == null) {
			this.xferctx = new TransferContext();
			this.xferctx.load(run);
		}
		
		switch (this.uploadstate) {
			case REQUEST_UPLOAD: {
				this.requestUpload();
				break;
			}
			case SEND_STREAM: {
				this.sendStream();
				break;
			}
			case RECORD_OUTCOME: {
				this.recordOutcome();
				break;
			}
			case FREE_LOCAL_CHANNEL: {
				this.freeLocalChannel();
				break;
			}
			case UPLOAD_DONE: {
				this.xferctx.run.complete();
				break;
			}		
		}
	}
	
	protected void transition(UploadState next) {
		this.uploadstate = next;
		
		// shed the current stack and come back clean on this run
		Hub.instance.getWorkPool().submit(this.xferctx.run);
	}
	
	
	public void requestUpload() {
		this.xferctx.run.nextStep("Request Upload");
		this.xferctx.run.setProgressMessage("Sending start upload request");		
    	
		RecordStruct rec = new RecordStruct();
		
		try {
			rec.setField("FilePath", this.xferctx.remote);
			rec.setField("Params", this.xferctx.xferparams);
			rec.setField("ForceOverwrite", this.xferctx.overWrite);
			rec.setField("FileSize", Files.size(this.xferctx.local));
		} 
		catch (IOException x) {
			this.xferctx.run.error(1, "Start Upload error: " + x);
    		UploadFile.this.transition(UploadState.FREE_LOCAL_CHANNEL);
    		return;
		}
		
		Message msg = new Message(this.xferctx.servicename, "FileStore", "StartUpload", rec);
		
		this.session.establishDataStream("Uploading " + this.xferctx.local.getFileName(), "Upload", msg, new WrappedFuncCallback<RecordStruct>(this.xferctx.run) {
			@Override
			public void callback() {
				if (this.run.hasErrors()) { 
		    		UploadFile.this.transition(UploadState.FREE_LOCAL_CHANNEL);
				}
				else {
					UploadFile.this.xferctx.setStreamInfo(this.getResult());
					UploadFile.this.transition(UploadState.SEND_STREAM);
				}
			}
		});
	}
	
	public void sendStream() {		
		final TaskRun xferrun =  this.xferctx.run;
		
		xferrun.nextStep("Upload File");
		xferrun.setProgressMessage("Uploading File");		
    	
		try {
			Path local = UploadFile.this.xferctx.local;
			
			this.session.sendStream(FileChannel.open(local), Files.size(local), this.xferctx.streaminfo.getFieldAsInteger("Size", 0), this.xferctx.channelid, new WrappedOperationCallback(xferrun) {						
				@Override
				public void callback() {
					// no need to copy messages with a wrapped callback
					
			    	if (!xferrun.hasErrors()) 
			    		xferrun.setProgressMessage("File Upload Complete");		
			    	else
			    		xferrun.error("Send Stream Error: " + xferrun.getMessage());
			    	
		    		UploadFile.this.transition(UploadState.RECORD_OUTCOME);
				}
			});
		} 
		catch (Exception x) {
			xferrun.errorTr(454, x);
			xferrun.error(1, "Send Stream Error: " + xferrun.getMessage());
    		UploadFile.this.transition(UploadState.RECORD_OUTCOME);
		}
    }
	
	public void recordOutcome() {
		this.xferctx.run.nextStep("Record File Outcome");		
		
    	String status = "Failure";    	
		RecordStruct evidence = new RecordStruct();

		try {
			Path local = UploadFile.this.xferctx.local;
			evidence.setField("Size", Files.size(local));
			
			// collect evidence
			String hashMethod = this.xferctx.streaminfo.getFieldAsString("BestEvidence");
			
			// if looking for more evidence than Size, calc the hash
			if (!"Size".equals(hashMethod)) {
				try {
					FuncResult<String> res = HashUtil.hash(hashMethod, Files.newInputStream(local));
					this.xferctx.run.copyMessages(res); 
					
					if (!res.hasErrors())
						evidence.setField(hashMethod, res.getResult());
				}
				catch (Exception x) {
					this.xferctx.run.error(1, "Unable to read file for hash: " + x);
				}
			}
		}
		catch (Exception x) {
			this.xferctx.run.error("Error collecting evidence: " + x);
		}
		
		if (this.xferctx.run.hasErrors()) {
			this.xferctx.run.setProgressMessage("Upload failed");
		}
		else {
			this.xferctx.run.setProgressMessage("Integrity good, approving!");
			status = "Success";
		}
		
		Message msg = new Message(this.xferctx.servicename, "FileStore", "FinishUpload", 
				new RecordStruct(
						new FieldStruct("FilePath", this.xferctx.remote),		
						new FieldStruct("Status", status),
						new FieldStruct("Evidence", evidence),
						new FieldStruct("Params", this.xferctx.xferparams)
				));
		
		UploadFile.this.session.sendMessage(msg, new ServiceResult(this.xferctx.run) {							
			@Override
			public void callback() {
				UploadFile.this.xferctx.run.copyMessages(this);					
				UploadFile.this.transition(UploadState.FREE_LOCAL_CHANNEL);
			}
		});
	}

	public void freeLocalChannel() {
		this.xferctx.run.nextStep("Cleanup");
		this.xferctx.run.setProgressMessage("Freeing channel");		
		
		this.session.freeDataChannel(this.xferctx.channelid, new WrappedOperationCallback(this.xferctx.run) {							
			@Override
			public void callback() {
				UploadFile.this.transition(UploadState.UPLOAD_DONE);
			}
		});		
	}
	
	@Override
	public void cancel(TaskRun run) {
		if (StringUtil.isNotEmpty(this.xferctx.channelid))
			this.session.abortStream(this.xferctx.channelid);
	}

	@Override
	public void completed(TaskRun run) {
	}
}
