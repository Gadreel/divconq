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
package divconq.sql;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import divconq.hub.Hub;
import divconq.lang.FuncResult;
import divconq.lang.OperationContext;
import divconq.lang.OperationResult;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.struct.scalar.NullStruct;
import divconq.util.StringUtil;
import divconq.xml.XElement;

// TODO there is much much more to do to make this easier to support many different db engines
public class SqlManager {
	public final DateTimeFormatter stampFmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");

	protected Map<String, SqlDatabase> databases = new HashMap<String, SqlManager.SqlDatabase>();
	
	public String getNowAsString() {
		return this.stampFmt.print(new DateTime(DateTimeZone.UTC));
	}
	
	public String getDateAsString(DateTime dt) {
		return this.stampFmt.print(dt.toDateTime(DateTimeZone.UTC));
	}
	
	public String getDateAsString(long dt) {
		return this.stampFmt.print(new DateTime(dt, DateTimeZone.UTC));
	}
	
	public SqlDatabase getDatabase(String name) {
		return this.databases.get(name);
	}
	
	public void init(OperationResult or, XElement config) {
		if (config == null)
			return;
		
		for (XElement del : config.selectAll("Database")) {
			String name = del.getAttribute("Name", "default");
			
			SqlDatabase db = new SqlDatabase();
			db.name = name;
			db.init(or, del);
			
			this.databases.put(name, db);
		}
	}
	
	public void stop() {
		for (SqlDatabase db : this.databases.values())
			db.stop();
	}
	
	public class SqlDatabase {
		protected String connstring = null;
		protected String name = null;
		
		protected SqlEngine engine = null;
		
		// single connection style engines
		protected Connection conn = null;	
		protected Semaphore lock = new Semaphore(0);
		
		protected boolean poolmode = false;
		protected ConcurrentLinkedQueue<Connection> pool = new ConcurrentLinkedQueue<>();
	
		public void init(OperationResult or, XElement del) {
			if (del == null) 
				return;
	
			try {
				String driver = del.getAttribute("Driver");
				
		        Class.forName(driver);
	    		
		        this.connstring = Hub.instance.getClock().getObfuscator().decryptHexToString(
	    				del.getAttribute("Connection")
	    		);
		        
		        // if null then try unencrypted
		        if (this.connstring == null)
		        	this.connstring = del.getAttribute("Connection");
		        
		        if (this.connstring.startsWith("jdbc:h2:"))
		        	this.engine = SqlEngine.H2;
		        else if (this.connstring.startsWith("jdbc:sqlserver:"))
		        	this.engine = SqlEngine.SqlServer;
		        else if (this.connstring.startsWith("jdbc:mariadb:"))
		        	this.engine = SqlEngine.MariaDb;
		        else if (this.connstring.startsWith("jdbc:mysql:"))
		        	this.engine = SqlEngine.MySQL;
		        else {
					or.errorTr(189, this.connstring.substring(0, Math.min(this.connstring.length(), 15)));
					return;
		        }
		        
		        this.poolmode = "Pooled".equals(del.getAttribute("Mode"));
		        
		        if (this.engine == SqlEngine.SqlServer || this.engine == SqlEngine.MariaDb || this.engine == SqlEngine.MySQL) {
			        or.info(0, "Using database " + this.name + " with multiple connections.");
		        }
		        else {
			        this.conn = DriverManager.getConnection(this.connstring);
			        
			        or.info(0, "Connected to database " + this.name + " single connection.");
				
			        this.releaseConnection(this.conn);
		        }
			} 
			catch (Exception x) {
				or.errorTr(190, this.name, x);
				return;
			}
		}
		
		public void stop() {
			try {
				if (this.engine == SqlEngine.SqlServer || this.engine == SqlEngine.MariaDb || this.engine == SqlEngine.MySQL) {
					if (this.poolmode) {
						Connection conn = this.pool.poll();
						
						while (conn != null) {
							try {
								conn.close();
							} 
							catch (Exception x) {
								// unimportant
							}
							
							conn = this.pool.poll();
						}
					}
				}
				else if (this.engine == SqlEngine.H2) 
					this.conn.close();
			} 
			catch (Exception x) {
				// unimportant
			}
		}
		
