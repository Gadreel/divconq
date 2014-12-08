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
package divconq.tasks.sql;

import divconq.bus.IService;
import divconq.mod.ExtensionBase;
import divconq.work.TaskRun;

public class AuthService extends ExtensionBase implements IService {

	@Override
	public void handle(TaskRun request) {
		// TODO Auto-generated method stub
		
	}
	/* model after mumps code
	@Override
	public void handle(final TaskRun request) {
		final Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");

		final OperationContext tc = OperationContext.get();
		final UserContext uc = tc.getUserContext();
		
		IDatabaseInterface db = Hub.instance.getDatabase();
		
		if (db == null) {
			request.errorTr(303);
			request.complete();
			return;
		}
		
		System.out.println("Auth: " + feature + " - " + op);
		
		if ("Authentication".equals(feature)) {
			if ("SignIn".equals(op)) {
				SelectFields selt = new SelectFields(
						new SelectField("dcUsername"),
						new SelectField("dcFirstName"),
						new SelectField("dcLastName"),
						new SelectField("dcEmail")
				);
				
				WhereExpression whr = new WhereOr(
						new WhereEqual(new WhereField("Id"), uc.getUserId())
				);
				
				SelectDirectRequest req = new SelectDirectRequest("dcUser", selt, whr);				
				
				db.submit(req, new ObjectCallback() {
					@Override
					public void process(ObjectResult res) {
						ListStruct reslist = res.getResultAsList();
						
						if (res.hasErrors() || (reslist.getSize() == 0)) {
							OperationContext.useNewGuest();		
							request.errorTr(442);
						}
						else {
							RecordStruct urec = reslist.getItemAsRecord(0);
							
							request.setResult(new RecordStruct(
									new FieldStruct("Username", urec.getFieldAsString("dcUsername")),
									new FieldStruct("FirstName", urec.getFieldAsString("dcFirstName")),
									new FieldStruct("LastName", urec.getFieldAsString("dcLastName")),
									new FieldStruct("Email", urec.getFieldAsString("dcEmail"))
							));
						}
						
						request.complete();
					}
				});						
				
				return;
			}			
			
			if ("Verify".equals(op)) {
				String authToken = uc.getAuthToken();
				
				if (StringUtil.isNotEmpty(authToken)) {
					VerifySessionRequest tp1 = new VerifySessionRequest(uc.getUserId(), uc.getAuthToken());
					
					db.submit(tp1, new ObjectCallback() {
						@Override
						public void process(ObjectResult res) {
							if (res.hasErrors()) {
								OperationContext.useNewGuest();		
								request.errorTr(442);
							}
							else {
								RecordStruct urec = res.getResultAsRec();
								
								System.out.println("verify existing");
								
								AuthService.this.verifyContext(tc, urec);
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
				
				SignInRequest tp1 = new SignInRequest(creds.getFieldAsString("Username"), 
						creds.getFieldAsString("Password"), creds.getFieldAsString("ConfirmationCode"));
				
				// TODO for all services, be sure we return all messages from the submit with the message
				db.submit(tp1, new ObjectCallback() {
					@Override
					public void process(ObjectResult res) {
						if (res.hasErrors()) {
							OperationContext.useNewGuest();		
							request.errorTr(442);
						}
						else {
							RecordStruct urec = res.getResultAsRec();
							
							System.out.println("verify new");
							
							AuthService.this.newContext(tc, urec);
						}
						
						request.complete();
					}
				});
				
				return;
			}			
			
			if ("SignOut".equals(op)) {
				db.submit(new SignOutRequest(uc.getAuthToken()), new ObjectCallback() {
					@Override
					public void process(ObjectResult res) {
						// the result doesn't matter, we still say success
						
						OperationContext.useNewGuest();		
						request.complete();
					}
				});
				
				return;
			}		
		}
		else if ("Recovery".equals(feature)) {
			if ("Initiate".equals(op)) {
				RecordStruct creds = uc.getCredentials();  
				
				if (creds == null) {
					request.errorTr(442);
					request.complete();
					return;
				}
				
				final InitiateRecoveryRequest req = new InitiateRecoveryRequest(creds.getFieldAsString("Username"));
				
				db.submit(req, new ObjectCallback() {
					@Override
					public void process(ObjectResult res) {
						if (res.hasErrors()) 
							request.errorTr(442);
						else 
							request.setResult(new RecordStruct(new FieldStruct("Code", req.getCode())));
						
						request.complete();
					}
				});
				
				return;
			}			
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}

	protected void verifyContext(OperationContext tc, RecordStruct urec) {
		OperationContext.use(tc.toBuilder()
				.withVerified(true)
				.withAuthTags(urec.getFieldAsList("AuthorizationTags"))
		);
	}

	protected void newContext(OperationContext tc, RecordStruct urec) {
		OperationContext.use(tc.toBuilder()
				.withVerified(true)
				.withAuthToken(urec.getFieldAsString("AuthToken"))
				.withUserId(urec.getFieldAsString("UserId"))
				.withUsername(tc.getUserContext().getCredentials().getFieldAsString("Username"))
				.withFullName(urec.getFieldAsString("FirstName") + " " + urec.getFieldAsString("LastName"))		// TODO make locale smart
				.withEmail(urec.getFieldAsString("Email"))
				.withAuthTags(urec.getFieldAsList("AuthorizationTags"))
		);
	}
	*/
}
