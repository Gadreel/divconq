package divconq.ctp.net;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import divconq.ctp.CtpAdapter;
import divconq.ctp.CtpConstants;
import divconq.ctp.f.BlockCommand;
import divconq.ctp.f.CtpFCommand;
import divconq.ctp.cmd.ProgressCommand;
import divconq.filestore.CommonPath;
import divconq.filestore.IFileSelector;
import divconq.filestore.IFileStoreFile;
import divconq.filestore.ISyncFileCollection;
import divconq.filestore.select.FileSelection;
import divconq.hub.Hub;
import divconq.lang.op.FuncCallback;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.work.IWork;
import divconq.work.TaskRun;

public class CtpReadWork extends FuncCallback<IFileStoreFile> implements IWork {
	protected CtpAdapter adapter = null;
	protected IFileSelector selector = null;
	protected ChannelFutureListener future = null;
	protected CommonPath relativeTo = CommonPath.ROOT;
	
	public void setAdapter(CtpAdapter v) {
		this.adapter = v;
	}
	
	public void setSelector(IFileSelector v) {
		this.selector = v;
		this.relativeTo = v.path();
	}
	
	public void setFuture(ChannelFutureListener v) {
		this.future = v;
	}
	
	@Override
	public void run(TaskRun trun) {
		if (this.selector instanceof ISyncFileCollection) {
			// TODO optimize, fill a whole buffer before writing
			
			FuncResult<IFileStoreFile> res = ((ISyncFileCollection)this.selector).next();
			
			if (res.hasErrors()) {
				// TODO what to do here...ABORT
				System.out.println("abort - could not select next file");
				return;
			}
			
			this.setResult(res.getResult());
			this.resetCalledFlag();
			this.callback();
		}
		else {
			this.selector.next(this);
		}
	}

	@Override
	public void callback() {
		try {
			// TODO if errors...ABORT
			
			if (this.isEmptyResult()) {
				this.adapter.sendCommand(CtpFCommand.STREAM_FINAL);
				OperationContext.get().getTaskRun().complete();
				return;
			}
			
			IFileStoreFile file = this.getResult();
			
			BlockCommand cmd = new BlockCommand();
			
			FileSelection selection = this.selector.selection();
			
			// TODO if CTP_F_ATTR_PREFERED then use session settings - from adapter?
			
			if (selection.hasAttr(CtpConstants.CTP_F_ATTR_PATH) || selection.hasAttr(CtpConstants.CTP_F_ATTR_PREFERED))
				cmd.setPath(file.path().subpath(this.relativeTo).toString());
			
			if (selection.hasAttr(CtpConstants.CTP_F_ATTR_IS_FOLDER) || selection.hasAttr(CtpConstants.CTP_F_ATTR_PREFERED))
				cmd.setIsFolder(file.isFolder());
			
			if (selection.hasAttr(CtpConstants.CTP_F_ATTR_SIZE) || selection.hasAttr(CtpConstants.CTP_F_ATTR_PREFERED))
				cmd.setSize(file.getSize());
			
			if (selection.hasAttr(CtpConstants.CTP_F_ATTR_MODTIME) || selection.hasAttr(CtpConstants.CTP_F_ATTR_PREFERED))
				cmd.setModTime(file.getModificationTime().getMillis());
			
			if (selection.hasAttr(CtpConstants.CTP_F_ATTR_PERMISSIONS) || selection.hasAttr(CtpConstants.CTP_F_ATTR_PREFERED))
				cmd.setPermissions(CtpConstants.CTP_F_PERMISSIONS_READ & CtpConstants.CTP_F_PERMISSIONS_WRITE);   // TODO file.getPermissions());
			
			
			if (selection.hasAttr(CtpConstants.CTP_F_ATTR_DATA)) {
				// send headers
				this.adapter.sendCommand(cmd);
				
				// send block 1
				cmd = new BlockCommand();
				
				ByteBuf d = Hub.instance.getBufferAllocator().buffer(24);
				
				d.writeLong(1);
				d.writeLong(2);
				d.writeLong(3);
				
				cmd.setData(d);
				
				this.adapter.sendCommand(cmd);
				
				// progress 1
				this.adapter.sendCommand(new ProgressCommand(33));
				
				// send block 2
				cmd = new BlockCommand();
				
				d = Hub.instance.getBufferAllocator().buffer(24);
				
				d.writeLong(1);
				d.writeLong(2);
				d.writeLong(3);
				
				cmd.setData(d);
				
				this.adapter.sendCommand(cmd);
				
				// progress 2
				this.adapter.sendCommand(new ProgressCommand(66));
				
				// send block 3
				cmd = new BlockCommand();
				
				d = Hub.instance.getBufferAllocator().buffer(24);
				
				d.writeLong(1);
				d.writeLong(2);
				d.writeLong(3);
				
				cmd.setData(d);
				
				this.adapter.sendCommand(cmd);
				
				// progress 3
				this.adapter.sendCommand(new ProgressCommand(99));
				
				// send end
				cmd = new BlockCommand();
				cmd.setEof(true);
				
				this.adapter.sendCommandNotify(cmd, this.future);
			}
			else {
				cmd.setEof(true);
				
				this.adapter.sendCommandNotify(cmd, this.future);
			}
		}
		catch (Exception x) {
			System.out.println("Ctp-F Server error: " + x);
		}
	}
}
