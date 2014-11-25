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

package divconq.interchange.sshd;

import java.util.HashMap;
import java.util.Map;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.nativefs.NativeFileSystemView;

public class FileSystemFactoryImpl implements FileSystemFactory {
	protected SshdModule severinfo = null;
	
    public FileSystemFactoryImpl(SshdModule server) {
		this.severinfo = server;
	}

	public FileSystemView createFileSystemView(Session session) {
		Map<String, String> roots = new HashMap<>();
		
		roots.put("/", "/Work/Temp/Dest");
		
		return new NativeFileSystemView("brad", roots, "/");
		
		//ApiSession api = this.severinfo.getApiSession(session);
		
        //return new FileSystemViewImpl(api);
    }
}