		public Connection acquireConnection() {
			Hub.instance.getCountManager().allocateNumberCounter("dcSqlAcquireConnection").increment();
			
			if (this.engine == SqlEngine.SqlServer  || this.engine == SqlEngine.MariaDb  || this.engine == SqlEngine.MySQL) {
		        try {
					Connection conn = null;

					if (this.poolmode) {
						conn = this.pool.poll();
						
						if ((conn != null) && !conn.isValid(2)) 
							conn = null;
					}
					
					if (conn == null) {
						conn = DriverManager.getConnection(this.connstring);
						Hub.instance.getCountManager().allocateNumberCounter("dcSqlConnectionCreate").increment();
					}
					
					return conn;
				} 
		        catch (SQLException x) {
				}
	        }
	        else if (this.engine == SqlEngine.H2) {
				try {
					this.lock.acquire();
					return this.conn;
				} 
				catch (InterruptedException e) {
				}
	        }
			
			Hub.instance.getCountManager().allocateNumberCounter("dcSqlAcquireConnectionFail").increment();
			
			return null;
		}
		
		public void releaseConnection(Connection conn) {
			Hub.instance.getCountManager().allocateNumberCounter("dcSqlReleaseConnection").increment();
			
			if (this.engine == SqlEngine.SqlServer || this.engine == SqlEngine.MariaDb || this.engine == SqlEngine.MySQL) {
				if (this.poolmode) {
					this.pool.add(conn);
					return;
				}
				
		        try {
					conn.close();
				} 
		        catch (SQLException x) {
				}
	        }
	        else if (this.engine == SqlEngine.H2) {
	        	this.lock.release();
	        }
		}

		public SqlEngine getEngine() {
			return this.engine;
		}
		
		public boolean testConnection() {
			Connection conn = this.acquireConnection();
			
			if (conn == null) 
				return false;
		    
	    	this.releaseConnection(conn);
	    	
		    return true;
		}
		
		// warning - may not use same connection between calls
		public String getLastIdSql() {
			if (this.engine == SqlEngine.H2)
				return "SELECT IDENTITY() AS lid";
			
			if (this.engine == SqlEngine.SqlServer)
				return "SELECT @@IDENTITY AS lid";
			
			if (this.engine == SqlEngine.MariaDb || this.engine == SqlEngine.MySQL)
				return "SELECT LAST_INSERT_ID() AS lid";
			
			return null;
		}
		
		/**
		 * We should always talk in UTC...
		 *  
		 * @return
		 */
		public String nowFunc() {
			if (this.getEngine() == SqlEngine.SqlServer)
				return "GETUTCDATE()";
			
			if (this.getEngine() == SqlEngine.MariaDb || this.getEngine() == SqlEngine.MySQL)
				return "UTC_TIMESTAMP()";

			return "NOW()";
		}
		
		// TODO
		// only support MINUTES at present
		public String timeUnit(TimeUnit unit) {
			if (unit == TimeUnit.MINUTES)
				return "MINUTE";
			
			return null;
		}
		
		public String modNowFunc(TimeUnit unit, int amt) {
			return this.modTimeFunc(this.nowFunc(), unit, amt);
		}
		
		public String modTimeFunc(String time, TimeUnit unit, int amt) {
			String unitname = this.timeUnit(unit);
			
			// h2 syntax
			String expr = "DATEADD('" + unitname + "', " + amt + ", " + time + ") ";
			
			if (this.getEngine() == SqlEngine.SqlServer)
				expr = "DATEADD(" + unitname + ", " + amt + ", " + time + ") ";
			
			if (this.getEngine() == SqlEngine.MariaDb || this.getEngine() == SqlEngine.MySQL)
				expr = "DATE_ADD(" + time + ", INTERVAL " + amt + " " + unitname + ") ";
			
			return expr;
		}
		
		public String formatColumn(String name) {
			if (this.getEngine() == SqlEngine.SqlServer)
				return "[" + name + "]";
			
			if (this.getEngine() == SqlEngine.MariaDb || this.getEngine() == SqlEngine.MySQL)
				return "`" + name + "`";

			// TODO check what H2 uses...
			
			return name;
		}
		
		public void processException(Exception x, OperationResult or) {
			if (x instanceof SQLException) {
				SQLException sx = (SQLException)x;
				
				if (this.getEngine() == SqlEngine.MariaDb || this.getEngine() == SqlEngine.MySQL) {
					// duplicate id error - this is not always an error for the caller
					// we treat it as an error return code but log it as an Info
					if (sx.getErrorCode() == 1062) {
						or.exit(194, OperationContext.get().tr("_code_194", this.name, x));
						return;
					}
				}
				else if (this.getEngine() == SqlEngine.SqlServer) {
					if (sx.getErrorCode() == 2627) {
						or.exit(194, OperationContext.get().tr("_code_194", this.name, x));
						return;
					}
				}
				
				// TODO add other databases
				
				or.errorTr(195, this.name, ((SQLException) x).getErrorCode(), x);	    	
				return;
			}
			
			or.errorTr(186, this.name, x);	    	
		}
		
