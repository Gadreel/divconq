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
package divconq.bus;

import divconq.bus.Message;
import divconq.bus.net.StreamMessage;
import divconq.lang.op.OperationResult;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.work.TaskRun;

public class MessageUtil {
	static public Message success(String... flds) {
		Message m = new Message();
		
		if (flds.length > 0) {
			RecordStruct body = new RecordStruct();
			
			String name = null;
			
			for (String o : flds) {
				if (o == null)
					continue;
				
				if (name != null) {
					body.setField(name, o);
					name = null;
				}
				else {
					name = o;
				}
			}
			
			m.setField("Body", body);
		}
		
		return m;
	}

	public static Message success(Struct body) {
		Message m = new Message();
		m.setField("Body", body);
		return m;
	}
	
	// TODO discourage use of these following - use OC  .toLogMessage(); instead 
	static public Message error(int code, String msg) {
		OperationResult or = new OperationResult();
		or.error(code, msg);
		
		return or.toLogMessage();
	}
	
	static public Message errorTr(int code, Object... params) {
		OperationResult or = new OperationResult();
		or.errorTr(code, params);
		
		return or.toLogMessage();
	}
	
	static public Message fromRecord(RecordStruct msg) {
		Message m = new Message();
		
		m.copyFields(msg);
		
		return m;
	}
	
    static boolean addressReply(Message msg, RecordStruct original) {
    	if ((msg == null) || original.isFieldEmpty("RespondTo"))
    		return false;
    	
    	msg.setField("ToHub", original.getFieldAsString("FromHub"));
    	msg.setField("Service", original.getFieldAsString("RespondTo"));
    	msg.setField("Feature", "Reply");
    	msg.setField("Op", "Deliver");
    	msg.setField("Tag", original.getFieldAsString("RespondTag"));
    	
    	return true;
    }
    
	static public StreamMessage streamError(int code, String msg) {
		OperationResult or = new OperationResult();
		or.error(code, msg);
		
		return MessageUtil.streamMessages(or);
	}
	
	static public StreamMessage streamErrorTr(int code, Object... params) {
		OperationResult or = new OperationResult();
		or.errorTr(code, params);
		
		return MessageUtil.streamMessages(or);
	}
	
	static public StreamMessage streamMessages(OperationResult ri) {
		StreamMessage m = new StreamMessage();
		
		if (ri != null) 
			m.setField("Messages", ri.getMessages());
		
		return m;
	}
	
	static public StreamMessage streamFromRecord(RecordStruct msg) {
		StreamMessage m = new StreamMessage();
		
		m.copyFields(msg);
		
		return m;
	}
	
    static public void streamAddressReply(RecordStruct msg, RecordStruct original) {
    	if (msg == null) 
    		return;
    	
    	msg.setField("ToHub", original.getFieldAsString("Hub"));
    	msg.setField("ToSession", original.getFieldAsString("Session"));
    	msg.setField("ToChannel", original.getFieldAsString("Channel"));
    }
    
    // assumes body will be in StreamMessage format
    static public void streamAddressReply(Message msg, RecordStruct original) {
    	if (msg == null) 
    		return;
    	
    	msg.setToHub(original.getFieldAsString("Hub"));
    	
    	RecordStruct body = msg.getFieldAsRecord("Body");
    	
    	if (body == null) {
    		body = new RecordStruct();
    		msg.setField("Body", body);
    	}
	
    	body.setField("ToHub", original.getFieldAsString("Hub"));
    	body.setField("ToSession", original.getFieldAsString("Session"));
    	body.setField("ToChannel", original.getFieldAsString("Channel"));
    }

	public static StreamMessage streamFinal() {
		return new StreamMessage("Final");
	}

	public static Message message(TaskRun request) {
		return (Message) request.getTask().getParams();
	}

	public static RecordStruct bodyAsRecord(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		return msg.getFieldAsRecord("Body");
	}

	public static ListStruct bodyAsList(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		return msg.getFieldAsList("Body");
	}
	
	// search backward through log to find an error, if we hit a message with an Exit tag then
	// stop, as Exit resets Error (unless it is an error itself)
	// similar to findExitEntry but stops after last Error as we don't need to loop through all
	static public boolean hasErrors(RecordStruct rec) {
		ListStruct msgs = rec.getFieldAsList("Messages");
		
		if (msgs == null)
			return false;
		
		for (int i = msgs.getSize() - 1; i >= 0; i--) {
			RecordStruct msg =  (RecordStruct) msgs.getItem(i);
			
			if ("Error".equals(msg.getFieldAsString("Level")))
				return true;
		
			if (msg.hasField("Tags")) {
				ListStruct tags = msg.getFieldAsList("Tags");
				
				if (tags.stringStream().anyMatch(tag -> tag.equals("Exit")))
					break;
			}
		}
		
		return false;
	}

	static public long getCode(RecordStruct rec) {
		RecordStruct entry = MessageUtil.findExitEntry(rec);
		
		if (entry == null)
			return 0;
		
		return entry.getFieldAsInteger("Code", 0);
	}

	static public String getMessage(RecordStruct rec) {
		RecordStruct entry = MessageUtil.findExitEntry(rec);
		
		if (entry == null)
			return null;
		
		return entry.getFieldAsString("Message");
	}

	static public RecordStruct findExitEntry(RecordStruct rec) {
		return MessageUtil.findExitEntry(rec, 0, -1);
	}

	// search backward through log to find an exit, if we hit a message with an Exit tag then
	// stop, as Exit resets Error.  now return the first error after Exit.  if no errors after
	// then return Exit
	static public RecordStruct findExitEntry(RecordStruct rec, int msgStart, int msgEnd) {
		ListStruct msgs = rec.getFieldAsList("Messages");
		
		if (msgs == null)
			return null;
		
		if (msgEnd == -1)
			msgEnd = msgs.getSize();
		
		RecordStruct firsterror = null;
		
		for (int i = msgEnd - 1; i >= msgStart; i--) {
			RecordStruct msg =  (RecordStruct) msgs.getItem(i);
			
			if ("Error".equals(msg.getFieldAsString("Level")))
				firsterror = msg;
		
			if (msg.hasField("Tags")) {
				ListStruct tags = msg.getFieldAsList("Tags");
				
				if (tags.stringStream().anyMatch(tag -> tag.equals("Exit")))
					return (firsterror != null) ? firsterror : msg;
			}
		}
		
		return firsterror;
	}	
}
