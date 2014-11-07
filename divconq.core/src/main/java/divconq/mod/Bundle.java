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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.Adler32;

import javax.tools.JavaFileObject;

import divconq.hub.Hub;
import divconq.lang.op.FuncResult;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

public class Bundle extends ClassLoader {
	protected List<String> libraryNames = new ArrayList<String>();
	protected List<LibLoader> libloaders = new ArrayList<LibLoader>();
	
	public Bundle(ClassLoader parent) {
		super(parent);
	}	
	
	public void addLibrary(String pck, String name, String alias) throws IOException {
		// TODO - if parent is Bundle and if parent has this library, skip this step
		
		// TODO add support for package (pck)
		
		this.libraryNames.add(name);
		
		String path = Hub.instance.getLibraryPath(name, alias);
		
		if (path != null) {
			LibLoader lib = null;
			
			if (path.endsWith(".jar")) 
				lib = new JarLibLoader(path);
			else 
				lib = new FolderLibLoader(path);
			
			this.libloaders.add(lib);
		}
		else {
			// TODO get the library from a bus service
		}			
	}

	public Object getInstance(String cname) {
		try {
			return this.getClass(cname).newInstance();
		} 
		catch (Exception x) {
			//System.out.println("err: " + x);
		}
		
		return null;
	}
	
	public FuncResult<XElement> getResourceAsXml(String name, boolean keepwhitespace) {
		try {
			InputStream is = this.getResourceAsStream(name);
			
			if (is == null) {
				FuncResult<XElement> res = new FuncResult<XElement>();
				res.errorTr(133, name);		
				return res;
			}
			
			return XmlReader.parse(is, keepwhitespace); 
		} 
		catch (Exception x) {
			FuncResult<XElement> res = new FuncResult<XElement>();
			res.errorTr(134, name, x);	
			return res;
		}
	}

	public Class<?> getClass(String cname) {
		try {
			return Class.forName(cname, true, this);
		} 
		catch (Exception x) {
		}
		
		return null;
	}
	
	public byte[] findClassEntry(String name) {
		return this.findFileEntry("/" + name.replace(".", "/") + ".class");
	}
	
	public Iterable<JavaFileObject> listPackageClasses(String packname) {
		Map<String, JavaFileObject> files = new HashMap<String, JavaFileObject>();
		
		packname = "/" + packname.replace(".", "/");
		
		for (int i = this.libloaders.size() - 1; i >= 0; i--) {
			LibLoader lib = this.libloaders.get(i);
			
			for (Entry<String, byte[]> pentry : lib.entries.entrySet()) {
				String name = pentry.getKey();
				
				if (name.startsWith(packname) && name.endsWith(".class"))
					files.put(name, new BundleFile(name, pentry.getValue()));
			}
		}	
		
		// TODO list parent too if parent is a Bundle
		
		return files.values();
	}
	
	public byte[] findFileEntry(String name) {
		for (LibLoader lib : this.libloaders) {
			byte[] cd = lib.getEntry(name);
			
			if (cd != null)
				return cd;
		}		
		
		ClassLoader p = this.getParent();
		
		if (p instanceof Bundle)
			return ((Bundle)p).findFileEntry(name);
		
		return null;
	}

	public boolean hasFileEntry(String fpath) {
		for (LibLoader lib : this.libloaders) 
			if (lib.hasEntry(fpath))
				return true;
		
		ClassLoader p = this.getParent();
		
		if (p instanceof Bundle)
			return ((Bundle)p).hasFileEntry(fpath);
		
		return false;
	}
	
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		byte[] cd = this.findClassEntry(name);
		
		if (cd != null) 
			return super.defineClass(name, cd, 0, cd.length);
		
		return super.findClass(name);
	}
	
	@Override
	public InputStream getResourceAsStream(String name) {
		byte[] entry = this.findFileEntry(name);
		
		if (entry == null)
			return null;
		
		return new ByteArrayInputStream(entry);
	}

	public void adler(Adler32 ad) {
		for (LibLoader lib : this.libloaders) 
			lib.adler(ad);
	}
}
