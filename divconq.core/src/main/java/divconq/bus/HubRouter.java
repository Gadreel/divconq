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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReentrantLock;

import divconq.bus.net.Session;
import divconq.bus.net.SocketInfo;
import divconq.bus.net.StreamMessage;
import divconq.bus.net.StreamSession;
import divconq.hub.Hub;
import divconq.hub.HubEvents;
import divconq.lang.FuncCallback;
import divconq.lang.OperationContext;
import divconq.lang.OperationResult;
import divconq.lang.UserContext;
import divconq.log.Logger;
import divconq.session.DataStreamChannel;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;
import divconq.work.TaskObserver;
import divconq.work.TaskRun;
import divconq.work.Task;
 
public class HubRouter {
	protected String hubid = null;
	protected String squad = null;
	protected CopyOnWriteArraySet<String> services = new CopyOnWriteArraySet<>();
	
	// used with local router
	protected boolean local = false;
	protected ConcurrentHashMap<String, IService> registered = new ConcurrentHashMap<String, IService>();
	protected ReplyService localReplies = null;
	
	protected boolean usekeepalive = true;
	
	// used with direct connections
	protected List<Session> sessions = new CopyOnWriteArrayList<>();				// direct connection
	protected List<HubRouter> tunnels = new CopyOnWriteArrayList<>();			// tunnel (proxy) connection
	protected ReentrantLock sessionlock = new ReentrantLock();
	protected int next = 0;	
	
	protected List<StreamSession> streamsessions = new CopyOnWriteArrayList<>();				// direct connection
	protected int streamnext = 0;	
	
	// used with tunnel connections
	protected HashMap<String, HubRouter> proxied = new HashMap<>();
	
	// use with direct and tunnel
	protected HashMap<String, StreamPath> streampaths = new HashMap<>();
	
	public String getHubId() {
		return this.hubid;
	}
	
	public String getSquadId() {
		return this.squad;
	}
	
	public Collection<String> getServices() {
		return this.services;
	}
	
	
	public Collection<HubRouter> getProxiedHubs() {
		return this.proxied.values();
	}
	
	public boolean isLocal() {
		return this.local;
	}
	
	public boolean isDirect() {
		return (this.sessions.size() > 0) && (this.streamsessions.size() > 0);
	}
	
	public boolean isTunneled() {
		return (this.tunnels.size() > 0);
	}
	
	public boolean isActive() {
		return this.local || (this.sessions.size() > 0) || (this.tunnels.size() > 0);
	}

	public void setUseKeepAlive(boolean v) {
		this.usekeepalive = v;
	}
	
	// use for remote router
	public HubRouter(String id) {
		this.hubid = id;
	}
	
	// use for local router
	public HubRouter() {
		this.local = true;
		this.hubid = OperationContext.getHubId();
		this.squad = Hub.instance.getResources().getSquadId();
		this.localReplies = new ReplyService();
		this.registerService(localReplies);
	}
	
	// for use with local only
	public void registerService(IService callback) {
		this.registered.put(callback.serviceName(), callback);
		this.services.add(callback.serviceName());
		Hub.instance.getBus().indexServices(this);
	}

	// for use with local only
	public void removeService(String name) {
		this.registered.remove(name);
		this.services.remove(name);
		Hub.instance.getBus().indexServices(this);		
	}
    
	// for use with local only
    public Message buildHello(String to) {
		Message cmd = new Message();
		cmd.setField("Kind", "HELLO");
		cmd.setField("Id", OperationContext.getHubId());
		cmd.setField("Squad", this.squad);
		
		// if we are idle then don't list our non-core services with other servers anymore
		// TODO we should still support the Reply service and maybe a few others...just not app level services?
		if (!Hub.instance.isIdled()) {
			cmd.setField("Services", new ListStruct(this.services));
			
			if (Hub.instance.getBus().isProxyMode()) {
				// make sure we list each hub only once in ProxiedServices
				HashMap<String, HubRouter> proxied = new HashMap<>();
				
				for (HubRouter hub : Hub.instance.getBus().getHubs()) {
					if (!hub.isLocal() && hub.isActive() && !hub.getHubId().equals(to)) {
						proxied.put(hub.getHubId(), hub);
						
						for (HubRouter phub : hub.getProxiedHubs())
							if (phub.isActive() && !phub.getHubId().equals(to))
								proxied.put(phub.getHubId(), phub);
					}
				}
				
				ListStruct plist = new ListStruct();
				
				for (HubRouter hub : proxied.values()) {
					plist.addItem(new RecordStruct(
							new FieldStruct("Id", hub.getHubId()),
							new FieldStruct("Squad", hub.getSquadId()),
							new FieldStruct("Services", new ListStruct(hub.getServices()))
					));
				}
				
				cmd.setField("ProxiedServices", plist);
			}
		}
		
		return cmd;
    }
    
