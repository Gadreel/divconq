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
package divconq.hub;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import divconq.lang.FuncResult;
import divconq.lang.OperationResult;
import divconq.locale.Localization;
import divconq.schema.SchemaManager;
import divconq.xml.XElement;

public class ProjectCompiler {
	public SchemaManager getSchema(OperationResult or, Collection<String> packages) {
		SchemaManager sm = new SchemaManager();
		
		// load in package order so that later packages override the original
		for (String pkg : packages) {
			File sdir = new File("./packages/" + pkg + "/schema");
			
			or.trace(0, "Checking for schemas in: " + sdir.getAbsolutePath());
			
			if (sdir.exists())
				// TODO make sure that we get in canonical order
				for (File schema : sdir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {					
						return name.endsWith(".xml");
					}
				})) {					
					or.trace(0, "Loading schema: " + schema.getAbsolutePath());
					
					or.copyMessages(sm.loadSchema(schema));
				}
		}

		or.trace(0, "Starting compiling schemas");
		or.copyMessages(sm.compile());

		or.trace(0, "Finished compiling schemas");
		
		return sm;
	}
	
	public FuncResult<List<File>> getProcs(Collection<String> packages) {
		FuncResult<List<File>> or = new FuncResult<List<File>>();		
		List<File> sm = new ArrayList<File>();
		or.setResult(sm);
		
		for (String pkg : packages) {
			File sdir = new File("./packages/" + pkg + "/m");
			
			or.trace(0, "Checking for procs in: " + sdir.getAbsolutePath());
			
			if (sdir.exists())
				for (File proc : sdir.listFiles()) {					
					or.trace(0, "Found proc: " + proc.getAbsolutePath());					
					sm.add(proc);
				}
		}

		or.trace(0, "Finished compiling proc list");
		
		return or;
	}
	
	public Localization getDictionary(OperationResult or, Collection<String> packages) {
		Localization loc = new Localization();
		
		// load in package order so that later packages override the original
		for (String pkg : packages) {
			File sdir = new File("./packages/" + pkg + "/dictionary");
			
			or.trace(0, "Checking for dictionary in: " + sdir.getAbsolutePath());
			
			if (sdir.exists())
				// TODO make sure that we get in canonical order
				for (File dict : sdir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {					
						return name.endsWith(".xml");
					}
				})) {
					or.trace(0, "Loading dictionary: " + dict.getAbsolutePath());
					
					loc.load(or, dict);
				}
		}

		or.trace(0, "Finished loading dictionaries");
		
		return loc;
	}
	
	public XElement getConfigShell(OperationResult or, Collection<String> components, String tier) {
		// TODO
		return null;
	}
}
