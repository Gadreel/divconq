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

/**
 * All database access required to support DivConq Work Queues goes into
 * this class.  Data access is abstracted out in DC so that we can later 
 * use other storage models.
 */
package divconq.work.sql;

import divconq.hub.Hub;
import divconq.lang.FuncResult;
import divconq.lang.OperationContext;
import divconq.lang.OperationResult;
import divconq.sql.SqlEngine;
import divconq.sql.SqlManager.SqlDatabase;
import divconq.sql.SqlNull;
import divconq.sql.SqlSelect;
import divconq.sql.SqlSelectInteger;
import divconq.sql.SqlSelectSqlDateTime;
import divconq.sql.SqlSelectString;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.StringUtil;
import divconq.work.IQueueDriver;
import divconq.work.TaskRun;
import divconq.work.Task;
import divconq.xml.XElement;

public class QueueDriver implements IQueueDriver {
	@Override
	public void init(OperationResult or, XElement config) {
	}
	
	@Override
	public void start(OperationResult or) {
		or.infoTr(160);
	}
	
	@Override
	public void stop(OperationResult or) {
		or.infoTr(161);
	}
	
	// return candidate list from   [ TaskIdentity: "vvv", WorkId: nnn, ClaimedStamp: "vvv" ]
	// where stamp is a string of yyyy-mm-dd hh:mm:ss.mmm
	@Override
	public FuncResult<ListStruct> findPotentialClaims(String pool, int howmanymax) {
		FuncResult<ListStruct> res = new FuncResult<ListStruct>();
	
		SqlDatabase db = Hub.instance.getSQLDatabase();
		
		if (db == null) {
			res.errorTr(156);
			return res;
		}
		
		// don't use local time for timeout stuff - use database time?
		
		// h2 syntax
		String where = "(dcClaimedStamp < DATEADD('MINUTE', -dcClaimTimeout, " + db.nowFunc() + ")) ";
		
		if (db.getEngine() == SqlEngine.SqlServer)
			where = "(dcClaimedStamp < DATEADD(MINUTE, -dcClaimTimeout, " + db.nowFunc() + ")) ";
		
		if (db.getEngine() == SqlEngine.MariaDb || db.getEngine() == SqlEngine.MySQL)
			where = "(dcClaimedStamp < DATE_SUB(" + db.nowFunc() + ", INTERVAL dcClaimTimeout minute)) ";
		
		where += "AND (dcDestBucket = ?) "
				+ "AND ((dcDestSquad = ?) OR (dcDestSquad IS NULL)) AND ((dcDestHubId = ?) OR (dcDestHubId IS NULL))";
		
		FuncResult<ListStruct> rsres = db.executeQueryLimit(
				new SqlSelect[] { 
						new SqlSelectString("dcTaskIdentity", "TaskIdentity", null), 
						new SqlSelectSqlDateTime("dcClaimedStamp", "ClaimedStamp", null, true), 
						new SqlSelectInteger("dcWorkId", "WorkId", 0), 
						new SqlSelectInteger("dcClaimTimeout", "ClaimTimeout", 0), 
						new SqlSelectSqlDateTime("dcAddedStamp", "AddedStamp", null) 
				}, 
				"dcWorkQueue",					// from 
				where, 							// where
				null, 							// group by
				"AddedStamp", 				// order by
				howmanymax, 					// limit
				false,
				pool,											// param 1 - pool name
				Hub.instance.getResources().getSquadId(),		// param 2 - squad name
				Hub.instance.getResources().getHubId()			// param 3  - hub id
		);	
		
		res.copyMessages(rsres);
		
		ListStruct rs = rsres.getResult();
		
		res.setResult(rs);
		
		return res;
	}
	