	// for use with local only
    public StreamMessage buildStreamHello(String to) {
		StreamMessage cmd = new StreamMessage("HELLO");
		
		cmd.setField("Id", OperationContext.getHubId());
		cmd.setField("Squad", this.squad);
		
		return cmd;
    }

	// for use with local only
	public String registerForReply(Message msg, ServiceResult callback) {
		return this.localReplies.registerForReply(msg, callback);
	}

	// for use with local only
	public ReplyService getReplyService() {
		return this.localReplies;
	}
	
	public OperationResult deliverMessage(Message msg) {
		OperationResult or = new OperationResult();
		
		if (msg == null) {
			or.error(1, "Message missing.");  // TODO code
			return or;
		}
        
        // route to local
        if (this.local) {    	
            msg.removeField("Kind");  // not used locally
    		
	        //System.out.println("dcBus received message: " + msg);
	        
			String srv = msg.getFieldAsString("Service");
		
			if (srv == null) {
				or.error(1, "Message missing service.");  // TODO code
				return or;
			}
			
			IService cb = this.registered.get(srv);
			
			if (cb == null) {
				or.error(1, "Service not on this hub.");  // TODO code
				return or;
			}
			
			// TODO if cb is ReplyService then get the task context from there instead of the message ... update the signin/verify user to work with message body for new user context
	
			// TODO now that TaskContext is immutable we could optimize local Bus calls by not freezing and thawing except remotely - future optimization
			OperationContext tc = OperationContext.allocate(msg);
			
			if ((tc == null) || (tc.getUserContext() == null)) {
				or.errorTr(442);
				return or;
			}
			
			Task tb = new Task()
				.withContext(tc)
				.withParams(msg)
				.withBucket("Bus")		// typically this will fall back into Default
				.withWork(task -> {			
					FuncCallback<UserContext> fcb = new FuncCallback<UserContext>() {
						@Override
						public void callback() {
							UserContext uc = this.getResult();
							
							if (uc != null) {
								// TODO only if the user context changed, then update the operation context
								//if (!uc.equals(this.getContext().getUserContext()))
								
								// update the operation context
								OperationContext.use(uc, tc.toBuilder());
							}
							
							//Message rmsg = null;
							
							//System.out.println("d2: " + msg);
							
							// if there were verify errors copy them and do not continue
							task.copyMessages(this);
							
							if (task.hasErrors()) {
							    task.complete();
							    return;
							}

							// validate the structure of the message
							task.copyMessages(Hub.instance.getSchema().validateRequest(msg));	
							
							// if invalid structure then do not continue
							if (task.hasErrors()) {
							    task.complete();
							    return;
							}
							
							// when making a valid call to any service, you are elevated to system access for the duration of the request
							// RPC users get a new context with each call though, and reply will not violate any security
							// so it is up to each service to call only other services as appropriate - that is each service 
							// is ultimately responsible for security
							if (!OperationContext.get().isElevated())
								OperationContext.use(OperationContext.get().toBuilder().withElevated(true));
							
							cb.handle(task);
							
							//System.out.println("d3: " + msg);
							//System.out.println("d4: " + rmsg);
							
							// reply may be async so this could be null - when async the service handler calls sendReply directly
							// which is also a reason not to add a lot of logic here, as it won't get called in async cases
							//if (rmsg != null)			
							//    Hub.instance.getBus().sendReply(rmsg, msg);
							
							//task.complete();
						}
					};
					
					// don't verify a Verify request or it'll be stuck forever making new verify checks
					if (msg.isVerifyRequest()) {
						fcb.setResult(tc.getUserContext());
						fcb.completed();
					}
					else 
						tc.verify(fcb);
				});
					
			or.copyMessages(Hub.instance.getWorkPool().submit(tb, new TaskObserver() {
				@Override
				public void completed(TaskRun or) {
					// send a response, be it just error messages or a full body
				    Hub.instance.getBus().sendReply(or.toLogMessage(), msg);
				}
			}));		
			
			//System.out.println("Call to " + srv + " -- " + tb.getId());
		
			return or;
        }

        // route to remote
		Session sess = this.nextDirectRoute();
		
		if (sess == null) {	
			HubRouter tunnel = this.nextTunnelRoute();
			
			if (tunnel != null)
				return tunnel.deliverMessage(msg);
			
			if (!"HELLO".equals(msg.getFieldAsString("Kind")))
				or.error(1, "Unable to route message to proxied hub: " + msg); // TODO log, better code
			
			return or;
		}
		
		if (!sess.write(msg))			
			or.error(1, "Unable to route message to remote hub: " + msg); // TODO log, better code
		
		return or;
	}
	
