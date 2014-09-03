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

import java.nio.file.Path;
import java.nio.file.Paths;

import divconq.interchange.CommonPath;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;
import divconq.work.TaskRun;

public class TransferContext {
	protected TaskRun run = null;
	protected Path local = null;
	protected CommonPath remote = null;
	protected String servicename = null;
	protected String hashmethod = null;
	protected String channelid = null;
	protected RecordStruct streaminfo = null;
	protected Struct xferparams = null;
	protected boolean overWrite = false;
	protected long offset = 0;
	
	public void setStreamInfo(RecordStruct v) {
		this.streaminfo = v;
		this.channelid = v.getFieldAsString("ChannelId");
		
		RecordStruct params = run.getTask().getParams();
		
		params.setField("StreamInfo", this.streaminfo);
	}
	
	protected void load(TaskRun run) {
		this.run = run;
		
		RecordStruct params = run.getTask().getParams();
		
		if (params == null) {
			run.error(1, "Missing params");
			return;
		}
		
		this.local = Paths.get(params.getFieldAsString("LocalPath"));
		this.remote = new CommonPath(params.getFieldAsString("RemotePath"));
		this.hashmethod = params.getFieldAsString("HashMethod");
		this.xferparams = params.getFieldAsStruct("TransferParams");
		this.offset = params.getFieldAsInteger("Offset", 0);
		this.overWrite = params.getFieldAsBooleanOrFalse("ForceOverwrite");
		
		if (StringUtil.isEmpty(this.hashmethod))
			this.hashmethod = "SHA128";		// default
		
		this.servicename = params.getFieldAsString("ServiceName");
		
		if (StringUtil.isEmpty(this.servicename))
			this.servicename = "dciFileServer";		// default
		
		if ((this.local == null) || (this.local.getNameCount() < 1)) {
			run.error(1, "Invalid source");  
    		return;
		}
		
		if ((this.remote == null) || this.remote.isRoot()) {
			run.error(1, "Invalid dest");  
    		return;
		}
	}
}