		public FuncResult<Integer> executeFreestyle(String sql, Object... params) {
			FuncResult<Integer> res = new FuncResult<Integer>();
			
			Connection conn = this.acquireConnection();
			
			// if connection is bad/missing then just try again later
			if (conn == null) {
				res.errorTr(185, this.name);
				return res;
			}
		    
		    try {					
			    FuncResult<PreparedStatement> psres = this.prepStatement(conn, sql, params);
			    
			    res.copyMessages(psres);
			    
			    if (res.hasErrors())
			    	return res;
			    
			    PreparedStatement pstmt = psres.getResult();
		    	
				Hub.instance.getCountManager().countObjects("dcSqlExecuteCount", pstmt);
				
				res.setResult(pstmt.executeUpdate());
				
    			pstmt.close();
		    } 
		    catch (Exception x) {
		    	this.processException(x, res);
		    	
				Hub.instance.getCountManager().countObjects("dcSqlExecuteFail", sql);
		    } 
		    finally {
		    	this.releaseConnection(conn);
		    }
		    
			return res;
		}
		
		// return a list of records where each row is a record in this collection
		// -- NOTE: column names are all lower case
		public FuncResult<ListStruct> executeQueryFreestyle(String sql, Object... params) {
			FuncResult<ListStruct> res = new FuncResult<ListStruct>();
			ListStruct list = new ListStruct();
			
			res.setResult(list);
			
			Connection conn = this.acquireConnection();
			
			// if connection is bad/missing then just try again later
			if (conn == null) {
				res.errorTr(185, this.name);
				return res;
			}
			
		    ResultSet rs = null;
		    
		    try {					
			    FuncResult<PreparedStatement> psres = this.prepStatement(conn, sql, params);
			    
			    res.copyMessages(psres);
			    
			    if (res.hasErrors())
			    	return res;
			    
			    PreparedStatement pstmt = psres.getResult();
		    	
				Hub.instance.getCountManager().countObjects("dcSqlQueryCount", pstmt);
		    	
				// MariaDB hint that this turns on streaming... review TODO
				//pstmt.setFetchSize(Integer.MIN_VALUE);
				
				rs = pstmt.executeQuery();
				
				ResultSetMetaData md = rs.getMetaData();
			    int columns = md.getColumnCount();
			    
				while (rs.next()) {
					RecordStruct rec = new RecordStruct();
					
					for(int i=1; i<=columns; i++) 
						rec.setField(md.getColumnLabel(i).toLowerCase(), rs.getObject(i));
					
					list.addItem(rec);
				}				
				
    			pstmt.close();
		    } 
		    catch (Exception x) {
		    	this.processException(x, res);
		    	
				Hub.instance.getCountManager().countObjects("dcSqlQueryFail", sql);
		    } 
		    finally {
		    	try {
		    		if (rs != null)
		    			rs.close();
				} 
		    	catch (SQLException x) {
				}
		    	
		    	this.releaseConnection(conn);
		    }
		    
			return res;
		}
		
		// return a list of records where each row is a record in this collection
		public FuncResult<ListStruct> executeQueryPage(SqlSelect[] select, String from, String where, String groupby, String orderby, int offset, int pagesize, Object... params) {
			FuncResult<ListStruct> res = new FuncResult<ListStruct>();
			
			// acquire
			Connection conn = this.acquireConnection();
			
			// if connection is bad/missing then just try again later
			if (conn == null) {
				res.errorTr(185, this.name);
				return res;
			}
			
			// prepare
		    FuncResult<PreparedStatement> psres = this.prepPage(conn, select, from, where, groupby, orderby, offset, pagesize, params);
		    
		    res.copyMessages(psres);
		    
		    if (res.hasErrors())
		    	return res;
		    
		    PreparedStatement pstmt = psres.getResult();
		    
		    // execute
	    	FuncResult<ListStruct> res2 = callAndFormat(select, pstmt);
	    	res2.copyMessages(res);		    	
	    	res = res2;

	    	try {
	    		if (pstmt != null)
	    			pstmt.close();
			} 
	    	catch (SQLException x) {
			}
	    	
	    	// release
	    	this.releaseConnection(conn);
		    
			return res;
		}
		
