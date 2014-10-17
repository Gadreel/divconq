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
package divconq.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;

import divconq.interchange.CommonPath;
import divconq.io.LineIterator;
import divconq.lang.OperationResult;

// see Hub, it clears the temp files
public class FileUtil {
	// only use this in tests or completely safe functions, this is not meant to provide secure random services to entire framework
	// SecureRandom is way too slow for what we need here
	static public final Random testrnd = new Random();
	
	static public String randomFilename() {
		return UUID.randomUUID().toString().replace("-", "");
	}
	
	static public String randomFilename(String ext) {
		return UUID.randomUUID().toString().replace("-", "") + "." + ext;
	}
	
	static public File allocateTemp() {
		return FileUtil.allocateTemp("tmp");
	}
	
	static public File allocateTemp(String ext) {
		File temps = new File("./temp");
		
		temps.mkdirs();
		
		String fname = FileUtil.randomFilename(ext);
		
		return new File(temps, fname);
	}
	
	static public File allocateTempFolder() {
		File temps = new File("./temp/" + FileUtil.randomFilename());		
		temps.mkdirs();		
		return temps;
	}
	
	static public Path allocateTempFolder2() {
		try {
			Path temps = Paths.get("./temp/" + FileUtil.randomFilename());		

			Files.createDirectories(temps);
			
			return temps;
		} 
		catch (IOException x) {
		}
		
		return null;
	}
	
	static public File pathTempFolder(String name) {
		File temps = new File("./temp/" + name);
		return temps;
	}
	
	static public void cleanupTemp() {
		File temps = new File("./temp");
		
		if (!temps.exists())
			return;
		  
		for(File next : temps.listFiles()) {
			if (next.isDirectory())
				continue;
			
			long time = System.currentTimeMillis();
			long modified = next.lastModified();
			  
			if(modified + (60 * 60 * 1000) > time)			// wait an hour 
				continue;
		      
			next.delete();
		}
	}
	
	static public String getFileExtension(Path p) {
		String fname = p.getFileName().toString();
		
		if (fname == null)
			return null;
		
		int pos = fname.lastIndexOf('.');
		
		if (pos == -1)
			return null;
		
		return fname.substring(pos + 1);
	}
	
	static public String getFileExtension(CommonPath p) {
		String fname = p.getFileName();
		
		if (fname == null)
			return null;
		
		int pos = fname.lastIndexOf('.');
		
		if (pos == -1)
			return null;
		
		return fname.substring(pos + 1);
	}
	
	static public String getFileExtension(String fname) {
		if (fname == null)
			return null;
		
		int pos = fname.lastIndexOf('.');
		
		if (pos == -1)
			return null;
		
		return fname.substring(pos + 1);
	}

	// path to folder creates a temp file in folder
	// writes 64KB blocks 
	static public Path generateTestFile(Path path, String ext, int minblocks, int maxblocks) {
		if (!Files.isDirectory(path)) 
			return null;
		
		try {
			if (!Files.exists(path))
				Files.createDirectories(path);
		
			String fname = FileUtil.randomFilename(ext);
			path = path.resolve(fname);
	
			int blocks = minblocks + FileUtil.testrnd.nextInt(maxblocks - minblocks);
			
			// 64KB block
			byte[] buffer = new byte[64 * 1024];
			
			for (int i = 0; i < buffer.length / 256; i++)
				for (int j = 0; j < 256; j++)
					buffer[(i * 256) + j] = (byte)j;
			
			ByteBuffer bb = ByteBuffer.wrap(buffer);
			
			try (FileChannel fc = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.SYNC)) {
				while (blocks > 0) { 
					fc.write(bb);
					blocks--;
					bb.position(0);		// so we can write again
				}
			}
			
			return path;
		} 
		catch (IOException x) {
			System.out.println("generateTestFile Error: " + x);
		}
		
