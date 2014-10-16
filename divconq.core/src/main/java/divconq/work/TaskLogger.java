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
package divconq.work;

import java.io.BufferedWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import divconq.lang.IOperationLogger;
import divconq.lang.OperationResult;
import divconq.lang.StringBuilder32;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;

public class TaskLogger extends RecordStruct implements IOperationLogger, ITaskObserver {
	protected ListStruct entries = new ListStruct();
	protected ListStruct replaces = new ListStruct();
	protected Path logfile = null;
	protected BufferedWriter bw = null;

	public void setLogFile(Path v) {
		this.logfile = v;
		this.setField("LogFile", v);
	}
	
	public TaskLogger() {
		this.setField("Entries", this.entries);
		this.setField("Replaces", this.replaces);
	}
	
	public void addReplace(String oldValue, String newValue) {
		this.replaces.addItem(
				new RecordStruct(
						new FieldStruct("Old", oldValue),
						new FieldStruct("New", newValue)
				)
		);
	}
	
	@Override
	public Struct deepCopy() {
		TaskLogger cp = new TaskLogger();
		this.doCopy(cp);
		return cp;
	}

	@Override
	public String logToString() {
		StringBuilder32 sb = new StringBuilder32();
		
		for (Struct s : this.entries.getItems()) {
			String line = this.formatLogEntry((RecordStruct)s);
			
			if (StringUtil.isNotEmpty(line))
				sb.append(line + "\n");
		}
		
		return sb.toString();
	}
	
	public String formatMessage(String msg) {
		for (Struct s : this.replaces.getItems()) { 
			RecordStruct re = (RecordStruct) s;
			
			msg = msg.replace(re.getFieldAsString("Old"), re.getFieldAsString("New"));
		}
		
		return msg;
	}
	
	public String formatLogEntry(RecordStruct entry) {
		DateTime occured = entry.getFieldAsDateTime("Occurred");
		
		String lvl = entry.getFieldAsString("Level");
		
		lvl = StringUtil.alignLeft(lvl, ' ', 6);
		
		String msg = this.formatMessage(entry.getFieldAsString("Message"));
		
		// return null if msg was filtered
		if (StringUtil.isEmpty(msg))
			return null;
		
		return occured + " " + lvl + msg;
	}
	
	@Override
	public void log(OperationResult or, RecordStruct entry) {
		this.entries.addItem(entry);
		
		if (this.bw == null)
			return;
		
		try {
			String line = this.formatLogEntry((RecordStruct)entry);
			
			if (StringUtil.isNotEmpty(line)) {
				this.bw.append(line);
				this.bw.newLine();
				this.bw.flush();
			}
		}
		catch (Exception x) {
			// life is bad if we get here, don't think we should do anything
			// but let the system melt down
		}
	}

	@Override
	public void boundary(OperationResult or, String... tags) {
	}

	@Override
	public void step(OperationResult or, int num, int of, String name) {
	}

	@Override
	public void progress(OperationResult or, String msg) {
	}

	@Override
	public void amount(OperationResult or, int v) {
	}

	@Override
	public void prep(TaskRun or) {
		// don't open login file til start		
	}

	@Override
	public void start(TaskRun run) {
		// super class might already set logfile, but if not see if we need to
		if ((this.logfile == null) && !this.isFieldEmpty("LogFile")) 
			this.logfile = Paths.get(this.getFieldAsString("LogFile"));
		
		RecordStruct params = run.getTask().getParams();
		
		// if temp folder is set then we will log into there
		String tempfolder = ((params != null) && !params.isFieldEmpty("_TempFolder"))  
			? params.getFieldAsString("_TempFolder")
			: null;
		
		if ((this.logfile == null) && StringUtil.isNotEmpty(tempfolder)) {
			String fname = !this.isFieldEmpty("LogFileName")  
					? this.getFieldAsString("LogFileName")
					: DateTimeFormat.forPattern("yyyyMMdd'_'HHmmss").print(new DateTime(DateTimeZone.UTC)) + ".log";
			
    		this.logfile = Paths.get(tempfolder, fname);
		}
		
		// if we do have a log file try to open it
		if (this.logfile != null) {
			try {
				Files.createDirectories(this.logfile.getParent());

				this.bw = Files.newBufferedWriter(this.logfile, Charset.forName("UTF-8"));
			}
			catch (Exception x) {
				run.errorTr(181, x);
			}
		}
		
		// automatically clean references to the temp folder in log file
		if (StringUtil.isNotEmpty(tempfolder)) {
			this.addReplace(tempfolder, "");
			this.addReplace(tempfolder.replace('\\', '/'), "");
		}
	}

	@Override
	public void completed(TaskRun or) {
	}

	@Override
	public void stop(TaskRun or) {
		if (this.bw == null)
			return;
		
		try {
			this.bw.flush();
			this.bw.close();
			
			String fn = this.logfile.getFileName().toString();

			// automatically rename .tmp to .log
			if (fn.endsWith(".tmp")) {
				fn = fn.substring(0, fn.length() - 4) + ".log";
				
				Path finallog = this.logfile.resolveSibling(fn);
				
				// if there happens to be a log already there, replace it
				Files.move(this.logfile, finallog, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
				
				this.logfile = finallog;
			}
		}
		catch (Exception x) {
			// don't care, nothing we can do
			System.out.println("Error copying log file: " + x);
		}
	}
}