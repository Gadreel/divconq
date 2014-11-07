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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import divconq.hub.Hub;
import divconq.lang.op.OperationResult;
 
// a possible enhancement is to always prefer a local hub if present
public class ServiceRouter {
	protected String name;	
    
	protected final ConcurrentHashMap<String, HubRouter> hubs = new ConcurrentHashMap<>();
	
    // used for round robin load balancing
	protected HubRouter localhub = null;
	protected final List<HubRouter> directhublist = new CopyOnWriteArrayList<>();
	protected final List<HubRouter> tunnelhublist = new CopyOnWriteArrayList<>();
	protected int next = 0;
	
	protected ReentrantLock lock = new ReentrantLock();
	
	public ServiceRouter(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public OperationResult sendMessage(final Message msg) {
    	String to = msg.getFieldAsString("ToHub");
    	
    	if ("*".equals(to))
    		to = null;

    	// repeat until one of the hubs accepts, it could be that one hub is disconnecting and returns
    	// "false" on the send!!
    	OperationResult or = null;
    	
    	for (int i = 0; i < 3; i++) {
	    	HubRouter hub = (to != null) ? this.hubs.get(to) : this.nextHub();
	    	
			if (hub != null) {
				or = hub.deliverMessage(msg);
				
				// if sent then jump out of loop
				if (!or.hasErrors() || Hub.instance.isStopping())
					break;
			}
			
			// wait for stuff to clear up (if connects/disconnects are in progress)
			try {
				Thread.sleep(1000);
			} 
			catch (InterruptedException x) {
			}
    	}
		
		if (or == null) {
			or = new OperationResult();
			or.error(1, "No hub available");  	// TODO code
		}
		
		return or;
    }

	public boolean isAvailable() {
    	for (int i = 0; i < 3; i++) {
	    	HubRouter hub = this.nextHub();
	    	
			if ((hub != null) && hub.isActive())
				return true;
			
			// wait for stuff to clear up (if connects/disconnects are in progress)
			try {
				Thread.sleep(1000);
			} 
			catch (InterruptedException x) {
			}
    	}
		
		return false;
    }
	
	// there is room for improvement here - this approach assumes that direct/local connections a preferred
	// and that tunnel connections will never be more than 2 layers as in talking between 101 and 201 
	// where 111 and 211 are tunnels for the two targets - anything more complex than this would require
	// a better algorithm
	// |---------|---------|------------|---------|---------|
	// | LAN 101 | DMZ 111 | [internet] | DMZ 211 | DMZ 201 | 
	// |---------|---------|------------|---------|---------|
	public HubRouter nextHub() {
		this.lock.lock();
		
		try {
			// try local connections first
			if (this.localhub != null)
				return this.localhub;
			
			// try direct connections second
			int subcount = this.directhublist.size();

			// prefer a direct hub over a proxy hub
			if (subcount > 0) {
				if (this.next >= subcount)
					this.next = 0;
	
				HubRouter np = this.directhublist.get(this.next);
				
				//System.out.println("Choosing between: " + subcount + " hubs picked: " + np.getName());
				
				this.next++;
				return np;
			}
			
			// try tunnel connections third
			subcount = this.tunnelhublist.size();

			// prefer a direct hub over a proxy hub
			if (subcount > 0) {
				if (this.next >= subcount)
					this.next = 0;
	
				HubRouter np = this.tunnelhublist.get(this.next);
				
				//System.out.println("Choosing between: " + subcount + " hubs picked: " + np.getName());
				
				this.next++;
				return np;
			}
			
			return null;
		}
		finally {
			this.lock.unlock();
		}
	}
    
    public void index(HubRouter hub) {
		this.lock.lock();
		
		try {
			boolean relevant = hub.isActive() && hub.getServices().contains(this.name);	    	
			boolean current = this.directhublist.contains(hub);
			
			if (relevant == current)
				return;
			
			if (relevant) {
				this.hubs.put(hub.getHubId(), hub);
				
				if (hub.isLocal())
					this.localhub = hub;
				else if (hub.isDirect())
					this.directhublist.add(hub);
				else if (hub.isTunneled())
					this.tunnelhublist.add(hub);
			}
			else {
				this.hubs.remove(hub.getHubId());
				
				if (hub.isLocal()) {
					this.localhub = null;
				}
				else {
					this.directhublist.remove(hub);
					this.tunnelhublist.remove(hub);
				}
			}
		}
		finally {
			this.lock.unlock();
		}
    }
    
    public void remove(HubRouter hub) {
		this.lock.lock();
		
		try {
			this.hubs.remove(hub.getHubId());
			
			if (hub.isLocal()) {
				this.localhub = null;
			}
			else {
				this.directhublist.remove(hub);
				this.tunnelhublist.remove(hub);
			}
		}
		finally {
			this.lock.unlock();
		}
    }
    
    public Collection<HubRouter> hubList() {
    	return this.hubs.values();
    }
}
