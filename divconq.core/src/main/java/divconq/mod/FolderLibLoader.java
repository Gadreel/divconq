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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import divconq.util.IOUtil;

public class FolderLibLoader extends LibLoader {
	public FolderLibLoader(String name) {
		super(name);

		Path directory = Paths.get(name);

		try {
			Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path sfile, BasicFileAttributes attrs) throws IOException {
					Path relatiev = directory.relativize(sfile);
					String relpath = relatiev.toString();
					
					FolderLibLoader.this.entries.put(relpath, IOUtil.readEntireFileToMemory(sfile).toArray());
					
					return FileVisitResult.CONTINUE;
				}
			});
		}
		catch (IOException x) {
        	System.out.println(x);
		}
        
        //System.out.println("lib loaded: " + name);
	}
}
