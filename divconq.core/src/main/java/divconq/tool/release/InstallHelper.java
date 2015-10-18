package divconq.tool.release;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import divconq.struct.Struct;
import divconq.xml.XElement;

public class InstallHelper {
	protected XElement relchoice = null;
	protected PackagesHelper availpackages = null;
	protected String prinpackage = null;
	protected String prinpackagenm = null;
	protected Set<String> instpkgs = new HashSet<>(); 
	protected Set<String> relopts = new HashSet<>(); 
	protected boolean includeinstaller = false;
	
	public boolean init(PackagesHelper availpackages, XElement relchoice) {
		this.relchoice = relchoice;
		this.availpackages = availpackages;
		
		this.includeinstaller = Struct.objectToBooleanOrFalse(relchoice.getAttribute("IncludeInstaller"));
		this.prinpackage = relchoice.getAttribute("PrincipalPackage");
		
		int pspos = prinpackage.lastIndexOf('/');
		this.prinpackagenm = (pspos != -1) ? prinpackage.substring(pspos + 1) : prinpackage;
		
		instpkgs.add(prinpackage);
		
		if (includeinstaller)
			instpkgs.add("dc/dcInstall");
		
		if (relchoice.hasAttribute("Options"))
			relopts.addAll(Arrays.asList(relchoice.getAttribute("Options").split(",")));
		
		relchoice.selectAll("Package").stream().forEach(pkg -> instpkgs.add(pkg.getAttribute("Name")));
		
		System.out.println("Selected packages: " + instpkgs);
		
		String[] coreinst = instpkgs.toArray(new String[instpkgs.size()]);
		
		for (int i = 0; i < coreinst.length; i++) {
			if (!availpackages.collectPackageDependencies(this, coreinst[i])) {
				System.out.println("Error with package dependencies");
				return false;
			}
		}
		
		System.out.println("All release packages: " + instpkgs);
		return true;
	}
	
	// does the official release contain this...or do the domains
	public boolean containsPathExtended(Path npath) {
		if (npath.getName(1).toString().equals("public")) {
			if (npath.getNameCount() < 4)
				return false;
			
			String alias = npath.getName(3).toString();
			
			return relchoice.selectAll("Domain").stream().anyMatch(domain -> alias.equals(domain.getAttribute("Alias")));
		}
		
		return this.containsPath(npath);
	}

	// does the official release contain this...
	public boolean containsPath(Path npath) {
		AtomicBoolean fnd = new AtomicBoolean();
		
		String p = npath.toString().substring(2);
		
		this.instpkgs.forEach(pname -> {
			if (p.startsWith("packages/" + pname))
				fnd.set(true);
			
			availpackages.get(pname)
				.selectAll("DependsOn").stream()
				.filter(doel -> !doel.hasAttribute("Option") || this.relopts.contains(doel.getAttribute("Option")))
				.forEach(doel -> {
					// copy all libraries we rely on
					// TODO consider lib handling
					//doel.selectAll("Library").forEach(libel -> {
					//	
					//	Path src = Paths.get("./lib/" + libel.getAttribute("File"));
					//});
					
					// copy all files we rely on
					doel.selectAll("File").forEach(libel -> {
						if (p.equals(libel.getAttribute("Path")))
								fnd.set(true);;
					});
					
					// copy all folders we rely on
					doel.selectAll("Folder").forEach(libel -> {
						if (p.startsWith(libel.getAttribute("Path")))
							fnd.set(true);;
					});
				});
			
			// copy the released packages libraries
			// TODO handle package libs in main lib?
			//Path libsrc = Paths.get("./packages/" + pname + "/lib");
		});
		
		return fnd.get();
	}
	
	public boolean hasOption(String opt) {
		return relopts.contains(opt);
	}
	
	public void addPackage(String name) {
		this.instpkgs.add(name);
	}
}