	// confirm a claim on a work queue entry, error if not able to claim
	// return the stamp used
	// where stamp is a string of yyyy-mm-dd hh:mm:ss.mmm
	@Override
	public FuncResult<RecordStruct> makeClaim(RecordStruct info) {
		FuncResult<RecordStruct> res = new FuncResult<RecordStruct>();
		
		Hub.instance.getCountManager().countObjects("dcWorkQueueClaims", info);
		
		String taskidentity = info.getFieldAsString("TaskIdentity"); 
		String oldstamp = info.getFieldAsString("ClaimedStamp");
		
		SqlDatabase db = Hub.instance.getSQLDatabase();
		
		if (db == null) {
			res.errorTr(156);
			Hub.instance.getCountManager().countObjects("dcWorkQueueClaimFails", info);
			return res;
		}
		
		// MAKE OUR CLAIM
		
		String sql = "UPDATE dcWorkQueue SET dcClaimedStamp = " + db.nowFunc() + ", dcClaimedHub = ? WHERE dcTaskIdentity = ? AND dcClaimedStamp = ?";
        
		FuncResult<Integer> ures = db.executeUpdate(sql, 
				Hub.instance.getResources().getHubId(),			// param 1 - hub id making the claim 
				taskidentity, 									// param 2 - task to claim
				oldstamp										// param 3 - time stamp we had when last we heard about this task - if we are out of date then we don't earn the task
		);
		
		res.copyMessages(ures);
		
		if (ures.getResult() != 1) {
			res.errorTr(162, taskidentity);
			Hub.instance.getCountManager().countObjects("dcWorkQueueClaimFails", info);			
			return res;
		}
		
		// UPDATE OUR KNOWN STAMP - without this we cannot continue to talk to this task record 
		// this must execute within 1 minute of code above - which shouldn't be a problem, but just FYI
		// if it does not it could get treated as timeout and someone else make a claim
		
		FuncResult<Struct> rsres = db.executeQueryScalar(
				new SqlSelectSqlDateTime("dcClaimedStamp", null, true), 
				"dcWorkQueue",					// from 
				"dcClaimedHub = ? AND dcTaskIdentity = ?", 							// where
				null, 
				Hub.instance.getResources().getHubId(),			// param 1  - hub id
				taskidentity 									// param 2 - task to claim
		);	
		
		res.copyMessages(rsres);
		
		info.setField("ClaimedStamp", Struct.objectToString(rsres.getResult()));
		
		res.setResult(info);
		
		return res;
	}
	
	// update claim stamp on a work queue entry, error if not able to claim
	// return the stamp used
	// where stamp is a string of yyyy-mm-dd hh:mm:ss.mmm
	@Override
	public OperationResult updateClaim(Task info) {
		OperationResult res = new OperationResult();
		
		Hub.instance.getCountManager().countObjects("dcWorkQueueClaimUpdates", info);		
		
		String taskidentity = info.getId(); 
		String oldstamp = info.getClaimedStamp();
		
		SqlDatabase db = Hub.instance.getSQLDatabase();
		
		if (db == null) {
			res.errorTr(156);
			Hub.instance.getCountManager().countObjects("dcWorkQueueClaimUpdateFails", info);		
			return res;
		}
		
		String sql = "UPDATE dcWorkQueue SET dcClaimedStamp = " + db.nowFunc() + " WHERE dcTaskIdentity = ? AND dcClaimedStamp = ?";
        
		FuncResult<Integer> ures = db.executeUpdate(sql, 
				taskidentity, 	// param 1 - task we claimed
				oldstamp		// param 2 - time stamp we had when last we heard about this task - if we are out of date then we don't keep the task
		);
		
		res.copyMessages(ures);
		
		if (ures.getResult() != 1) {
			res.errorTr(188, taskidentity);
			Hub.instance.getCountManager().countObjects("dcWorkQueueClaimUpdateFails", info);		
			return res;
		}
		
		//System.out.println(">>>> Claim updated <<<< " + taskidentity);
		
		// UPDATE OUR KNOWN STAMP - without this we cannot continue to talk to this task record 
		
		FuncResult<Struct> rsres = db.executeQueryScalar(
				new SqlSelectSqlDateTime("dcClaimedStamp", null, true), 
				"dcWorkQueue",					// from 
				"dcClaimedHub = ? AND dcTaskIdentity = ?", 							// where
				null, 
				Hub.instance.getResources().getHubId(),			// param 1  - hub id
				taskidentity 									// param 2 - task to claim
		);	
		
		res.copyMessages(rsres);
		
		Struct rs = rsres.getResult();
		
		info.withClaimedStamp(Struct.objectToString(rs));
		
		return res;
	}

