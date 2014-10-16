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
package divconq.web;

import divconq.web.AssetInfo;
import divconq.web.WebContext;
import divconq.web.WebDomain;

public class DcwDomain extends WebDomain {	
	@Override
	protected AssetInfo findAsset(String name, WebContext ctx) {
		return ((DcwExtension)this.extension).findAsset(name);
	}
}
