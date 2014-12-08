package divconq.db.update;

import divconq.struct.FieldStruct;
import divconq.struct.RecordStruct;

/**
 * Use this field to update StaticSalar structures in dcDatabase.
 * 
 * @author Andy
 *
 */
public class ScalarField extends DbField {
	/**
	 * @param name of field
	 * @param value of field
	 * @param tags meta data for the field (alphanumeric and ':' are allowed in tag)
	 */
	public ScalarField(String name, Object value, FieldTags tags) {
		super(name, value, tags);
	}
	
	/**
	 * @param name of field
	 * @param value of field
	 */
	public ScalarField(String name, Object value) {
		super(name, value, null);
	}
	
	/**
	 * Mark a field as Retired
	 * 
	 * @param name of field
	 * @param tags meta data for the field (alphanumeric and ':' are allowed in tag)
	 */
	public ScalarField(String name, FieldTags tags) {
		super(name, tags);
	}
	
	/**
	 * Mark a field as Retired
	 */
	public ScalarField(String name) {
		super(name, null);
	}

	@Override
	public RecordStruct getParams(RecordStruct fields) {
		RecordStruct ret = new RecordStruct( 
				new FieldStruct("Data", this.value)
		);
		
		if (this.tags != null)
			ret.setField("Tags", this.tags);
		
		if (this.retired)
			ret.setField("Retired", true);
		
		return ret;
	}

	// TODO tags
	public static DbField buildImport(String name, RecordStruct data) {
		ScalarField fld = new ScalarField(name, data.getFieldAsAny("Data"));
		
		return fld;
	}
}