		// return a list of records where each row is a record in this collection
		public FuncResult<ListStruct> executeQueryLimit(SqlSelect[] select, String from, String where, String groupby, String orderby, int limit, boolean distinct, Object... params) {
			FuncResult<ListStruct> res = new FuncResult<ListStruct>();
			
			// acquire
			Connection conn = this.acquireConnection();
			
			// if connection is bad/missing then just try again later
			if (conn == null) {
				res.errorTr(185, this.name);
				return res;
			}
			
			// prepare
		    FuncResult<PreparedStatement> psres = this.prepLimit(conn, select, from, where, groupby, orderby, limit, distinct, params);
		    
		    res.copyMessages(psres);
		    
		    if (res.hasErrors())
		    	return res;
		    
		    PreparedStatement pstmt = psres.getResult();
		    
		    // execute
	    	FuncResult<ListStruct> res2 = callAndFormat(select, pstmt);
	    	res2.copyMessages(res);		    	
	    	res = res2;

	    	try {
	    		if (pstmt != null)
	    			pstmt.close();
			} 
	    	catch (SQLException x) {
			}
	    	
	    	// release
	    	this.releaseConnection(conn);
		    
			return res;
		}
		
		// return a list of records where each row is a record in this collection
		public FuncResult<ListStruct> executeQuery(SqlSelect[] select, String from, String where, String groupby, String orderby, Object... params) {
			FuncResult<ListStruct> res = new FuncResult<ListStruct>();
			
			// acquire
			Connection conn = this.acquireConnection();
			
			// if connection is bad/missing then just try again later
			if (conn == null) {
				res.errorTr(185, this.name);
				return res;
			}
			
			// prepare
		    FuncResult<PreparedStatement> psres = this.prep(conn, select, from, where, groupby, orderby, params);
		    
		    res.copyMessages(psres);
		    
		    if (res.hasErrors())
		    	return res;
		    
		    PreparedStatement pstmt = psres.getResult();
		    
		    // execute
	    	FuncResult<ListStruct> res2 = this.callAndFormat(select, pstmt);
	    	res2.copyMessages(res);		    	
	    	res = res2;

	    	try {
	    		if (pstmt != null)
	    			pstmt.close();
			} 
	    	catch (SQLException x) {
			}
	    	
	    	// release
	    	this.releaseConnection(conn);
		    
			return res;
		}
		
		// return a single value (row/column) from table 
		public FuncResult<Struct> executeQueryScalar(SqlSelect select, String from, String where, String orderby, Object... params) {
			FuncResult<Struct> res = new FuncResult<Struct>();
    		res.setResult(NullStruct.instance);
			
			// acquire
			Connection conn = this.acquireConnection();
			
			// if connection is bad/missing then just try again later
			if (conn == null) {
				res.errorTr(185, this.name);
				return res;
			}
			
			SqlSelect[] selects = new SqlSelect[] { select };
			
			// prepare
		    FuncResult<PreparedStatement> psres = this.prep(conn, selects, from, where, null, orderby, params);
		    
		    res.copyMessages(psres);
		    
		    if (res.hasErrors())
		    	return res;
		    
		    PreparedStatement pstmt = psres.getResult();
		    
		    // execute
	    	FuncResult<ListStruct> res2 = this.callAndFormat(selects, pstmt);
	    	res.copyMessages(res2);
	    	
	    	ListStruct lrs = res2.getResult();
	    	
	    	if (lrs.getSize() > 0) {
	    		RecordStruct rec = lrs.getItemAsRecord(0);
	    		
	    		res.setResult(rec.getField(select.name));
	    	}

	    	try {
	    		if (pstmt != null)
	    			pstmt.close();
			} 
	    	catch (SQLException x) {
			}
	    	
	    	// release
	    	this.releaseConnection(conn);
		    
			return res;
		}
		
		// return a single String value (row/column) from table 
		public String executeQueryString(OperationResult log, String col, String from, String where, String orderby, Object... params) {
			FuncResult<Struct> rsres = this.executeQueryScalar(new SqlSelectString(col), from, where, orderby, params);
			
			log.copyMessages(rsres);
			
			if (rsres.hasErrors() || rsres.isEmptyResult()) 
				return null;
			
			return Struct.objectToString(rsres.getResult());
		}
		
		// return a single Integer value (row/column) from table 
		public Long executeQueryInteger(OperationResult log, String col, String from, String where, String orderby, Object... params) {
			FuncResult<Struct> rsres = this.executeQueryScalar(new SqlSelectInteger(col), from, where, orderby, params);
			
			log.copyMessages(rsres);
			
			if (rsres.hasErrors() || rsres.isEmptyResult()) 
				return null;
			
			return Struct.objectToInteger(rsres.getResult());
		}
		
		// return a single Boolean value (row/column) from table 
		public Boolean executeQueryBoolean(OperationResult log, String col, String from, String where, String orderby, Object... params) {
			FuncResult<Struct> rsres = this.executeQueryScalar(new SqlSelectBoolean(col), from, where, orderby, params);
			
			log.copyMessages(rsres);
			
			if (rsres.hasErrors() || rsres.isEmptyResult()) 
				return null;
			
			return Struct.objectToBoolean(rsres.getResult());
		}
		
