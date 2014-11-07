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
package divconq.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import divconq.bus.IService;
import divconq.bus.Message;
import divconq.count.CountManager;
import divconq.hub.Hub;
import divconq.hub.ISystemWork;
import divconq.hub.SysReporter;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationContextBuilder;
import divconq.lang.op.OperationObserver;
import divconq.lang.op.OperationResult;
import divconq.log.Logger;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;
import divconq.work.TaskRun;
import divconq.work.Task;
import divconq.xml.XElement;

public class Sessions implements IService {
	protected ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<String, Session>();

	public String serviceName() {
		return "Session";
	}	
	
	public Collection<Session> list() {
		return this.sessions.values();
	}
	
	public void init(OperationResult or, XElement config) {
		ISystemWork sessioncleanup = new ISystemWork() {
			@Override
			public void run(SysReporter reporter) {
				reporter.setStatus("Reviewing session plans");
				
				if (!Hub.instance.isStopping()) {
					// sessions only last 1 minute
					long clearGuest = System.currentTimeMillis() - (65 * 1000);		// TODO configure
					//long clearUser = new DateTime().minusSeconds(301).getMillis();		// TODO maybe user can last longer?
					
					for (Session sess : Sessions.this.sessions.values()) {
						sess.reviewPlan();
						
						// TODO add isLongRunnning check into the mix...
						// TODO add plans into mix - check both tasks and channels for completeness (terminate only on complete, vs on timeout, vs never)
						if ((sess.lastAccess < clearGuest) && !sess.keep) {
							Logger.info("Killing inactive session: " + sess.getId());
							Sessions.this.terminate(sess.getId());
							//System.out.println("cleanup");
						}
					}
				}
				
				reporter.setStatus("After reviewing session plans");
			}

			@Override
			public int period() {
				return 60;	// TODO configure?
			}
		};
		
		Hub.instance.getClock().addSlowSystemWorker(sessioncleanup);	
	}
		
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
		
		if ("Manager".equals(feature)) {
			RecordStruct req = msg.getFieldAsRecord("Body");
			
			if ("Start".equals(op)) {
				Session s = new Session(OperationContext.get());
				this.sessions.put(s.getId(), s);
				//return s.call(req.getFieldAsList("Batch"));
				request.complete();
				return;
			}
			else if ("End".equals(op)) {
				if (!req.isFieldEmpty("Id") && !req.isFieldEmpty("AccessCode")) {
					Session s = this.sessions.get(req.getFieldAsString("Id"));
					
					if (s.getKey().equals(req.getFieldAsString("AccessCode"))) {
						//this.sessions.remove(s.getId());
						//s.end();
						this.terminate(s.getId());
						
						request.complete();
						return;
					}
				}
				
				request.error(1, "Unable to end session, missing Id or Code or session already terminated.");		// TODO better codes
				request.complete();
				return;
			}
		}
		else if ("Session".equals(feature)) {
			RecordStruct req = msg.getFieldAsRecord("Body");
			
			if (!req.isFieldEmpty("Id") && !req.isFieldEmpty("AccessCode")) {
				Session s = this.sessions.get(req.getFieldAsString("Id"));
				
				if ((s != null) && s.getKey().equals(req.getFieldAsString("AccessCode"))) 
					//return s.call(req.getFieldAsList("Batch"));
					request.complete();
					return;
			}
			
			request.error(1, "Unable to use session, missing Id or Code or session already terminated.");		// TODO better codes
			request.complete();
			return;
		}
		else if ("Reply".equals(feature)) {
			String tag = msg.getFieldAsString("Tag");
			
			// pull session id out of the tag
			int pos = tag.indexOf('_', 30);
			String sessionid = tag.substring(0, pos);	
			tag = tag.substring(pos + 1);
			
			// strip out session id, restore original tag
			msg.setField("Tag", tag);
			msg.setField("Service", "Replies");
			
			Session s = this.sessions.get(sessionid);
			
			if (s == null) {
				request.error("Missing session");
				request.complete();
				return;
			}
			
			// if we get this far consider it delivered - as a far as we know anyway
			request.complete();
			
			// session activity, don't time out
			s.touch();
			s.deliver(msg);
			
			return;
		}
		/* TODO probably out of date 
		else if ("InBox".equals(feature)) {
			if ("Deliver".equals(op)) {
				String tag = msg.getFieldAsString("Tag");
				
				// pull session id out of the tag
				int pos = tag.indexOf('_', 30);
				
				if (pos == -1)
					return MessageUtil.errorTr(451, tag);
				
				String sessionid = tag.substring(0, pos);	
				
				Session s = this.sessions.get(sessionid);
				
				if (s == null)
					return MessageUtil.errorTr(450, sessionid);
				
				tag = tag.substring(pos + 1);
				
				// pull destination out of the tag
				pos = tag.indexOf('_');
				
				if (pos == -1) {
					msg.setField("Service", tag);
					msg.removeField("Feature");
					msg.removeField("Op");
					msg.removeField("Tag");
				}
				else {
					msg.setField("Service", tag.substring(0, pos));
					tag = tag.substring(pos + 1);
					
					pos = tag.indexOf('_');
					
					if (pos == -1) {
						msg.setField("Feature", tag);
						msg.removeField("Op");
						msg.removeField("Tag");
					}
					else {
						msg.setField("Feature", tag.substring(0, pos));
						tag = tag.substring(pos + 1);
						
						pos = tag.indexOf('_');
						
						if (pos == -1) {
							msg.setField("Op", tag);
							msg.removeField("Tag");
						}
						else {
							msg.setField("Op", tag.substring(0, pos));
							tag = tag.substring(pos + 1);
							
							// restore original tag
							msg.setField("Tag", tag);
						}
					}
				}
				
				s.deliver(msg);
				
				// TODO get some results from delivery attempt
			    Hub.instance.getBus().sendReply(MessageUtil.success(), msg, "Sessions", "InBox", "Deliver");
				return null;
			}
		}
		*/
		
