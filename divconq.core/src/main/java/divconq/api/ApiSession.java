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

import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import divconq.bus.Message;
import divconq.bus.MessageUtil;
import divconq.hub.Hub;
import divconq.lang.FuncCallback;
import divconq.lang.OperationCallback;
import divconq.lang.OperationContext;
import divconq.lang.TimeoutPlan;
import divconq.lang.UserContext;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.xml.XElement;

abstract public class ApiSession {
	static public ApiSession createLocalSession(String domain) {
		return Hub.instance.createLocalApiSession(domain);
	}
	
	static public ApiSession createSessionFromConfig(String name) {
		return Hub.instance.createApiSession(name);
	}
	
	protected String sessionid = null;
	protected String sessionKey = null;
	protected String hubid = null;
	protected UserContext user = null;
	protected ReplyService replies = new ReplyService();
	public Message lastResult = null;
	
	public ReplyService getReplyService() {
		return this.replies;
	}
	
	public Message getLastResult() {
		return this.lastResult;
	}
	
	abstract public void init(IApiSessionFactory fac, XElement config);
	
	public void receiveMessage(Message msg) {
		// we need to restore/set the local operation context if anything is done here.
		// don't use the messages op context, it only applies within its own matrix
		// at least for starters let's set to guest
		
		OperationContext.useNewGuest();
		
		System.out.println("Got a message for ApiSession, not sure what to do with it!\n\n" + msg);
		
		// TODO else look at other services published through this session (raise message in event to API)
		// push message back out to CoreApi
	}

	public UserContext getUser() {
		return this.user;
	}

	public String getSessionId() {
		return this.sessionid;
	}

	public String getHubId() {
		return this.hubid;
	}
	
	public String getSessionKey() {
		return this.sessionKey;
	}
	
	abstract public void sendForgetMessage(Message msg);
	
	public Message sendMessage(Message msg) {
		return this.sendMessage(msg, TimeoutPlan.Regular);
	}
	
	public Message sendMessage(final Message msg, TimeoutPlan timeoutPlan) {
		this.lastResult = null;
		
		final CountDownLatch latch = new CountDownLatch(1);
		
		this.sendMessage(msg, new ServiceResult(timeoutPlan) {
			@Override
			public void callback() {
				ApiSession.this.lastResult = this.getResult();
				
				latch.countDown();
			}
		});			
		
		try {
			latch.await();
		} 
		catch (InterruptedException x) {
			this.lastResult = MessageUtil.errorTr(445, x);
		}
		
		return this.lastResult;
	}
	
	abstract public void sendMessage(Message msg, ServiceResult callback);
	
	public void allocDataChannel(String title, final FuncCallback<String> callback) {
		Message msg = new Message("Session", "DataChannel", "Allocate", new RecordStruct(new FieldStruct("Title", title)));
		
		this.sendMessage(msg, new ServiceResult() {
			@Override
			public void callback() {
				callback.copyMessages(this);				
				
				if (!this.hasErrors())
					callback.setResult(this.getBodyAsRec().getFieldAsString("ChannelId"));
				
				callback.completed();				
			}
		});			
	}

	public void freeDataChannel(String channelid, final OperationCallback callback) {
		Message msg = new Message("Session", "DataChannel", "Free", new RecordStruct(new FieldStruct("ChannelId", channelid)));
		
		this.sendMessage(msg, new ServiceResult() {
			@Override
			public void callback() {
				callback.copyMessages(this);				
				callback.completed();				
			}
		});			
	}
	
	public void establishDataStream(String title, String mode, Message streamRequest, final FuncCallback<RecordStruct> callback) {
		Message msg = new Message("Session", "DataChannel", "Establish", new RecordStruct(
				new FieldStruct("Title", title),
				new FieldStruct("Mode", mode),
				new FieldStruct("StreamRequest", streamRequest)
		));
		
		this.sendMessage(msg, new ServiceResult() {
			@Override
			public void callback() {
				callback.copyMessages(this);				
				
				if (!this.hasErrors())
					callback.setResult(this.getBodyAsRec());
				
				callback.completed();				
			}
		});			
	}
	
