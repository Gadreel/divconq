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
package divconq.service.simple;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

import divconq.bus.IService;
import divconq.bus.Message;
import divconq.hub.Hub;
import divconq.lang.OperationContext;
import divconq.lang.OperationResult;
import divconq.lang.UserContext;
import divconq.mod.ExtensionBase;
import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;
import divconq.util.HexUtil;
import divconq.util.StringUtil;
import divconq.work.TaskRun;
import divconq.xml.XElement;

public class AuthService extends ExtensionBase implements IService {
	protected Set<String> sessions = new HashSet<String>(); 
	protected SecureRandom random = new SecureRandom();
	protected String authpass = null;
	
	@Override
	public void init(OperationResult log, XElement config) {
		super.init(log, config);
		
		if (config != null) 
			this.authpass = config.getAttribute("Password");			
	}
	
	@Override
	public String serviceName() {
		// TODO if through Loader then get name there (super)
		return "dcAuth";
	}
	
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");

		UserContext uc = request.getContext().getUserContext();
		
		//System.out.println("Auth: " + feature + " - " + op);
		
		if ("Authentication".equals(feature)) {
			if ("SignIn".equals(op)) {
							
				request.setResult(new RecordStruct(
						new FieldStruct("UserName", "root"),
						new FieldStruct("FirstName", "Root"),
						new FieldStruct("LastName", "User"),
						new FieldStruct("Email", "root@locahost")
				));
				request.complete();
				return;
			}			
			
			if ("Verify".equals(op)) {
				String authToken = uc.getAuthToken();
				
				if (StringUtil.isNotEmpty(authToken)) {
					if (this.sessions.contains(authToken)) {
						//System.out.println("verify existing");
						
						request.complete();
						return;
					}
				}				
				
				// else try to authenticate
				RecordStruct creds = uc.getCredentials();  // msg.getFieldAsRecord("Credentials");
				
				if (creds == null) {
					request.setContext(OperationContext.allocateGuest());
					request.errorTr(442);
					request.complete();
					return;
				}
				
				String uname = creds.getFieldAsString("UserName"); 
				String passwd = creds.getFieldAsString("Password");
				
				if (StringUtil.isNotEmpty(this.authpass)) {
					passwd = Hub.instance.getClock().getObfuscator().hashStringToHex(passwd);
					
					if (!"root".equals(uname) || !this.authpass.equals(passwd)) {
						request.setContext(OperationContext.allocateGuest());
						request.errorTr(442);
						request.complete();
						return;
					}
				}
				else {
					if (!"root".equals(uname) || !"A1s2d3f4".equals(passwd)) {
						request.setContext(OperationContext.allocateGuest());
						request.errorTr(442);
						request.complete();
						return;
					}
				}
				
				byte[] feedbuff = new byte[32];
				this.random.nextBytes(feedbuff);
				String token = HexUtil.bufferToHex(feedbuff);

				this.sessions.add(token);
				
				this.newContext(request, token);
				
				request.complete();
				return;
			}			
			
			if ("SignOut".equals(op)) {
				String authToken = uc.getAuthToken();
				
				if (StringUtil.isNotEmpty(authToken)) 
					this.sessions.remove(authToken);
				
				request.setContext(OperationContext.allocateGuest());
				
				request.complete();
				return;
			}		
		}
		else if ("Recovery".equals(feature)) {
			if ("Initiate".equals(op)) {
				request.complete();
				return;
			}			
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}

	protected void newContext(TaskRun request, String token) {
		// create the new context
		OperationContext v = request.getContext().toBuilder().elevateToRootTask().withAuthToken(token).toOperationContext();
		
		// make sure we use the new context in our return
		request.setContext(v);
		
		System.out.println("verify new");
	}
}
