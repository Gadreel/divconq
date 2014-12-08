package divconq.db.update;

import java.util.UUID;

import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;
import divconq.util.StringUtil;

/**
 * Use this field to update StaticList structures in dcDatabase.
 * 
 * @author Andy
 *
 */
public class ListField extends ScalarField {
	protected String subkey = null;
	
	/**
	 * @return subkey (id for this item in the list)
	 */
	public String getSubkey() {
		return this.subkey;
	}
	
	/**
	 * @param name of field
	 * @param subkey id for this item in the list (leave null to assign new id)
	 * @param value of field
	 * @param tags meta data for the field (alphanumeric and ':' are allowed in tag)
	 */
	public ListField(String name, String subkey, Object value, FieldTags tags) {
		super(name, value, tags);
		
		if (StringUtil.isEmpty(subkey))
			subkey = UUID.randomUUID().toString().replace("-", "");
		
		this.subkey = subkey;
	}
	
	/**
	 * @param name of field
	 * @param subkey id for this item in the list (leave null to assign new id)
	 * @param value of field
	 */
	public ListField(String name, String subkey, Object value) {
		super(name, value);
		
		if (StringUtil.isEmpty(subkey))
			subkey = UUID.randomUUID().toString().replace("-", "");
		
		this.subkey = subkey;
	}
	
	/**
	 * Mark a field as Retired.
	 * 
	 * @param name of field
	 * @param subkey id for this item in the list (leave null to assign new id)
	 * @param tags meta data for the field (alphanumeric and ':' are allowed in tag)
	 */
	public ListField(String name, String subkey, FieldTags tags) {
		super(name, tags);
		
		if (StringUtil.isEmpty(subkey))
			subkey = UUID.randomUUID().toString().replace("-", "");
		
		this.subkey = subkey;
	}
	
	/**
	 * Mark a field as Retired.
	 * 
	 * @param name of field
	 * @param subkey id for this item in the list 
	 */
	public ListField(String name, String subkey) {
		super(name);
		
		if (StringUtil.isEmpty(subkey))
			subkey = UUID.randomUUID().toString().replace("-", "");
		
		this.subkey = subkey;
	}

	@Override
	public RecordStruct getParams(RecordStruct fields) {
		RecordStruct data = new RecordStruct( 
				new FieldStruct("Data", this.value)
		);
		
		if (this.tags != null)
			data.setField("Tags", this.tags);
		
		if (this.retired)
			data.setField("Retired", true);
		
		RecordStruct ret = fields.getFieldAsRecord(this.name);
		
		if (ret == null)
			ret = new RecordStruct();

		ret.setField(this.subkey, data);
		
		return ret;		
	}

	// TODO tags
	public static DbField buildImport(String name, RecordStruct data) {
		ListField fld = new ListField(name, data.getFieldAsString("Sid"), data.getFieldAsAny("Data"));
		
		return fld;
	}
}
