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
package divconq.mod;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

// see http://atamur.blogspot.nl/2009/10/using-built-in-javacompiler-with-custom.html
// and http://www.ibm.com/developerworks/java/library/j-jcomp/index.html
public class RuntimeCompilerManager implements JavaFileManager {
	protected StandardJavaFileManager standardFileManager = null;
	protected Bundle bundle = null;

	public RuntimeCompilerManager(Bundle bundle,	StandardJavaFileManager standardFileManager) {
		this.bundle = bundle;
		this.standardFileManager = standardFileManager;
	}

	@Override
	public ClassLoader getClassLoader(Location location) {
		return this.bundle;
	}

	@Override
	public String inferBinaryName(Location location, JavaFileObject file) {
		if (file instanceof BundleFile) 
			return ((BundleFile) file).getName();

		// if it's not CustomJavaFileObject, then it's coming from
		// standard file manager - let it handle the file
		return this.standardFileManager.inferBinaryName(location, file);
	}

	@Override
	public boolean isSameFile(FileObject a, FileObject b) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean handleOption(String current, Iterator<String> remaining) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasLocation(Location location) {
		return location == StandardLocation.CLASS_PATH || location == StandardLocation.PLATFORM_CLASS_PATH; 
	}

	@Override
	public JavaFileObject getJavaFileForInput(Location location, String className, JavaFileObject.Kind kind) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void flush() throws IOException {
		// do nothing
	}

	@Override
	public void close() throws IOException {
		// do nothing
	}

	@Override
	public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
		if (location == StandardLocation.PLATFORM_CLASS_PATH) 
			return this.standardFileManager.list(location, packageName, kinds, recurse);

		if ((location == StandardLocation.CLASS_PATH) && kinds.contains(JavaFileObject.Kind.CLASS)) {
			// TODO possibly filter what packages can be accessed?
			
			if (packageName.startsWith("java"))  
				return this.standardFileManager.list(location, packageName, kinds, recurse);

			return this.bundle.listPackageClasses(packageName);
		}
		
		return Collections.emptyList();
	}

	@Override
	public int isSupportedOption(String option) {
		return -1;
	}
}
