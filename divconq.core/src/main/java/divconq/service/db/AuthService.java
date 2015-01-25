package divconq.service.db;

import divconq.bus.IService;
import divconq.bus.Message;
import divconq.db.DataRequest;
import divconq.db.IDatabaseManager;
import divconq.db.ObjectResult;
import divconq.db.common.RequestFactory;
import divconq.db.query.LoadRecordRequest;
import divconq.db.query.SelectFields;
import divconq.hub.Hub;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationContextBuilder;
import divconq.lang.op.UserContext;
import divconq.mod.ExtensionBase;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;
import divconq.work.TaskRun;

public class AuthService extends ExtensionBase implements IService {
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");

		final OperationContext tc = OperationContext.get();
		final UserContext uc = tc.getUserContext();
		
		IDatabaseManager db = Hub.instance.getDatabase();
		
		if (db == null) {
			request.errorTr(303);
			request.complete();
			return;
		}
		
		//System.out.println("Auth: " + feature + " - " + op);
		
		if ("Authentication".equals(feature)) {
			if ("SignIn".equals(op)) {
				LoadRecordRequest req = new LoadRecordRequest()
					.withTable("dcUser")
					.withId(uc.getUserId())
					.withNow()
					.withSelect(new SelectFields()
						.withField("dcUsername", "Username")
						.withField("dcFirstName", "FirstName")
						.withField("dcLastName", "LastName")
						.withField("dcEmail", "Email")
					);				
				
				db.submit(req, new ObjectResult() {
					@Override
					public void process(CompositeStruct result) {
						if (request.hasErrors() || (result == null)) {
							AuthService.this.clearUserContext(request.getContext());
							request.errorTr(442);
						}
						
						request.returnValue(result);
					}
				});						
				
				return;
			}			
			
			// TODO now that we trust the token in Session this won't get called often - think about how to keep
			// auth token fresh in database
			if ("Verify".equals(op)) {
				String authToken = uc.getAuthToken();
				
				if (StringUtil.isNotEmpty(authToken)) {
					DataRequest tp1 = RequestFactory.verifySessionRequest(uc.getUserId(), uc.getAuthToken());
					
					db.submit(tp1, new ObjectResult() {
						public void process(CompositeStruct result) {
							RecordStruct urec = (RecordStruct) result;
							OperationContext ctx = request.getContext();
							
							if (request.hasErrors() || (urec == null)) {
								AuthService.this.clearUserContext(ctx);
								request.errorTr(442);
							}
							else {
								//System.out.println("verify existing");
	
								OperationContext.switchUser(ctx, ctx.getUserContext().toBuilder() 
										.withVerified(true)
										.withAuthTags(urec.getFieldAsList("AuthorizationTags"))
										.toUserContext()
								);
							}
							
							request.complete();
						}
					});
					
					return;
				}				
				
				// else try to authenticate
				RecordStruct creds = uc.getCredentials();  // msg.getFieldAsRecord("Credentials");
				
				if (creds == null) {
					request.errorTr(442);
					request.complete();
					return;
				}
				
				//System.out.println("auth 1: " + request.getContext().isElevated());
				
				DataRequest tp1 = RequestFactory.signInRequest(creds.getFieldAsString("Username"), 
						creds.getFieldAsString("Password"), creds.getFieldAsString("ConfirmationCode"));
				
				// TODO for all services, be sure we return all messages from the submit with the message
				db.submit(tp1, new ObjectResult() {
					@Override
					public void process(CompositeStruct result) {
						RecordStruct sirec = (RecordStruct) result;
						OperationContext ctx = request.getContext();
						
						//System.out.println("auth 2: " + request.getContext().isElevated());
						
						if (request.hasErrors() || (sirec == null)) {
							AuthService.this.clearUserContext(ctx);
							request.errorTr(442);
						}
						else {
							//System.out.println("verify new");

							RecordStruct urec = sirec.getFieldAsRecord("UserInfo");
							
							ListStruct atags = urec.getFieldAsList("AuthorizationTags");
							
							atags.addItem("User");
							
							// TODO add in group tags							
							//atags.addItem(x);							
							
							OperationContext.switchUser(ctx, ctx.getUserContext().toBuilder() 
									.withVerified(true)
									.withAuthToken(urec.getFieldAsString("AuthToken"))
									.withUserId(urec.getFieldAsString("UserId"))
									.withUsername(tc.getUserContext().getCredentials().getFieldAsString("Username"))
									.withFullName(urec.getFieldAsString("FirstName") + " " + urec.getFieldAsString("LastName"))		// TODO make locale smart
									.withEmail(urec.getFieldAsString("Email"))
									.withAuthTags(atags)
									.toUserContext()
							);
							
							Hub.instance.getSessions().findOrCreateTether(request.getContext());
						}
						
						request.complete();
					}
				});
				
				return;
			}			
			
			if ("SignOut".equals(op)) {
				db.submit(RequestFactory.signOutRequest(uc.getAuthToken()), new ObjectResult() {
					@Override
					public void process(CompositeStruct result) {
						Hub.instance.getSessions().terminate(request.getContext().getSessionId());
						
						AuthService.this.clearUserContext(request.getContext());
						request.complete();
					}
				});
				
				return;
			}		
		}
		else if ("Recovery".equals(feature)) {
			if ("InitiateSelf".equals(op) || "InitiateAdmin".equals(op)) {
				String user = msg.bodyRecord().getFieldAsString("User");  
				
				DataRequest req = RequestFactory.initiateRecoveryRequest(user);
				
				db.submit(req, new ObjectResult() {
					@Override
					public void process(CompositeStruct result) {
						if (this.hasErrors()) { 
							request.errorTr(442);
						}
						else {
							String code = ((RecordStruct)result).getFieldAsString("Code");
							String email = ((RecordStruct)result).getFieldAsString("Email");
							String email2 = ((RecordStruct)result).getFieldAsString("BackupEmail");
							
							// TODO send email
							
							System.out.println("Sending recovery code: " + code + " to " + email + " and " + email2);
						}
						
						if ("InitiateAdmin".equals(op))
							// return the code/emails to the admin
							request.returnValue(request);
						else
							// don't return to guest
							request.complete();
					}
				});
				
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
}