	// return claim stamp
	@Override
	public FuncResult<String> reserveUniqueWork(String taskidentity) {		
		FuncResult<String> res = new FuncResult<String>();
		
		Hub.instance.getCountManager().countObjects("dcWorkQueueReserves", taskidentity);		
		
		SqlDatabase db = Hub.instance.getSQLDatabase();
	
		// put in distant future so no one else can claim/reserve it 
		String claimstamp = "9999-01-01 00:00:00.000";
		
        String sql = "INSERT INTO dcWorkQueue(dcTaskIdentity, dcClaimedStamp, dcAddedStamp) "
        		+ " VALUES (?,?," + db.nowFunc() + ")";
        
		FuncResult<Integer> ires = db.executeUpdate(sql, 
					taskidentity, 	// param 1 - task we are reserving
					claimstamp		// param 2 - lock it with a future stamp
		);
		
		res.copyMessages(ires);
		
		if (ires.getCode() == 194) {
			res.infoTr(162, taskidentity);
			return res;
		}
		
		if (ires.getResult() != 1) { 
			res.errorTr(183, taskidentity);
			Hub.instance.getCountManager().countObjects("dcWorkQueueReserveFails", taskidentity);		
			return res;
		}

        sql = "SELECT Id FROM dcWork WHERE dcTaskIdentity = ?";
		
		FuncResult<Struct> rsres = db.executeQueryScalar(
				new SqlSelectString("Id"), 
				"dcWork",					// from 
				"dcTaskIdentity = ?", 							// where
				null, 
				taskidentity 									// param 1 - task to claim
		);	 
		
		res.copyMessages(rsres);
		
		if (rsres.isEmptyResult()) {
			res.setResult(claimstamp);
			return res;
		}
		
		// if there is a record here it means the work was already created and completed before we got a chance
		// to even add - remove our attempt at adding
		res.errorTr(183, taskidentity);		
		
		// therefore we remove any claim we might have
        sql = "DELETE FROM dcWorkQueue WHERE dcTaskIdentity = ?";
        
		FuncResult<Integer> dres = db.executeUpdate(sql, 
				taskidentity
		);
		
		res.copyMessages(dres);
		
		return res;
	}

	// return claim stamp
	@Override
	public FuncResult<String> reserveCurrentWork(String taskidentity) {		
		FuncResult<String> res = new FuncResult<String>();
		
		Hub.instance.getCountManager().countObjects("dcWorkQueueReserves", taskidentity);		
		
		SqlDatabase db = Hub.instance.getSQLDatabase();
	
		// put in distant future so no one else can claim/reserve it 
		String claimstamp = "9999-01-01 00:00:00.000";
		
        String sql = "INSERT INTO dcWorkQueue(dcTaskIdentity, dcClaimedStamp, dcAddedStamp) "
        		+ " VALUES (?,?," + db.nowFunc() + ")";
        
		FuncResult<Integer> ires = db.executeUpdate(sql, 
					taskidentity, 	// param 1 - task we are reserving
					claimstamp		// param 2 - lock it with a future stamp
		);
		
		res.copyMessages(ires);
		
		if (ires.getCode() == 194) {
			res.infoTr(162, taskidentity);
			return res;
		}
		
		if (ires.getResult() != 1) { 
			res.errorTr(183, taskidentity);
			Hub.instance.getCountManager().countObjects("dcWorkQueueReserveFails", taskidentity);		
			return res;
		}
		
		res.setResult(claimstamp);
		
		return res;
	}
	
