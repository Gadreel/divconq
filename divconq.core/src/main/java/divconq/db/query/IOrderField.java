package divconq.db.query;

import divconq.struct.Struct;

/**
 * A field may be used as a rule for how to order results.
 * 
 * @author Andy
 *
 */
public interface IOrderField {
	/**
	 * @return query parameters for this selected field.
	 */
	Struct getParams();
}
