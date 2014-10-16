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
package divconq.test;

/*
import java.io.IOException;
import java.net.Socket;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;
*/

public class TestXmlRpc {
	public static void main(String[] args) {		
		/*
        HttpParams params = new SyncBasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUserAgent(params, "HttpComponents/1.1");
        HttpProtocolParams.setUseExpectContinue(params, true);

        HttpProcessor httpproc = new ImmutableHttpProcessor(new HttpRequestInterceptor[] {
                // Required protocol interceptors
                new RequestContent(),
                new RequestTargetHost(),
                // Recommended protocol interceptors
                new RequestConnControl(),
                new RequestUserAgent(),
                new RequestExpectContinue()});
        
        HttpRequestExecutor httpexecutor = new HttpRequestExecutor();
        
        HttpContext context = new BasicHttpContext(null);
        HttpHost host = new HttpHost("www.divconq.com", 80);

        DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
        ConnectionReuseStrategy connStrategy = new DefaultConnectionReuseStrategy();

        context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
        context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);

        try {
            
            String target = "/xmlrpc.php";
            
            if (!conn.isOpen()) {
                Socket socket = new Socket(host.getHostName(), host.getPort());
                conn.bind(socket, params);
            }
            
            BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("POST", target);
            request.setEntity(new StringEntity(
                    "<?xml version=\"1.0\"?><methodCall><methodName>wp.getPage</methodName><params>"
                    		+ "<param><value><int>1</int></value></param>"
                    		+ "<param><value><int>566</int></value></param>"
            		+ "<param><value><string>Andy_White</string></value></param>"
            		+ "<param><value><string>s2d3f4</string></value></param>"
            		+ "</params></methodCall>", "UTF-8"));
            
            System.out.println(">> Request URI: " + request.getRequestLine().getUri());
            
            request.setParams(params);
            httpexecutor.preProcess(request, httpproc, context);
            HttpResponse response = httpexecutor.execute(request, conn, context);
            response.setParams(params);
            httpexecutor.postProcess(response, httpproc, context);
            
            System.out.println("<< Response: " + response.getStatusLine());
            System.out.println(EntityUtils.toString(response.getEntity()));
            System.out.println("==============");
            if (!connStrategy.keepAlive(response, context)) {
                conn.close();
            } else {
                System.out.println("Connection kept alive...");
            }
        } 
        catch (Exception x) {
        	System.out.println("err: " + x);
		} 
        finally {
            try {
				conn.close();
			} 
            catch (IOException x) {
			}
        }
		*/
		
        /*
		
		<?xml version="1.0"?>
		<methodCall>
		  <methodName>wp.getUsersBlogs</methodName>
		  <params>
		    <param>
		        <value><string>40</string></value>
		    </param>
		  </params>
		</methodCall>
		*/
	}

}
