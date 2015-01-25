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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTimeZone;

import divconq.bus.IService;
import divconq.bus.Message;
import divconq.hub.Hub;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationContextBuilder;
import divconq.lang.op.UserContext;
import divconq.locale.LocaleUtil;
import divconq.mod.ExtensionBase;
import divconq.session.Session;
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
	
	protected boolean xmlMode = false;
	protected Map<String, DomainUsers> xmlData = null;
	
	@Override
	public void init(XElement config) {
		super.init(config);
		
		if (config != null) 
			this.authpass = config.getAttribute("Password");
		
		XElement mdomains = Hub.instance.getConfig().selectFirst("Domains");
		
		if (mdomains != null) {
			this.xmlData = new HashMap<>();
			this.xmlMode = true;
			
			for (XElement mdomain : mdomains.selectAll("Domain")) {
				String id = mdomain.getAttribute("Id");
				
				DomainUsers du = new DomainUsers();
				du.load(id, mdomain);
				
				this.xmlData.put(id, du);
			}
		}
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
				
				String uname = uc.getUsername();
				
				if (this.xmlMode) {
					DomainUsers du = this.xmlData.get(uc.getDomainId());
					
					if (du == null) {
						this.clearUserContext(request.getContext());
						request.error("Domain not found");
					}
					else {
						RecordStruct urec = du.info(uname);
						
						if (urec == null) {
							this.clearUserContext(request.getContext());
							request.error("User not found");
						}
						else {
							request.setResult(urec);
						}
					}
				}
				else {
					request.setResult(new RecordStruct(
							new FieldStruct("Username", "root"),
							new FieldStruct("FirstName", "Root"),
							new FieldStruct("LastName", "User"),
							new FieldStruct("Email", "root@locahost")
					));
				}
				
				request.complete();
				return;
			}			
			
			if ("Verify".equals(op)) {
				String authToken = uc.getAuthToken();
				
				if (StringUtil.isNotEmpty(authToken)) {
					//System.out.println("---------- Token not empty");
					
					if (this.xmlMode) {
						Session sess = Hub.instance.getSessions().lookup(request.getContext().getSessionId());
						
						//System.out.println("---------- Xml Mode");
						
						if ((sess != null) && authToken.equals(sess.getUser().getAuthToken())) {
							//System.out.println("---------- Token verified");
							
							// verified
							request.complete();
							return;
						}
					}
					else if (this.sessions.contains(authToken)) {
						//System.out.println("verify existing");
						
						request.complete();
						return;
					}
				}				
				
				//System.out.println("---------- Token empty or bad");
				
				// else try to authenticate
				RecordStruct creds = uc.getCredentials();  // msg.getFieldAsRecord("Credentials");
				
				if (creds == null) {
					this.clearUserContext(request.getContext());
					request.errorTr(442);
					request.complete();
					return;
				}
				
				//System.out.println("---------- Using Creds");
				
				String uname = creds.getFieldAsString("Username"); 
				String passwd = creds.getFieldAsString("Password");
				
				if (this.xmlMode) {
					//System.out.println("---------- Xml Mode");
					
					DomainUsers du = this.xmlData.get(uc.getDomainId());
					
					if ((du == null) || !du.verify(uname, passwd)) {
						this.clearUserContext(request.getContext());
						request.errorTr(442);
						request.complete();
						return;
					}
				}
				else if (StringUtil.isNotEmpty(this.authpass)) {
					passwd = Hub.instance.getClock().getObfuscator().hashStringToHex(passwd);
					
					if (!"root".equals(uname) || !this.authpass.equals(passwd)) {
						this.clearUserContext(request.getContext());
						request.errorTr(442);
						request.complete();
						return;
					}
				}
				else {
					if (!"root".equals(uname) || !"A1s2d3f4".equals(passwd)) {
						this.clearUserContext(request.getContext());
						request.errorTr(442);
						request.complete();
						return;
					}
				}
				
				byte[] feedbuff = new byte[32];
				this.random.nextBytes(feedbuff);
				String token = HexUtil.bufferToHex(feedbuff);
				
				//System.out.println("---------- Verified and token");
				
				// create the new context
				if (this.xmlMode) 
					uc = this.xmlData.get(uc.getDomainId()).context(uname, token);
				else 
					uc = request.getContext().toBuilder().elevateToRootTask().withAuthToken(token).toUserContext();
				
				// make sure we use the new context in our return
				OperationContext.switchUser(request.getContext(), uc);
				
				if (this.xmlMode) {
					//Session ss = 
					Hub.instance.getSessions().findOrCreateTether(request.getContext());
					//System.out.println("---------- Session added: " + ss);
				}
				else {
					this.sessions.add(token);
				}
				
				//System.out.println("verify new");
				
				request.complete();
				return;
			}			
			
			if ("SignOut".equals(op)) {
				String authToken = uc.getAuthToken();
				
				if (this.xmlMode) {
					Hub.instance.getSessions().terminate(request.getContext().getSessionId());
					//System.out.println("---------- Session removed");
				}
				else if (StringUtil.isNotEmpty(authToken)) 
					this.sessions.remove(authToken);
				
				this.clearUserContext(request.getContext());
				
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
	
	// be sure we keep the domain id
	public void clearUserContext(OperationContext ctx) {
		UserContext uc = ctx.getUserContext();
		
		OperationContext.switchUser(ctx, new OperationContextBuilder()
			.withGuestUserTemplate()
			.withDomainId(uc.getDomainId())
			.toUserContext());
	}
	
	public class DomainUsers {
		protected String did = null;
		protected Map<String, XElement> cachedElement = new HashMap<>();
		protected Map<String, XElement> cachedIndex = new HashMap<>();
		protected Map<String, RecordStruct> cachedUserRecord = new HashMap<>();
		protected Map<String, OperationContextBuilder> cachedUserContext = new HashMap<>();
		
		public boolean verify(String username, String password) {
			XElement usr = this.cachedIndex.get(username);
			
			if (usr == null)
				return false;
			
			String upass = usr.getAttribute("Password");
			
			// any setting in the config file is set with Hub crypto not domain crypto
			String passwd = Hub.instance.getClock().getObfuscator().hashStringToHex(password);

			// if they are the same length then xml file must have hashed value too, use it
			if ((passwd != null) && (passwd.length() == upass.length()))
				return passwd.equals(upass);
			
			// if not same length then xml file is probably plain text, use that
			return password.equals(upass);
		}
		
		public UserContext context(String username, String token) {
			XElement usr = this.cachedIndex.get(username);
			
			if ((usr == null))
				return null;

			String uid = usr.getAttribute("Id");
			
			return this.cachedUserContext.get(uid).withAuthToken(token).toUserContext(); 
		}
		
		public RecordStruct info(String username) {
			XElement usr = this.cachedIndex.get(username);
			
			if (usr == null) 
				return null;

			String uid = usr.getAttribute("Id");
			
			return this.cachedUserRecord.get(uid);
		}
		
		public void load(String did, XElement domain) {
			this.did = did;
			
			for (XElement usr : domain.selectAll("User")) {
				String uid = usr.getAttribute("Id");
				
				this.cachedElement.put(uid, usr);
				this.cachedIndex.put(usr.getAttribute("Username"), usr);
				
				this.cachedUserRecord.put(uid, new RecordStruct(
					new FieldStruct("Username", usr.getAttribute("Username")),
					new FieldStruct("FirstName", usr.getAttribute("First")),
					new FieldStruct("LastName", usr.getAttribute("Last")),
					new FieldStruct("Email", usr.getAttribute("Email"))
				));
				
				List<XElement> tags = usr.selectAll("AuthTag");
				
				String[] atags = new String[tags.size() + 1];
				
				atags[0] = "User";
				
				for (int i = 1; i < atags.length; i++) 
					atags[i] = tags.get(i - 1).getText();
				
				this.cachedUserContext.put(uid, new OperationContextBuilder()
						.withDomainId(did)
						.withUserId(uid)
						.withUsername(usr.getAttribute("Username"))
						.withFullName(usr.getAttribute("First") + " " + usr.getAttribute("Last"))
						.withEmail(usr.getAttribute("Email"))
						.withVerified(true)
						.withAuthTags(atags)
						.withLocale(LocaleUtil.getDefaultLocale())
						.withChronology("/" + DateTimeZone.getDefault().getID())	// ISOChronology w/ default zone
				);		
			}
		}
	}
}