	@Override
	public FuncResult<String> submit(Task info) {
		FuncResult<String> res = new FuncResult<>();
    	
		String forbucket = info.getBucket();
		String taskidentity = info.getId();
		
		ListStruct tags = info.getTags();
		
		String tag1 = ((tags != null) && (tags.getSize() > 0)) ? tags.getItemAsString(0) : null;
		String tag2 = ((tags != null) && (tags.getSize() > 1)) ? tags.getItemAsString(1) : null;
		String tag3 = ((tags != null) && (tags.getSize() > 2)) ? tags.getItemAsString(2) : null;
		
		String forsquad = info.getSquad();
		String forhub = info.getHub();
		
		String reservedclaim = info.getClaimedStamp();
		
		SqlDatabase db = Hub.instance.getSQLDatabase();
        
        String sql = "INSERT INTO dcWork (dcTaskIdentity, dcAddStamp, dcTitle, dcTask, dcDestBucket, dcDestSquad, dcDestHubId, dcTag1, dcTag2, dcTag3, dcMaxTries) "
        		+ " VALUES (?," + db.nowFunc() + ",?,?,?,?,?,?,?,?,?)";
        
		FuncResult<Long> ires = db.executeInsertReturnId(sql,
				taskidentity, 
				info.getTitle(), 
				info.freezeToRecord().toString(),
				forbucket,
				StringUtil.isNotEmpty(forsquad) ? forsquad : SqlNull.VarChar,
				StringUtil.isNotEmpty(forhub) ? forhub : SqlNull.VarChar,
				StringUtil.isNotEmpty(tag1) ? tag1 : SqlNull.VarChar,
				StringUtil.isNotEmpty(tag2) ? tag2 : SqlNull.VarChar,
				StringUtil.isNotEmpty(tag3) ? tag3 : SqlNull.VarChar,
				info.getMaxTries()
		);
		
		res.copyMessages(ires);
		
		long workid = ires.getResult();   
		
		if (workid == 0) {
			res.errorTr(158, taskidentity);
			return res;
		}
		
		int cnt = 0;
		
		if (reservedclaim == null) {
	        sql = "INSERT INTO dcWorkQueue(dcTaskIdentity, dcWorkId, dcDestBucket, dcDestSquad, dcDestHubId, dcClaimTimeout, dcAddedStamp) "
	        		+ " VALUES (?,?,?,?,?,?," + db.nowFunc() + ")";
	        
			FuncResult<Integer> ires2 = db.executeUpdate(sql, 
					taskidentity, 
					workid, 
					forbucket,
					StringUtil.isNotEmpty(forsquad) ? forsquad : SqlNull.VarChar,
					StringUtil.isNotEmpty(forhub) ? forhub : SqlNull.VarChar,
					info.getTimeout()
			);
	        
			res.copyMessages(ires2);
			
			cnt = ires2.getResult();
		}
		else {
			// if already reserved, then just make it available to the queue by switching back to 1970
			sql = "UPDATE dcWorkQueue SET dcClaimedStamp = ?, dcWorkId = ?, dcDestBucket = ?, dcDestSquad = ?, dcDestHubId = ?, dcClaimTimeout = ? WHERE dcTaskIdentity = ? AND dcClaimedStamp = ?";
	        
			FuncResult<Integer> ires2 = db.executeUpdate(sql, 
					"1970-01-01 00:00:00.000", 		// set claim stamp back to 1970 - means we are open to be claimed
					workid, 
					forbucket, 
					StringUtil.isNotEmpty(forsquad) ? forsquad : SqlNull.VarChar,
					StringUtil.isNotEmpty(forhub) ? forhub : SqlNull.VarChar,
					info.getTimeout(), 
					taskidentity, 
					reservedclaim
			);
	        
			res.copyMessages(ires2);
			
			cnt = ires2.getResult();
		}
		
		if (cnt != 1) {
			res.errorTr(159, taskidentity);
	        
			sql = "DELETE FROM dcWork WHERE Id = ?";
	        
			db.executeUpdate(sql, workid);
		}
		else
			res.setResult(workid + "");
		
		Hub.instance.getCountManager().countObjects("dcWorkQueueAddWorkTotal", taskidentity);		
		
		return res;
	}
	
