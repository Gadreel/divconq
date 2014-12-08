package divconq.db.common;

import org.joda.time.DateTime;

import divconq.db.DataRequest;
import divconq.db.ReplicatedDataRequest;
import divconq.log.DebugLevel;
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
	static public DataRequest signInRequest(String username, String password, String code) {
		RecordStruct params = new RecordStruct()
				.withField("Username", (username != null) ? username.trim().toLowerCase() : null)
				.withField("At", new DateTime());
		
		if (StringUtil.isNotEmpty(password)) 
			params.withField("Password", password.trim());		// password crypto handled in stored proc
		else
			params.withField("RecoverExpire", new DateTime().minusMinutes(30));		// 30 minutes before recovery expires
		
		if (StringUtil.isNotEmpty(code))
			params.withField("Code", code.trim());
		
		if (AddUserRequest.meetsPasswordPolicy(password, true).hasLogLevel(DebugLevel.Warn))
			params.withField("Suspect", true);
		
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
	static public DataRequest startSessionRequest(String userid, String username) {
		return new ReplicatedDataRequest("dcStartSession")
			.withParams(new RecordStruct()
				// TODO in db .withField("At", new DateTime())
				.withField("Username", (username != null) ? username.trim().toLowerCase() : null)
				.withField("UserId", userid)
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
	
	static public DataRequest removeFromSet(String table, String field, String id, ListStruct subids) {
		return removeFromSet(table, field, new ListStruct(id), subids);
	}
	
	static public DataRequest removeFromSet(String table, String field, ListStruct recs, ListStruct subids) {
		return new ReplicatedDataRequest("dcUpdateSet")
			.withParams(new RecordStruct()
					.withField("Operation", "RemoveFromSet")
					.withField("Table", table)
					.withField("Records", recs)
					.withField("Field", field)
					.withField("SubIds", subids)
			);
	}
	
	static public DataRequest addToSet(String table, String field, String id, ListStruct subids) {
		return addToSet(table, field, new ListStruct(id), subids);
	}
	
	static public DataRequest addToSet(String table, String field, ListStruct recs, ListStruct subids) {
		return new ReplicatedDataRequest("dcUpdateSet")
			.withParams(new RecordStruct()
					.withField("Operation", "AddToSet")
					.withField("Table", table)
					.withField("Records", recs)
					.withField("Field", field)
					.withField("SubIds", subids)
			);
	}
	
	static public DataRequest makeSet(String table, String field, String id, ListStruct subids) {
		return makeSet(table, field, new ListStruct(id), subids);
	}
	
	static public DataRequest makeSet(String table, String field, ListStruct recs, ListStruct subids) {
		return new ReplicatedDataRequest("dcUpdateSet")
			.withParams(new RecordStruct()
					.withField("Operation", "MakeSet")
					.withField("Table", table)
					.withField("Records", recs)
					.withField("Field", field)
					.withField("SubIds", subids)
			);
	}
	
}
