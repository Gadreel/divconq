package divconq.tool.release;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import divconq.lang.op.FuncResult;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

public class PackagesHelper {
	protected Path pkgspath = Paths.get("./packages");
	protected Map<String, XElement> availpackages = new HashMap<>();
	
	public void init() throws Exception {
		Files.walkFileTree(pkgspath, new SimpleFileVisitor<Path>() {
			public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) throws java.io.IOException {
				Path pkgdesc = path.resolve("package.xml");
				
				if (Files.exists(pkgdesc)) {
					
					FuncResult<XElement> xres = XmlReader.loadFile(path.resolve("package.xml"), false);
					
					if (xres.hasErrors()) 
						System.out.println("package.xml found, but not usable: " + path);
					else 
						availpackages.put(pkgspath.relativize(path).toString().replace('\\', '/'), xres.getResult());
					
					return FileVisitResult.SKIP_SUBTREE; 
				}
				
				return FileVisitResult.CONTINUE; 
			}
		});
		
		System.out.println("Available packages: " + availpackages.keySet());
	}
	
	public XElement get(String pname) {
		return this.availpackages.get(pname);
	}
	
	// return true on success
	public boolean collectPackageDependencies(InstallHelper inst, String pname) {
		if (!availpackages.containsKey(pname)) {
			System.out.println("Required Package not found: " + pname);
			return false;
		}
		
		// filter DependsOn by Option
		for (XElement doel : availpackages.get(pname).selectAll("DependsOn")) {
			if (doel.hasAttribute("Option") && !inst.hasOption(doel.getAttribute("Option")))
				continue;
			
			 // copy all libraries we rely on
			for (XElement pkg : doel.selectAll("Package")) {
				String doname = pkg.getAttribute("Name");
				inst.addPackage(doname);
				if (!this.collectPackageDependencies(inst, doname))
					return false;
			}
		}

		return true;
	}
}