	public void receiveMessage(Session session, Message msg) {
		// update our service list if SERVICES message
    	if ("HELLO".equals(msg.getFieldAsString("Kind"))) {
    		this.squad = msg.getFieldAsString("Squad");
    		
    		// copy the existing list
    		Collection<HubRouter> oldplist = new ArrayList<>(this.proxied.values()); 
    		
    		this.proxied.clear();
    		this.services.clear();
    		
    		ListStruct plist = msg.getFieldAsList("ProxiedServices");
    		
    		if (plist != null) {
    			for (Struct pitem : plist.getItems()) {
    				RecordStruct prec = (RecordStruct) pitem;
    				HubRouter phub = Hub.instance.getBus().allocateOrGetHub(prec.getFieldAsString("Id"));
    				
    				/*
    				if (phub == null) {
    					System.out.println("Could not allocate hub: " + prec.getFieldAsString("Id") + " >> me -- " + TaskContext.getHubId());
    					continue;
    				}
    				*/

   					phub.addTunnel(prec, this);
    				
    				// we don't need to clear our tunnel from this proxied hub
    				oldplist.remove(phub);
    				
    				this.proxied.put(phub.getHubId(), phub);
    			}
    		}
    		
    		this.clearMyTunnels(oldplist);
    		
    		ListStruct slist = msg.getFieldAsList("Services");
    		
    		if (slist != null) {
    			this.services.addAll(slist.toStringList());
    			int sessionsize = this.sessions.size();
    			Hub.instance.getCountManager().allocateSetNumberCounter("dcBus_" + this.getHubId() + "_Sessions", sessionsize);
    		}
    		
			Hub.instance.getBus().indexServices(this);
			
			return;
    	}            
        
    	// if message is not HELLO then it needs to be routed to the correct hub
    	String srv = msg.getFieldAsString("Service");
    	
		ServiceRouter router = Hub.instance.getBus().getServiceRouter(srv); 
		
		if (router == null) {
			// TODO log warning
			return;
		}

		// this will end up in the proper work pool after routing
		OperationResult routeres = router.sendMessage(msg);
		
		if (routeres.hasErrors()) {	
			// TODO log error
			return;
		}
	}
	
	// TODO from this point on release StreamMessages
	public void receiveMessage(StreamSession session, StreamMessage msg) {
		// update our service list if SERVICES message
    	if ("HELLO".equals(msg.getFieldAsString("Op"))) {
        	msg.release();
    		this.squad = msg.getFieldAsString("Squad");
			return;
    	}            

		String hub = msg.getFieldAsString("ToHub");
		
		HubRouter router = Hub.instance.getBus().allocateOrGetHub(hub);
		
		OperationResult routeres = router.deliverMessage(msg);
		
		if (routeres.hasErrors()) { 	
			StreamMessage rmsg = MessageUtil.streamMessages(routeres);
			Hub.instance.getBus().sendReply(rmsg, msg);
		}
	}

	public void addSession(Session session) {
		this.sessionlock.lock();
		
		int sessionsize = this.sessions.size();
		
		try {
			this.sessions.add(session);
			
			sessionsize = this.sessions.size();
			
			Hub.instance.getCountManager().allocateSetNumberCounter("dcBus_" + this.getHubId() + "_Sessions", sessionsize);
			
			if ((sessionsize == 1) && this.isDirect()) 	    	    	
    	    	// let hub know we are connected, in another thread
    	    	Hub.instance.getWorkPool().submit(trun -> Hub.instance.fireEvent(HubEvents.BusConnected, null));
		}
		finally {
			this.sessionlock.unlock();
		}

		Logger.info("Connect on dcBus, " + this.getHubId() + " sessions available: " + sessionsize);
	}

