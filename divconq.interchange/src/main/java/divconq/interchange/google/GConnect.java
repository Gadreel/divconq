package divconq.interchange.google;

import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;

import divconq.hub.DomainInfo;
import divconq.hub.Hub;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.struct.CompositeParser;
import divconq.struct.CompositeStruct;
import divconq.struct.RecordStruct;
import divconq.xml.XElement;

public class GConnect {
	// TODO error handling

	static public String allocateToken() {
		DomainInfo di = OperationContext.get().getDomain();
		
		XElement settings = di.getSettings();
		
		if (settings == null)
			return null;
		
		String apiid = Hub.instance.getResources().isForTesting() ? "Google-Test" : "Google";
		
		for (XElement api : settings.selectAll("ApiList/Api")) {
			if (!apiid.equals(api.getAttribute("Id")))
				continue;
			
			String clientId = api.getAttribute("ClientId");
			String clientSecret = api.getAttribute("ClientSecret");
			String rtoken = api.getAttribute("RefreshToken");
			
			clientId = Hub.instance.getClock().getObfuscator().decryptHexToString(clientId);
			clientSecret = Hub.instance.getClock().getObfuscator().decryptHexToString(clientSecret);
			rtoken = Hub.instance.getClock().getObfuscator().decryptHexToString(rtoken);
			
			return GConnect.allocateToken(clientId, clientSecret, rtoken);
		}
		
		return null;
	}
	
	static public String allocateToken(String clientId, String clientSecret, String rtoken) {
        OAuthRequest request = new OAuthRequest(Verb.POST, "https://accounts.google.com/o/oauth2/token");
        request.addBodyParameter("grant_type", "refresh_token");
        request.addBodyParameter("refresh_token", rtoken); 
        request.addBodyParameter("client_id", clientId);
        request.addBodyParameter("client_secret", clientSecret);
        
        Response response = request.send();
        
        FuncResult<CompositeStruct> h = CompositeParser.parseJson(response.getBody());
        
        if (h.isNotEmptyResult()) 
        	return ((RecordStruct) h.getResult()).getFieldAsString("access_token");
        
        return null;
	}
}
