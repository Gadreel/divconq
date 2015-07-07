package divconq.web.importer;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import divconq.hub.DomainInfo;
import divconq.lang.CountDownCallback;
import divconq.lang.op.OperationCallback;
import divconq.lang.op.OperationContext;
import divconq.xml.XElement;

public class ImportWebsiteTool {
	protected Map<String, FileImporter> feedpaths = new HashMap<String, FileImporter>();
	protected Set<String> uuidsused = new HashSet<String>();
	
	public boolean allowUuid(String id) {
		return this.uuidsused.add(id);
	}
	
	public void importSite(OperationCallback op) {
		// =============== COLLECT ==============
		XElement feed = op.getContext().getDomain().getSettings().find("Feed");
		
		if (feed != null) {
			// there are two special channels - Pages and Blocks
			for (XElement chan : feed.selectAll("Channel")) 
				this.collectChannel(chan);
		}

		/* TODO need to update Stored Proc to handle Path instead of UUID as a key
		this.collectWWW();
		*/		
		
		if (op.hasErrors()) {
			op.error("Unable to import site, file collection failed");
			return;
		}
		
		// =============== PREP ==============
		this.prep();
		
		if (op.hasErrors()) {
			op.error("Unable to import site, file prep failed");
			return;
		}
		
		// =============== IMPORT ==============
		this.doImport(op);
	}
	
	public void importFeedFile(Path file, OperationCallback op) {
		this.collectFeedFile(file);
		
		if (op.hasErrors()) {
			op.error("Unable to import site, file collection failed");
			return;
		}
		
		// =============== PREP ==============
		this.prep();
		
		if (op.hasErrors()) {
			op.error("Unable to import site, file prep failed");
			return;
		}
		
		// =============== IMPORT ==============
		this.doImport(op);
	}
	
	public void doImport(OperationCallback cb) {
		CountDownCallback cd = new CountDownCallback(1, new OperationCallback() {
			@Override
			public void callback() {
				// =============== DONE ==============
				if (cb.hasErrors()) 
					cb.info("Website import completed with errors!");
				else
					cb.info("Website import completed successfully");
				
				cb.complete();
			}
		});
		
		for (FileImporter fi : this.feedpaths.values())
			fi.doImport(this, cd);
		
		cd.countDown();
	}
	
	public void prep() {
		for (FileImporter fi : this.feedpaths.values())
			fi.preCheck(this);
	}
	
	public void collectChannel(XElement chan) {
		String alias = chan.getAttribute("Alias");
		
		if (alias == null)
			alias = chan.getAttribute("Name");
		
		// don't index blocks be themselves
		//if ("Blocks".equals(alias))
		//	return;
		
		// TODO remove
		//if (!"Pages".equals(alias))
		//	return;
		
		String perpath = chan.getAttribute("Path", "");		// or empty string
		
		this.collectArea("feed", alias, false, perpath);
		
		this.collectArea("feed", alias, true, perpath);
	}
	
	public void collectWWW() {
		this.collectArea("www", false);
		this.collectArea("www", true);
	}
	
	public void collectArea(String area, boolean preview) {
		this.collectArea(area, "", preview, "");
	}
	
