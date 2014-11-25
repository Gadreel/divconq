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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import divconq.io.LineIterator;
import divconq.lang.Memory;
import divconq.lang.chars.Utf8Encoder;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationResult;

public class IOUtil {
    /**
     * Read entire text file into a string.
     * 
     * @param file to read
     * @return file content if readable, otherwise null
     */
    public static String readEntireFile(File file) {
    	// TODO improve to funcresult - update to nio2
    	BufferedReader br = null;
    	
        try {
            StringBuilder sb = new StringBuilder();
            
        	br = new BufferedReader(new FileReader(file));
        	
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            
            return sb.toString();
        } 
        catch (IOException x) {
		} 
        finally {
        	closeQuietly(br);
        }
        
        return null;
    }
    
    /**
     * Read entire text file into a string.
     * 
     * @param file to read
     * @return file content if readable, otherwise null
     */
    public static FuncResult<CharSequence> readEntireFile(Path file) {
    	FuncResult<CharSequence> res = new FuncResult<>();
    	
    	BufferedReader br = null;
    	
        try {
            StringBuilder sb = new StringBuilder();
            
        	br = Files.newBufferedReader(file, Charset.forName("UTF-8"));		
        	
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            
            res.setResult(sb);
        } 
        catch (IOException x) {
        	res.error("Unabled to read file " + file + ", error: " + x);
		} 
        finally {
        	closeQuietly(br);
        }
        
        return res;
    }
    
    /**
     * Read entire sream into a string.
     * 
     * @param stream to read
     * @return stream content if readable, otherwise null
     */
    public static String readEntireStream(InputStream stream) {
    	// TODO improve to funcresult - update to nio2
    	BufferedReader br = null;
    	
        try {
            StringBuilder sb = new StringBuilder();
            
        	br = new BufferedReader(new InputStreamReader(stream));
        	
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            
            return sb.toString();
        } 
        catch (IOException x) {
		} 
        finally {
        	closeQuietly(br);
        }
        
        return null;
    }
    