	// see dcTask in schema
	// must pass in WorkId and ClaimedStamp to function correctly with startWork
	@Override
	public FuncResult<Task> loadWork(RecordStruct info) {
		long workid = info.getFieldAsInteger("WorkId");
    	
		SqlDatabase db = Hub.instance.getSQLDatabase();
		
		FuncResult<Task> res = new FuncResult<>();
		
		FuncResult<ListStruct> rsres = db.executeQuery(
				new SqlSelect[] { 
						new SqlSelectSqlDateTime("dcAddStamp", "AddStamp", null), 
						new SqlSelectString("dcStatus", "Status", null), 
						new SqlSelectString("dcTask", "Task", null), 
						new SqlSelectInteger("dcCurrentTry", "CurrentTry", 0)
				}, 
				"dcWork",					// from 
				"Id = ? AND Active = 1",	// where
				null, 						// group by
				null, 						// order by
				workid						// param 1 - dcWork record id
		);	
		
		res.copyMessages(rsres);
		
		ListStruct rs = rsres.getResult();
		
		if (rs.isEmpty()) {
			res.errorTr(166, workid);
			return res;
		}
		
		RecordStruct rec = rs.getItemAsRecord(0);
		
		Task tb = new Task(rec.getFieldAsRecord("Task"))
			.withWorkId(workid + "")
			.withClaimedStamp(info.getFieldAsString("ClaimedStamp"))
			.withStatus(rec.getFieldAsString("Status"))
			.withAddStamp(rec.getFieldAsDateTime("AddStamp"))
			.withCurrentTry((int)rec.getFieldAsInteger("CurrentTry", 0));
		
		res.setResult(tb);
		
		return res;
	}
	
	// return audit id
	@Override
	public FuncResult<String> startWork(String workid) {
		FuncResult<String> res = new FuncResult<>();
    	
		SqlDatabase db = Hub.instance.getSQLDatabase();
		
		String sql = null;
		
		FuncResult<ListStruct> rsres = db.executeQuery(
				new SqlSelect[] { 
						new SqlSelectString("dcTaskIdentity", "TaskIdentity", null), 
						new SqlSelectString("dcStatus", "Status", null), 
						new SqlSelectInteger("dcCurrentTry", "LastTry", 0), 
						new SqlSelectInteger("dcMaxTries", "MaxTry", 0) 
				}, 
				"dcWork",					// from 
				"Id = ?", 					// where
				null, 						// group by
				null, 						// order by
				StringUtil.parseInt(workid)						// param 1
		);
		
		res.copyMessages(rsres);
		
		ListStruct rs = rsres.getResult();
		
		if (rs.isEmpty()) {
			res.errorTr(166, workid);
			return res;
		}
		
		RecordStruct rec = rs.getItemAsRecord(0);
		
		String taskidentity = rec.getFieldAsString("TaskIdentity");
		String status = rec.getFieldAsString("Status");
		int trynum = (int)rec.getFieldAsInteger("LastTry", 0);
		int maxtries = (int)rec.getFieldAsInteger("MaxTry", 0);
		
		if ("Completed".equals(status)) {
			res.errorTr(167, taskidentity);
			return res;
		}
		
		trynum++;
		
		if (trynum > maxtries) {
	        sql = "UPDATE dcWork SET dcStatus = ? WHERE Id = ?";
	        
			FuncResult<Integer> ires2 = db.executeUpdate(sql, "Failed", workid);
	        
			res.copyMessages(ires2);
	        
			if (ires2.getResult() != 1) 
				res.errorTr(164, taskidentity);

			// delete even if error above, we should not be on queue now
			sql = "DELETE FROM dcWorkQueue WHERE dcTaskIdentity = ?";
	        
			ires2 = db.executeUpdate(sql, taskidentity);
	        
			res.copyMessages(ires2);
			
			if (ires2.getResult() != 1) 
				res.warnTr(165, taskidentity);		// warning only because at least we know there is no entry in the queue
			
			res.errorTr(168, taskidentity);
			return res;
		}
		
		// create a new audit entry
        sql = "INSERT INTO dcWorkAudit (dcWorkId, dcTryNum, dcHub, dcStartStamp) "
        		+ " VALUES (?,?,?," + db.nowFunc() + ")";
        
        int ftrynum = trynum;
        
		FuncResult<Long> ires2 = db.executeInsertReturnId(sql, workid, ftrynum, OperationContext.getHubId());
        
		res.copyMessages(ires2);
		
		if (ires2.getResult() == 0) {
			res.errorTr(170, taskidentity);
			return res;
		}
		
        sql = "UPDATE dcWork SET dcCurrentTry = ?, dcLastAudit = ?, dcStatus = ? WHERE Id = ?";
        
        FuncResult<Integer> ires3 = db.executeUpdate(sql, ftrynum, ires2.getResult(), "Running", workid);
        
		res.copyMessages(ires3);
		
		if (ires3.getResult() != 1) {
			res.errorTr(164, taskidentity);
			return res;
		}
		
		res.setResult(ires2.getResult() + "");
		
		Hub.instance.getCountManager().countObjects("dcWorkQueueStarts", taskidentity);		
		
		return res;
	}
	
