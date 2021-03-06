package divconq.db.common;

import divconq.db.DataRequest;
import divconq.db.ReplicatedDataRequest;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;

public class RequestFactory {
	/**
	 * Send an empty request to the database that merely returns the string "PONG".
	 * This request is helpful for verifying that the database is connected and 
	 * responding.
	 * 
	 * @author Andy
	 *
	 */
	static public DataRequest ping() {
		return new DataRequest("dcPing");
	}
	
	static public DataRequest echo(String text) {
		return new DataRequest("dcEcho").withParams(new RecordStruct().withField("Text", text));
	}
	
	/**
	 * Sign in to confirm user, includes user name, password and optional confirm code.
	 * 
	 * @param username entered by user
	 * @param password entered by user
	 * @param code confirm code entered by user
	 */
	static public DataRequest signInRequest(String username, String password, String keyprint) {
		RecordStruct params = new RecordStruct()
				.withField("Username", (username != null) ? username.trim().toLowerCase() : null);
		
		if (StringUtil.isNotEmpty(password)) 
			params.withField("Password", password.trim());		// password crypto handled in stored proc
		
		if (StringUtil.isNotEmpty(keyprint))
			params.withField("ClientKeyPrint", keyprint.trim());
		
		return new ReplicatedDataRequest("dcSignIn").withParams(params);
	}
	
	static public DataRequest signOutRequest(String token) {
		return new ReplicatedDataRequest("dcSignOut")
			.withParams(new RecordStruct()
				.withField("AuthToken", token)
			);
	}
	
	/**
	 * @param userid the user to verify
	 * @param token the authtoken to verify
	 */
	static public DataRequest verifySessionRequest(String userid, String token) {
		return new ReplicatedDataRequest("dcVerifySession")
			.withParams(new RecordStruct()
					.withField("UserId", userid)
					.withField("AuthToken", token)
			);
	}
	
	/**
	 * Start session for user via user name or user id.
	 * 
	 * @param userid of user
	 * @param username of user
	 */
	static public DataRequest startSessionRequest(String userid) {
		return new ReplicatedDataRequest("dcStartSession")
			.withParams(new RecordStruct()
				.withField("UserId", userid)
			);
	}
	
	static public DataRequest startSessionRequestFromName(String username) {
		return new ReplicatedDataRequest("dcStartSession")
			.withParams(new RecordStruct()
				.withField("Username", (username != null) ? username.trim().toLowerCase() : null)
			);
	}
	
	/**
	 * Initiate sign on recovery.
	 * 
	 * @param user identifying info entered by user (username or email)
	 */
	static public DataRequest initiateRecoveryRequest(String user) {
		return new ReplicatedDataRequest("dcInitiateRecovery")
			.withParams(new RecordStruct()
					.withField("User", (user != null) ? user.trim().toLowerCase() : null)
			);
	}
	
	static public DataRequest removeFromSet(String table, String field, String id, ListStruct values) {
		return removeFromSet(table, field, new ListStruct(id), values);
	}
	
	static public DataRequest removeFromSet(String table, String field, ListStruct recs, ListStruct values) {
		return new ReplicatedDataRequest("dcUpdateSet")
			.withParams(new RecordStruct()
					.withField("Operation", "RemoveFromSet")
					.withField("Table", table)
					.withField("Records", recs)
					.withField("Field", field)
					.withField("Values", values)
			);
	}
	
	static public DataRequest addToSet(String table, String field, String id, ListStruct values) {
		return addToSet(table, field, new ListStruct(id), values);
	}
	
	static public DataRequest addToSet(String table, String field, ListStruct recs, ListStruct values) {
		return new ReplicatedDataRequest("dcUpdateSet")
			.withParams(new RecordStruct()
					.withField("Operation", "AddToSet")
					.withField("Table", table)
					.withField("Records", recs)
					.withField("Field", field)
					.withField("Values", values)
			);
	}
	
	static public DataRequest makeSet(String table, String field, String id, ListStruct values) {
		return makeSet(table, field, new ListStruct(id), values);
	}
	
	static public DataRequest makeSet(String table, String field, ListStruct recs, ListStruct values) {
		return new ReplicatedDataRequest("dcUpdateSet")
			.withParams(new RecordStruct()
					.withField("Operation", "MakeSet")
					.withField("Table", table)
					.withField("Records", recs)
					.withField("Field", field)
					.withField("Values", values)
			);
	}
	
}