		// return a single row from table 
		public FuncResult<RecordStruct> executeQueryRecord(SqlSelect[] selects, String from, String where, Object... params) {
			FuncResult<RecordStruct> res = new FuncResult<RecordStruct>();
			
			// acquire
			Connection conn = this.acquireConnection();
			
			// if connection is bad/missing then just try again later
			if (conn == null) {
				res.errorTr(185, this.name);
				return res;
			}
			
			// prepare
		    FuncResult<PreparedStatement> psres = this.prep(conn, selects, from, where, null, null, params);
		    
		    res.copyMessages(psres);
		    
		    if (res.hasErrors())
		    	return res;
		    
		    PreparedStatement pstmt = psres.getResult();
		    
		    // execute
	    	FuncResult<ListStruct> res2 = this.callAndFormat(selects, pstmt);
	    	res2.copyMessages(res);
	    	
	    	ListStruct lrs = res2.getResult();
	    	
	    	if (lrs.getSize() > 0) 
	    		res.setResult(lrs.getItemAsRecord(0));
	    	else
	    		res.setResult(new RecordStruct());	// always return something - caller should do an empty check

	    	try {
	    		if (pstmt != null)
	    			pstmt.close();
			} 
	    	catch (SQLException x) {
			}
	    	
	    	// release
	    	this.releaseConnection(conn);
		    
			return res;
		}
		
		// return a single row (the first) from table 
		public FuncResult<RecordStruct> executeQueryRecordFirst(SqlSelect[] selects, String from, String where, String orderby, Object... params) {
			FuncResult<RecordStruct> res = new FuncResult<RecordStruct>();
			
			// acquire
			Connection conn = this.acquireConnection();
			
			// if connection is bad/missing then just try again later
			if (conn == null) {
				res.errorTr(185, this.name);
				return res;
			}
			
			// prepare
		    FuncResult<PreparedStatement> psres = this.prep(conn, selects, from, where, null, orderby, params);
		    
		    res.copyMessages(psres);
		    
		    if (res.hasErrors())
		    	return res;
		    
		    PreparedStatement pstmt = psres.getResult();
		    
		    // execute
	    	FuncResult<ListStruct> res2 = this.callAndFormat(selects, pstmt);
	    	res2.copyMessages(res);
	    	
	    	ListStruct lrs = res2.getResult();
	    	
	    	if (lrs.getSize() > 0) 
	    		res.setResult(lrs.getItemAsRecord(0));
	    	else
	    		res.setResult(new RecordStruct());	// always return something - caller should do an empty check

	    	try {
	    		if (pstmt != null)
	    			pstmt.close();
			} 
	    	catch (SQLException x) {
			}
	    	
	    	// release
	    	this.releaseConnection(conn);
		    
			return res;
		}
		
		// return a list of records where each row is a record in this collection
		public FuncResult<ListStruct> callAndFormat(SqlSelect[] select, PreparedStatement pstmt) {
			FuncResult<ListStruct> res = new FuncResult<ListStruct>();
			ListStruct list = new ListStruct();
			
			res.setResult(list);
			
		    ResultSet rs = null;
		    
		    try {					
				Hub.instance.getCountManager().countObjects("dcSqlQueryCount", pstmt);
		    	
				// MariaDB hint that this turns on streaming... review TODO
				//pstmt.setFetchSize(Integer.MIN_VALUE);
				
				rs = pstmt.executeQuery();
				
				ResultSetMetaData md = rs.getMetaData();
			    int columns = md.getColumnCount();
			    
			    if (columns > select.length) {
					res.error(1, "Mismatched column name list");		// TODO code tr
					return res;
			    }
			    
				while (rs.next()) {
					RecordStruct rec = new RecordStruct();
					
					for(int i=1; i<=columns; i++) {
						String name = select[i - 1].name;
						
						rec.setField(name, select[i - 1].format(rs.getObject(i)));
					}
					
					list.addItem(rec);
				}				
		    } 
		    catch (Exception x) {
		    	this.processException(x, res);
		    	
				Hub.instance.getCountManager().countObjects("dcSqlQueryFail", pstmt.toString());
		    } 
		    finally {
		    	try {
		    		if (rs != null)
		    			rs.close();
				} 
		    	catch (SQLException x) {
				}
		    }
		    
			return res;
		}

