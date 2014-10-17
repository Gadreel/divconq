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

import divconq.script.ActivityManager;
import divconq.script.IInstructionProvider;
import divconq.script.Instruction;
import divconq.script.inst.ctp.CtpSend;
import divconq.script.inst.ctp.CtpSession;
import divconq.script.inst.ext.SendEmail;
import divconq.script.inst.ext.ShellProcess;
import divconq.script.inst.file.File;
import divconq.script.inst.file.FileOps;
import divconq.script.inst.file.LocalFile;
import divconq.script.inst.file.LocalFileStore;
import divconq.script.inst.file.LocalFolder;
import divconq.script.inst.file.TempFile;
import divconq.script.inst.file.TempFolder;
import divconq.script.inst.sql.SqlInsert;
import divconq.script.inst.sql.SqlUpdate;
import divconq.xml.XElement;

public class CoreInstructionProvider implements IInstructionProvider {
	@Override
	public Instruction createInstruction(XElement def) {
		String tag = def.getName();
		
		if ("Main".equals(tag))
			return new Main();
		
		if ("Function".equals(tag))
			return new Function();
		
		if ("Return".equals(tag))
			return new Return();
		
		if ("ReturnIfErrored".equals(tag))
			return new ReturnIfErrored();
		
		if ("CallFunction".equals(tag))
			return new CallFunction();
		
		if ("Var".equals(tag))
			return new Var();
		
		if ("Global".equals(tag))
			return new Global();
		
		if ("With".equals(tag))
			return new With();
		
		if ("For".equals(tag))
			return new For();
		
		if ("ForEach".equals(tag))
			return new ForEach();
		
		if ("While".equals(tag))
			return new While();
		
		if ("Until".equals(tag))
			return new Until();
		
		if ("If".equals(tag))
			return new If();
		
		if ("IfEmpty".equals(tag))
			return new IfEmpty();
		
		if ("IfNotEmpty".equals(tag))
			return new IfNotEmpty();
		
		if ("IfErrored".equals(tag))
			return new IfErrored();
		
		if ("Else".equals(tag))
			return new Else();
		
		if ("Switch".equals(tag))
			return new Switch();
		
		if ("Case".equals(tag))
			return new Case();
		
		if ("BreakIf".equals(tag))
			return new BreakIf();
		
		if ("ContinueIf".equals(tag))
			return new ContinueIf();
		
		if ("Break".equals(tag))
			return new Break();
		
		if ("Continue".equals(tag))
			return new Continue();
		
		if ("Exit".equals(tag))
			return new Exit();
		
		if ("ExitIfErrored".equals(tag))
			return new ExitIfErrored();
		
		if ("ResetErrored".equals(tag))
			return new ResetErrored();
		
		if ("OnError".equals(tag))
			return new OnError();
		
		if ("Debugger".equals(tag))
			return new Debugger();
		
		if ("Console".equals(tag))
			return new Console();
		
		if ("Trace".equals(tag))
			return new Trace();
		
		if ("Debug".equals(tag))
			return new Debug();
		
		if ("Info".equals(tag))
			return new Info();
		
		if ("Warn".equals(tag))
			return new Warn();
		
		if ("Error".equals(tag))
			return new Error();
		
		if ("Progress".equals(tag))
			return new Progress();
		
		if ("Sleep".equals(tag))
			return new Sleep();
		
		if ("ShellProcess".equals(tag))
			return new ShellProcess();
		
		if ("SendEmail".equals(tag))
			return new SendEmail();
		
		if ("SqlInsert".equals(tag))
			return new SqlInsert();
		
		if ("SqlUpdate".equals(tag))
			return new SqlUpdate();
		
		if ("FileOps".equals(tag))
			return new FileOps();
		
		if ("LocalFolder".equals(tag))
			return new LocalFolder();
		
		if ("LocalFile".equals(tag))
			return new LocalFile();
		
		if ("LocalFileStore".equals(tag))
			return new LocalFileStore();
		
		if ("TempFolder".equals(tag))
			return new TempFolder();
		
		if ("TempFile".equals(tag))
			return new TempFile();
		
		if ("File".equals(tag))
			return new File();
		
		if ("Folder".equals(tag))
			return new File();
		
		if ("CtpSession".equals(tag))
			return new CtpSession();
		
		if ("CtpSend".equals(tag))
			return new CtpSend();
		
		if ("CtpSendForget".equals(tag))
			return new CtpSend();
		
		return null;
	}

	public static void init(ActivityManager man) {
		IInstructionProvider cip = new CoreInstructionProvider();
		man.addTag("Main", cip);
		man.addTag("Function", cip);
		man.addTag("Return", cip);
		man.addTag("ReturnIfErrored", cip);
		man.addTag("CallFunction", cip);
		man.addTag("Var", cip);
		man.addTag("Global", cip);
		man.addTag("With", cip);
		man.addTag("For", cip);
		man.addTag("ForEach", cip);
		man.addTag("ExitIfErrored", cip);
		man.addTag("ResetErrored", cip);
		man.addTag("OnError", cip);
		man.addTag("Debugger", cip);
		man.addTag("Trace", cip);
		man.addTag("Debug", cip);
		man.addTag("Info", cip);
		man.addTag("Progress", cip);
		man.addTag("Warn", cip);
		man.addTag("Error", cip);
		man.addTag("Console", cip);
		man.addTag("While", cip);
		man.addTag("Until", cip);
		man.addTag("If", cip);
		man.addTag("IfErrored", cip);
		man.addTag("IfEmpty", cip);
		man.addTag("IfNotEmpty", cip);
		man.addTag("Else", cip);
		man.addTag("Switch", cip);
		man.addTag("Case", cip);
		man.addTag("BreakIf", cip);
		man.addTag("ContinueIf", cip);
		man.addTag("Break", cip);
		man.addTag("Continue", cip);
		man.addTag("Exit", cip);
		man.addTag("Sleep", cip);
		
		// TODO need to be enhanced
		man.addTag("SqlUpdate", cip);
		man.addTag("SqlInsert", cip);
		
		// TODO these are not core probably
		man.addTag("ShellProcess", cip);
		man.addTag("SendEmail", cip);
		man.addTag("CtpSession", cip);
		man.addTag("CtpSend", cip);
		man.addTag("CtpSendForget", cip);
		
		// TODO these are not core probably
		man.addTag("FileOps", cip);
		
		man.addTag("TempFolder", cip);
		man.addTag("TempFile", cip);
		
		man.addTag("LocalFolder", cip);
		man.addTag("LocalFile", cip);
		man.addTag("LocalFileStore", cip);
		
		man.addTag("Folder", cip);
		man.addTag("File", cip);
	}
}
