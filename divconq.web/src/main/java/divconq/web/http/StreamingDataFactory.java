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
package divconq.web.http;

import java.nio.charset.Charset;

import divconq.http.multipart.DefaultHttpDataFactory;
import divconq.http.multipart.FileUpload;
import divconq.session.DataStreamChannel;

import io.netty.handler.codec.http.HttpRequest;

public class StreamingDataFactory extends DefaultHttpDataFactory {
	protected DataStreamChannel dsc = null;
	protected String op = null;
	
	public StreamingDataFactory(DataStreamChannel dsc, String op) {
		super(false);
		this.dsc = dsc;
		this.op = op;
	}
	
	@Override
	public FileUpload createFileUpload(HttpRequest request, String name, String filename, String contentType, String contentTransferEncoding, Charset charset, long size) {
		return new StreamingHttpData(this.dsc, this.op, filename, charset, size);
	}	
}
