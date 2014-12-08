package divconq.db.update;

import divconq.struct.RecordStruct;

/**
 * Base class for supporting the four database structures: StaticScalar, StaticList,
 * DynamicScalar and DynamicList
 * 
 * @author Andy
 *
 */
abstract public class DbField {
	protected String name = null;
	protected Object value = null;
	protected FieldTags tags = null;
	protected boolean retired = false;
	
	/**
	 * @return field name
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * @return field value
	 */
	public Object getValue() {
		return this.value;
	}
	
	/**
	 * @return field is deleted
	 */
	public boolean isDeleted() {
		return this.retired;
	}
	
	/**
	 * @param v true if this field entry is to be treated as deleted
	 */
	public void setDeleted(boolean v) {
		this.retired = v;
	}
	
	/**
	 * Tags are a way to add meta data to a field.  Alphanumeric and ':' are allowed in tags.
	 * 
	 * @return field tags
	 */
	public String getTags() {
		return (this.tags != null) ? this.tags.tags : null;
	}
	
	/**
	 * @param name of field
	 * @param value of field
	 * @param tags meta data for the field (alphanumeric and ':' are allowed in tag)
	 */
	public DbField(String name, Object value, FieldTags tags) {
		this.name = name;
		this.value = value;
		this.tags = tags;
	}
	
	/**
	 * Marks a field as Retired.
	 * 
	 * @param name of field
	 * @param value of field
	 * @param tags meta data for the field (alphanumeric and ':' are allowed in tag)
	 */
	public DbField(String name, FieldTags tags) {
		this.name = name;
		this.retired = true;
		this.tags = tags;
	}
	
	abstract public RecordStruct getParams(RecordStruct fields);
}