	@Override
	public OperationResult endWork(TaskRun task) {		
		OperationResult res = new OperationResult();
		
		Task info = task.getTask();
		
		boolean finaltry = task.getTask().getFinalTry();
		
		Long auditid = StringUtil.parseInt(info.getAuditId());
		Long workid = StringUtil.parseInt(info.getWorkId());
		
		String msg = task.getMessage(); 
		String log = task.getLog(); 
		
		SqlDatabase db = Hub.instance.getSQLDatabase();
		
        String sql = "UPDATE dcWorkAudit SET dcEndStamp = " + db.nowFunc() + ", dcCode = ?, dcMessage = ?, dcLogFile = ? WHERE Id = ?";
        
		FuncResult<Integer> ires2 = db.executeUpdate(sql, 
				task.getCode(),
				StringUtil.isNotEmpty(msg) ? msg : SqlNull.Text,
				StringUtil.isNotEmpty(log) ? log : SqlNull.Text,
				auditid
		);
        
		res.copyMessages(ires2);
        
		if (ires2.getResult() != 1) 
			res.errorTr(163, task.getTask().getId());

		// continue even if error, other tables should be updated
		
        sql = "UPDATE dcWork SET dcStatus = ? WHERE Id = ?";
        
		ires2 = db.executeUpdate(sql, info.getStatus(), workid);
        
		res.copyMessages(ires2);
        
		if (ires2.getResult() != 1) 
			res.errorTr(164, task.getTask().getId());
        
		if (finaltry) {
			sql = "DELETE FROM dcWorkQueue WHERE dcTaskIdentity = ?";
	        
			ires2 = db.executeUpdate(sql, task.getTask().getId());
	        
			res.copyMessages(ires2);
	        
			if (ires2.getResult() != 1) 
				res.warnTr(165, task.getTask().getId());		// warning only because at least we know there is no entry in the queue
		}
		
		Hub.instance.getCountManager().countObjects("dcWorkQueueEnds", task.getTask().getId());		
		
		return res;
	}
	
	@Override
	public OperationResult trackWork(TaskRun task, boolean ended) {		
		OperationResult res = new OperationResult();
		
		Task info = task.getTask();
		Long auditid = StringUtil.parseInt(info.getAuditId());
		
		String msg = task.getMessage(); 
		String log = task.getLog(); 
		
		String progress = task.getProgressMessage();
		long completed = task.getAmountCompleted();
		long steps = task.getSteps();
		long step = task.getCurrentStep();
		String sname = task.getCurrentStepName();
    	
		SqlDatabase db = Hub.instance.getSQLDatabase();
		
        String sql = "UPDATE dcWorkAudit SET dcCode = ?, dcMessage = ?, dcLogFile = ?, dcProgress = ?, dcCompleted = ?, dcSteps = ?, dcStep = ?, dcStepName = ? WHERE Id = ?";
        
		FuncResult<Integer> ires2 = db.executeUpdate(sql, 
				task.getCode(),
				StringUtil.isNotEmpty(msg) ? msg : SqlNull.Text,
				StringUtil.isNotEmpty(log) ? log : SqlNull.Text,
				StringUtil.isNotEmpty(progress) ? progress : SqlNull.Text,
				completed,
				steps,
				step,
				StringUtil.isNotEmpty(sname) ? sname : SqlNull.Text,				
				auditid
		);
        
		res.copyMessages(ires2);
        
		if (ires2.getResult() != 1) 
			res.errorTr(163, task.getTask().getId());
		
		// don't overwrite endstamp, unless ended is set
		if (ended) {
	        sql = "UPDATE dcWorkAudit SET dcEndStamp = " + db.nowFunc() + " WHERE Id = ?";
	        
			ires2 = db.executeUpdate(sql, auditid);
	        
			res.copyMessages(ires2);
		}
		
		return res;
	}
	
