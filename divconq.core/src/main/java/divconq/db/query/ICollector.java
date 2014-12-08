package divconq.db.query;

import divconq.struct.Struct;

/**
 * A collector is used to provide record ids to a Select or List request.  If a collector
 * is not used then all the records in the table are checked (where condition) and
 * potentially returned.  With a collector, only the ids provided by the collector 
 * are considered.
 * 
 * @author Andy
 *
 */
public interface ICollector {
	/**
	 * @return query parameters 
	 */
	Struct getParams();
}
