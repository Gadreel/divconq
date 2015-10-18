package divconq.cms.feed.proc;

import java.util.function.Consumer;

import org.joda.time.DateTime;

import divconq.db.DatabaseInterface;
import divconq.db.ICollector;
import divconq.db.DatabaseTask;
import divconq.db.util.ByteUtil;
import divconq.lang.op.OperationResult;
import divconq.struct.RecordStruct;

public class FeedScan implements ICollector {
	@Override
	public void collect(DatabaseInterface conn, DatabaseTask task, OperationResult log, RecordStruct collector, Consumer<Object> uniqueConsumer) {
		RecordStruct extras = collector.getFieldAsRecord("Extras");
		
		// TODO verify fields
		
		String chan = extras.getFieldAsString("Channel");
		DateTime fromdate = extras.getFieldAsDateTime("FromDate");
		Object lasttime = null;
		
		if (fromdate != null)
			lasttime = conn.inverseTime(fromdate);
		
		DateTime todate = extras.getFieldAsDateTime("ToDate");
		Long totime = null;
		
		if (todate != null)
			totime = conn.inverseTime(todate);
		
		Object lastid = extras.getFieldAsString("LastId");
		long max = extras.getFieldAsInteger("Max", 100);
		long cnt = 0;
		
		// TODO add Tags filter
		
		// TODO how to do the Preview index

		/*
		 * ^dcmFeedIndex(did, channel, publish datetime, id)=[content tags]
		 */
		
		String did = task.getDomain();
		
		try {
			if (lasttime == null) 
				lasttime = ByteUtil.extractValue(conn.nextPeerKey("dcmFeedIndex", did, chan, null));
			
			while ((cnt < max) && (lasttime != null) && ((totime == null) || (totime.compareTo(((Number) lasttime).longValue()) > 0))) {
				lastid = ByteUtil.extractValue(conn.nextPeerKey("dcmFeedIndex", did, chan, lasttime, lastid));		// might return null

				// try the next publish time
				if (lastid == null) {
					lasttime = ByteUtil.extractValue(conn.nextPeerKey("dcmFeedIndex", did, chan, lasttime));
					
					continue;
				}
				
				// TODO check tags
				
				uniqueConsumer.accept(lastid);
			
				cnt++;
			}
		}
		catch (Exception x) {
			log.error("Error scanning feed index: " + x);
		}
	}
}
