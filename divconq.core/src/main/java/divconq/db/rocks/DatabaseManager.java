/* ************************************************************************
#
#  DivConq
#
#  http://divconq.com/
#
#  Copyright:
#    Copyright 2012 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package divconq.db.rocks;

import static divconq.db.Constants.*;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.rocksdb.BackupableDB;
import org.rocksdb.BackupableDBOptions;
import org.rocksdb.CompactionStyle;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import divconq.db.DatabaseAudit;
import divconq.db.DatabaseTask;
import divconq.db.IDatabaseManager;
import divconq.db.IDatabaseRequest;
import divconq.db.DatabaseResult;
import divconq.db.IStoredProc;
import divconq.db.util.ByteUtil;
import divconq.hub.DomainInfo;
import divconq.hub.Hub;
import divconq.lang.op.OperationContext;
import divconq.schema.DbProc;
import divconq.struct.CompositeStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.ISettingsObfuscator;
import divconq.util.StringUtil;
import divconq.xml.XAttribute;
import divconq.xml.XElement;

/**
 * 
 * @author Andy
 *
 */
public class DatabaseManager implements IDatabaseManager {
	protected AtomicLong nextseq = new AtomicLong();
	protected XElement config = null;
	protected DatabaseAudit auditlevel = DatabaseAudit.Stamps;
	protected boolean replicate = false;
	
	protected RocksDB db = null;
	protected Options options = null;
	
