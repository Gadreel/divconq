package divconq.cms.importer;

import java.nio.file.Path;

import divconq.cms.feed.FeedAdapter;
import divconq.db.DataRequest;
import divconq.db.ObjectResult;
import divconq.db.ReplicatedDataRequest;
import divconq.hub.Hub;
import divconq.lang.CountDownCallback;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class FeedImporter extends FileImporter {
	protected FeedAdapter pubfeed = null;
	protected FeedAdapter prefeed = null;
	
	@Override
	public void preCheck(ImportWebsiteTool util) {
		this.pubfeed = this.prepFeed(this.pubfile);
		this.prefeed = this.prepFeed(this.prefile);
		
		if (OperationContext.get().hasErrors())
			return;
	}

	private FeedAdapter prepFeed(Path path) {
		if (path == null)
			return null;
		
		OperationResult op = new OperationResult();
		
		FeedAdapter adapt = new FeedAdapter();
		adapt.init(this.key, path);
		adapt.validate();
		
		// if an error occurred during the init or validate, don't use the feed
		if (op.hasErrors())
			return null;
		
		return adapt;
	}
	
	@Override
	public void doImport(ImportWebsiteTool util, CountDownCallback cd) {
		if ((this.pubfeed == null) && (this.prefeed == null))
			return;
		
		cd.increment();
		
		XElement pubxml = (this.pubfeed != null) ? this.pubfeed.getXml() : null;
		XElement prexml = (this.prefeed != null) ? this.prefeed.getXml() : null;

		// if no file is present then delete record for feed
		if ((pubxml == null) && (prexml == null)) {
			Hub.instance.getDatabase().submit(
				new ReplicatedDataRequest("dcmFeedDelete")
					.withParams(new RecordStruct()
						.withField("Channel", this.alias)
						.withField("Path", this.key)
				), 
				new ObjectResult() {
					@Override
					public void process(CompositeStruct result3b) {
						cd.countDown();
					}
				});
		
			return;
		}
		
		// if at least one xml file then update/add a record for the feed
		
		RecordStruct feed = new RecordStruct()
			.withField("Channel", this.alias)
			.withField("Path", this.key)
			.withField("Editable", true);
		
		String authtags = (this.pubfeed != null) ? this.pubfeed.getAttribute("AuthTags") : this.prefeed.getAttribute("AuthTags");
		
		if (StringUtil.isEmpty(authtags))
			feed.withField("AuthorizationTags", new ListStruct());
		else
			feed.withField("AuthorizationTags", new ListStruct((Object[]) authtags.split(",")));

		if (pubxml != null) {
			ListStruct ctags = new ListStruct();
			
			for (XElement tag : pubxml.selectAll("Tag")) {
				String alias = tag.getAttribute("Alias");
				
				if (StringUtil.isNotEmpty(alias))
					ctags.addItem(alias);
			}
			
			feed.withField("ContentTags", ctags);
		}
		else if (prexml != null) {
			ListStruct ctags = new ListStruct();
			
			for (XElement tag : prexml.selectAll("Tag")) {
				String alias = tag.getAttribute("Alias");
				
				if (StringUtil.isNotEmpty(alias))
					ctags.addItem(alias);
			}
			
			feed.withField("ContentTags", ctags);
		}
		
		if (pubxml != null) {
			// public fields
			
			String primelocale = pubxml.getAttribute("Locale"); 
	
			ListStruct pubfields = new ListStruct();
			feed.withField("Fields", pubfields);
			
			for (XElement fld : pubxml.selectAll("Field")) 
				pubfields.addItem(new RecordStruct()
					.withField("Name", fld.getAttribute("Name"))
					.withField("Locale", fld.getAttribute("Locale", primelocale))		// prime locale can be override for field, though it means little besides adding to search info
					.withField("Value", fld.getValue())
				);
			
			for (XElement afel : pubxml.selectAll("Alternate")) {
				String alocale = afel.getAttribute("Locale");
				
				for (XElement fld : afel.selectAll("Field")) 
					pubfields.addItem(new RecordStruct()
						.withField("Name", fld.getAttribute("Name"))
						.withField("Locale", alocale)
						.withField("Value", fld.getValue())
					);
			}
	
			ListStruct pubparts = new ListStruct();
			feed.withField("PartContent", pubparts);
			
			for (XElement fld : pubxml.selectAll("PagePart"))
				pubparts.addItem(new RecordStruct()
					.withField("Name", fld.getAttribute("For"))
					.withField("Format", fld.getAttribute("Format"))
					.withField("Locale", fld.getAttribute("Locale", primelocale))		// prime locale can be override for specific part, this is not an alternate, just the default locale for that part
					.withField("Value", pubfeed.getPartValue(primelocale, fld, false))
				);
			
			for (XElement afel : pubxml.selectAll("Alternate")) {
				String alocale = afel.getAttribute("Locale");
				
				for (XElement fld : afel.selectAll("PagePart")) 
					pubparts.addItem(new RecordStruct()
						.withField("Name", fld.getAttribute("For"))
						.withField("Format", fld.getAttribute("Format"))
						.withField("Locale", alocale)
						.withField("Value", pubfeed.getPartValue(alocale, fld, false))
					);
			}
		}
		
		if (prexml != null) {
			// preview fields
			
			String primelocale = prexml.getAttribute("Locale"); 
	
			ListStruct prefields = new ListStruct();
			feed.withField("PreviewFields", prefields);
			
			for (XElement fld : prexml.selectAll("Field")) 
				prefields.addItem(new RecordStruct()
					.withField("Name", fld.getAttribute("Name"))
					.withField("Locale", fld.getAttribute("Locale", primelocale))		// prime locale can be override for field, though it means little besides adding to search info
					.withField("Value", fld.getValue())
				);
			
			for (XElement afel : prexml.selectAll("Alternate")) {
				String alocale = afel.getAttribute("Locale");
				
				for (XElement fld : afel.selectAll("Field")) 
					prefields.addItem(new RecordStruct()
						.withField("Name", fld.getAttribute("Name"))
						.withField("Locale", alocale)
						.withField("Value", fld.getValue())
					);
			}	
	
			ListStruct preparts = new ListStruct();
			feed.withField("PreviewPartContent", preparts);
			
			for (XElement fld : prexml.selectAll("PagePart")) 
				preparts.addItem(new RecordStruct()
					.withField("Name", fld.getAttribute("For"))
					.withField("Format", fld.getAttribute("Format"))
					.withField("Locale", fld.getAttribute("Locale", primelocale))		// prime locale can be override for specific part, this is not an alternate, just the default locale for that part
					.withField("Value", prefeed.getPartValue(primelocale, fld, true))
				);
			
			for (XElement afel : prexml.selectAll("Alternate")) {
				String alocale = afel.getAttribute("Locale");
				
				for (XElement fld : afel.selectAll("PagePart")) 
					preparts.addItem(new RecordStruct()
						.withField("Name", fld.getAttribute("For"))
						.withField("Format", fld.getAttribute("Format"))
						.withField("Locale", alocale)
						.withField("Value", prefeed.getPartValue(alocale, fld, true))
					);
			}
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
