package divconq.hub;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import divconq.filestore.CommonPath;
import divconq.io.CacheFile;
import divconq.io.LocalFileStore;
import divconq.lang.op.OperationResult;
import divconq.xml.XElement;

public class HubPackages {
	protected LocalFileStore packagefilestore = null;

	protected List<HubPackage> packages = new ArrayList<>(); 
	protected List<HubPackage> reversepackages = new ArrayList<>();
	
	public List<HubPackage> getPackages() {
		return this.packages;
	}
	
	public List<HubPackage> getReversePackages() {
		return this.reversepackages;
	}
	
	public LocalFileStore getPackageFileStore() {
		return this.packagefilestore;
	}
	
	public void load(XElement cel) {
		for (XElement pack : cel.selectAll("Packages/Package")) {
			HubPackage p = new HubPackage(pack);
			
			this.packages.add(p);
			this.reversepackages.add(0, p);
		}
	} 
	/**
	 * Get a reference to a library file (typically a JAR) for this Project.
	 *  
	 * @param filename name of the JAR, path relative to the lib/ folder
	 * @return File reference if found, if not error messages in FuncResult
	 * /
	public FuncResult<File> getLibrary(String filename -- pass in list ) {
		FuncResult<File> res = new FuncResult<File>(); 
		
		for (HubPackage rcomponent : this.reversepackages) {
			File f = new File("./packages/" + rcomponent.getName() + "/lib/" + filename);
			
			if (f.exists()) {
				res.setResult(f);
				return res;
			}
		}
		
		res.errorTr(200, "./lib/" + filename);
		
		return res;
	}
	*/

	// return the path to the first file that matches, if any, in the package list
	public Path lookupPath(String path) {
		return this.lookupPath(this.reversepackages, path);
	}

	public Path lookupPath(List<HubPackage> packages, String path) {
		for (HubPackage rcomponent : packages) {
			Path rpath = this.packagefilestore.resolvePath("/" + rcomponent.getName() + path);
			
			if (Files.exists(rpath)) 
				return rpath;
		}
		
		return null;
	}

	public Path lookupPath(Path path) {
		return this.lookupPath(this.reversepackages, path);
	}

	// only works with relative paths
	public Path lookupPath(List<HubPackage> packages, Path path) {
		if (path.isAbsolute()) 
			return null;
		
		for (HubPackage rcomponent : packages) {
			Path rpath = this.packagefilestore.resolvePath("/" + rcomponent.getName()).resolve(path).normalize().toAbsolutePath();
			
			if (Files.exists(rpath)) 
				return rpath;
		}
		
		return null;
	}

	public Path lookupPath(CommonPath path) {
		return this.lookupPath(this.reversepackages, path);
	}

	public Path lookupPath(List<HubPackage> packages, CommonPath path) {
		for (HubPackage rcomponent : packages) {
			Path rpath = this.packagefilestore.resolvePath("/" + rcomponent.getName() + path);
			
			if (Files.exists(rpath)) 
				return rpath;
		}
		
		return null;
	}
	
	// use CacheFile once and then let go, call this every time you need it or you may be holding on to stale content
	public CacheFile cacheLookupPath(String path) {
		return this.cacheLookupPath(this.reversepackages, path);
	}
	
	// use CacheFile once and then let go, call this every time you need it or you may be holding on to stale content
	public CacheFile cacheLookupPath(List<HubPackage> packages, String path) {
		Path rpath = this.lookupPath(packages, path);
		
		if (rpath != null)
			return this.packagefilestore.cacheResolvePath(rpath);
		
		return null;
	}
	
	// use CacheFile once and then let go, call this every time you need it or you may be holding on to stale content
	public CacheFile cacheLookupPath(Path path) {
		return this.cacheLookupPath(this.reversepackages, path);
	}
	
	// use CacheFile once and then let go, call this every time you need it or you may be holding on to stale content
	public CacheFile cacheLookupPath(List<HubPackage> packages, Path path) {
		Path rpath = this.lookupPath(packages, path);
		
		if (rpath != null)
			return this.packagefilestore.cacheResolvePath(rpath);
		
		return null;
	}
	
	// use CacheFile once and then let go, call this every time you need it or you may be holding on to stale content
	public CacheFile cacheLookupPath(CommonPath path) {
		return this.cacheLookupPath(this.reversepackages, path);
	}
	
	// use CacheFile once and then let go, call this every time you need it or you may be holding on to stale content
	public CacheFile cacheLookupPath(List<HubPackage> packages, CommonPath path) {
		Path rpath = this.lookupPath(packages, path);
		
		if (rpath != null)
			return this.packagefilestore.cacheResolvePath(rpath);
		
		return null;
	}
	
	public boolean cacheHas(String path) {
		for (HubPackage rcomponent : packages) {
			boolean file = this.packagefilestore.cacheHas("/" + rcomponent.getName() + "/" + path);

			if (file)
				return true;
		}
		
		return false;
	}		
	
	// build a subset of the reverse package list from a list of package names
	// this will give the order in which to lookup stuff for package files
	public List<HubPackage> buildLookupList(Collection<String> packagenames) {
		List<HubPackage> res = new ArrayList<>();
		
		for (HubPackage pkg : this.reversepackages) {
			for (String pname : packagenames) {
				if (pkg.getName().equals(pname)) {
					res.add(pkg);
					break;
				}
			}
		}
		
		return res;
	}

	public void init(OperationResult or, XElement config) {
		if (config == null)
			config = new XElement("PackageFileStore");
		
		this.packagefilestore = new LocalFileStore();
		this.packagefilestore.start(or, config);
		
		/*
		this.packagefilestore.register(new FuncCallback<FileStoreEvent>() {
			@Override
			public void callback() {
				// --- TODO 
				//System.out.println("TODO tell all domains to reload");
			}
		});
		*/
	}

	public void stop(OperationResult or) {
		this.packagefilestore.stop(or);		
	}
}