		return null; 
	}
	
	static public OperationResult confirmOrCreateDir(Path path) {
		OperationResult or = new OperationResult();
		
		if (path == null) {
			or.error("Path is null");
			return or;
		}
		
        if (Files.exists(path)) {
            if (!Files.isDirectory(path)) 
            	or.error(path + " exists and is not a directory. Unable to create directory.");
            
            return or;
        } 
        
        try {
        	Files.createDirectories(path);
        }
        catch (FileAlreadyExistsException x) {
        	// someone else created a file under our noses
            if (!Files.isDirectory(path)) 
            	or.error(path + " exists and is not a directory. Unable to create directory.");
        }
        catch (Exception x) {
        	or.error("Unable to create directory " + path + ", error: " + x);
        }
        
        return or;
    }
    
	// TODO add secure delete option - JNA?
	// TODO add delete followup feature someday
    public static OperationResult deleteDirectory(Path directory) {
		OperationResult or = new OperationResult();
		
		deleteDirectory(or, directory);
		
		return or;
    }
	
    public static void deleteDirectory(OperationResult or, Path directory) {
		if (directory == null) {
			or.error("Path is null");
			return;
		}
		
        if (Files.notExists(directory)) 
            return;
        
		if (!Files.isDirectory(directory)) {
			or.error("Path is not a folder: " + directory);
			return;
		}

		try {
			Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path sfile, BasicFileAttributes attrs) throws IOException {
					Files.delete(sfile);
					
					return FileVisitResult.CONTINUE;
				}
				
				@Override
				public FileVisitResult postVisitDirectory(Path sfile, IOException x1) throws IOException {
					if (x1 != null)
						throw x1;
					
					Files.delete(sfile);
					
					return FileVisitResult.CONTINUE;
				}
			});
		}
		catch (IOException x) {
			or.error("Unable to delete directory: " + directory + ", error: " + x);
		}
    }

    /**
     * Returns an Iterator for the lines in a <code>File</code>.
     * <p>
     * This method opens an <code>InputStream</code> for the file.
     * When you have finished with the iterator you should close the stream
     * to free internal resources. This can be done by calling the
     * {@link LineIterator#close()} or
     * {@link LineIterator#closeQuietly(LineIterator)} method.
     * <p>
     * The recommended usage pattern is:
     * <pre>
     * LineIterator it = FileUtils.lineIterator(file, "UTF-8");
     * try {
     *   while (it.hasNext()) {
     *     String line = it.nextLine();
     *     /// do something with line
     *   }
     * } finally {
     *   LineIterator.closeQuietly(iterator);
     * }
     * </pre>
     * <p>
     * If an exception occurs during the creation of the iterator, the
     * underlying stream is closed.
     *
     * @param file  the file to open for input, must not be {@code null}
     * @param encoding  the encoding to use, {@code null} means platform default
     * @return an Iterator of the lines in the file, never {@code null}
     * @throws IOException in case of an I/O error (file closed)
     * @since 1.2
     */
    public static LineIterator lineIterator(File file, String encoding) throws IOException {
        try (InputStream in = Files.newInputStream(file.toPath())) {
            return IOUtil.lineIterator(in, Charset.forName(encoding));
        } 
        catch (Exception ex) {
            throw ex;
        }
    }

    /**
     * Returns an Iterator for the lines in a <code>File</code> using the default encoding for the VM.
     *
     * @param file  the file to open for input, must not be {@code null}
     * @return an Iterator of the lines in the file, never {@code null}
     * @throws IOException in case of an I/O error (file closed)
     * @since 1.3
     * @see #lineIterator(File, String)
     */
    public static LineIterator lineIterator(final File file) throws IOException {
        return lineIterator(file, "UTF-8");
    }
    
    public static OperationResult copyFileTree(Path source, Path target) {
    	return copyFileTree(source, target, null);
    }
    
    public static OperationResult copyFileTree(Path source, Path target, Predicate<Path> filter) {
    	OperationResult or = new OperationResult();
    	
        try {
        	if (Files.notExists(target)) 
        		Files.createDirectories(target);

			Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
			        new SimpleFileVisitor<Path>() {
			            @Override
			            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
			                throws IOException
			            {
			                Path targetdir = target.resolve(source.relativize(dir));
			                
			                try {
			                    Files.copy(dir, targetdir, StandardCopyOption.COPY_ATTRIBUTES);
			                } 
			                catch (FileAlreadyExistsException x) {
			                     if (!Files.isDirectory(targetdir))
			                         throw x;
			                }
			                
			                return FileVisitResult.CONTINUE;
			            }
			            
			            @Override
			            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
			                throws IOException
			            {
			            	if ((filter == null) || filter.test(file))
			            		Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.COPY_ATTRIBUTES);
			                
			                return FileVisitResult.CONTINUE;
			            }
			        });
		} 
        catch (IOException x) {
			or.error("Error copying file tree: " + x);
		}
        
        return or;
    }
    
    static public long parseFileSize(String size) {
		Long x = StringUtil.parseLeadingInt(size);
		
		if (x == null)
			return 0;
		
		size = size.toLowerCase();
		
		if (size.endsWith("kb"))
			x *= 1024;
		else if (size.endsWith("mb"))
			x *= 1024 * 1024;
		else if (size.endsWith("gb"))
			x *= 1024 * 1024 * 1024;
		
		return x;
    }
 }