	public void bindSourceChannel(String channelid, RecordStruct addressing, final OperationCallback callback) {
		// TODO validate addressing 
		
		RecordStruct body = new RecordStruct(
				new FieldStruct("ChannelId", channelid),
				new FieldStruct("Mode", "Source"),
				new FieldStruct("Hub", addressing.getFieldAsAny("Hub")),
				new FieldStruct("Session", addressing.getFieldAsAny("Session")),
				new FieldStruct("Channel", addressing.getFieldAsAny("Channel"))
		);
		
		Message msg = new Message("Session", "DataChannel", "Bind", body);
		
		this.sendMessage(msg, new ServiceResult() {
			@Override
			public void callback() {
				callback.copyMessages(this);				
				callback.completed();				
			}
		});			
	}
	
	public void bindDestChannel(String channelid, RecordStruct addressing, final OperationCallback callback) {
		// TODO validate addressing 
		
		RecordStruct body = new RecordStruct(
				new FieldStruct("ChannelId", channelid),
				new FieldStruct("Mode", "Destination"),
				new FieldStruct("Hub", addressing.getFieldAsAny("Hub")),
				new FieldStruct("Session", addressing.getFieldAsAny("Session")),
				new FieldStruct("Channel", addressing.getFieldAsAny("Channel"))
		);
		
		Message msg = new Message("Session", "DataChannel", "Bind", body);
		
		this.sendMessage(msg, new ServiceResult() {
			@Override
			public void callback() {
				callback.copyMessages(this);				
				callback.completed();				
			}
		});			
	}
	
	abstract public void sendStream(ScatteringByteChannel in, long size, long offset, String channelid, OperationCallback or);
	abstract public void receiveStream(WritableByteChannel out, long size, long offset, String channelid, OperationCallback callback);
	abstract public void abortStream(String channelid);

	public void thawContext(Message result) {
		if (result == null)
			return;	
		
		RecordStruct info = result.getFieldAsRecord("Body");
		
		if (info == null)
			return;
		
		this.user = UserContext.allocate(info);		
		
		this.sessionKey = info.getFieldAsString("SessionKey");
		this.sessionid =  info.getFieldAsString("SessionId");
		
		this.hubid = this.sessionid.substring(0, this.sessionid.indexOf('_'));
	}
	
	public void clearToGuest() {
		this.user = UserContext.allocateGuest();
	}
	
	public boolean startSession() {
		return this.startSession(null);
	}
	
	public boolean startSession(String user, String pass) {
		return this.startSession(new RecordStruct(
				new FieldStruct("UserName", user),
				new FieldStruct("Password", pass)
			));
	}
	
	public boolean startSession(String user, String pass, String code) {
		return this.startSession(new RecordStruct(
				new FieldStruct("UserName", user),
				new FieldStruct("Password", pass),
				new FieldStruct("ConfirmationCode", code)
			));
	}
	
	public boolean startSessionAsGuest() {
		return this.startSession(null);
	}
	
	public boolean startSession(RecordStruct creds) {
		// new creds means new user, start as guest
		this.clearToGuest();
		
		Message msg = new Message();
		msg.setField("Service", "Session");
		msg.setField("Feature", "Control");
		msg.setField("Op", "Start");
		
		if (creds != null)
			msg.setField("Credentials", creds);
		
		Message rmsg = this.sendMessage(msg);
		
		if (rmsg == null)
			return false;
		
		this.thawContext(rmsg);
		
		// a real user or a guest will both be verified
		return !rmsg.hasErrors() && this.getUser().isVerified();
	}
	
	public void stop() {
		Message msg = new Message();
		msg.setField("Service", "Session");
		msg.setField("Feature", "Control");
		msg.setField("Op", "Terminate");
		
		this.sendForgetMessage(msg);
		
		this.clearToGuest();
		this.stop();
	}
	
	abstract public void stopped();
	
	public Collection<Message> checkInBox() {
		Message msg = new Message();
		msg.setField("Service", "Session");
		msg.setField("Feature", "Control");
		msg.setField("Op", "CheckInBox");
		
		Message rmsg = this.sendMessage(msg);
		
		if (rmsg.hasErrors())
			return null;
		
		ListStruct mlist = rmsg.getFieldAsList("Body"); 
		
		if (mlist == null)
			return null;
		
		ArrayList<Message> msgs = new ArrayList<Message>();
		
		for (Struct m : mlist.getItems()) {
			if (m instanceof RecordStruct) 
				msgs.add(MessageUtil.fromRecord((RecordStruct) m));
		}
		
		return msgs;
	}
}
