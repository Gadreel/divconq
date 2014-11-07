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
package divconq.interchange.mws;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;

import org.joda.time.format.ISODateTimeFormat;

import divconq.lang.op.FuncResult;
import divconq.util.StringUtil;
import divconq.xml.XElement;
import divconq.xml.XmlReader;

public class MwsServices {
	static final public String ORDERS_API = "/Orders/2013-09-01";
	
	protected URI endpoint = null;
	protected String awsKeyId = null;
	protected String awsSecretKey = null;
	protected String awsSeller = null;
	protected String marketplaceId = null;
	
	public void setAwsKeyId(String awsKeyId) {
		this.awsKeyId = awsKeyId;
	}
	
	public MwsServices(String endpoint, String awsKeyId, String awsSecretKey, String awsSeller, String marketplaceId) {
		this.endpoint = Endpoints.lookup(endpoint);
		this.awsKeyId = awsKeyId;
		this.awsSecretKey = awsSecretKey;
		this.awsSeller = awsSeller;
		this.marketplaceId = marketplaceId;
	}
	
	public FuncResult<XElement> executeListOrders(String stamp) {
        Map<String, String> params = new HashMap<String, String>();
        
        if (StringUtil.isNotEmpty(stamp))
        	params.put("LastUpdatedAfter", stamp);
        
    	params.put("MarketplaceId.Id.1", this.marketplaceId);
    	
    	return this.execute(MwsServices.ORDERS_API, "ListOrders", params);
	}
	
	public FuncResult<XElement> executeListOrderItems(String oid) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("AmazonOrderId", oid);
    	
    	return this.execute(MwsServices.ORDERS_API, "ListOrderItems", params);
	}
	
	public FuncResult<XElement> execute(String path, String action, Map<String, String> params) {
		FuncResult<XElement> res = new FuncResult<>();
		
		try {
	        URI ep = this.endpoint.resolve(path);
	        
			URL url = ep.toURL();
			
			String urlParameters = "";
			
	        Map<String, String> sorted = new TreeMap<String, String>();
	        sorted.put("Action", action);
	        sorted.put("AWSAccessKeyId", this.awsKeyId);
	        sorted.put("SellerId", this.awsSeller);
	        
	        sorted.put("SignatureMethod", "HmacSHA256");
	        sorted.put("SignatureVersion", "2");
	        sorted.put("Timestamp", ISODateTimeFormat.dateTime().withZoneUTC().print(System.currentTimeMillis()));  
	        sorted.put("Version", "2013-09-01");
	        
	        sorted.putAll(params);
	        
	        boolean firstparam = true;
	        
	        for (Entry<String, String> pair : sorted.entrySet()) {
	        	if (firstparam)
	        		firstparam = false;
	        	else
	            	urlParameters += "&";
	        		
	            urlParameters += Util.urlEncode(pair.getKey(), false) + "=" + Util.urlEncode(pair.getValue(), false);
	        }
			
	        String algorithm = "HmacSHA256";
	        String stringToSign = null;
	
	        StringBuilder data = new StringBuilder();
	        
	        data.append("POST\n");
	        
	        data.append(ep.getHost().toLowerCase());
	        
	        if (!Util.usesStandardPort(ep)) {
	            data.append(":");
	            data.append(ep.getPort());
	        }
	        
	        data.append("\n");
	        
	        data.append(Util.urlEncode(ep.getPath(), true)
	        		+ "\n" + urlParameters);
	        
	        stringToSign = data.toString();
	
	        String x = Util.sign(stringToSign, this.awsSecretKey, algorithm);
	        
	        //System.out.println("Got: " + x);
	        
	        urlParameters += "&Signature=" + Util.urlEncode(x, false); 
	
			res.debug("Sending 'POST' request to URL : " + ep);
			HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
	 
			con.setRequestMethod("POST");
			con.setRequestProperty("User-Agent", "DivConq/1.0 (Language=Java/8)");
	 
			// Send post request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();
	 
			int responseCode = con.getResponseCode();
			res.debug("Response Code : " + responseCode);
	 
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			
			String inputLine;
			StringBuffer response = new StringBuffer();
	 
			while ((inputLine = in.readLine()) != null) 
				response.append(inputLine);
			
			in.close();
	 
			FuncResult<XElement> xres = XmlReader.parse(response.toString(), false);
			
			res.setResult(xres.getResult());
       }
	   catch (Exception x) {
		   res.error("Error loading result" + x);
	   }
		
		return res;
	}
}
