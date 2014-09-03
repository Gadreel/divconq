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
package divconq.count;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

import divconq.lang.OperationResult;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class CountManager implements Observer {
	protected ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

	public void init(OperationResult or, XElement find) {
		// configure events and tasks as a result of conditions of counters
	}

	public void stop(OperationResult or) {
	}
	
	public Counter removeCounter(String name) {
		Counter c = this.counters.remove(name);
		
		if (c != null)
			c.deleteObserver(this);
		
		return c;
	}

	public Counter getCounter(String name) {
		return this.counters.get(name);
	}
	
	public Collection<Counter> getCounters() {
		return this.counters.values();
	}

	public NumberCounter allocateNumberCounter(String name) {
		if (StringUtil.isEmpty(name))
			return null;
		
		Counter c = this.counters.get(name);
		
		if (c == null) {
			c = new NumberCounter(name);
			c.addObserver(this);
			this.counters.put(name, c);
		}
		
		if (! (c instanceof NumberCounter))
			return null;
		
		return (NumberCounter)c;
	}

	public NumberCounter allocateSetNumberCounter(String name, long value) {
		NumberCounter nc = this.allocateNumberCounter(name);
	
		if (nc != null)
			nc.setValue(value);
		
		return nc;
	}
	
	public NumberCounter countObjects(String name, Object obj) {
		NumberCounter nc = this.allocateNumberCounter(name);
		
		if (nc != null) {
			// TODO - find a better way for memory and GC - nc.setCurrentObject(obj);  -- we used to get Task or Session or such in here!! and hold it for indefinite periods
			nc.increment();
		}
		
		return nc;
	}
	
	public NumberCounter allocateSetNumberCounter(String name, double value) {
		NumberCounter nc = this.allocateNumberCounter(name);
		
		if (nc != null)
			nc.setValue(value);
		
		return nc;
	}

	public NumberCounter allocateSetNumberCounter(String name, BigDecimal value) {
		NumberCounter nc = this.allocateNumberCounter(name);
		
		if (nc != null)
			nc.setValue(value);
		
		return nc;
	}

	public StringCounter allocateStringCounter(String name) {
		if (StringUtil.isEmpty(name))
			return null;
		
		Counter c = this.counters.get(name);
		
		if (c == null) {
			c = new StringCounter(name);
			c.addObserver(this);
			this.counters.put(name, c);
		}
		
		if (! (c instanceof StringCounter))
			return null;
		
		return (StringCounter)c;
	}

	public StringCounter allocateSetStringCounter(String name, String value) {
		StringCounter sc = this.allocateStringCounter(name);
		
		if (sc != null)
			sc.setValue(value);
		
		return sc;
	}

	public BooleanCounter allocateBooleanCounter(String name) {
		if (StringUtil.isEmpty(name))
			return null;
		
		Counter c = this.counters.get(name);
		
		if (c == null) {
			c = new BooleanCounter(name);
			c.addObserver(this);
			this.counters.put(name, c);
		}
		
		if (! (c instanceof BooleanCounter))
			return null;
		
		return (BooleanCounter)c;
	}

	public BooleanCounter allocateSetBooleanCounter(String name, Boolean value) {
		BooleanCounter bc = this.allocateBooleanCounter(name);
		
		if (bc != null)
			bc.setValue(value);
		
		return bc;
	}

	@Override
	public void update(Observable o, Object arg) {
		if (!(o instanceof Counter))
			return;
		
		// TODO we won't get here anymore because Counter's setChanged now always sends in a null  
		
		Counter c = (Counter)o;
		
		// TODO future add conditional logic checks so configure to watch for certain conditions
		// this is an example of throwing the evaluation out into another thread - this is so we don't hold the lock on the counter too long
		// improve on this design
		
		if ("dcBusMessageSent".equals(c.getName())) {
			// assuming dcBusMessageSent is a number counter
			// when we hit 15
			if (BigDecimal.valueOf(15).compareTo(((NumberCounter)c).getValue()) == 0) {
				// then a tigger has happened, copy the counter to keep the state intact
				/*
				final Counter fc = c.clone();
				
				// then off load the work of processing the event
				// TODO maybe the event's system will always offload internally so we don't need to here
				Hub.instance.getWorkPool().submit(new IWork() {				
					@Override
					public void run(Task task) {
							System.out.println("event x: " + fc);   // trigger an alert
				
							task.complete();
					}
				});
				*/
				
				//System.out.println("dcBusMessageSent test event :)");
			}
		}
		
		// we leave right after the clone so no major overhead incurred
	}
}