	@Override
	public ListStruct list() {
		ListStruct res = new ListStruct();
		
		SqlDatabase db = Hub.instance.getSQLDatabase();
		
		if (db == null)
			return res;
			
		FuncResult<ListStruct> rsres = db.executeQuery(
				new SqlSelect[] { 
						new SqlSelectInteger("w.Id", "Id", 0), 
						new SqlSelectString("w.dcTaskIdentity", "TaskIdentity", null), 
						new SqlSelectString("w.dcTitle", "Title", null), 
						new SqlSelectString("w.dcStatus", "Status", null), 
						new SqlSelectSqlDateTime("q.dcClaimedStamp", "ClaimedAt", null), 
						new SqlSelectInteger("q.dcClaimTimeout", "ClaimTimeout", 0), 
						new SqlSelectString("q.dcClaimedHub", "ClaimedBy", null), 
						new SqlSelectInteger("w.dcCurrentTry", "LastTry", 0), 
						new SqlSelectInteger("w.dcMaxTries", "MaxTry", 0), 
						new SqlSelectSqlDateTime("w.dcAddStamp", "Added", null) 
				}, 
				"dcWorkQueue AS q INNER JOIN dcWork AS w ON (q.dcWorkId = w.Id)",					// from 
				null, 							// where
				null, 							// group by
				"w.dcAddStamp" 					// order by
		);
		
		if (rsres.hasErrors())
			return res;
		
		for (Struct rss : rsres.getResult().getItems()) {
			RecordStruct rec = (RecordStruct)rss;
			
			FuncResult<ListStruct> rsres2 = db.executeQuery(
					new SqlSelect[] { 
							new SqlSelectInteger("dcTryNum", "Try", 0), 
							new SqlSelectString("dcHub", "Hub", null), 
							new SqlSelectString("dcMessage", "Message", null), 
							new SqlSelectInteger("dcCode", "Code", 0) 
					}, 
					"dcWorkAudit",					// from 
					"dcWorkId = ?", 							// where
					null, 							// group by
					"dcTryNum", 				// order by
					rec.getFieldAsInteger("Id", 0)			// param 1 - work id
			);
			
			rec.setField("Audit", rsres2.getResult());
			
			res.addItem(rec);
		}
		
		return res;
	}
	
	@Override
	public RecordStruct status(String taskid, String workid) {
		SqlDatabase db = Hub.instance.getSQLDatabase();
		
		if (db == null)
			return null;
			
		FuncResult<RecordStruct> rsres = db.executeQueryRecord(
				new SqlSelect[] { 
						new SqlSelectString("w.Id", "WorkId", null), 
						new SqlSelectString("w.dcTaskIdentity", "TaskId", null), 
						new SqlSelectString("w.dcStatus", "Status", null), 
						new SqlSelectString("w.dcTitle", "Title", null), 
						new SqlSelectInteger("w.dcMaxTries", "MaxTry", 0), 
						new SqlSelectSqlDateTime("w.dcAddStamp", "Added", null),
						
						new SqlSelectSqlDateTime("a.dcStartStamp", "Start", null), 
						new SqlSelectSqlDateTime("a.dcEndStamp", "End", null), 
						new SqlSelectString("dcHub", "Hub", null),
						
						new SqlSelectInteger("a.dcTryNum", "Try", 0), 
						new SqlSelectInteger("a.dcCode", "Code", 0), 
						new SqlSelectString("a.dcMessage", "Message", null), 
						new SqlSelectString("a.dcLogFile", "Log", null), 
						new SqlSelectString("a.dcProgress", "Progress", null), 
						new SqlSelectString("a.dcStepName", "StepName", null), 
						new SqlSelectInteger("a.dcCompleted", "Completed", 0), 
						new SqlSelectInteger("a.dcStep", "Step", 0), 
						new SqlSelectInteger("a.dcSteps", "Steps", 0)
				}, 
				"dcWork AS w INNER JOIN dcWorkAudit AS a ON (a.dcWorkId = w.Id)",					// from 
				"w.Active = 1 AND w.dcTaskIdentity = ? AND w.Id = ?", 								// where
				taskid,
				StringUtil.parseInt(workid)
		);
		
		if (rsres.hasErrors() || rsres.isEmptyResult())
			return null;
		
		return rsres.getResult();
	}
}
