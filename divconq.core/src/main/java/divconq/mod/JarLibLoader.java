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

import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;

public class JarLibLoader extends LibLoader {
	public JarLibLoader(String name) {
		super(name);
		
		JarArchiveInputStream stream = null;

        try {
            InputStream theFile = new FileInputStream(this.name);
            stream = new JarArchiveInputStream(theFile);            
            
            JarArchiveEntry entry = stream.getNextJarEntry();
            
            while(entry != null) {
            	if (!entry.isDirectory()) {
            		//if (entry.getName().endsWith("Container.class"))
            		//	System.out.println("at cont");
            		
            		int esize = (int) entry.getSize();
            		
            		if (esize > 0) {
	            		int eleft = esize;
	            		byte[] buff = new byte[esize];
	            		int offset = 0;
	            		
	            		while (offset < esize) {
			            	int d = stream.read(buff, offset, eleft);
			            	offset += d;
			            	eleft -= d;
	            		}
	            		
		            	this.entries.put("/" + entry.getName(), buff);
            		}
            	}
            	
            	entry = stream.getNextJarEntry();
            }
		}
        catch (Exception x) {
        	// TODO logging
        	System.out.println(x);
        }
        finally {
        	try {
	        	if (stream != null)
	        		stream.close();
        	}
        	catch(Exception x) {
        		
        	}
        }		
	}
}
