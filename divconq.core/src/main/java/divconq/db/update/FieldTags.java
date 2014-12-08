package divconq.db.update;

import divconq.util.StringUtil;

/**
 * Meta data about the value/date/etc of the field.  Could be a measure of the 
 * quality of data, reference to source of data or what ever.
 * 
 * @author Andy
 *
 */
public class FieldTags {
	protected String tags = null;
	
	/**
	 * @param tags meta data for the field (alphanumeric and ':' are allowed in tag)
	 */
	public FieldTags(String... tags) {
		if ((tags != null) && (tags.length > 0))
			this.tags = "|" + StringUtil.join(tags, "|") + "|";
	}
}
