package divconq.web.http;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;

public class HttpContentCompressor extends io.netty.handler.codec.http.HttpContentCompressor {
	@Override
	protected Result beginEncode(HttpResponse headers, String acceptEncoding) throws Exception {
        String contentEncoding = headers.headers().get(HttpHeaders.Names.CONTENT_ENCODING);
        
        // identity should mean "no encoding"
        if (HttpHeaders.Values.IDENTITY.equalsIgnoreCase(contentEncoding)) 
            return null;

		return super.beginEncode(headers, acceptEncoding);
	}
}
