package divconq.web;

import divconq.struct.RecordStruct;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.stream.ChunkedInput;

public interface IInnerContext {
    Request getRequest();
    Response getResponse();
    WebDomain getDomain();
    WebSite getSite();
    IWebMacro getMacro(String name);
    void setAltParams(RecordStruct v);
    RecordStruct getAltParams();

	void send();
	void sendStart(int contentLength);
	void send(ByteBuf content);
	void send(ChunkedInput<HttpContent> content);
	void sendEnd();
	void close();
}
