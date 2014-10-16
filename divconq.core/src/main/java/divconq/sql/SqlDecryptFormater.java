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
package divconq.sql;

import divconq.hub.Hub;
import divconq.sql.ISqlFormater;
import divconq.struct.Struct;

public class SqlDecryptFormater implements ISqlFormater {
	final static public SqlDecryptFormater instance = new SqlDecryptFormater(); 

	@Override
	public Object format(Object v) {
		CharSequence cs = Struct.objectToCharsStrict(v);
		
		if (cs == null)
			return null;
		
		return Hub.instance.getClock().getObfuscator().decryptHexToString(cs);
	}
}