	public void remove(SocketInfo info) {
		for (Session sess : this.sessions) { 
			if (sess.getSocketInfo() == info) {
				this.removeSession(sess);
				break;
			}
		}
		
		for (StreamSession sess : this.streamsessions) { 
			if (sess.getSocketInfo() == info) {
				this.removeSession(sess);
				break;
			}
		}
	}

	public void removeSession(Session session) {
		this.sessionlock.lock();
		
		boolean direct = this.isDirect();
		
		int sessionsize = this.sessions.size();
		int priorsize = sessionsize;
		
		try {
			this.sessions.remove(session);
			
			sessionsize = this.sessions.size();
			
			Hub.instance.getCountManager().allocateSetNumberCounter("dcBus_" + this.getHubId() + "_Sessions", sessionsize);
			
			session.close();
		}
		finally {
			this.sessionlock.unlock();
		}

		if (sessionsize != priorsize)
			Logger.info("Disconnect on dcBus, " + this.getHubId() + " sessions available: " + sessionsize);
		
		// if no longer connected then get the agent out of the manager's hair
		if (sessionsize == 0) {
	    	// let hub know we are disconnected, in another thread
			if (direct)
				Hub.instance.getWorkPool().submit(trun -> Hub.instance.fireEvent(HubEvents.BusDisconnected, null));
			
			this.clearMyTunnels(this.proxied.values());
			Hub.instance.getBus().indexServices(this);
		}
	}
	
	public void addSession(StreamSession session) {
		this.sessionlock.lock();
		
		int sessionsize = this.streamsessions.size();
		
		try {
			this.streamsessions.add(session);
			
			sessionsize = this.streamsessions.size();
			
			// TODO make debug
			Logger.info("Connect on dcBus, " + this.getHubId() + " stream added: " + sessionsize);
			
			Hub.instance.getCountManager().allocateSetNumberCounter("dcBus_" + this.getHubId() + "_SteramSessions", sessionsize);
			
			if ((sessionsize == 1) && this.isDirect()) 	    	    	
    	    	// let hub know we are connected, in another thread
    	    	Hub.instance.getWorkPool().submit(trun -> Hub.instance.fireEvent(HubEvents.BusConnected, null));
		}
		finally {
			this.sessionlock.unlock();
		}

		Logger.info("Connect on dcBus, " + this.getHubId() + " stream sessions available: " + sessionsize);
	}

	public void removeSession(StreamSession session) {
		this.sessionlock.lock();
		
		boolean direct = this.isDirect();
		int sessionsize = this.streamsessions.size();
		int priorsize = sessionsize;
		
		try {
			this.streamsessions.remove(session);
			
			sessionsize = this.streamsessions.size();
			
			Hub.instance.getCountManager().allocateSetNumberCounter("dcBus_" + this.getHubId() + "_StreamSessions", sessionsize);
			
			session.close();
		}
		finally {
			this.sessionlock.unlock();
		}

		if (sessionsize != priorsize) 
			Logger.info("Disconnect on dcBus, " + this.getHubId() + " stream sessions available: " + sessionsize);
		
		// if no longer connected then get the agent out of the manager's hair
		if (sessionsize == 0) {
			if (direct)
		    	// let hub know we are disconnected, in another thread
		    	Hub.instance.getWorkPool().submit(trun -> Hub.instance.fireEvent(HubEvents.BusDisconnected, null));
		}
	}
	
	public void addTunnel(RecordStruct prec, HubRouter tunnel) {
		this.squad = prec.getFieldAsString("Squad");
		
		ListStruct slist = prec.getFieldAsList("Services");

		this.services.clear();
		this.services.addAll(slist.toStringList());
		
		this.sessionlock.lock();
		
		int proxysize = this.tunnels.size();
		int priorsize = proxysize;
		
		try {
			if (!this.tunnels.contains(tunnel)) {
				this.tunnels.add(tunnel);			
				proxysize = this.tunnels.size();
			}
			
			Hub.instance.getCountManager().allocateSetNumberCounter("dcBus_" + this.getHubId() + "_Proxies", proxysize);
		}
		finally {
			this.sessionlock.unlock();
		}

		if (proxysize != priorsize)
			Logger.info("Connect on dcBus, " + this.getHubId() + " proxies available: " + proxysize);
		
		// if no longer connected then get the agent out of the manager's hair
		if (proxysize == 1) 
			Hub.instance.getBus().indexServices(this);
	}
	
