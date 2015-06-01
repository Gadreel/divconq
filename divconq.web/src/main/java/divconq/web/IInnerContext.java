package divconq.web;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.stream.ChunkedInput;

public interface IInnerContext {
    Request getRequest();
    Response getResponse();
    WebDomain getDomain();
    IWebMacro getMacro(String name);

	void send();
	void sendStart(int contentLength);
	void send(ByteBuf content);
	void send(ChunkedInput<HttpContent> content);
	void sendEnd();
	void close();
}
