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
package divconq.test.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import divconq.api.ApiSession;
import divconq.hub.ILocalCommandLine;

public class ScriptUI implements ILocalCommandLine {
	@Override
	public void run(final Scanner scan, final ApiSession api) {
		List<String> oparams  = new ArrayList<String>();
		
		oparams.add("c:\\msysgit\\cmd\\git.exe");
		oparams.add("status");
		
		final ProcessBuilder pb = new ProcessBuilder(oparams);
		pb.redirectErrorStream(true);
		pb.directory(new File("d:\\dev\\divconq\\hub"));
		
		try {
			Process proc = pb.start();
			
			BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			
			String line = null;
			
			while ((line = input.readLine()) != null) {
				line = line.trim();
				
				// TODO configure if empty lines should be removed
				//if (StringUtil.isEmpty(line))
				//	continue;
	
				System.out.println(line);
			}
			
			input.close();
		  
			long ecode = (long) proc.exitValue();
			
			System.out.println("exit code: " + ecode);
		}
		catch (Exception x) {
			System.out.println("shell error: " + x);
		}
		
		
		/*
		System.out.println("before");
		
		final CountDownLatch latch = new CountDownLatch(1);

		ScriptUtility.goSwing(new OperationCallback() {			
			@Override
			public void callback() {
				latch.countDown();
			}
		});

		try {
			latch.await();
		}
		catch (Exception x) {			
		}
		
		System.out.println("after");
		*/
	}
}
