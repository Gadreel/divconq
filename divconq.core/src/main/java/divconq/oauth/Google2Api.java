package divconq.oauth;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.scribe.builder.api.DefaultApi20;
import org.scribe.exceptions.OAuthException;
import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.model.OAuthConfig;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuth20ServiceImpl;
import org.scribe.oauth.OAuthService;
import org.scribe.utils.OAuthEncoder;
import org.scribe.utils.Preconditions;

/**
 * Google OAuth2.0 
 * Released under the same license as scribe (MIT License)
 * @author yincrash
 * 
 * find at https://gist.github.com/yincrash/2465453
 * 
 * even better would be to grab from:
 * https://github.com/codolutions/scribe-java
 * 
 * above is best and most current I can find, but need to collect whole code from there
 * 
 */
public class Google2Api extends DefaultApi20 {
	  public static final String REFRESH_TOKEN = "refresh_token";
	  public static final String GRANT_TYPE = "grant_type";
	  public static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
	  public static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
	  public static final String ACCESS_TYPE = "access_type";
	  public static final String ACCESS_TYPE_OFFLINE = "offline";
	  public static final String APPROVAL_PROMPT = "approval_prompt";
	  public static final String APPROVAL_PROMPT_FORCE = "force";	

    private static final String AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/auth?response_type=code&client_id=%s&redirect_uri=%s";
    private static final String SCOPED_AUTHORIZE_URL = AUTHORIZE_URL + "&scope=%s";
    private static final String SUFFIX_OFFLINE = "&" + ACCESS_TYPE + "=" + ACCESS_TYPE_OFFLINE
            + "&" + APPROVAL_PROMPT + "=" + APPROVAL_PROMPT_FORCE;

    
    @Override
    public String getAccessTokenEndpoint() {
        return "https://accounts.google.com/o/oauth2/token";
    }
    
    @Override
    public AccessTokenExtractor getAccessTokenExtractor() {
        return new AccessTokenExtractor() {
            
            @Override
            public Token extract(String response) {
                Preconditions.checkEmptyString(response, "Response body is incorrect. Can't extract a token from an empty string");

                Matcher matcher = Pattern.compile("\"access_token\" : \"([^&\"]+)\"").matcher(response);
                if (matcher.find())
                {
                  String token = OAuthEncoder.decode(matcher.group(1));
                  return new Token(token, "", response);
                } 
                else
                {
                  throw new OAuthException("Response body is incorrect. Can't extract a token from this: '" + response + "'", null);
                }
            }
        };
    }

    @Override
    public String getAuthorizationUrl(OAuthConfig config) {
    	//  + SUFFIX_OFFLINE TODO not used unless in offline mode
    	
        // Append scope if present
        if (config.hasScope()) {
            return String.format(SCOPED_AUTHORIZE_URL + SUFFIX_OFFLINE, config.getApiKey(),
                    OAuthEncoder.encode(config.getCallback()),
                    OAuthEncoder.encode(config.getScope()));
        } else {
            return String.format(AUTHORIZE_URL + SUFFIX_OFFLINE, config.getApiKey(),
                    OAuthEncoder.encode(config.getCallback()));
        }
    }
    
    @Override
    public Verb getAccessTokenVerb() {
        return Verb.POST;
    }
    
    @Override
    public OAuthService createService(OAuthConfig config) {
        return new GoogleOAuth2Service(config);
    }
    
    private class GoogleOAuth2Service extends OAuth20ServiceImpl {

        private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
        private static final String GRANT_TYPE = "grant_type";
        //private DefaultApi20 api;
        private OAuthConfig config;

        public GoogleOAuth2Service(OAuthConfig config) {
            super(Google2Api.this, config);
            //this.api = api;
            this.config = config;
        }
        
        @Override
        public Token getAccessToken(Token requestToken, Verifier verifier) {
            OAuthRequest request = new OAuthRequest(Google2Api.this.getAccessTokenVerb(), Google2Api.this.getAccessTokenEndpoint());
            
            switch (Google2Api.this.getAccessTokenVerb()) {
            case POST:
                request.addBodyParameter(OAuthConstants.CLIENT_ID, config.getApiKey());
                
                if (config.getApiSecret() != null && config.getApiSecret().length() > 0)
                    request.addBodyParameter(OAuthConstants.CLIENT_SECRET, config.getApiSecret());
                
                if (requestToken == null) {
                    request.addBodyParameter(OAuthConstants.CODE, verifier.getValue());
                    request.addBodyParameter(OAuthConstants.REDIRECT_URI, config.getCallback());
                    request.addBodyParameter(GRANT_TYPE, GRANT_TYPE_AUTHORIZATION_CODE);
                } else {
                    request.addBodyParameter(REFRESH_TOKEN, requestToken.getSecret());
                    request.addBodyParameter(GRANT_TYPE, GRANT_TYPE_REFRESH_TOKEN);
                }
                
                //request.addBodyParameter(OAuthConstants.CODE, verifier.getValue());
                //request.addBodyParameter(OAuthConstants.REDIRECT_URI, config.getCallback());
                //request.addBodyParameter(GRANT_TYPE, GRANT_TYPE_AUTHORIZATION_CODE);
                
                break;
            case GET:
            default:
                request.addQuerystringParameter(OAuthConstants.CLIENT_ID, config.getApiKey());
                
                if (config.getApiSecret() != null && config.getApiSecret().length() > 0)
                    request.addBodyParameter(OAuthConstants.CLIENT_SECRET, config.getApiSecret());
                
                request.addQuerystringParameter(OAuthConstants.CODE, verifier.getValue());
                request.addQuerystringParameter(OAuthConstants.REDIRECT_URI, config.getCallback());
                if(config.hasScope()) request.addQuerystringParameter(OAuthConstants.SCOPE, config.getScope());
            }
            
            Response response = request.send();
            
            return Google2Api.this.getAccessTokenExtractor().extract(response.getBody());
        }
    }

}