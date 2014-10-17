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
package divconq.tasks.test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import divconq.hub.Hub;
import divconq.lang.OperationContext;
import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;
import divconq.util.HexUtil;
import divconq.util.StringUtil;
import divconq.work.IWork;
import divconq.work.TaskRun;

public class AsyncHashWork implements IWork {
	@Override
	public void run(final TaskRun run) {
		run.info(0, "AsyncHashTask at running in thread: " + Thread.currentThread().getName());
		
		RecordStruct params = run.getTask().getParams();
		
		if (params == null) {
			run.error(1, "Unable to Greet, missing params structure.");
			run.complete();
			return;
		}
		
		String name = params.getFieldAsString("Path");
		
		if (StringUtil.isEmpty(name)) {
			run.error(1, "Unable to Greet, missing Path param.");
			run.complete();
			return;
		}
		
		final Path path = Paths.get(name);
		final ByteBuffer buf = ByteBuffer.allocate(64 * 1024);
		
		AsynchronousFileChannel sbc = null;
			
        try {
			Set<OpenOption> opts = new HashSet<>();
			opts.add(StandardOpenOption.READ);
			
        	sbc = AsynchronousFileChannel.open(path,  opts, Hub.instance.getWorkPool());	            
        }
        catch (IOException x) {
        	run.error(1, "Unable to open file: " + x);            
		}
		
		sbc.read(buf, 0, sbc, new CompletionHandler<Integer, AsynchronousFileChannel>() {
		    protected MessageDigest md = null;
		    protected int pos = 0;
		    
			@Override
			public void completed(Integer result, AsynchronousFileChannel sbc) {
				run.info(0, "A) AsyncHashTask completed after read in context: " + OperationContext.get().getOpId());
				
	        	run.thawContext();
	        	
				run.info(0, "B) AsyncHashTask completed after read in context: " + OperationContext.get().getOpId());
				run.info(0, "C) AsyncHashTask completed after read in thread: " + Thread.currentThread().getName());

				if (this.md == null) {
					try {
						this.md = MessageDigest.getInstance("SHA-256");
					} 
					catch (NoSuchAlgorithmException x) {
						run.error(1, "Unable to create digest object: " + x);
			        	
			        	try {
							sbc.close();
						}
			        	catch (IOException x2) {
						}
					
			        	run.complete();
			        	
			        	return;
					}
				}
				
				if (result == -1) {
		        	try {
						sbc.close();
					}
		        	catch (IOException x) {
					}
		        	
		        	String sha256 = HexUtil.bufferToHex(this.md.digest());
		        	
		        	run.info(0, "Hash of " + path + " is " + sha256);
		        	
		        	run.setResult(new RecordStruct(
		        			new FieldStruct("Hash", sha256)
		        	));
		        	
					run.complete();
					return;
				}
		    	
				if (result > 0) {
					buf.flip();
					this.md.update(buf);
					this.pos += result;
				}
				
		        buf.clear();
		        
		        sbc.read(buf, this.pos, sbc, this);
			}

			@Override
			public void failed(Throwable x, AsynchronousFileChannel sbc) {
				run.info(0, "A) AsyncHashTask failed after read in context: " + OperationContext.get().getOpId());
				
	        	run.thawContext();
	        	
				run.info(0, "B) AsyncHashTask failed after read in context: " + OperationContext.get().getOpId());
				run.info(0, "C) AsyncHashTask failed after read in thread: " + Thread.currentThread().getName());
				
	        	run.error(1, "Async hash task failed to read file: " + x);
	        	
	        	try {
					sbc.close();
				}
	        	catch (IOException x2) {
				}
			
	        	run.complete();
			}
		});
	}
}
