package divconq.cms.importer;

import java.nio.file.Files;

import divconq.db.DataRequest;
import divconq.db.ObjectResult;
import divconq.db.ReplicatedDataRequest;
import divconq.hub.Hub;
import divconq.lang.CountDownCallback;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

public class PageImporter extends FileImporter {
	protected XElement pubxml = null;
	protected XElement prexml = null;
	
	@Override
	public void preCheck(ImportWebsiteTool util) {
		if (this.pubfile != null) {
			if (Files.exists(this.pubfile)) {
				FuncResult<XElement> res = XmlReader.loadFile(this.pubfile, true);
				
				if (res.hasErrors())
					OperationContext.get().error("Bad page file - " + this.key + " | " + this.pubfile);
				
				this.pubxml = res.getResult();
			}
		}
		
		if (this.prefile != null) {
			if (Files.exists(this.prefile)) {
				FuncResult<XElement> res = XmlReader.loadFile(this.prefile, true);
				
				if (res.hasErrors())
					OperationContext.get().error("Bad page file - " + this.key + " | " + this.prefile);
				
				this.prexml = res.getResult();
			}
		}
	}

	@Override
	public void doImport(ImportWebsiteTool util, CountDownCallback cd) {
		// 			<!-- these others become fields: Title, Keywords, Description, Published -->

		cd.increment();
		
		RecordStruct feed = new RecordStruct()
			.withField("Channel", this.alias)
			.withField("Path", this.key)
			.withField("Editable", false);
		
		if (this.pubxml != null) {
			String authtags = this.pubxml.getAttribute("AuthTags");
			
			if (StringUtil.isEmpty(authtags))
				feed.withField("AuthorizationTags", new ListStruct());
			else
				feed.withField("AuthorizationTags", new ListStruct((Object[]) authtags.split(",")));
			
			// public fields
			
			String primelocale = pubxml.getAttribute("Locale");
			
			if (StringUtil.isEmpty(primelocale))
				primelocale = OperationContext.get().getDomain().getLocale();
	
			ListStruct pubfields = new ListStruct();
			feed.withField("Fields", pubfields);
			
			pubfields.addItem(new RecordStruct()
				.withField("Name", "Title")
				.withField("Locale", primelocale)
				.withField("Value", this.pubxml.getAttribute("Title"))		// TODO probably clean out the macros or run them
			);
			
			XElement kwords = this.pubxml.find("Keywords");

			if (kwords != null)
				pubfields.addItem(new RecordStruct()
					.withField("Name", "Keywords")
					.withField("Locale", primelocale)
					.withField("Value", kwords.getValue())		// TODO probably clean out the macros or run them
				);
			
			XElement desc = this.pubxml.find("Description");
			
			if (desc != null)
				pubfields.addItem(new RecordStruct()
					.withField("Name", "Description")
					.withField("Locale", primelocale)
					.withField("Value", desc.getValue())		// TODO probably clean out the macros or run them
				);
			
			/* TODO
			ListStruct pubparts = new ListStruct();
			feed.withField("PartContent", pubparts);
			*/
		}
		
		if (this.prexml != null) {
			// preview fields
			
			String primelocale = prexml.getAttribute("Locale"); 
			
			if (StringUtil.isEmpty(primelocale))
				primelocale = OperationContext.get().getDomain().getLocale();
	
			ListStruct prefields = new ListStruct();
			feed.withField("PreviewFields", prefields);
			
			prefields.addItem(new RecordStruct()
				.withField("Name", "Title")
				.withField("Locale", primelocale)
				.withField("Value", this.prexml.getAttribute("Title"))		// TODO probably clean out the macros or run them
			);
			
			XElement kwords = this.prexml.find("Keywords");

			if (kwords != null)
				prefields.addItem(new RecordStruct()
					.withField("Name", "Keywords")
					.withField("Locale", primelocale)
					.withField("Value", kwords.getValue())		// TODO probably clean out the macros or run them
				);
			
			XElement desc = this.prexml.find("Description");
			
			if (desc != null)
				prefields.addItem(new RecordStruct()
					.withField("Name", "Description")
					.withField("Locale", primelocale)
					.withField("Value", desc.getValue())		// TODO probably clean out the macros or run them
				);
	
			/* TODO
			ListStruct preparts = new ListStruct();
			feed.withField("PreviewPartContent", preparts);
			*/
		}
			
		// don't bother checking if it worked in our response to service
		DataRequest req3b = new ReplicatedDataRequest("dcmFeedUpdate")
			.withParams(feed);

		Hub.instance.getDatabase().submit(req3b, new ObjectResult() {
			@Override
			public void process(CompositeStruct result3b) {
				cd.countDown();
			}
		});
	}
}
