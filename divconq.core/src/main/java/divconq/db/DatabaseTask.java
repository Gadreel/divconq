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
package divconq.db;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import divconq.hub.Hub;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.builder.ICompositeBuilder;

/**
 * Internal class used to track a request and response across thread boundaries
 * (after being submitted to queue) and also assists with the post processing
 * of results and notifying the submitter via the callback.
 * 
 * @author Andy
 *
 */
public class DatabaseTask {
	protected DatabaseResult result = null;		// this is the official OC
	protected RecordStruct request = null;
	protected List<String> domains = null;
	
	public DatabaseResult getResult() {
		return this.result;
	}
	
	public void setResult(DatabaseResult v) {
		this.result = v;
	}
	
	public void setRequest(RecordStruct v) {
		this.request = v;
	}
	
	public ICompositeBuilder getBuilder() {
		return this.result.getResult();
	}
	
	public RecordStruct getRequest() {
		return this.request;
	}
	
	public String getDomain() {
		if ((this.domains == null) || (this.domains.size() == 0))		
			return this.request.getFieldAsString("Domain");
		
		return this.domains.get(this.domains.size() - 1);
	}
	
	public BigDecimal getStamp() {
		return this.request.getFieldAsDecimal("Stamp");
	}
	
	public String getName() {
		return this.request.getFieldAsString("Name");
	}
	
	public boolean isReplicating() {
		return this.request.getFieldAsBooleanOrFalse("Replicating");
	}
	
	public CompositeStruct getParams() {
		return this.request.getFieldAsComposite("Params");
	}

	public RecordStruct getParamsAsRecord() {
		return this.request.getFieldAsRecord("Params");
	}

	public ListStruct getParamsAsList() {
		return this.request.getFieldAsList("Params");
	}
	
	/**
	 * Called after "result" is filled.  Sets about with post processing and call backs.
	 */
	public void complete() {
		this.result.useContext();	// this is OK because the firing thread does not have a significant ctx 
		
		// TODO change this to a Validate call on DatabaseResult
		if (this.result.getResult() instanceof ObjectResult) {
			// TODO not currently working, review
			CompositeStruct res = ((ObjectResult)this.result.getResult()).getResultAsComposite();
			Hub.instance.getSchema().validateProcResponse(this.request.getFieldAsString("Name"), res);
		}
		
		this.result.complete();
	}

	public void pushDomain(String did) {
		if (this.domains == null)
			this.domains = new ArrayList<>();
		
		this.domains.add(did);
	}

	public void popDomain() {
		if (this.domains != null)
			this.domains.remove(this.domains.size() - 1);
	}
}