		// Enlist		
		
		request.error(1, "Sessions does not support this feature or operation.");		// TODO better codes
		request.complete();
	}

	public Session lookup(String sessionid) {
		if (StringUtil.isEmpty(sessionid))
			return null;
		
		return this.sessions.get(sessionid);
	}

	public Session lookupAuth(String sessionid, String accesscode) {
		Session s = this.sessions.get(sessionid);
		
		if ((s != null) && s.getKey().equals(accesscode)) 
			return s;
		
		return null;
	}

	public Session create(String origin, String domain) {
		Session s = new Session(origin, Hub.instance.resolveDomainId(domain));
		this.sessions.put(s.getId(), s);
		return s;
	}

	// a root and elevated session that doesn't timeout
	public Session createForService() {
		Session s = new Session(new OperationContextBuilder().withRootUserTemplate());
		s.setKeep(true);
		
		this.sessions.put(s.getId(), s);
		return s;
	}

	// based on current context/user
	public Session createForContext(OperationContext ctx) {
		Session s = new Session(ctx);
		this.sessions.put(s.getId(), s);
		return s;
	}
	
	// runs a single task and then terminates session
	public SessionTaskInfo createForSingleTaskAndDie(Task info) {		
		Session session = this.createForContext(info.getContext());
		
		if (session == null)
			return null;
		
		// this maybe overkill but setting up a single use reference to the session
		// for use with terminating session later - this is to help with GC
		// which is a causing issues so we want it to be as obvious as possible
		// that a session and related tasks and task contexts are free when done
		final AtomicReference<Session> sessref = new AtomicReference<>(); 
		
		sessref.set(session);
		
		OperationObserver listener = new OperationObserver() {			
			@Override
			public void completed(OperationContext or) {
				Session session = sessref.get();
				
				if (session != null) {
					Hub.instance.getSessions().terminate(session.id);
					sessref.set(null);
				}
			}
		};
		
		TaskRun run = session.submitTask(info, listener);
		
		// completion listener will be run even if we get here with errors
		// so no need to worry about terminate
		
		return new SessionTaskInfo(session, run);
	}
	
	// runs a single task and then idles for "30" minutes
	public SessionTaskInfo createForSingleTaskAndWait(Task info) {
		final Session session = this.createForContext(info.getContext());
		
		if (session == null)
			return null;
		
		OperationObserver listener = new OperationObserver() {			
			@Override
			public void completed(OperationContext or) {
				session.touch();		// keep alive relative to end time
			}
		};
		
		TaskRun run = session.submitTask(info, listener);
		
		return new SessionTaskInfo(session, run);
	}

	public List<TaskRun> collectTasks(String... tags) {
		List<TaskRun> matches = new ArrayList<TaskRun>();
		
		for (Session sess : Sessions.this.sessions.values()) 
			sess.collectTasks(matches, tags);
		
		return matches;
	}
	
	public int countTasks(String... tags) {
		int num = 0;
		
		for (Session sess : Sessions.this.sessions.values()) 
			num += sess.countTasks(tags);
		
		return num;
	}
	
	public int countIncompleteTasks(String... tags) {
		int num = 0;
		
		for (Session sess : Sessions.this.sessions.values()) 
			num += sess.countIncompleteTasks(tags);
		
		return num;
	}

	public void terminate(String id) {
		Session s = this.sessions.remove(id);

		if (s != null)
			s.end();
	}

	public void recordCounters() {
		CountManager cm = Hub.instance.getCountManager();
		
		long totalKeepers = 0;
		long totalTasks = 0;
		long totalIncompleteTasks = 0;
		
		Collection<Session> lsessions = this.sessions.values();
		HashMap<String,Long> tagcount = new HashMap<>();
		
		for (Session sess : lsessions) {
			if (sess.getKeep())
				totalKeepers++;
			
			totalTasks += sess.countTasks();
			totalIncompleteTasks += sess.countIncompleteTasks();
			
			sess.countTags(tagcount);
		}
		
		cm.allocateSetNumberCounter("dcSessionCount", lsessions.size());
		cm.allocateSetNumberCounter("dcSessionKeepersCount", totalKeepers);
		cm.allocateSetNumberCounter("dcSessionTaskCount", totalTasks);
		cm.allocateSetNumberCounter("dcSessionTaskIncompleteCount", totalIncompleteTasks);
		
		for (Entry<String, Long> tagentity : tagcount.entrySet()) 
			cm.allocateSetNumberCounter("dcSessionTag_" + tagentity.getKey() + "_Count", tagentity.getValue());
	}
}