	public void collectArea(String area, String alias, boolean preview, String perpath) {
		try {
			DomainInfo di = OperationContext.get().getDomain();
			String wwwpathf1 = preview ? "/" + area +  "-preview/" + alias : "/" + area +  "/" + alias;
			Path wwwsrc1 = di.resolvePath(wwwpathf1).toAbsolutePath().normalize();
			
			//System.out.println("abs: " + feedsrc1);
			
			if (Files.exists(wwwsrc1)) {
				Files.walkFileTree(wwwsrc1, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path sfile, BasicFileAttributes attrs) throws IOException {
						Path relpath = wwwsrc1.relativize(sfile);
						
						//System.out.println("rel: " + relpath);
						
						String innerpath = relpath.toString();
						FileImporter imp = null;
						
						if (innerpath.endsWith(".dcui.xml")) {
							innerpath = perpath + "/" + innerpath.substring(0, innerpath.length() - 9);
							imp = new PageImporter();
						}
						else if (innerpath.endsWith(".dcf.xml")) {
							innerpath = perpath + "/" + innerpath.substring(0, innerpath.length() - 8);
							imp = new FeedImporter();
						}
						
						if ((imp != null) && !ImportWebsiteTool.this.feedpaths.containsKey(innerpath)) {
							imp.setKey(innerpath);
							imp.setArea(area);
							imp.setAlias(alias);
							
							if (preview) {
								OperationContext.get().info("Indexing preview only " + alias + " > " + innerpath);
								
								ImportWebsiteTool.this.feedpaths.put(innerpath, imp);
								
								imp.setPreviewFile(sfile);
							}
							else {
								OperationContext.get().info("Indexing " + alias + " > " + innerpath);
							
								ImportWebsiteTool.this.feedpaths.put(innerpath, imp);
								
								imp.setFile(sfile);
								
								String prefeedpath = "/" + area + "-preview/" + alias + "/" + relpath;
								Path prefeedsrc = di.resolvePath(prefeedpath).normalize();
								
								if (Files.exists(prefeedsrc)) {
									OperationContext.get().info("Indexing preview also " + alias + " > " + innerpath);
									
									imp.setPreviewFile(prefeedsrc);
								}
							}
						}
							
						return FileVisitResult.CONTINUE;
					}
				});
			}
		}
		catch (IOException x) {
			OperationContext.get().error("Error indexing " + area + ": " + alias + " : " + x);
		}
	}
	
	public void collectFeedFile(Path sfile) {
		XElement feed = OperationContext.get().getDomain().getSettings().find("Feed");
		
		if ((feed == null) || (sfile == null)) 
			return;
		
		sfile = sfile.toAbsolutePath().normalize();
		
		DomainInfo di = OperationContext.get().getDomain();
		
		Path dmpath = di.getPath().toAbsolutePath().normalize();
		
		Path relpath = dmpath.relativize(sfile);
		
		if (relpath.getNameCount() < 2)
			return;
		
		//String wwwpathf1 = preview ? "/" + area +  "-preview/" + alias : "/" + area +  "/" + alias;
		//Path wwwsrc1 = di.resolvePath(wwwpathf1).toAbsolutePath().normalize();
		
		String area = "feed";
		String alias = relpath.getName(1).toString();
		String perpath = "";
		
		// there are two special channels - Pages and Blocks
		for (XElement chan : feed.selectAll("Channel")) { 
			String calias = chan.getAttribute("Alias");
			
			if (calias == null)
				calias = chan.getAttribute("Name");
			
			if (calias.equals(alias)) {
				perpath = chan.getAttribute("Path", "");		// or empty string
				break;
			}			
		}
		
		System.out.println("relpath: " + relpath);
		
		relpath = relpath.subpath(2, relpath.getNameCount());
		
		//System.out.println("abs: " + feedsrc1);

		//Path relpath = wwwsrc1.relativize(sfile);
		
		//System.out.println("rel: " + relpath);
		
		String cleanpath = relpath.toString();
		
		int pos = cleanpath.indexOf('.');
		
		if (pos != -1)
			cleanpath = cleanpath.substring(0, pos); // + ".dcf.xml";
		
		String innerpath = perpath + "/" + cleanpath;

		FileImporter imp = new FeedImporter();
		
		imp.setKey(innerpath);
		imp.setArea(area);
		imp.setAlias(alias);

		OperationContext.get().info("Indexing " + alias + " > " + innerpath);
	
		ImportWebsiteTool.this.feedpaths.put(innerpath, imp);
		
		String pubfeedpath = "/" + area + "/" + alias + "/" + cleanpath + ".dcf.xml";
		Path pubfeedsrc = di.resolvePath(pubfeedpath).normalize();
		
		if (Files.exists(pubfeedsrc)) {
			OperationContext.get().info("Indexing published " + alias + " > " + innerpath);
			imp.setFile(pubfeedsrc);
		}
		
		String prefeedpath = "/" + area + "-preview/" + alias + "/" + cleanpath + ".dcf.xml";
		Path prefeedsrc = di.resolvePath(prefeedpath).normalize();
		
		if (Files.exists(prefeedsrc)) {
			OperationContext.get().info("Indexing preview " + alias + " > " + innerpath);
			imp.setPreviewFile(prefeedsrc);
		}
	}

	public void clear() {
		this.feedpaths.clear();
		this.uuidsused.clear();
	}
}
