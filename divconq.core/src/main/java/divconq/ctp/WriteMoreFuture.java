package divconq.ctp;

import java.lang.ref.WeakReference;

import divconq.work.TaskRun;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

public class WriteMoreFuture implements ChannelFutureListener {
	protected WeakReference<TaskRun> runRef = null;
	
	public WriteMoreFuture(TaskRun run) {
		this.runRef = new WeakReference<>(run);
	}
	
	@Override
	public void operationComplete(ChannelFuture future) throws Exception {
		TaskRun r = this.runRef.get();
		
		if (r != null)
			r.resume();
		
		// TODO else error
	}
}
