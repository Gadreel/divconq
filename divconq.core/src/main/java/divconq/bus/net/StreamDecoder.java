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
package divconq.bus.net;

import java.util.List;

import divconq.lang.OperationContext;
import divconq.log.Logger;
import divconq.struct.RecordStruct;
import divconq.struct.builder.ObjectBuilder;
import divconq.struct.serial.BufferToCompositeParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class StreamDecoder extends ByteToMessageDecoder {
    enum State {
        HEADER,
        PAYLOAD_SIZE,
        PAYLOAD
    }

    protected State state = State.HEADER;
    
    protected BufferToCompositeParser headerparser = null;		
    protected ObjectBuilder builder = null;
    protected int size = 0;
    
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if ((in instanceof EmptyByteBuf) || (in.readableBytes() == 0))
			return;
		
    	OperationContext.useHubContext();
    	
		Logger.trace("Decoding Stream Data: " + in.readableBytes());
		
		switch (this.state) {
		case HEADER: {
			if (this.headerparser == null) {
				this.builder = new ObjectBuilder();
				this.headerparser = new BufferToCompositeParser(this.builder);
			}
			
			this.headerparser.parseStruct(in);
			
			// if not done wait for more bytes
			if (!this.headerparser.isDone())
				return;
			
			this.state = State.PAYLOAD_SIZE;
			
			// deliberate fall through 
		}
		case PAYLOAD_SIZE: {
            if (in.readableBytes() < 4) 
                return;
            
            this.size = in.readInt();
			
			this.state = State.PAYLOAD;
			
			// deliberate fall through 
		}
		case PAYLOAD: {
			// return here, without any state reset, means we need more before we can decide what to do
            if (in.readableBytes() < this.size) 
                return;
            
            // we have enough data to send the message...
            StreamMessage msg = new StreamMessage();
            
            // add Data only if there are some bytes, otherwise skip buffer allocation
            if (this.size > 0) {
            	ByteBuf bb = in.readSlice(this.size);
            	bb.retain();
            	msg.setData(bb);
            	//msg.retain();		// yes, two retains
            }
            
            msg.copyFields((RecordStruct) this.builder.getRoot());
			out.add(msg);
            
			// set state to start over - ready to process next message 
			this.headerparser = null;
			this.size = 0;
			this.state = State.HEADER;
		}
		}
	}
}
