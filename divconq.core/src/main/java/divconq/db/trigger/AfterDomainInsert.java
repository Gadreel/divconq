package divconq.db.trigger;

import static divconq.db.Constants.DB_GLOBAL_INDEX_2;
import static divconq.db.Constants.DB_GLOBAL_RECORD;
import static divconq.db.Constants.DB_GLOBAL_RECORD_META;
import static divconq.db.Constants.DB_GLOBAL_ROOT_DOMAIN;
import static divconq.db.Constants.DB_GLOBAL_ROOT_USER;

import java.math.BigDecimal;

import org.joda.time.DateTime;

import divconq.db.DatabaseException;
import divconq.db.DatabaseInterface;
import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.db.TablesAdapter;
import divconq.lang.BigDateTime;
import divconq.lang.op.OperationResult;

public class AfterDomainInsert implements IStoredProc {
	@Override
	public void execute(DatabaseInterface conn, DatabaseTask task, OperationResult log) {
		String id = task.getParamsAsRecord().getFieldAsString("Id");
		
		// ===========================================
		//  insert root domain index
		// ===========================================
		try {
			conn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_DOMAIN, "dcDomain", DB_GLOBAL_ROOT_DOMAIN, "dcDomainIndex", 
					id, task.getStamp(), "Data", id);
		} 
		catch (DatabaseException x) {
			log.error("Unable to set dcDomainIndex: " + x);
			return;
		}
		
		// ===========================================
		//  insert a template for the root user of this new domain
		// ===========================================

		try {
			BigDecimal stamp = task.getStamp();
			String did = task.getDomain();
			
			String unamesub = conn.allocateSubkey();
			
			// insert root user name
			conn.set(DB_GLOBAL_RECORD, did, "dcUser", DB_GLOBAL_ROOT_USER, "dcUsername", unamesub, stamp, "Data", "root");
			// increment index count
			conn.inc(DB_GLOBAL_INDEX_2, did, "dcUser", "dcUsername", "root");					
			// set the new index new
			conn.set(DB_GLOBAL_INDEX_2, did, "dcUser", "dcUsername", "root", DB_GLOBAL_ROOT_USER, unamesub, stamp, null);

			// TODO enhance to take email from root domain's root user
			String email = "awhite@filetransferconsulting.com";

			task.pushDomain(DB_GLOBAL_ROOT_DOMAIN);
			
			TablesAdapter db = new TablesAdapter(conn, task); 
			
			Object rdemail = db.getDynamicScalar("dcUser", DB_GLOBAL_ROOT_USER, "dcEmail", new BigDateTime());

			if (rdemail != null)
				email = (String)rdemail;

			task.popDomain();
			
			String emailsub = conn.allocateSubkey();
			
			// insert root user email
			conn.set(DB_GLOBAL_RECORD, did, "dcUser", DB_GLOBAL_ROOT_USER, "dcEmail", emailsub, stamp, "Data", email);
			// increment index count
			conn.inc(DB_GLOBAL_INDEX_2, did, "dcUser", "dcEmail", email);					
			// set the new index new
			conn.set(DB_GLOBAL_INDEX_2, did, "dcUser", "dcEmail", email, DB_GLOBAL_ROOT_USER, emailsub, stamp, null);
			
			// TODO enhance how confirm code is generated/returned
			
			// insert root user confirmation code - they have N minutes to login with recovery code
			conn.set(DB_GLOBAL_RECORD, did, "dcUser", DB_GLOBAL_ROOT_USER, "dcConfirmCode", stamp, "Data", "A1s2d3f4");
			conn.set(DB_GLOBAL_RECORD, did, "dcUser", DB_GLOBAL_ROOT_USER, "dcRecoverAt", stamp, "Data", new DateTime());
			
			// insert root user auth tags
			conn.set(DB_GLOBAL_RECORD, did, "dcUser", DB_GLOBAL_ROOT_USER, "dcAuthorizationTag", "Admin", stamp, "Data", "Admin");
			
			// insert root domain record count
			conn.set(DB_GLOBAL_RECORD_META, did, "dcUser", "Count", 1);
		} 
		catch (DatabaseException x) {
			log.error("Unable to set dcDomainIndex: " + x);
			return;
		}
	}
}
