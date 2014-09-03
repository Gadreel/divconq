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
package divconq.api;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;

import divconq.bus.Message;
import divconq.bus.MessageUtil;
import divconq.bus.net.StreamMessage;
import divconq.hub.Hub;
import divconq.lang.OperationCallback;
import divconq.lang.OperationResult;
import divconq.session.IStreamDriver;
import divconq.session.ISessionAdapter;
import divconq.session.Session;
import divconq.session.DataStreamChannel;
import divconq.struct.ListStruct;
import divconq.xml.XElement;

// TODO test to be sure that the user associated with the session is not
// mixed up with the user calling the session, a single task should be able
// to run multiple local sessions, all impersonating different users
public class LocalSession extends ApiSession {
	protected Session session = null;
	
	@Override
	public void init(IApiSessionFactory fac, XElement config) {
		this.init(Hub.instance.getSessions().create("hub:", config.getAttribute("Domain")), config);
	}
	
	public void init(Session session, XElement config) {
		this.session = session;
		
		this.session.setKeep(true);
		this.session.setAdatper(new LocalSessionAdatper());
		
		this.user = this.session.getUser();
	}
	
	public class LocalSessionAdatper implements ISessionAdapter {			
		@Override
		public void deliver(Message msg) {
			String to = msg.getFieldAsString("Service");
			
			if ("Replies".equals(to)) 
				LocalSession.this.replies.handle(msg);
			else
				LocalSession.this.receiveMessage(msg);
		}

		@Override
		public ListStruct popMessages() {				
			return null;	// nothing to do, we deliver direct 
		}

		@Override
		public void stop() {
		}
	}	

	@Override
	public void stop() {
		Hub.instance.getSessions().terminate(this.session.getId());		
	}	
	
	@Override
	public void stopped() {
		this.replies.forgetReplyAll();		
	}
	
	@Override
	public void sendForgetMessage(Message msg) {
		this.session.touch();
		this.session.setTaskContext("hub:");
		this.session.sendMessage(msg);
	}
	
	@Override
	public void sendMessage(final Message msg, final ServiceResult callback) {		
		msg.removeField("RespondTo");		// always to Replies, no other supported
		
		this.replies.registerForReply(msg, callback);

		this.session.setTaskContext("hub:");
		this.session.sendMessage(msg);
	}

	@Override
	public void abortStream(String channelid) {
		DataStreamChannel chan = this.session.getChannel(channelid);
		
		if (chan != null)
			chan.abort();
	}
	
	/**
	 * Upload and close the stream
	 */
	@Override
	public void sendStream(ScatteringByteChannel in, long size, long offset, final String channelid, final OperationCallback callback) {
		final DataStreamChannel chan = this.session.getChannel(channelid);
		
		if (chan == null) {
			callback.error(1, "Missing channel");
			callback.completed();
			return;
		}
		
		chan.setDriver(new IStreamDriver() {
			@Override
			public void cancel() {
				callback.error(1, "Transfer canceled");				
				chan.complete();
				callback.completed();
			}
			
			@Override
			public void message(StreamMessage msg) {
				if (msg.isFinal()) {
					System.out.println("Final on channel: " + channelid);
					chan.complete();
					callback.completed();
				}
			}

			@Override
			public void nextChunk() {
				// won't chunk so won't happen here
			}
		});		
		
		long sent = offset;
		int seq = 0;
		
		if (size > 0) {
			callback.setAmountCompleted((int)(sent * 100 / size));
			chan.setAmountCompleted((int)(sent * 100 / size));		// keep the channel active so it does not timeout
		}
	
		try {
			ByteBuf bb = Unpooled.directBuffer(64 * 1024);
			
			long toskip = offset;
			
			if (in instanceof SeekableByteChannel) {
				((SeekableByteChannel)in).position(toskip);
			}
			else {
				while (toskip > 0) {
					int skip = (int) Math.min(bb.capacity(), toskip);
					toskip -= bb.writeBytes(in, skip);
					bb.clear();
				}
			}
			
			chan.touch();
			
			// now start writing the upload
			int amt = bb.writeBytes(in, bb.capacity());
			
			while (amt != -1) {
				bb.retain();		// this ups ref cnt to 2 - we plan to reuse the buffer
				
				StreamMessage b = new StreamMessage("Block", bb);  
				b.setField("Sequence", seq);
				
				OperationResult sr = chan.send(b);
				
				if (sr.hasErrors()) {
					chan.close();
					break;
				}
				
				seq++;
				sent += amt;
				
				if (size > 0) { 
					callback.setAmountCompleted((int)(sent * 100 / size));
					chan.setAmountCompleted((int)(sent * 100 / size));		// keep the channel active so it does not timeout
				}
				
				callback.touch();
				chan.touch();
				
				// by the time we get here, that buffer has been used up and we can use it for the next buffer
				if (bb.refCnt() != 1) 
					throw new IOException("Buffer reference count is not correct");
				
				// stop writing if canceled
				if (chan.isClosed())
					break;
				
				bb.clear();
				
				amt = bb.writeBytes(in, bb.capacity());
			}
			
			// we are now done with it
			bb.release();
			
			// final only if not canceled
			if (!chan.isClosed())
				chan.send(MessageUtil.streamFinal());
		} 
		catch (IOException x) {
			callback.error(1, "Local read error: " + x);
			
			chan.send(MessageUtil.streamError(1, "Source read error: " + x));
			chan.close();
			
			callback.completed();
		} 
		finally {
			try {
				in.close();
			} 
			catch (IOException x) {
			}
		}
	}

	/**
	 * download and close the stream
	 */
	@Override
	public void receiveStream(final WritableByteChannel out, final long size, final long offset, final String channelid, final OperationCallback callback) {
		final DataStreamChannel chan = this.session.getChannel(channelid);
		
		if (chan == null) {
			callback.error(1, "Missing channel");
			callback.completed();
			return;
		}
		
		callback.setAmountCompleted(0);
		
		chan.setDriver(new IStreamDriver() {
			protected long amt = offset;
			protected long seq = 0;
			
			@Override
			public void cancel() {
				callback.error(1, "Error from source: ");
				chan.complete();
				this.flushClose();
			}
			
			@Override
			public void message(StreamMessage msg) {
				int seqnum = (int) msg.getFieldAsInteger("Sequence", 0);
				
				if (seqnum != this.seq) {
					this.error(1, "Bad sequence number: " + seqnum);
					return;
				}
				
				try {
					if (msg.hasData()) {
						int camt = msg.getData().readableBytes();
						
						for (ByteBuffer bb : msg.getData().nioBuffers()) 
							out.write(bb);
						
						this.amt += camt;
					}
					
					seq++;
					
					if (size > 0) 
						callback.setAmountCompleted((int)(this.amt * 100 / size));
					
					if (msg.isFinal()) {
						chan.complete();
						this.flushClose();
					}
				} 
				catch (IOException x) {
					this.error(1, "Error writing stream: " + x);
				}
			}
			
			public void error(int code, String msg) {
				callback.error(1, msg);
				chan.send(MessageUtil.streamError(code, msg));
				this.flushClose();
			}
			
			public void flushClose() {
				try {
					out.close();
				} 
				catch (IOException x) {
				}
				
				callback.completed();
			}

			@Override
			public void nextChunk() {
				// doesn't matter for dest
			}
		});		
		
		chan.touch();
		
		// get the data flowing
		OperationResult sr = chan.send(new StreamMessage("Start"));
		
		if (sr.hasErrors()) 
			chan.close();
	}

	@Override
	public void clearToGuest() {
		this.session.clearToGuest();
	}
}