	@Override
	public void init(XElement config) {
		OperationContext or = OperationContext.get();
		
		or.trace(0, "dcDatabase Initializing");
		
		if (config == null) {
			or.errorTr(210);
			return;
		}
		
		this.config = config;

		// TODO load audit and replication settings
		
		RocksDB.loadLibrary();
		
		this.options = new Options().setCreateIfMissing(true)
			//.createStatistics()
			//.setWriteBufferSize(8 * SizeUnit.KB)
			//.setMaxWriteBufferNumber(3)
			// TODO .setCompressionType(CompressionType.SNAPPY_COMPRESSION)
			// .setMaxBackgroundCompactions(3)
			// .setCompactionStyle(CompactionStyle.UNIVERSAL)
				;
		
				/* TODO enable merge operator for inc support, see inc below
				 * 
				 * possibly just make everything work with uint64add builtin to Rocks
		.setMergeOperator(new MergeOperator() {			
			@Override
			public long newMergeOperatorHandle() {
				// TODO Auto-generated method stub
				return 0;
			}
		});
		*/
		
		this.db = null;
		
		String dbpath = config.getAttribute("Path", "./DataStore");

		try {
			Files.createDirectories(Paths.get(dbpath));
			
			this.db = RocksDB.open(this.options, dbpath);		
			
			// TODO look into adding auto backup support
			//BackupableDBOptions bdb = new BackupableDBOptions("./DSBak", true, true, false, true, 0, 0);
			
			// TODO be sure compacting is working
			
			// make sure we always have an alphaa and an omega present
			byte[] x = this.db.get(DB_OMEGA_MARKER_ARRAY);
			
			if (x == null) {
				String obclass = "divconq.util.BasicSettingsObfuscator";
				String obseed = StringUtil.buildSecurityCode(64); 
				
				ISettingsObfuscator obfuscator = DomainInfo.prepDomainObfuscator(obclass, obseed);
				
				if (obfuscator == null) {
					or.error("dcDatabase prep error, obfuscator bad");
					return;
				}
			
				RocksInterface dbconn = new RocksInterface(this);
				
				dbconn.put(DB_ALPHA_MARKER_ARRAY, DB_ALPHA_MARKER_ARRAY);
				dbconn.put(DB_OMEGA_MARKER_ARRAY, DB_OMEGA_MARKER_ARRAY);
				
				BigDecimal stamp = this.allocateStamp(0);
				
				// insert root domain title
				dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_DOMAIN, "dcDomain", DB_GLOBAL_ROOT_DOMAIN, "dcTitle", stamp, "Data", "Root Domain");
				
				dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_DOMAIN, "dcDomain", DB_GLOBAL_ROOT_DOMAIN, "dcAlias", stamp, "Data", "root");
				
				// insert root domain name
				dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_DOMAIN, "dcDomain", DB_GLOBAL_ROOT_DOMAIN, "dcName", "root", stamp, "Data", "root");
				dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_DOMAIN, "dcDomain", DB_GLOBAL_ROOT_DOMAIN, "dcName", "localhost", stamp, "Data", "localhost");
				
				dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_DOMAIN, "dcDomain", DB_GLOBAL_ROOT_DOMAIN, "dcObscureClass", stamp, "Data", obclass);
				dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_DOMAIN, "dcDomain", DB_GLOBAL_ROOT_DOMAIN, "dcObscureSeed", stamp, "Data", obseed);

				XElement domainsettings = new XElement("Settings",
						new XElement("Web", 
								new XAttribute("UI", "Custom"),
								new XAttribute("SiteTitle", "Root Domain Manager"),
								new XAttribute("SiteAuthor", "DivConq"),
								new XAttribute("SiteCopyright", new DateTime().getYear() + ""),
								new XAttribute("HomePath", "/dcw/root/Home.dcui.xml"),								
								new XElement("Package", 
										new XAttribute("Name", "dcWeb")
								),
								new XElement("Global", 
										new XAttribute("Style", "/dcw/css/app.css")
								),
								new XElement("Global", 
										new XAttribute("Script", "/dcw/js/root.js")
								)
						)
				);
				
				dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_DOMAIN, "dcDomain", DB_GLOBAL_ROOT_DOMAIN, "dcCompiledSettings", stamp, "Data", domainsettings);
				
				// insert root domain index
				dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_DOMAIN, "dcDomain", DB_GLOBAL_ROOT_DOMAIN, "dcDomainIndex", DB_GLOBAL_ROOT_DOMAIN, stamp, "Data", DB_GLOBAL_ROOT_DOMAIN);
				
				// insert hub domain record id sequence
				dbconn.set(DB_GLOBAL_RECORD_META, "dcDomain", "Id", "00000", 1);
				
				// insert root domain record count
				dbconn.set(DB_GLOBAL_RECORD_META, DB_GLOBAL_ROOT_DOMAIN, "dcDomain", "Count", 1);
						
				// insert root user name
				dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_DOMAIN, "dcUser", DB_GLOBAL_ROOT_USER, "dcUsername", "root", stamp, "Data", "root");
				// increment index count
				dbconn.inc(DB_GLOBAL_INDEX_2, DB_GLOBAL_ROOT_DOMAIN, "dcUser", "dcUsername", "root");					
				// set the new index new
				dbconn.set(DB_GLOBAL_INDEX_2, DB_GLOBAL_ROOT_DOMAIN, "dcUser", "dcUsername", "root", DB_GLOBAL_ROOT_USER, "root", stamp, null);

				// insert root user email
				dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_DOMAIN, "dcUser", DB_GLOBAL_ROOT_USER, "dcEmail", "awhite@filetransferconsulting.com", stamp, "Data", "awhite@filetransferconsulting.com");
				// increment index count
				dbconn.inc(DB_GLOBAL_INDEX_2, DB_GLOBAL_ROOT_DOMAIN, "dcUser", "dcEmail", "awhite@filetransferconsulting.com");					
				// set the new index new
				dbconn.set(DB_GLOBAL_INDEX_2, DB_GLOBAL_ROOT_DOMAIN, "dcUser", "dcEmail", "awhite@filetransferconsulting.com", DB_GLOBAL_ROOT_USER, "awhite@filetransferconsulting.com", stamp, null);
				
				// insert root user password (not hashed/protected initially)
				dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_DOMAIN, "dcUser", DB_GLOBAL_ROOT_USER, "dcPassword", "0", stamp, "Data", obfuscator.hashStringToHex("A1s2d3f4"));
				
				// insert root user auth tags
				dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_DOMAIN, "dcUser", DB_GLOBAL_ROOT_USER, "dcAuthorizationTag", "SysAdmin", stamp, "Data", "SysAdmin");
				dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_DOMAIN, "dcUser", DB_GLOBAL_ROOT_USER, "dcAuthorizationTag", "Admin", stamp, "Data", "Admin");
				dbconn.set(DB_GLOBAL_RECORD, DB_GLOBAL_ROOT_DOMAIN, "dcUser", DB_GLOBAL_ROOT_USER, "dcAuthorizationTag", "PowerUser", stamp, "Data", "PowerUser");
				
				// insert hub domain record id sequence - set to 2 because root and guest are both users - guest just isn't entered
				dbconn.set(DB_GLOBAL_RECORD_META, "dcUser", "Id", "00000", 2);
				
				// insert root domain record count
				dbconn.set(DB_GLOBAL_RECORD_META, DB_GLOBAL_ROOT_DOMAIN, "dcUser", "Count", 1);
			}
		} 
		catch (Exception x) {
			or.error("dcDatabase error: " + x);
		}
		
		or.info(0, "dcDatabase Started");
    }
	
	protected boolean isOffline() {
		// TODO check db instance directly
		return false;  //!Hub.instance.isRunning() && !Hub.instance.isIdled() && !Hub.instance.isBooted();		
	}

	@Override
	public void stop() {
		OperationContext or = OperationContext.get();
		
		// TODO need a way to wait for all existing requests to complete while also not allowing new
		// requests in - perhaps a reworking of isOffline into isAvailable
		
		if (this.db != null)
			this.db.close();

		if (this.options != null)
			this.options.dispose();
		
		or.info(0, "dcDb Stopped");
    }
	
	@Override
	public void submit(IDatabaseRequest request, DatabaseResult cb) {
		// has to be a callback or it just won't work
		if (cb == null) 
			return;
		
		if (request == null) {
			cb.errorTr(307);
			cb.complete();
			return;
		}
		
		if (this.isOffline()) {
			cb.errorTr(308);		
			cb.complete();
			return;
		}
		
		// ========== build request ===============
		
		// TODO use DB context, restore after call
		
		boolean replicate = request.isReplicate();
		String name = request.getProcedure();
		CompositeStruct params = request.buildParams();
		
		// if did not pass build - request validation implicit during build
		if (cb.hasErrors()) {
			cb.errorTr(311);
			cb.complete();
			return;
		}
		
		Hub.instance.getSchema().validateProcRequest(name, params);
		
		// if did not pass schema validation
		if (cb.hasErrors()) {
			cb.errorTr(311);
			cb.complete();
			return;
		}
	
		// task/user context - including domain id - automatically travel with this request
		RecordStruct req = new RecordStruct();
		
		req.setField("Replicate", replicate);		// means this should replicate, where as Replicating means we are doing replication currently
		req.setField("Name", name);
		req.setField("Stamp", this.allocateStamp(0));
		req.setField("Params", params);
		req.setField("Domain", request.hasDomain() ? request.getDomain() : cb.getContext().getUserContext().getDomainId());

		cb.trace(0, "dcDb call prepared for procedure: " + name);
		
		//System.out.println("===============================================================");
		//System.out.println("db request:\n" + buff);
		
		DatabaseTask task = new DatabaseTask();
		task.setResult(cb);
		task.setRequest(req);
		
		//DatabaseTask task = this.buildRequest(request, cb);
		
		if (cb.hasErrors() || (task == null)) {
			cb.errorTr(309);	
			cb.complete();
			return;
		}
		
		DbProc proc = Hub.instance.getSchema().getDb().getProc(name);
		
		String spname = proc.execute;
		
		try {
			Class<?> spclass = Class.forName(spname);				
			IStoredProc sp = (IStoredProc) spclass.newInstance();
			sp.execute(new RocksInterface(this), task, cb);
			
			// TODO is audit level is high enough? then audit request
			// - audit req, operation context - including log and error state 
			
			// AUDIT after execute so that additional parameters can be collected for replication
			
			/*
			 * maybe audit by domain id, user id, stamp instead of task id...which really means little
			 * though in debug mode maybe also index audit by task id in case we need to trace the audit for the task
			 * 
			 * STAMP has hid embedded in it so no need for Stamp,hid combo
			 * 
			 * replace TaskId with simple entry id? seq value... dbNumber so audit up to 15 digits? more?
			 * then start at 1 again?  assume that audit cleanup will cover old?
			 * 
			 ;s ^dcAudit(TaskId,Stamp,hid,"Operation")=$s(Errors+0=0:"Call",1:"FailedCall")
			 ;m ^dcAudit(TaskId,Stamp,hid,"Params")=Params
			 ;s ^dcAudit(TaskId,Stamp,hid,"Execute")=FuncName
			 ;s ^dcAudit(TaskId,Stamp,hid,"UserId")=UserId
			 ;s ^dcAudit(TaskId,Stamp,hid,"HubId")=HubId
			 ;
			 ;s ^dcAuditTime(Stamp,hid)=TaskId     ; TODO add user index?
			 ;
			 ;n latestTs
			 ;lock +^dcReplication("Local",hid)
			 ;s latestTs=^dcReplication("Local",hid)
			 ;i Stamp]]latestTs s ^dcReplication("Local",hid)=Stamp
			 ;lock -^dcReplication("Local",hid)
			 ;
			 
			 ^dcReplacation("CompleteStamp")=Stamp   - the latest stamp for which all replications are complete
			 
			*/			
		} 
		catch (Exception x) {
			cb.error("Unable to load/start procedure class: " + x);
			cb.complete();
			return;
		}
		
		// TODO use hub level counters for this
		//this.totalTasksSubmitted.incrementAndGet();
	}
	
	public RocksInterface allocateAdapter() {
		return new RocksInterface(this);
	}
	
	public boolean isAuditDisabled() {
		return (this.auditlevel == DatabaseAudit.None);
	}
	
	/**
	 * @param offset in seconds from now
	 * @return a valid timestamp for use in dcDb auditing
	 */
	protected BigDecimal allocateStamp(int offset) {
		if (this.auditlevel == DatabaseAudit.None)
			return BigDecimal.ZERO;
		
		long ns = this.nextseq.getAndIncrement();
		
		if (ns > 9999) {		
			synchronized (this.nextseq) {
				ns = this.nextseq.get();
				
				if (ns > 9999)
					this.nextseq.set(0);
				
				ns = this.nextseq.getAndIncrement();
			}
		}
		
		BigDecimal ret = new BigDecimal("-" + new DateTime(DateTimeZone.UTC).plusSeconds(offset).getMillis() + "." + 
			StringUtil.leftPad(ns + "", 4, "0") + OperationContext.getHubId());
		
		System.out.println("new stamp: " + ret.toPlainString());
		
		return ret;
	}

	synchronized public Long inc(byte[] key, int i) throws RocksDBException {
		//this.db.merge(writeOpts, key, value);
		// TODO eventually replace this with 
		//MergeOperator mo = new generic
		
		Long id = Struct.objectToInteger(ByteUtil.extractValue(this.db.get(key)));
		
		id = (id == null) ? i : id + i;
		
		this.db.put(key, ByteUtil.buildValue(id));

		return id;
	}
}
