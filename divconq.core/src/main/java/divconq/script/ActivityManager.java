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
package divconq.script;

import java.util.HashMap;
import java.util.Map;



import divconq.hub.Hub;
import divconq.lang.OperationResult;
import divconq.schema.DataType;
import divconq.script.inst.CoreInstructionProvider;
import divconq.script.mutator.Substring;
import divconq.struct.Struct;
import divconq.xml.XElement;

public class ActivityManager {
	// operator is type specific - [Type][Name] = mutator
	protected Map<String, Map<String, IOperator>> operationExtensions = new HashMap<String, Map<String,IOperator>>();
	protected Map<String, IInstructionProvider> instProviders = new HashMap<String, IInstructionProvider>();
	
	public void addTag(String name, IInstructionProvider cip) {
		this.instProviders.put(name, cip);
	}
	
	public ActivityManager() {
		// TODO load from config
		
		HashMap<String, IOperator> stringextensions = new HashMap<String, IOperator>();
		stringextensions.put("Substring", new Substring());
		
		this.operationExtensions.put("String", stringextensions);
		
		CoreInstructionProvider.init(this);		
	}
	
	// create anonymous var
	public Struct createVariable(OperationResult mr, String type) {
		DataType mut = Hub.instance.getSchema().getType(type);
		
		if (mut == null)
			return null;
		
		Struct ret = mut.create(mr);
		
		if (ret == null)
			return null;
		
		return ret;
	}
	
	public Instruction createInstruction(XElement def) {
		IInstructionProvider mut = this.instProviders.get(def.getName());
		return (mut != null) ? mut.createInstruction(def) : null;		
	}

	public void operate(StackEntry stack, Struct target, XElement code) {
		if (target == null) {
			// TODO log
			
			stack.resume();
			return;
		}
		
		DataType dt = target.getType();
		
		if (dt != null) {
			// look for an extension for this mutator first
			
			Map<String, IOperator> typeextensions = this.operationExtensions.get(dt.getId());
			
			if (typeextensions != null) {
				IOperator mut = typeextensions.get(code.getName());
		
				if (mut != null) { 
					mut.operation(stack, code, target);
					return;
				}
			}
		}
		
		// fall back on the default operation handler for the target type
		target.operation(stack, code);
	}
	