		// caller needs to close statememt
		// will not return open statement and errors
		public FuncResult<PreparedStatement> prepStatement(Connection conn, String sql, Object... params) {
			FuncResult<PreparedStatement> res = new FuncResult<PreparedStatement>();
			
			// if connection is bad/missing then just try again later
			if (conn == null) {
				res.errorTr(185, this.name);
				return res;
			}
			
		    PreparedStatement pstmt = null;
		    
		    try {					
		    	pstmt = conn.prepareStatement(sql); 

		    	for (int i = 0; i < params.length; i++) {
		    		Object param = params[i];
		    		
		    		// null params are intentionally not supported - allows us to optionally add params to a complex query
		    		// for NULL support see SqlNull enum
		    		if (param == null)
		    			continue;
		    		
		    		if (param instanceof DateTime)
		    			param = SqlManager.this.getDateAsString((DateTime) param);
		    		
		    		if (param instanceof String) {
		    			if (this.engine == SqlEngine.MariaDb || this.engine == SqlEngine.MySQL)
		    				pstmt.setString(i + 1, (String)param);
		    			else if (this.engine == SqlEngine.SqlServer) 
		    				pstmt.setNString(i + 1, (String)param);
		    			else if (this.engine == SqlEngine.H2) 
		    				pstmt.setNString(i + 1, (String)param);
		    			
		    			continue;
		    		}
		    		
		    		if (param instanceof BigDecimal) {
		    			pstmt.setBigDecimal(i + 1, (BigDecimal) param);
		    			continue;
		    		}
		    		
		    		if (param instanceof Double) {
		    			pstmt.setDouble(i + 1, (double) param);
		    			continue;
		    		}
		    		
		    		if (param instanceof Integer) {
		    			pstmt.setInt(i + 1, (int) param);
		    			continue;
		    		}
		    		
		    		if (param instanceof Long) {
		    			pstmt.setLong(i + 1, (long) param);
		    			continue;
		    		}
		    		
		    		if (param instanceof SqlNull) {
		    			if (param == SqlNull.DateTime)
		    				pstmt.setNull(i + 1, Types.DATE);
		    			else if (param == SqlNull.VarChar) {
			    			if (this.engine == SqlEngine.MariaDb || this.engine == SqlEngine.MySQL)
			    				pstmt.setNull(i + 1, Types.VARCHAR);
			    			else if (this.engine == SqlEngine.SqlServer) 
			    				pstmt.setNull(i + 1, Types.NVARCHAR);
			    			else if (this.engine == SqlEngine.H2) 
			    				pstmt.setNull(i + 1, Types.NVARCHAR);
		    			}
		    			else if (param == SqlNull.BigDecimal)
		    				pstmt.setNull(i + 1, Types.DECIMAL);
		    			else if (param == SqlNull.Double)
		    				pstmt.setNull(i + 1, Types.FLOAT);
		    			else if (param == SqlNull.Int)
		    				pstmt.setNull(i + 1, Types.INTEGER);
		    			else if (param == SqlNull.Long)
		    				pstmt.setNull(i + 1, Types.BIGINT);
		    			else if (param == SqlNull.Text)
		    				pstmt.setNull(i + 1, Types.CLOB);		// TODO test
		    			
		    			continue;
		    		}
		    	}
				
				res.setResult(pstmt);
		    } 
		    catch (Exception x) {
		    	this.processException(x, res);
		    	
				Hub.instance.getCountManager().countObjects("dcSqlPrepFail", sql);
		    	
		    	try {
		    		if (pstmt != null)
		    			pstmt.close();
				} 
		    	catch (SQLException x2) {
				}
		    } 
		    
			return res;
		}

		// caller needs to close statememt
		public FuncResult<PreparedStatement> prepPage(Connection conn, SqlSelect[] select, String from, String where, String groupby, String orderby, int offset, int pagesize, Object... params) {
			// if connection is bad/missing then just try again later
			if (conn == null) {
				FuncResult<PreparedStatement> res = new FuncResult<PreparedStatement>();
				res.errorTr(185, this.name);
				return res;
			}
		    
		    String sql = "SELECT ";
		    
		    for (int i = 0; i < select.length; i++) {
		    	if (i > 0)
		    		sql += ", ";
		    	
		    	sql += select[i].toSql(this);
		    }
		    
		    sql += " FROM " + from;
		    
		    if (StringUtil.isNotEmpty(where))
		    	sql += " WHERE " + where;
		    
		    if (StringUtil.isNotEmpty(groupby))
		    	sql += " GROUP BY " + groupby;
		    
	        if (StringUtil.isEmpty(orderby)) {
				FuncResult<PreparedStatement> res = new FuncResult<PreparedStatement>();
				res.error(1, "Order By required with paging");
				return res;
			}

			if (this.engine == SqlEngine.MariaDb || this.engine == SqlEngine.MySQL) {
		        sql = "SELECT * FROM ( " + sql + " ) AS recset ORDER BY " 
		        	+ orderby + " LIMIT " + offset + "," + pagesize + ";";
			}
			else if (this.engine == SqlEngine.SqlServer) {
		        sql = "WITH RecordPager AS ( " 
		        		+ "SELECT *, ROW_NUMBER() OVER (ORDER BY " + orderby + ") AS RowNumber " 
		        		+ "FROM ( " + sql + " ) AS recset " 
		       		+ ") "
		       		+ "SELECT * FROM RecordPager WHERE RowNumber BETWEEN " + (offset + 1) 
		       		+ " AND " + (offset + pagesize);				
			}
			else if (this.engine == SqlEngine.H2) {
				// TODO
			}
		    
			// TODO support for other dbms
			// http://en.wikipedia.org/wiki/Select_(SQL)
			// http://stackoverflow.com/questions/2771439/jdbc-pagination
			// http://stackoverflow.com/questions/1986998/resultset-to-pagination
			// http://stackoverflow.com/questions/971964/limit-10-20-in-sqlserver
			
			return this.prepStatement(conn, sql, params);
		}

