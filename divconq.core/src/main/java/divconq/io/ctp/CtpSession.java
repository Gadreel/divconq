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
package divconq.io.ctp;

import io.netty.channel.Channel;
import divconq.log.Logger;

public class CtpSession {
	protected Channel chan = null;
	
	public Channel getChannel() {
		return this.chan;
	}
	
	public void setChannel(Channel chan) {
		this.chan = chan;
	}
	
	public void close() {
		try {
			if (this.chan != null)
				this.chan.close().await(2000);
		} 
		catch (InterruptedException x) {
			// ignore 
		}
	}
	
	public void write(CtpMessage m) {
		try {
			if (this.chan != null) {
				this.chan.writeAndFlush(m);   
			}
		}
		catch (Exception x) {
			Logger.error("Error writing Ctp message: " + m);
			Logger.error("Error writing Ctp message: " + x);
			
			this.close();
		}
	}

	public void receiveMessage(CtpMessage msg) {        
	}
}
