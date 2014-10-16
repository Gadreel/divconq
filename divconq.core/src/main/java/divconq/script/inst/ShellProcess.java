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
package divconq.script.inst;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import divconq.script.ExecuteState;
import divconq.script.Instruction;
import divconq.script.StackEntry;
import divconq.struct.ScalarStruct;
import divconq.struct.Struct;
import divconq.struct.scalar.IntegerStruct;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class ShellProcess extends Instruction {
	static public final Pattern logpattern = Pattern.compile("^(\\d\\d\\d)\\s.+$");
	
	@Override
	public void run(final StackEntry stack) {
		List<XElement> params = this.source.selectAll("Param");
		List<String> oparams  = new ArrayList<String>();

		String cmd = stack.stringFromSource("Command");
		
		if (cmd.endsWith(".bat")) {
			oparams.add("cmd.exe");
			oparams.add("/c");
		}
		
		oparams.add(cmd);
		
		for (int i = 0; i < params.size(); i++) {
			String v = stack.stringFromElement(params.get(i), "Value");
			
			if (StringUtil.isEmpty(v)) {
				stack.log().error("Missing value for parameter: " + (i + 1));
				// TODO error code and set last
				stack.setState(ExecuteState.Done);		// done is fine, the script should decide what to do with the rrror
				stack.resume();
			}
			
			// TODO test this
			//if (stack.boolFromElement(params.get(i), "Quote", false))
				v = "\"" + v.replace("\"", "\"\"") + "\"";			// TODO could be ^ instead??
			
			oparams.add(v);
		}
		
		final ProcessBuilder pb = new ProcessBuilder(oparams);
		pb.redirectErrorStream(true);
		pb.directory(new File(stack.stringFromSource("WorkingFolder")));
		
		// must keep in same thread so we obey the rules of the pool we are running in
		try {
			Process proc = pb.start();
			
			BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			
			String line = null;
			
			while ((line = input.readLine()) != null) {
				line = line.trim();
				
				// TODO configure if empty lines should be removed
				if (StringUtil.isEmpty(line))
					continue;
				
				Matcher m = ShellProcess.logpattern.matcher(line);
				int code = 0;
						
				if (m.matches()) {
					code = Integer.parseInt(m.group(1));
					line = line.substring(4);
				}
				
				if (code < 300)
					stack.log().info(0, line);
				else if (code < 500)
					stack.log().warn(0, line);
				else
					stack.log().error(400000 + code, line);		// TODO configure error start code
			}
			
			input.close();
			  
			long ecode = (long) proc.exitValue();
			
			if (ecode >= 500)
				ecode += 400000;		// TODO fix
			
			stack.setLastCode(ecode);
			
	        Struct var = stack.refFromSource("Target");
			
			if (var instanceof ScalarStruct) 
				((ScalarStruct) var).adaptValue(ecode);
			
	        String handle = stack.stringFromSource("Handle");

			if (handle != null) 
	            stack.addVariable(handle, new IntegerStruct(ecode));
		} 
		catch (IOException x) {
			// TODO 
			stack.log().error(2, "IO Error " + x);
			stack.setLastCode(1L);
		}
		
		stack.setState(ExecuteState.Done);
		stack.resume();
	}
	
	@Override
	public void cancel(StackEntry stack) {
		// TODO see if we can do something about this
	}
}