	public void removeTunnel(HubRouter tunnel) {
		this.sessionlock.lock();
		
		int proxysize = this.tunnels.size();
		
		try {
			this.tunnels.remove(tunnel);
			
			proxysize = this.tunnels.size();
			
			Hub.instance.getCountManager().allocateSetNumberCounter("dcBus_" + this.getHubId() + "_Proxies", proxysize);
		}
		finally {
			this.sessionlock.unlock();
		}

		Logger.info("Disconnect on dcBus, " + this.getHubId() + " proxies available: " + proxysize);
		
		// if no longer connected then get the agent out of the manager's hair
		if (proxysize == 0) 
			Hub.instance.getBus().indexServices(this);
	}

	public void close() {
		// calling close will trigger a call to remove (above) via the Manager
		for (Session s : this.sessions)
			s.close();
	}
	
	public void clearMyTunnels(Collection<HubRouter> list) {
		for (HubRouter hub : list) 
			hub.removeTunnel(this);
	}

	// round robin approach to finding routes
	public Session nextDirectRoute() {
		this.sessionlock.lock();
		
		try {
			int subcount = this.sessions.size();
			
			if (subcount == 0)
				return null;
			
			if (this.next >= subcount)
				this.next = 0;

			Session np = this.sessions.get(this.next);
			this.next++;
			return np;
		}
		finally {
			this.sessionlock.unlock();
		}
	}

	// round robin approach to finding routes
	public HubRouter nextTunnelRoute() {
		this.sessionlock.lock();
		
		try {
			int subcount = this.tunnels.size();
			
			if (subcount == 0)
				return null;
			
			if (this.next >= subcount)
				this.next = 0;

			HubRouter np = this.tunnels.get(this.next);
			this.next++;
			return np;
		}
		finally {
			this.sessionlock.unlock();
		}
	}
	
	public Collection<Session> getSessions() {
		return this.sessions;
	}

	public int getCountSessions(SocketInfo info) {
		int cnt = 0;
		
		for (Session sess : this.sessions)
			if (sess.getSocketInfo() == info)
				cnt++;
		
		return cnt;
	}

	public int getCountStreamSessions(SocketInfo info) {
		int cnt = 0;
		
		for (StreamSession sess : this.streamsessions)
			if (sess.getSocketInfo() == info)
				cnt++;
		
		return cnt;
	}

