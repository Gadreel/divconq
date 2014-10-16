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

import java.util.HashMap;
import java.util.Map;

import divconq.hub.DomainInfo;
import divconq.hub.Hub;
import divconq.interchange.CommonPath;
import divconq.util.IOUtil;
import divconq.web.WebExtension;

public class DcwExtension extends WebExtension {
	protected Map<String, AssetInfo> assetviews = new HashMap<String, AssetInfo>();	

	public AssetInfo findAsset(String path) {
		return this.assetviews.get(path);
	}
	
	@Override
	public void online() {
		try {
			try {
				// provide the JSON schema defs
				// load the schema into JSON...
				byte[] jsondef = IOUtil.charsEncodeAndCompress(Hub.instance.getSchema().toJsonDef().toString());
				
				String path = "/Asset/Core/Schema";
				
				AssetInfo info = new AssetInfo(new CommonPath(path), jsondef);
				info.setMime("application/json");
				info.setCompressed(true);
				this.assetviews.put(path, info);
			}
			catch (Exception x) {			
			}
			
			for (DomainInfo d : Hub.instance.getDomains()) {
				final String did = d.getId();
				
				DcwDomain domain = new DcwDomain();
				domain.init(DcwExtension.this, did);
				DcwExtension.this.dsitemap.put(did, domain);		
			}
		}
		catch (Exception x) {
			// TODO
		}
	}	
}