	public static OperationResult saveEntireFile(Path dest, String content) {
		OperationResult or = new OperationResult();
		
		try {
			Files.createDirectories(dest.getParent());
			Files.write(dest, Utf8Encoder.encode(content), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
		} 
		catch (Exception x) {
			or.error(1, "Error saving file contents: " + x);
		}
		
		return or;
	}
    
	public static boolean saveEntireFile2(Path dest, String content) {
		try {
			Files.createDirectories(dest.getParent());
			Files.write(dest, Utf8Encoder.encode(content), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
		} 
		catch (Exception x) {
			return false;
		}
		
		return true;
	}

    /*
	public static File safeSave(InputStream in, String dest) {
		File f = new File(dest);
		f.getParentFile().mkdirs();		// TODO check
		
		FileOutputStream fos = null;
		
		try {
			fos = new FileOutputStream(f);
			IOUtils.copy(in, fos);
		} 
		catch (Exception x) {
			// TODO 
		}
		finally {
        	IOUtils.closeQuietly(fos);
        	IOUtils.closeQuietly(in);
		}
		
		return f;
	}
	*/
	
	public static byte[] charsEncodeAndCompress(CharSequence v) {	
		try {
			byte[] buffer = Utf8Encoder.encode(v);
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			GzipCompressorOutputStream cos = new GzipCompressorOutputStream(bos);
			cos.write(buffer);
			cos.close();
			bos.close();
			
			return bos.toByteArray();
		}
		catch (Exception x) {
		}
		
		return null;
	}

	// TODO improve to funcresult
	public static Memory readEntireFileToMemory(File file) {
		return IOUtil.readEntireFileToMemory(file.toPath());
	}

	// TODO improve to funcresult
	public static Memory readEntireFileToMemory(Path file) {
		try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            Memory mem = new Memory();		// TODO improve mem to read right from channel...
            
            ByteBuffer bb = ByteBuffer.allocate(4096);

            int amt = ch.read(bb);
            
            while (amt != -1) {
            	bb.flip();
            	mem.write(bb);
            	bb.clear();
            	amt = ch.read(bb);
            }            
            
            mem.setPosition(0);
            
            return mem;
        } 
        catch (IOException x) {
		} 
        
        return null;
	}
	
	static public boolean isLegalFilename(String name) {
		if (StringUtil.isEmpty(name))
			return false;
		
		if (name.equals(".") || name.contains("..") || name.contains("*") || name.contains("\"") || name.contains("/") || name.contains("\\")
				 || name.contains("<") || name.contains(">") || name.contains(":") || name.contains("?") || name.contains("|"))
			return false;
		
		return true;
	}
	
    public static void closeQuietly(Closeable... closeables) {
		if (closeables == null) 
			return;
		
		for (Closeable closeable : closeables) { 
			try {
				if (closeable != null) 
					closeable.close();
			} 
			catch (IOException x) {
				// ignore
			}
		}
    }

    /**
     * Returns an Iterator for the lines in an <code>InputStream</code>, using
     * the character encoding specified (or default encoding if null).
     * <p>
     * <code>LineIterator</code> holds a reference to the open
     * <code>InputStream</code> specified here. When you have finished with
     * the iterator you should close the stream to free internal resources.
     * This can be done by closing the stream directly, or by calling
     * {@link LineIterator#close()} or {@link LineIterator#closeQuietly(LineIterator)}.
     * <p>
     * The recommended usage pattern is:
     * <pre>
     * try {
     *   LineIterator it = IOUtils.lineIterator(stream, charset);
     *   while (it.hasNext()) {
     *     String line = it.nextLine();
     *     /// do something with line
     *   }
     * } finally {
     *   IOUtils.closeQuietly(stream);
     * }
     * </pre>
     *
     * @param input  the <code>InputStream</code> to read from, not null
     * @param encoding  the encoding to use, null means platform default
     * @return an Iterator of the lines in the reader, never null
     * @throws IllegalArgumentException if the input is null
     * @throws IOException if an I/O error occurs, such as if the encoding is invalid
     * @since 2.3
     */
    public static LineIterator lineIterator(InputStream input, Charset encoding) throws IOException {
        return new LineIterator(new InputStreamReader(input, encoding));
    }
    
    public static long byteArrayToLong(byte[] b) {
        return  (b[0] & 0xff) << 56
        		| (b[1] & 0xff) << 48 
        		| (b[2] & 0xff) << 40 
        		| (b[3] & 0xff) << 32 
        		| (b[4] & 0xff) << 24 
        		| (b[5] & 0xff) << 16 
        		| (b[6] & 0xff) << 8 
        		| (b[7] & 0xff);
    }

    public static byte[] longToByteArray(long a) {
        byte[] ret = new byte[8];
        
        ret[0] = (byte) ((a >> 56) & 0xff);
        ret[1] = (byte) ((a >> 48) & 0xff);
        ret[2] = (byte) ((a >> 40) & 0xff);
        ret[3] = (byte) ((a >> 32) & 0xff);
        ret[4] = (byte) ((a >> 24) & 0xff);
        ret[5] = (byte) ((a >> 16) & 0xff);   
        ret[6] = (byte) ((a >> 8) & 0xff);   
        ret[7] = (byte) (a & 0xff);
        
        return ret;
    }    
    
    public static int byteArrayToInt(byte[] b) {
        return  (b[0] & 0xff) << 24
        		| (b[1] & 0xff) << 16 
        		| (b[2] & 0xff) << 8 
        		| (b[3] & 0xff);
    }

    public static byte[] intToByteArray(int a) {
        byte[] ret = new byte[4];
        
        ret[0] = (byte) ((a >> 24) & 0xff);
        ret[1] = (byte) ((a >> 16) & 0xff);   
        ret[2] = (byte) ((a >> 8) & 0xff);   
        ret[3] = (byte) (a & 0xff);
        
        return ret;
    }    
 }
