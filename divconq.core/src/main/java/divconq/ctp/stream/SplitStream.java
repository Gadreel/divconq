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

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import divconq.ctp.f.FileDescriptor;
import divconq.script.StackEntry;
import divconq.util.FileUtil;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class SplitStream extends BaseStream implements IStreamSource {
	protected int seqnum = 1;
	protected int size = 10 * 1024 * 1024;
	protected String template = "file-%seq%.bin";
	
	protected int currchunk = 0;
	protected FileDescriptor sfile = null;
    protected List<FileDescriptor> outlist = new ArrayList<>();
    protected List<ByteBuf> outbuf = new ArrayList<>();
	
    public SplitStream() {
    }

	@Override
	public void init(StackEntry stack, XElement el) {
		this.seqnum = (int) stack.intFromElement(el, "StartAt", this.seqnum);
		
		String size = stack.stringFromElement(el, "Size", "10MB");
		
		this.size = (int) FileUtil.parseFileSize(size);
		
		String temp = stack.stringFromElement(el, "Template");
		
		if (StringUtil.isNotEmpty(temp))
			this.template = temp;
	}
    
	// make sure we don't return without first releasing the file reference content
	@Override
	public ReturnOption handle(FileDescriptor file, ByteBuf data) {
    	if (file == FileDescriptor.FINAL) 
    		return this.downstream.handle(file, data);
    	
    	ByteBuf in = data;

    	if (in != null) {
    		while (in.isReadable()) {
    			int amt = Math.min(in.readableBytes(), this.size - this.currchunk);
    			
    			ByteBuf out = in.copy(in.readerIndex(), amt);
    			
    			in.skipBytes(amt);
    			this.currchunk += amt;
    		
    			boolean eof = (this.currchunk == this.size) || (!in.isReadable() && file.isEof());
    			
    			this.nextMessage(out, file, eof);
    			
    			if (eof) {
    				this.seqnum++;
    				this.currchunk = 0;
    			}
			}
    		
    		in.release();
    	}
    	else if (file.isEof()) {
			this.nextMessage(null, file, false);
    	}
    	
		// write all messages in the queue
		while (this.outlist.size() > 0) {
			ReturnOption ret = this.downstream.handle(this.outlist.remove(0), this.outbuf.remove(0));
			
			if (ret != ReturnOption.CONTINUE)
				return ret;
		}
    	
       	return ReturnOption.CONTINUE;
    }
    
    public void nextMessage(ByteBuf out, FileDescriptor curr, boolean eof) {
		// create the output message
    	FileDescriptor blk = new FileDescriptor();
		
        blk.setModTime(System.currentTimeMillis());		
        
        // keep the path, just vary the name to the template
        blk.setPath(curr.path().resolvePeer("/" + this.template.replace("%seq%", this.seqnum + "")));
        
        blk.setEof(eof);
        
        if (eof)
        	blk.setSize(this.currchunk);
        else
        	blk.setSize(0);						// don't know yet
        
        this.outlist.add(blk);
        this.outbuf.add(out);
    }
    
    @Override
    public void read() {
		// write all messages in the queue
		while (this.outlist.size() > 0) {
			ReturnOption ret = this.downstream.handle(this.outlist.remove(0), this.outbuf.remove(0));
			
			if (ret != ReturnOption.CONTINUE)
				return;
		}
		
    	this.upstream.read();
    }
}
