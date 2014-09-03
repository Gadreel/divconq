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
import divconq.xml.XElement;

public class CoreInstructionProvider implements IInstructionProvider {
	@Override
	public Instruction createInstruction(XElement def) {
		String tag = def.getName();
		
		if ("Main".equals(tag))
			return new Main();
		
		if ("Function".equals(tag))
			return new Function();
		
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
		
		if ("Switch".equals(tag))
			return new Switch();
		
		if ("Case".equals(tag))
			return new Case();
		
		if ("Break".equals(tag))
			return new Break();
		
		if ("Continue".equals(tag))
			return new Continue();
		
		if ("Exit".equals(tag))
			return new Exit();
		
		if ("ExitIfErrored".equals(tag))
			return new ExitIfErrored();
		
		if ("Console".equals(tag))
			return new Console();
		
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
		
		return null;
	}

	public static void init(ActivityManager man) {
		IInstructionProvider cip = new CoreInstructionProvider();
		man.addTag("Main", cip);
		man.addTag("Function", cip);
		man.addTag("CallFunction", cip);
		man.addTag("Var", cip);
		man.addTag("Global", cip);
		man.addTag("With", cip);
		man.addTag("For", cip);
		man.addTag("ForEach", cip);
		man.addTag("Debug", cip);
		man.addTag("Info", cip);
		man.addTag("Progress", cip);
		man.addTag("Warn", cip);
		man.addTag("Error", cip);
		man.addTag("Console", cip);
		man.addTag("While", cip);
		man.addTag("Until", cip);
		man.addTag("If", cip);
		man.addTag("Switch", cip);
		man.addTag("Case", cip);
		man.addTag("Break", cip);
		man.addTag("Continue", cip);
		man.addTag("Exit", cip);
		man.addTag("ExitIfErrored", cip);
		man.addTag("Sleep", cip);
		
		man.addTag("SqlUpdate", cip);
		man.addTag("SqlInsert", cip);
		
		// TODO these are not core probably
		man.addTag("ShellProcess", cip);
		man.addTag("SendEmail", cip);
		
	}
}
