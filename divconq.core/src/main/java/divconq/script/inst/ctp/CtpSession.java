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
package divconq.script.inst.ctp;

import divconq.api.ApiSession;
import divconq.api.LocalSession;
import divconq.api.WebSession;
import divconq.hub.DomainInfo;
import divconq.hub.Hub;
import divconq.lang.op.OperationContext;
import divconq.script.ExecuteState;
import divconq.script.StackEntry;
import divconq.script.inst.With;
import divconq.session.Session;
import divconq.util.StringUtil;

public class CtpSession extends With {
	@Override
	public void prepTarget(StackEntry stack) {
        String name = stack.stringFromSource("Name");            
        String host = stack.stringFromSource("Host");
        String user = stack.stringFromSource("User");
        String pwd = stack.stringFromSource("Password");
        
        if (StringUtil.isEmpty(name)) {
			stack.setState(ExecuteState.Done);
			OperationContext.get().errorTr(527);
			stack.resume();
			return;
        }
        
        if (StringUtil.isEmpty(host)) {
			stack.setState(ExecuteState.Done);
			OperationContext.get().errorTr(528);
			stack.resume();
			return;
        }
        
        DomainInfo di = Hub.instance.resolveDomainInfo(host);
        
        ApiSession sess = null;
        
        // if we handle the domain then use local session
        if (di != null) {
        	Session session = Hub.instance.getSessions().create("hub:", host);
        	sess = new LocalSession();
    		((LocalSession)sess).init(session, stack.getInstruction().getXml());
    		
    		// then use root user
        	if (StringUtil.isEmpty(user)) {
        		((LocalSession)sess).startSessionAsRoot();
        	}
        	else if (!sess.startSession(user, pwd)) {
        		sess.close();
        		
				stack.setState(ExecuteState.Done);
				OperationContext.get().errorTr(530);
				stack.resume();
				return;            		
        	}
        }
        else {
        	if (StringUtil.isEmpty(user)) {
				stack.setState(ExecuteState.Done);
				OperationContext.get().errorTr(529);
				stack.resume();
				return;
        	}
        	
        	// TODO enhance this some
    		sess = new WebSession();
    		((WebSession)sess).init(stack.getInstruction().getXml());
    		
            if (!sess.startSession(user, pwd)) {
            	sess.close();
            	
				stack.setState(ExecuteState.Done);
				OperationContext.get().errorTr(530);
				stack.resume();
				return;
            }
        }
        
        OperationContext.get().getTaskRun().addCloseable(sess);
		
        stack.addVariable(name, sess);
        this.setTarget(stack, sess);
		
		this.nextOpResume(stack);
	}

	/*
	@Override
	public void run(final StackEntry stack) {
		if (stack.getState() == ExecuteState.Ready) {
            String name = stack.stringFromSource("Name");            
            String host = stack.stringFromSource("Host");
            String user = stack.stringFromSource("User");
            String pwd = stack.stringFromSource("Password");
            
            if (StringUtil.isEmpty(name)) {
				stack.setState(ExecuteState.Exit);
				OperationContext.get().errorTr(527);
				stack.resume();
				return;
            }
            
            if (StringUtil.isEmpty(host)) {
				stack.setState(ExecuteState.Exit);
				OperationContext.get().errorTr(528);
				stack.resume();
				return;
            }
            
            DomainInfo di = Hub.instance.resolveDomainInfo(host);
            
            ApiSession sess = null;
            
            // if we handle the domain then use local session
            if (di != null) {
            	Session session = Hub.instance.getSessions().create("hub:", host);
            	sess = new LocalSession();
        		((LocalSession)sess).init(session, stack.getInstruction().getXml());
        		
        		// then use root user
            	if (StringUtil.isEmpty(user)) {
            		((LocalSession)sess).startSessionAsRoot();
            	}
            	else if (!sess.startSession(user, pwd)) {
    				stack.setState(ExecuteState.Exit);
    				OperationContext.get().errorTr(530);
    				stack.resume();
    				return;            		
            	}
            }
            else {
            	if (StringUtil.isEmpty(user)) {
    				stack.setState(ExecuteState.Exit);
    				OperationContext.get().errorTr(529);
    				stack.resume();
    				return;
            	}
            	
            	// TODO enhance this some
        		sess = new WebSession();
        		((WebSession)sess).init(stack.getInstruction().getXml());
        		
                if (!sess.startSession(user, pwd)) {
    				stack.setState(ExecuteState.Exit);
    				OperationContext.get().errorTr(530);
    				stack.resume();
    				return;
                }
            }
    		
            stack.addVariable(name, sess);

			stack.getStore().setField("CurrNode", 0);
			stack.getStore().setField("Target", sess);
			stack.setState(ExecuteState.Resume);
			
			stack.resume();
		}		
		else
			super.run(stack);
	}
	*/
}
