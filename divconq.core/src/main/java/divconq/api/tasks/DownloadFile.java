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

import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

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

public class DownloadFile implements ISmartWork {
	public enum DownloadState {
		REQUEST_DOWNLOAD,
		RECEIVE_STREAM,
		RECORD_OUTCOME,
		FREE_LOCAL_CHANNEL,
		DOWNLOAD_DONE;
	}
	
	protected ApiSession session = null;
	protected TransferContext xferctx = null;
	protected DownloadState downloadstate = DownloadState.REQUEST_DOWNLOAD;

	@Override
	public void run(TaskRun run) {
		if (this.xferctx == null) {
			this.xferctx = new TransferContext();
			this.xferctx.load(run);
		}
		
		switch (this.downloadstate) {
			case REQUEST_DOWNLOAD: {
				this.requestDownload();
				break;
			}
			case RECEIVE_STREAM: {
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
			case DOWNLOAD_DONE: {
				this.xferctx.run.complete();
				break;
			}		
		}
	}
	
	protected void transition(DownloadState next) {
		this.downloadstate = next;
		
		// shed the current stack and come back clean on this run
		Hub.instance.getWorkPool().submit(this.xferctx.run);
	}
	
	
	public void requestDownload() {
		this.xferctx.run.nextStep("Request Download");
		this.xferctx.run.setProgressMessage("Sending start download request");		
    	
		RecordStruct rec = new RecordStruct();
		
		rec.setField("FilePath", this.xferctx.remote);		
		rec.setField("Params", this.xferctx.xferparams);
		rec.setField("Offset", this.xferctx.offset);
		
		Message msg = new Message(this.xferctx.servicename, "FileStore", "StartDownload", rec);
		
		this.session.establishDataStream("Downloading " + this.xferctx.remote.getFileName(), "Download", msg, new WrappedFuncCallback<RecordStruct>(this.xferctx.run) {
			@Override
			public void callback() {
				if (this.run.hasErrors()) { 
		    		DownloadFile.this.transition(DownloadState.FREE_LOCAL_CHANNEL);
				}
				else {
					DownloadFile.this.xferctx.setStreamInfo(this.getResult());
					DownloadFile.this.transition(DownloadState.RECEIVE_STREAM);
				}
			}
		});
	}
	
	public void sendStream() {		
		final TaskRun xferrun =  this.xferctx.run;
		
		xferrun.nextStep("Download File");
		xferrun.setProgressMessage("Downloading File");		
    	
		try {
			Path local = this.xferctx.local;
			
			FileChannel chn = (this.xferctx.offset > 0) 
					? FileChannel.open(local, StandardOpenOption.WRITE, StandardOpenOption.APPEND, StandardOpenOption.SYNC) 
					: FileChannel.open(local, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
			
			this.session.receiveStream(chn, this.xferctx.streaminfo.getFieldAsInteger("Size", 0), this.xferctx.offset, this.xferctx.channelid, new WrappedOperationCallback(xferrun) {						
				@Override
				public void callback() {
			    	if (!xferrun.hasErrors()) 
			    		xferrun.setProgressMessage("File Download Complete");		
			    	else
			    		xferrun.error("Receive Stream Error: " + xferrun.getMessage());
			    	
		    		DownloadFile.this.transition(DownloadState.RECORD_OUTCOME);
				}
			});
		} 
		catch (Exception x) {
			xferrun.errorTr(454, x);
			xferrun.error(1, "Send Stream Error: " + xferrun.getMessage());
    		DownloadFile.this.transition(DownloadState.RECORD_OUTCOME);
		}
    }
	
	public void recordOutcome() {
		this.xferctx.run.nextStep("Record File Outcome");		
		
    	String status = "Failure";    	
		RecordStruct evidence = new RecordStruct();

		try {
			Path local = DownloadFile.this.xferctx.local;
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
			this.xferctx.run.setProgressMessage("Download failed");
		}
		else {
			this.xferctx.run.setProgressMessage("Integrity good, approving!");
			status = "Success";
		}
		
		Message msg = new Message(this.xferctx.servicename, "FileStore", "FinishDownload", 
				new RecordStruct(
						new FieldStruct("FilePath", this.xferctx.remote),		
						new FieldStruct("Status", status),
						new FieldStruct("Evidence", evidence),
						new FieldStruct("Params", this.xferctx.xferparams)
				));
		
		DownloadFile.this.session.sendMessage(msg, new ServiceResult(this.xferctx.run) {							
			@Override
			public void callback() {
				DownloadFile.this.xferctx.run.copyMessages(this);					
				DownloadFile.this.transition(DownloadState.FREE_LOCAL_CHANNEL);
			}
		});
	}

	public void freeLocalChannel() {
		this.xferctx.run.nextStep("Cleanup");
		this.xferctx.run.setProgressMessage("Freeing channel");		
		
		this.session.freeDataChannel(this.xferctx.channelid, new WrappedOperationCallback(this.xferctx.run) {							
			@Override
			public void callback() {
				DownloadFile.this.transition(DownloadState.DOWNLOAD_DONE);
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
