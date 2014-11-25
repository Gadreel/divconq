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
package divconq.ctp.stream;

import io.netty.buffer.ByteBuf;

import java.io.IOException;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import divconq.ctp.f.FileDescriptor;
import divconq.hub.Hub;
import divconq.io.CyclingByteBufferOutputStream;
import divconq.lang.op.OperationContext;
import divconq.script.StackEntry;
import divconq.util.FileUtil;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class TarStream extends BaseStream implements IStreamSource {
    protected CyclingByteBufferOutputStream bstream = null;
    protected TarArchiveOutputStream tstream = null;
    protected boolean archiveopenflag = false;
    protected boolean finalflag = false;
    protected String nameHint = null;
    protected String lastpath = null;
    
    public TarStream() {
    }
    
    public TarStream withNameHint(String v) {
    	this.nameHint = v;
    	return this;
    }

	@Override
	public void init(StackEntry stack, XElement el) {
		this.nameHint = stack.stringFromElement(el, "NameHint");
	}

	@Override
    public void close() {
		//System.out.println("Tar killed");	// TODO
		
    	if (this.tstream != null)
			try {
				this.tstream.close();
			} 
    		catch (IOException x) {
			}
    	
    	this.bstream = null;
    	this.tstream = null;
    
    	super.close();
    }
    
	// make sure we don't return without first releasing the file reference content
	@Override
	public ReturnOption handle(FileDescriptor file, ByteBuf data) {
    	if (file == FileDescriptor.FINAL) {
    		if (this.tstream == null) 
        		return this.downstream.handle(file, data);
    			
    		this.finalflag = true;
    	}
    	
    	// I don't think tar cares about folder entries at this stage - tar is for file content only
    	// folder scanning is upstream in the FileSourceStream and partners
    	// TODO try with ending / to file name
    	if (file.isFolder())
    		return ReturnOption.CONTINUE;
    	
    	// init if not set for this round of processing 
    	if (this.tstream == null) {
    		this.bstream = new CyclingByteBufferOutputStream();
            this.tstream = new TarArchiveOutputStream(this.bstream);
            this.tstream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
    	}
        
    	ByteBuf in = data;
        ByteBuf out = null; 
    	
        // always allow for a header (512) and/or footer (1024) in addition to content
        int sizeEstimate = (in != null) ? in.readableBytes() + 2048 : 2048;
        out = Hub.instance.getBufferAllocator().heapBuffer(sizeEstimate);
    	
        this.bstream.installBuffer(out);
        
        // TODO if there is no output available to send and not EOF then just request more,
        // no need to send a message that is empty and not EOF
        
        FileDescriptor blk = new FileDescriptor();
		
		if (StringUtil.isNotEmpty(this.lastpath)) {
			blk.setPath(this.lastpath);
		}
		else {
			if (file.hasPath()) 
				this.lastpath = "/" + (StringUtil.isNotEmpty(this.nameHint) ? this.nameHint : file.path().getFileName()) + ".tar"; 
			else if (StringUtil.isNotEmpty(this.nameHint))
				this.lastpath = "/" + this.nameHint + ".tar";
			else
				this.lastpath = "/" + FileUtil.randomFilename() + ".tar";
			
			blk.setPath(this.lastpath);
		}
		
		blk.setModTime(System.currentTimeMillis());
    	
    	if (!this.archiveopenflag && !this.finalflag) {
    		TarArchiveEntry tentry = new TarArchiveEntry(file.getPath().toString().substring(1), true);		
    		tentry.setSize(file.getSize());
    		tentry.setModTime(file.getModTime());
    		
    		try {
				this.tstream.putArchiveEntry(tentry);
			} 
    		catch (IOException x) {
    			if (in != null)
    				in.release();
    			
				out.release();
				OperationContext.get().getTaskRun().kill("Problem writing tar entry: " + x);
				return ReturnOption.DONE;
			}
    		
    		this.archiveopenflag = true;
    	}
		
    	if (in != null)
			try {
				this.tstream.write(in.array(), in.arrayOffset(), in.writerIndex());
			} 
			catch (IOException x) {
				in.release();
				out.release();
				OperationContext.get().getTaskRun().kill("Problem writing tar body: " + x);
				return ReturnOption.DONE;
			}

        if (file.isEof()) {
        	try {
		        this.tstream.closeArchiveEntry();
			} 
			catch (IOException x) {
    			if (in != null)
    				in.release();
    			
				out.release();
				OperationContext.get().getTaskRun().kill("Problem closing tar entry: " + x);
				return ReturnOption.DONE;
			}
    		
    		this.archiveopenflag = false;
        }
        
        if (in != null)
        	in.release();
        
    	if (file == FileDescriptor.FINAL) {			
        	blk.setEof(true);
        	
        	try {
		        this.tstream.close();
			} 
			catch (IOException x) {
				//in.release();
				out.release();
				OperationContext.get().getTaskRun().kill("Problem closing tar stream: " + x);
				return ReturnOption.DONE;
			}
        	
    		this.tstream = null;
    		this.bstream = null;
    	}
    	else
    		this.bstream.uninstallBuffer();		// we are done with out forever, don't reference it
		
		System.out.println("tar sending: " + out.readableBytes());
		
		ReturnOption v = this.downstream.handle(blk, out);
       	
       	if (!this.finalflag)
       		return v;
       	
       	if (v == ReturnOption.CONTINUE)
    		return this.downstream.handle(FileDescriptor.FINAL, null);
       		
       	return ReturnOption.DONE;
    }
    
    @Override
    public void read() {
    	if (this.finalflag) {
    		this.downstream.handle(FileDescriptor.FINAL, null);
    		return;
    	}
    	
    	this.upstream.read();
    }
}