	/*
	 * goals
	 * x		better compiler
	 * x		no exceptions
	 * x		rewrite stack control
	 * 		advanced data types
	 * 		create functions
	 * 		globals
	 * x		improve flexible attributes 
	 * x		new source handling
	 * 		more instructions 
	 * 		component management
	 * 		freeze/thaw
	 * 		create libraries (call by function)
	 * 		create libraries (call by script)
	 * 
	 * 		when a var goes out of scope, call dispose  (close streams, sources, dests, remove temp files, etc)
	 * 
	 * 		check user level within instructions - error if user is not authorized
	 * 
	 * 
	 * value/var attributes
	 * 		"literal"
	 * 		"literal string with {$variable} in it"
	 * 
	 * 		` forces literal, so
	 * 		"`hello world"	resolves to "hello world"
	 * 		"``hello"		resolves to "`hello"
	 * 		"`$x"			resolves to "$x"
	 * 
	 * 		$ forces variable named after value
	 * 		"$x"			reference to x
	 * 		
	 * 		$$ forces variable by the name held in value
	 * 		"$$z"			resolves to reference to "x" if z = "x"
	 * 
		<Main Steps="[int][optional]">
			... insts
		</Main>
		
		<With Target="[string]" SetTo="[ref][optional]">
			... mutators
		</With>
		
		<Var Name="[string]" Type="[string]" SetTo="[ref][optional]">
			... mutators
		</Var>
		
			Composite Set/Merge - defaults to Xml if any non-text children or to Json if text only children
			
		<Var Name="msg1" Type="ResultMessage">
			<Merge Format"Xml,Yaml,Json">
				<Record>
					<Field Name="Kind" Value="Level" />
					<Field Name="Code" Value="3" />
					<Field Name="Message" Value="Howday partner!" />
				</Record>
			</Merge>
		</Var>
		
		<For Name="[string][optional]" From="[int][optional, default 0]" To="[int][optional, default max long]" Step="[int][optional, default 1]">
			... insts
		</For>
		
		<Debug Code="[int][optional]">
			message [string]
		</Debug>
		
		<Info Code="[int][optional]">
			message [string]
		</Info>
		
		<Warn Code="[int][optional]">
			message [string]
		</Warn>
		
		<Error Code="[int][optional]">
			message [string]
		</Error>

		**** logic **** 
		
		// Not may appear on any Logic or Condition
		<Logic Target="[ref][optional]" Equal|Equals="[reference]" Not="[boolean][optional]">
			... insts
		</Logic>

		<Logic Target="[ref][optional]" LessThan="[reference]">
			... insts
		</Logic>

		<Logic Target="[ref][optional]" GreaterThan="[reference]">
			... insts
		</Logic>

		<Logic Target="[ref][optional]" LessThanOrEqual="[reference]">
			... insts
		</Logic>

		<Logic Target="[ref][optional]" GreaterThanOrEqual="[reference]">
			... insts
		</Logic>

		// may combine expressions
		<Logic Target="[ref][optional]" GreaterThanOrEqual="[reference]" LessThan="[reference]">
			... insts
		</Logic>

		// condition with Name ignores the top Name, condition without Name uses top Name in compare
		<Logic Target="[ref][optional]">
			<Detail>		// automatically an AND expression
				<Condition ...>
				<And/Or>
					<Condition Target="[ref][optional]" ... any of the conditions above ...>
					<Condition ...>
				</And/Or>
				<Condition ...>
			</Detail>
			
			... insts
		</Logic>

		// if no conditions listed then just check to see if this variable - exists, is not null, not false and not empty string
		<Logic Target="[ref][optional]">
			... insts
		</Logic>
		
		// if no name then it is treated as CommandState
		<Logic>
			... insts
		</Logic>
		
		**** end logic **** 

		<While [any logic]>
			... insts
		</While>

		<Until [any logic]>
			... insts
		</Until>

		<If [any logic]>
			... insts
		</If>

		<Switch Target="[ref][optional]">
			... Case insts
		</Switch>

		<Case [any logic]>
			... insts
		</Case>
		
		<Break />
		
		<Continue />
		
		<Exit Code="[int]" Message="[string]" />

		<IfLastFlagSuccess>
			... insts
		</IfLastFlagSuccess>

		<IfLastFlagFail>
			... insts
		</IfLastFlagFail>

		// forces "^_" into Name attribute
		<IfLastResult [any logic]>
			... insts
		</IfLastResult>

		<IfExitFlagSuccess>
			... insts
		</IfExitFlagSuccess>

		<IfExitFlagFail>
			... insts
		</IfExitFlagFail>
		
		// we are an async language - don't really wait - instead schedule a call back in N seconds
		<Sleep Seconds="[int]" Period="ISO Period" Duration="dur" />
		
		<Progress Step="[int]" Name="[string]" Amount="[int]">
			message
		</Progress>
		
		- NO <Milestone Name="[string]" Release="[int]" Acquire="[int]" />
		
		// milestone handling
		<Await Name="[string]" />
		
		<Release Name="[string]" />
		
		// we are an async language - don't really wait - instead service call back triggers our call back
		<CallService Name="[string]" Feature="[string][optional]" Op="[string][optional]" Hub="[string]" Await="[boolean][optional, def true]" ResultTo="var to set - or just use default[ref][optional]" ResultCodeTo="...[int][optional]">
			<Args>
				<Field ... />
			</Args>
		</CallService>
		
		// execute an "program" which only sends back a code and a result
		// entire execution is one call, we use an async call back 
		// may report to collab for debugging though
		<CallScript Name="[string]" Hub="[string][optional]" Await="[boolean][optional, def true]" ResultTo="var to set - or just use default[ref][optional]" ResultCodeTo="...[int][optional]">
			<Args>
				<Field ... />
			</Args>
		</CallScript>
		
		** CallScript and CallService can both direct response to a mailbox if Await false this gets aysnc flow, if True just means to look in the mailbox for response in addition to _LastResult
		
		// execution is inline and we watch it become part of our stack - heavy but easier to debug
		<CallFunction Name="[string]" Library="lib.name.here [string][optional]" ResultTo="var to set - or just use default[ref][optional]" ResultCodeTo="...[int][optional]">
			<Args>
				<Field ... />
			</Args>
		</CallFunction>
		
		<Args> can be inline:
		
		<Args>
			<Field ... />
		</Args>
		
		or reference
		
		<Args Name="name of var [string]" />
		
		-------------------------------------------------------------------
		
		Examples
		
		<Script>
		<Types>
			... special types not in Repo, but useful to local - type validation is ensured (Strict) or warned (Permissive)
		</Types>

		// functions (local or lib) present a stack barrier, searches for variable names stop here
		
		<Function Name="" ParamType="AnyRecord or specific" ReturnType="AnyRecord or specific">
		</Function>
		
		... or can define inline
		
		<Function Name="">
			<ParamType Inherits="">
				...field defs... 
			</ParamType>
			
			<ReturnType Inherits="">
				...field defs... 
			</ReturnType>
		</Function>
		
		<Main>
			<ParamType Inherits="">
				...field defs... 
			</ParamType>
			
			<ReturnType Inherits="">
				...field defs... 
			</ReturnType>
		
		
			<Var Name="x" Type="Integer" SetTo="5">
				<Add Value="2" />
				<Inc />
				<Random From="x" To="y" />
			</Var>
		
			<List Name="y">
				<Scalar Value="2" />
				<Scalar Value="^x" />
				<Record>
					<Field Name="">
						scalar, record, list
					</Field>
				</Record>
			</List>
		
			<With Name="y">
				<RemoveItem Index="2" />
				<AddItem>
					<Scalar ... />
				</AddItem>
				<Clear />
				<InsertItem Index="0">
					<Scalar ... />
				</InsertItem>
			</With>
		
			<Record Name="z">
				<Field Name="">
					scalar, record, list
				</Field>
			</Record>
			
			// change string at 3rd position of property a (list)
			<With Name="z.a[2]">
				<Concat Value="nuy" />
			</With>
			
			<With Name="z">
				set field, remove field
			</With>
			
			<Component Name="var name" Class="id string - ftcFtp">
				<Settings>
					...
				</Settings>
			</Component>
		
			<For Name="bottles" From="4" To="0" Step="-1">
				<Switch Name="bottles">
				<Case GreaterThan="1">
					<Var Name="bottlesleft" Type="Integer" Value="{$bottles}">
						<Dec />
					</Var>

					<Info>
						{$bottles} bottles of beer on the wall, {$bottles} bottles of beer.
						Take one down and pass it around, {$bottlesleft} bottles of beer on the wall.
					</Info>
				</Case>
				<Case Equal="1">
					<Info>
						{$bottles} bottle of beer on the wall, {$bottles} bottle of beer.
						Take one down and pass it around, no more bottles of beer on the wall.
					</Info>
				</Case>
				<Case Equal="0">
					<Info>
						No more bottles of beer on the wall, no more bottles of beer.
						Go to the store and buy some more, 99 bottles of beer on the wall.
					</Info>
				</Case>
				</Switch>
			</For>
		</Main>
		</Script>

		<Var Name="strm1" Class="divconq.core.StreamController">
			<dccTransform>
				compress 
				encrypt
			</dccTransform>
		</Var>
		
		<Var Name="disk1" Class="divconq.protos.DiskController">
			<dcpChangeDirectory Path="./files/accounts" />
			<dcpGet Stream="$strm1" Match="*.xls" />
		</Var>		
				
		<Var Name="sftp1" Class="divconq.protos.SftpController">
			<dcpSettings>
				<Field ...
			</dcpSettings>
			<dcpSignOn User="" Password="" Key="" Cert="" />
			<dcpChangeDirectory Path="./accountbackup" />
			<dcpPut Stream="$strm1" />
			<dcpExit />
		</Var>
		
		<Var Name="disk1">
			<dcpExit />
		</Var>		

		-----------------------------
		
		upload

		// Navigator selects/filters based on name, path and attributes

		<Var Name="disk1" Class="divconq.protos.DiskDriver">
			<OpenNavigator Name="snav">
				<Root Path="./files/accounts" />
				
				<MatchFolders Pattern="." Recursive="True" />   same as   <MatchFolders Recursive="True" />
				<MatchFiles Pattern="*.xls" /> 
				
				<IgnoreFolders Pattern="..." />
				<IgnoreFiles Pattern="..." />
				
				<All | Any>
					<Size LessThan="..." />
					<Modified NewerThan="..." />
					<Tagged Name="" />
				</All | Any>
				
				... each All or Any listed must be true
				
				<Decrypt />			// each file is decrypted
				<Uncompress /> 		// each file is uncompressed, the contents are
			</OpenNavigator>
		</Var>		
		
		// in addition to FTP, SFTP, Disk, Box, S3, AS2 drivers there is also a Session driver 
		// for transfers to another session
		 *  
		<Var Name="sftp1" Class="divconq.protos.SftpDriver">
			<Settings>
				<Field ...
			</Settings>
			<SignIn User="" Password="" Key="" Cert="" />
			<OpenNavigator Name="dnav1">
				<Root Path="./accountbackup" />
				
				<Compress Name="..." />		// all incoming files are named under this, closed only when navigator is closed  
				<Encrypt />
			</OpenNavigator>
		</Var>
		
		// files can have a MarkForDelete call which will delete when navigator is told to Flush or Close
		// this way we can still iterate files while not yet deleting  
		
		// file
		<ForEach Name="sfile" In="$snav"> 
			<!-- like a while loop, for each file given by source -->
			<Var Name="shash" Type="String" />
			<Var Name="dhash" Type="String" />
			
			<Transform ...function local or library />
			
			<With Target="$dnav1">
				// Put has Resume="T/F" default to F 
				<Put Source="$sfile" Rename="..." Handle="dfile" />						// use to rename files as they are stored, macros are available
			</With>

			... if errors ...
			
			<With Target="$sfile">
				<Hash Method="Sha2" Target="$shash" /> 
			</With>
			
			<With Target="$dfile">
				<Hash Method="Sha2" Name="$dhash" /> 
			</With>
			
			... compare ...
			
			<With Target="$dfile">
				<Hash Method="Sha2" Name="$dhash" /> 
			</With>
		</ForEach>
	
		
		- ALT -
		
		// all components are automatically disposed at end of script 
		
		<With Target="$disk1">
			<Dispose />					// causes any navigators to dispose also
		</With>		
		
	 * 
	 */
}
