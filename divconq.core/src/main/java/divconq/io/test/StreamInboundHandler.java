package divconq.io.test;

import java.nio.file.Paths;

import divconq.hub.Hub;
import divconq.interchange.FileSystemFile;
import divconq.io.ctp.ClientHelloMessage;
import divconq.io.ctp.CtpMessage;
import divconq.io.ctp.CtpMessageDecoder;
import divconq.io.stream.CtpStreamSource;
import divconq.io.stream.StreamMessage;
import divconq.io.stream.StreamUtil;
import divconq.io.stream.TerminateStream;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationObserver;
import divconq.work.Task;
import divconq.work.TaskRun;
import io.netty.channel.ChannelHandlerContext;

/**
 * what needs to happen is CtpStreamSource gets a ref of StreamInboundHandler
 * and methods to say "give me more".
 * 
 *  the underlying ByteToMessageDecoder needs to have a "resume" feature
 *  that keeps reading from the current buffer and processing the
 *  messages
 *  
 *  rather than StreamInboundHandler being in the pipeline, probably it should just be a class
 *  that is handed to CtpStreamDecoder as one option for message handling.
 *  
 *  so this class may become obsolete as this code may begin to merge with CtpStreamSource.
 *  switching to Shared then is merely replacing CtpStreamSource with the switching handler
 *  and then in turn replacing with the shared handler.
 *  
 * @author andy
 *
 */
public class StreamInboundHandler extends CtpMessageDecoder {
	protected CtpStreamSource src = null;
	protected TaskRun srun = null;
	
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
    	CtpMessage m = (CtpMessage) msg;
    	
    	if (m instanceof ClientHelloMessage) {
    		System.out.println("HELLO from client!");
        	
        	m.release();
    	}
    	else if (m instanceof StreamMessage) {
    		System.out.println("STREAM from client! " + ((StreamMessage)m).getPath());
    		
    		// TODO read from context not channel?  so give context to CTP Stream??
    		if (this.src == null) {
    			this.src = new CtpStreamSource(ctx.channel(), (StreamMessage)msg);
    			
    			FileSystemFile dest = StreamUtil.localFile(Paths.get("c:/temp/testtar"));
    			
    			Task t = new Task()
    				.withTitle("Streaming In Test")
    				.withTimeout(0);
    			
    			t.withObserver(new OperationObserver() {
    				@Override
    				public void completed(OperationContext or) {
    					System.out.println("Transfer In is complete!!");
    	    			StreamInboundHandler.this.chanauto = true;
    					ctx.read();
    				}
    			});
    			
    			this.srun = StreamUtil.composeStream(t, 
    				this.src, 
    				dest.allocDest(true));
    			
    			this.chanauto = false;
    			
    			Hub.instance.getWorkPool().submit(this.srun);
    		}
    		else {
    			System.out.println("Read resume !!!!!!!!!!!!!!");
    			
    			this.src.setNext((StreamMessage)msg);
    			this.srun.resume();
    		}
    	}
    	else if (m instanceof TerminateStream) {
    		System.out.println("TERM STREAM from client! ");
    		
			this.src.handle(StreamMessage.FINAL);
			this.src = null;
			this.srun = null;
	    	
	    	m.release();
    	}
    }

	@Override
	public void handleMessage() {
		// TODO Auto-generated method stub
		
	}

}
