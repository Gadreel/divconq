package divconq.db.proc;

import divconq.db.DatabaseTask;
import divconq.db.IStoredProc;
import divconq.db.DatabaseInterface;
import divconq.db.util.ByteUtil;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationResult;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.struct.builder.ICompositeBuilder;
import divconq.util.HexUtil;

public class KeyQuery implements IStoredProc {
	@Override
	public void execute(DatabaseInterface adapter, DatabaseTask task, OperationResult or) {
		ICompositeBuilder resp = task.getBuilder();
		RecordStruct params = task.getParamsAsRecord();

		ListStruct keys = params.getFieldAsList("Keys");
		boolean explode = params.getFieldAsBooleanOrFalse("Explode");
		
		byte[] basekey = null;
		
		for (Struct ss : keys.getItems()) 
			basekey =  ByteUtil.combineKeys(basekey, HexUtil.decodeHex(ss.toString())); 
		
		//ByteUtil.buildKey(keys.toObjectList().toArray());
		
		this.listChildren(adapter, resp, basekey, explode);
		
		task.complete();
	}
	
	public void listChildren(DatabaseInterface adapter, ICompositeBuilder resp, byte[] basekey, boolean explode) {
		try {
			resp.startList();
			
			// null = start of list			
			byte[] subid = adapter.nextPeerKey(basekey, null);
			
			while (subid != null) {
				Object skey = ByteUtil.extractValue(subid);
				
				if (skey == null)
					break;
				
				byte[] ckey = ByteUtil.combineKeys(basekey, subid);
				
				resp.startRecord();
				
				resp.field("Key", HexUtil.bufferToHex(subid));
				resp.field("DisplayKey", skey);
				
				//System.out.println("- " + ByteUtil.extractValue(subid));
				
				if (adapter.isSet(ckey)) {
					byte[] v = adapter.get(ckey);
					resp.field("Value", HexUtil.bufferToHex(v));
					resp.field("DisplayValue", ByteUtil.extractValue(adapter.get(ckey)));
				}
				
				if (explode) {
					resp.field("Children");
					this.listChildren(adapter, resp, ckey, explode);
				}
				
				resp.endRecord();

				subid = adapter.nextPeerKey(basekey, subid);
			}
			
			/*
			<Record Id="dcDollarOItem">
				<Field Name="Key" Type="Any" Required="True" />
				<Field Name="Value" Type="Any" />
				<Field Name="Children">
					<List Type="dcDollarOItem" />
				</Field>
			</Record>
		 * 
			 */
			
			//System.out.println("Ending key list");
			
			resp.endList();
		}
		catch (Exception x) {
			OperationContext.get().error("KeyQueryProc: Unable to create list: " + x);
		}		
	}
}