	// from this point on release StreamMessage
	public OperationResult deliverMessage(final StreamMessage msg) {
		OperationResult or = new OperationResult();
		
		if (msg == null) {
			or.error(1, "Message missing.");  // TODO code
			return or;
		}
        
		String sessid = msg.getFieldAsString("ToSession");
		String chanid = msg.getFieldAsString("ToChannel");
		
        // route to local
        if (this.local) {    	
	        //System.out.println("dcBus received stream message: " + msg);
    		
    		final divconq.session.Session sess = Hub.instance.getSessions().lookup(sessid);
    		
    		if (sess == null) {
    			or.error(1, "Unable to find session: " + sessid);			// TODO make sure if this came off network that a response is sent
            	msg.release();
	            return or;
    		}
    		
    		final DataStreamChannel chan = sess.getChannel(chanid);
    		
    		if (chan == null) {
	            or.error(1, "Unable to find channel: " + chanid);
            	msg.release();
	            return or;
    		}
    		
    		// stream in foreground to keep data in order
    		// (over wire this is accomplished by always using the same network path)
    		chan.deliverMessage(msg);		// TODO return errors if any
		
			return or;
        }
        
        String pathid = (StringUtil.isEmpty(sessid) || StringUtil.isEmpty(chanid)) ? null : sessid + "_" + chanid;
        
        if (StringUtil.isNotEmpty(pathid)) {
	        StreamPath path = null;
	        
	        // get the path while locked because other wise the cleanup might steal the path after we get it but before we
	        // touch it - thus removing the path that is just about to get used - thus allowing for possible out of order
	        // data packets
			this.sessionlock.lock();
			
			try {
		        path = this.streampaths.get(pathid);
		        
		        if (path != null)
		        	path.touched = System.currentTimeMillis();
			}
			finally {
				this.sessionlock.unlock();
			}
	        
	        if (path != null) {
	        	if (path.direct != null) {
	        		if (!path.direct.write(msg))
	    				or.error(1, "Unable to route message to remote hub: " + msg); // TODO log, better code
	    			
	        		return or;
	        	}
	        	
	        	if (path.tunneled != null) {
	        		// TODO check that it is still connected/active
	        		path.tunneled.deliverMessage(msg);
	        		return or;
	        	}
	        }
        }

        // route to remote
		StreamSession sess = this.nextDirectStreamRoute();
		
		if (sess != null) {
			if (sess.write(msg)) {
		        if (StringUtil.isNotEmpty(pathid)) {
					StreamPath sp = new StreamPath();
					sp.direct = sess;

					// lock so we are not interfering with cleanup (see below)
					// if we get to here though our path is not a candidate for cleanup yet so
					// our operation is not a concern for cleanup, just don't change hash while cleanup is using it
					this.sessionlock.lock();
					
					try {
						this.streampaths.put(pathid, sp);
					}
					finally {
						this.sessionlock.unlock();
					}
		        }
			}
			else 
				or.error(1, "Unable to route message to remote hub: " + msg); // TODO log, better code
				
			return or;
		}
		
		HubRouter tunnel = this.nextTunnelRoute();
		
		if (tunnel != null) {
	        if (StringUtil.isNotEmpty(pathid)) {
				StreamPath sp = new StreamPath();
				sp.tunneled = tunnel;
				
				// lock so we are not interfering with cleanup (see below)
				// if we get to here though our path is not a candidate for cleanup yet so
				// our operation is not a concern for cleanup, just don't change hash while cleanup is using it
				this.sessionlock.lock();
				
				try {
					this.streampaths.put(pathid, sp);
				}
				finally {
					this.sessionlock.unlock();
				}
	        }
	        
			return tunnel.deliverMessage(msg);
		}
		
		if (!"HELLO".equals(msg.getFieldAsString("Kind")))
			or.error(1, "Unable to route message to proxied hub: " + msg); // TODO log, better code
		
		return or;
	}

	// round robin approach to finding routes
	public StreamSession nextDirectStreamRoute() {
		this.sessionlock.lock();
		
		try {
			int subcount = this.streamsessions.size();
			
			if (subcount == 0) {
				System.out.println("Missing stream routes to " + this.hubid);
				return null;
			}
			
			if (this.streamnext >= subcount)
				this.streamnext = 0;

			StreamSession np = this.streamsessions.get(this.streamnext);
			this.next++;
			return np;
		}
		finally {
			this.sessionlock.unlock();
		}
	}
	
	public class StreamPath {
		protected StreamSession direct = null;
		protected HubRouter tunneled = null;
		protected long touched = System.currentTimeMillis();
	}

	public void keepAlive() {
		if (!this.usekeepalive)
			return;
		
		// loop direct connections
		for (Session sess : this.sessions)
			sess.keepAlive();
		
		for (StreamSession sess : this.streamsessions) 
			sess.keepAlive();
	}
	
	public void cleanup() {
		// clearing the stream paths has no real damaging impact - it's purpose is to corral data sent by one session/channel
		// into using the same StreamSession for a burst of activity - such that data will not get out of sequence by
		// going across different StreamSessions and packet 2 arriving before packet 1
		// however, if data has been silent for 1 minute then packets are not out of order and as such not an issue if 
		// clear this and assign an alternative StreamSession later - should the data sender still be active but very slow 
		
		long timeout = System.currentTimeMillis() - 60000;		// 1 min of no activity
		List<StreamPath> cleanlist = new ArrayList<>();
		
		this.sessionlock.lock();
		
		try {
			for (StreamPath path : this.streampaths.values()) {
				// has path been quiet for too long?  
				if (path.touched < timeout)
					cleanlist.add(path);
			}
			
			for (StreamPath path : cleanlist)
				this.streampaths.remove(path);
		}
		finally {
			this.sessionlock.unlock();
		}
	}
}