		// caller needs to close statememt
		public FuncResult<PreparedStatement> prepLimit(Connection conn, SqlSelect[] select, String from, String where, String groupby, String orderby, int limit, boolean distinct, Object... params) {
			// if connection is bad/missing then just try again later
			if (conn == null) {
				FuncResult<PreparedStatement> res = new FuncResult<PreparedStatement>();
				res.errorTr(185, this.name);
				return res;
			}
		    
		    String sql = "SELECT ";
		    
		    if (distinct)
		    	sql += "DISTINCT ";
		    
		    for (int i = 0; i < select.length; i++) {
		    	if (i > 0)
		    		sql += ", ";
		    	
		    	sql += select[i].toSql(this);
		    }
		    
		    sql += " FROM " + from;
		    
		    if (StringUtil.isNotEmpty(where))
		    	sql += " WHERE " + where;
		    
		    if (StringUtil.isNotEmpty(groupby))
		    	sql += " GROUP BY " + groupby;
		    
	        if (StringUtil.isEmpty(orderby)) {
				FuncResult<PreparedStatement> res = new FuncResult<PreparedStatement>();
				res.error(1, "Order By required with limit");
				return res;
			}

			if (this.engine == SqlEngine.MariaDb || this.engine == SqlEngine.MySQL) {
		        sql = "SELECT * FROM ( " + sql + " ) AS unset ORDER BY " + orderby + " LIMIT " + limit + ";";
			}
			else if (this.engine == SqlEngine.SqlServer) {
		        sql = "SELECT TOP " + limit + " * FROM ( " + sql + " ) AS unset ORDER BY " + orderby;				
			}
			else if (this.engine == SqlEngine.H2) {
				// TODO
			}
		    
			return this.prepStatement(conn, sql, params);
		}

		// caller needs to close statememt
		public FuncResult<PreparedStatement> prep(Connection conn, SqlSelect[] select, String from, String where, String groupby, String orderby, Object... params) {
			// if connection is bad/missing then just try again later
			if (conn == null) {
				FuncResult<PreparedStatement> res = new FuncResult<PreparedStatement>();
				res.errorTr(185, this.name);
				return res;
			}
		    
		    String sql = "SELECT ";
		    
		    for (int i = 0; i < select.length; i++) {
		    	if (i > 0)
		    		sql += ", ";
		    	
		    	sql += select[i].toSql(this);
		    }
		    
		    sql += " FROM " + from;
		    
		    if (StringUtil.isNotEmpty(where))
		    	sql += " WHERE " + where;
		    
		    if (StringUtil.isNotEmpty(groupby))
		    	sql += " GROUP BY " + groupby;
		    
		    if (StringUtil.isNotEmpty(orderby))
		    	sql += " ORDER BY " + orderby;
		    
			return this.prepStatement(conn, sql, params);
		}
		
		public FuncResult<Integer> executeUpdate(String sql, Object... params) {
			FuncResult<Integer> res = new FuncResult<Integer>();
			res.setResult(0);
			
			// acquire
			Connection conn = this.acquireConnection();
			
			// if connection is bad/missing then just try again later
			if (conn == null) {
				res.errorTr(185, this.name);
				return res;
			}
			
			// prep
		    FuncResult<PreparedStatement> psres = this.prepStatement(conn, sql, params);
		    
		    res.copyMessages(psres);
		    
		    if (res.hasErrors())
		    	return res;
		    
		    PreparedStatement pstmt = psres.getResult();
		    
		    try {					
				Hub.instance.getCountManager().countObjects("dcSqlUpdateCount", pstmt);
		    
				// execute
				int cnt = pstmt.executeUpdate(); 

				res.setResult(cnt);
		    } 
		    catch (Exception x) {
		    	this.processException(x, res);
		    	
				Hub.instance.getCountManager().countObjects("dcSqlUpdateFail", sql);
		    } 
		    finally {
		    	try {
		    		if (pstmt != null)
		    			pstmt.close();
				} 
		    	catch (SQLException x) {
				}
		    	
		    	// release
		    	this.releaseConnection(conn);
		    }
		    
			return res;
		}
		
