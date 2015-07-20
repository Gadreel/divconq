package divconq.service.db;

import java.net.URL;
import java.net.URLEncoder;
import java.util.function.Consumer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;

import divconq.bus.IService;
import divconq.bus.Message;
import divconq.db.DataRequest;
import divconq.db.IDatabaseManager;
import divconq.db.ObjectFinalResult;
import divconq.db.ObjectResult;
import divconq.db.common.RequestFactory;
import divconq.db.query.LoadRecordRequest;
import divconq.db.query.SelectDirectRequest;
import divconq.db.query.SelectFields;
import divconq.db.query.WhereEqual;
import divconq.db.query.WhereField;
import divconq.db.update.InsertRecordRequest;
import divconq.db.update.UpdateRecordRequest;
import divconq.hub.Hub;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationContextBuilder;
import divconq.lang.op.UserContext;
import divconq.mod.ExtensionBase;
import divconq.struct.CompositeParser;
import divconq.struct.CompositeStruct;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.HexUtil;
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
		
		if ("Facebook".equals(feature)) {
			if ("LinkAccount".equals(op)) {
				// try to authenticate
				RecordStruct creds = msg.getFieldAsRecord("Body");
				
				String fbtoken = creds.getFieldAsString("AccessToken");
				
				RecordStruct fbinfo = AuthService.fbSignIn(fbtoken, null);		// TODO use FB secret key someday? for app proof...
				
				if (request.hasErrors() || (fbinfo == null)) {
					AuthService.this.clearUserContext(OperationContext.get());
					request.errorTr(442);
					request.complete();
					return;
				}
				
				// TODO allow only `verified` fb users?
				if (fbinfo.isFieldEmpty("id") || fbinfo.isFieldEmpty("email")
						 || fbinfo.isFieldEmpty("first_name") || fbinfo.isFieldEmpty("last_name")) {		
					AuthService.this.clearUserContext(OperationContext.get());
					request.errorTr(442);
					request.complete();
					return;
				}
				
				String uid = fbinfo.getFieldAsString("id");
									
				UpdateRecordRequest req = new UpdateRecordRequest();
				
				req
					.withTable("dcUser")
					.withId(OperationContext.get().getUserContext().getUserId())
					.withUpdateField("dcmFacebookId", uid);
				
				db.submit(req, new ObjectFinalResult(request) );
				
				return;
			}
		}
		else if ("Authentication".equals(feature)) {
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

			
			if ("SignInFacebook".equals(op)) {
				// TODO check domain settings that FB sign in is allowed
				
				// try to authenticate
				RecordStruct creds = msg.getFieldAsRecord("Body");
				
				//String uid = creds.getFieldAsString("UserId");
				String fbtoken = creds.getFieldAsString("AccessToken");
				
				RecordStruct fbinfo = AuthService.fbSignIn(fbtoken, null);		// TODO use FB secret key someday? for app proof...
				
				if (request.hasErrors() || (fbinfo == null)) {
					AuthService.this.clearUserContext(OperationContext.get());
					request.errorTr(442);
					request.complete();
					return;
				}
				
				// TODO allow only `verified` fb users?
				if (fbinfo.isFieldEmpty("id") || fbinfo.isFieldEmpty("email")
						 || fbinfo.isFieldEmpty("first_name") || fbinfo.isFieldEmpty("last_name")) {		
					AuthService.this.clearUserContext(OperationContext.get());
					request.errorTr(442);
					request.complete();
					return;
				}
				
				String uid = fbinfo.getFieldAsString("id");
				
				// sigin callback
				Consumer<String> signincb = new Consumer<String>() {					
					@Override
					public void accept(String userid) {
						DataRequest tp1 = RequestFactory.startSessionRequest(userid);
						
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
									request.complete();
									return;
								}

								ListStruct atags = sirec.getFieldAsList("AuthorizationTags");
								atags.addItem("User");
								
								OperationContext.switchUser(ctx, ctx.getUserContext().toBuilder() 
										.withVerified(true)
										.withAuthToken(sirec.getFieldAsString("AuthToken"))
										.withUserId(sirec.getFieldAsString("UserId"))
										.withUsername(sirec.getFieldAsString("Username"))
										.withFullName(sirec.getFieldAsString("FirstName") + " " + sirec.getFieldAsString("LastName"))		// TODO make locale smart
										.withEmail(sirec.getFieldAsString("Email"))
										.withAuthTags(atags)
										.toUserContext()
								);
								
								Hub.instance.getSessions().findOrCreateTether(request.getContext());
								
								request.returnValue(new RecordStruct(
										new FieldStruct("Username", sirec.getFieldAsString("Username")),
										new FieldStruct("FirstName", sirec.getFieldAsString("FirstName")),
										new FieldStruct("LastName", sirec.getFieldAsString("LastName")),
										new FieldStruct("Email", sirec.getFieldAsString("Email"))
								));
							}
						});
					}
				};
				
				// -----------------------------------------
				// find user - update or insert user record
				// -----------------------------------------
				
				db.submit(
						new SelectDirectRequest()
							.withTable("dcUser")
							.withSelect(new SelectFields()
									.withField("Id")
									.withField("dcUsername", "Username")
									.withField("dcFirstName", "FirstName")
									.withField("dcLastName", "LastName")
									.withField("dcEmail", "Email")
							)
							.withWhere(
									new WhereEqual(new WhereField("dcmFacebookId"), uid)		// TODO or where `username` = `fb email` - maybe?
							),
						new ObjectResult() {
							@Override
							public void process(CompositeStruct uLookupResult) {
								if (this.hasErrors() || (uLookupResult == null)) {
									request.error("Error finding user record");
									request.complete();
									return;
								}
								
								ListStruct ulLookupResult = (ListStruct) uLookupResult;
								
								if (ulLookupResult.getSize() == 0) {
									// insert new user record
									InsertRecordRequest req = new InsertRecordRequest();
									
									req
										.withTable("dcUser")		
										.withSetField("dcUsername", fbinfo.getFieldAsString("email"))
										.withSetField("dcEmail", fbinfo.getFieldAsString("email"))
										.withSetField("dcFirstName", fbinfo.getFieldAsString("first_name"))
										.withSetField("dcLastName", fbinfo.getFieldAsString("last_name"))
										.withSetField("dcmFacebookId", uid)
										.withSetField("dcConfirmed", true);									
									
									// TODO look at fb `locale` and `timezone` too
									
									db.submit(req, new ObjectResult() {										
										@Override
										public void process(CompositeStruct result) {
											if (this.hasErrors())
												request.complete();
											else
												signincb.accept(((RecordStruct)result).getFieldAsString("Id"));
										}
									});
								}
								else {
									String dcuid = ulLookupResult.getItemAsRecord(0).getFieldAsString("Id");
									
									UpdateRecordRequest req = new UpdateRecordRequest();
									
									req
										.withTable("dcUser")
										.withId(dcuid)
										// TODO add these once UpdateField works with Dynamic Scalar
										//.withUpdateField("dcUsername", fbinfo.getFieldAsString("email"))
										//.withUpdateField("dcEmail", fbinfo.getFieldAsString("email"))
										//.withUpdateField("dcFirstName", fbinfo.getFieldAsString("first_name"))
										//.withUpdateField("dcLastName", fbinfo.getFieldAsString("last_name"))
										.withUpdateField("dcmFacebookId", uid)
										.withUpdateField("dcConfirmed", true);									
									
									// TODO look at fb `locale` and `timezone` too
									
									db.submit(req, new ObjectResult() {										
										@Override
										public void process(CompositeStruct result) {
											if (this.hasErrors())
												request.complete();
											else
												signincb.accept(dcuid);
										}
									});
								}
							}
						}
				);
				
				return;
			}			
			
			// TODO now that we trust the token in Session this won't get called often - think about how to keep
			// auth token fresh in database - especially since the token will expire in 30 minutes
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
								ListStruct atags = urec.getFieldAsList("AuthorizationTags");
								atags.addItem("User");								
	
								OperationContext.switchUser(ctx, ctx.getUserContext().toBuilder() 
										.withVerified(true)
										.withUsername(urec.getFieldAsString("Username"))
										.withFullName(urec.getFieldAsString("FirstName") + " " + urec.getFieldAsString("LastName"))		// TODO make locale smart
										.withEmail(urec.getFieldAsString("Email"))
										.withAuthTags(atags)
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
							ListStruct atags = sirec.getFieldAsList("AuthorizationTags");
							atags.addItem("User");
							
							OperationContext.switchUser(ctx, ctx.getUserContext().toBuilder() 
									.withVerified(true)
									.withAuthToken(sirec.getFieldAsString("AuthToken"))
									.withUserId(sirec.getFieldAsString("UserId"))
									.withUsername(sirec.getFieldAsString("Username"))
									.withFullName(sirec.getFieldAsString("FirstName") + " " + sirec.getFieldAsString("LastName"))		// TODO make locale smart
									.withEmail(sirec.getFieldAsString("Email"))
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
	
	static public RecordStruct fbSignIn(String token, String secret) {
        try {
        	URL url = null;
        	
        	if (StringUtil.isEmpty(secret)) {
				url = new URL("https://graph.facebook.com/v2.2/me?access_token=" + URLEncoder.encode(token, "UTF-8"));
        	}
        	else {
	            Mac mac = Mac.getInstance("HmacSHA256");
	            
	            mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
	            
	            String verify = HexUtil.bufferToHex(mac.doFinal(token.getBytes())); 
	            
				//System.out.println("verify: " + verify);
				
				url = new URL("https://graph.facebook.com/v2.2/me?access_token=" + URLEncoder.encode(token, "UTF-8")
						+ "&appsecret_proof=" + URLEncoder.encode(verify, "UTF-8"));					
				
				//System.out.println("url: " + url);
        	}
        	
			HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
			 
			con.setRequestProperty("User-Agent", "DivConq/1.0 (Language=Java/8)");
	 
			int responseCode = con.getResponseCode();
	 
			if (responseCode == 200) {
				FuncResult<CompositeStruct> res = CompositeParser.parseJson(con.getInputStream());
				
				//System.out.println("res: " + res.getResult());
				
				return (RecordStruct) res.getResult();
			}
			
			OperationContext.get().error("FB Response Code : " + responseCode);
        } 
        catch (Exception x) {
            OperationContext.get().error("FB error: " + x);
        }
        
        return null;
	}
}