		public FuncResult<Integer> executeDelete(String sql, Object... params) {
			FuncResult<Integer> res = new FuncResult<Integer>();
			res.setResult(0);
			
			// acquire
			Connection conn = this.acquireConnection();
			
			// if connection is bad/missing then just try again later
			if (conn == null) {
				res.errorTr(185, this.name);
				return res;
			}
			
			// prep
		    FuncResult<PreparedStatement> psres = this.prepStatement(conn, sql, params);
		    
		    res.copyMessages(psres);
		    
		    if (res.hasErrors())
		    	return res;
		    
		    PreparedStatement pstmt = psres.getResult();
		    
		    try {					
				Hub.instance.getCountManager().countObjects("dcSqlDeleteCount", pstmt);
		    
				// execute
				int cnt = pstmt.executeUpdate(); 

				res.setResult(cnt);
		    } 
		    catch (Exception x) {
		    	this.processException(x, res);
		    	
				Hub.instance.getCountManager().countObjects("dcSqlDeleteFail", sql);
		    } 
		    finally {
		    	try {
		    		if (pstmt != null)
		    			pstmt.close();
				} 
		    	catch (SQLException x) {
				}
		    	
		    	// release
		    	this.releaseConnection(conn);
		    }
		    
			return res;
		}
				
		public FuncResult<Long> executeInsertReturnId(String sql, Object... params) {
			FuncResult<Long> res = new FuncResult<Long>();
			res.setResult(0L);
			
			// acquire
			Connection conn = this.acquireConnection();
			
			// if connection is bad/missing then just try again later
			if (conn == null) {
				res.errorTr(185, this.name);
				return res;
			}
			
			// prep
		    FuncResult<PreparedStatement> psres = this.prepStatement(conn, sql, params);
		    
		    res.copyMessages(psres);
		    
		    if (res.hasErrors())
		    	return res;
		    
		    PreparedStatement pstmt = psres.getResult();
		    
		    try {					
				Hub.instance.getCountManager().countObjects("dcSqlInsertCount", pstmt);
		    	
				// execute
				int cnt = pstmt.executeUpdate(); 

				pstmt.close();
				
				if (cnt == 1) {
					pstmt = conn.prepareStatement(this.getLastIdSql());
					
					ResultSet rs = pstmt.executeQuery();
					
					if (rs.next()) 
						res.setResult(rs.getLong("lid"));
				}
		    } 
		    catch (Exception x) {
		    	this.processException(x, res);
		    	
				Hub.instance.getCountManager().countObjects("dcSqlInsertFail", sql);
		    } 
		    finally {
		    	try {
		    		if (pstmt != null)
		    			pstmt.close();
				} 
		    	catch (SQLException x) {
				}
		    	
		    	// release
		    	this.releaseConnection(conn);
		    }
		    
			return res;
		}

		// TODO look into "get generated keys"
		public FuncResult<Integer> executeInsert(String sql, Object... params) {
			FuncResult<Integer> res = new FuncResult<Integer>();
			res.setResult(0);
			
			// acquire
			Connection conn = this.acquireConnection();
			
			// if connection is bad/missing then just try again later
			if (conn == null) {
				res.errorTr(185, this.name);
				return res;
			}
			
			// prep
		    FuncResult<PreparedStatement> psres = this.prepStatement(conn, sql, params);
		    
		    res.copyMessages(psres);
		    
		    if (res.hasErrors())
		    	return res;
		    
		    PreparedStatement pstmt = psres.getResult();
		    
		    try {					
				Hub.instance.getCountManager().countObjects("dcSqlInsertCount", pstmt);
		    	
				// execute
				int cnt = pstmt.executeUpdate(); 

				pstmt.close();

				res.setResult(cnt);
		    } 
		    catch (Exception x) {
		    	this.processException(x, res);
		    	
				Hub.instance.getCountManager().countObjects("dcSqlInsertFail", sql);
		    } 
		    finally {
		    	try {
		    		if (pstmt != null)
		    			pstmt.close();
				} 
		    	catch (SQLException x) {
				}
		    	
		    	// release
		    	this.releaseConnection(conn);
		    }
		    
			return res;
		}
	}
}
