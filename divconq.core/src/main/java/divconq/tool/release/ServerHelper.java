package divconq.tool.release;

import java.nio.file.Path;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import divconq.hub.Hub;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class ServerHelper {
	protected JSch jsch = new JSch();
	protected Session session = null;
	
	public boolean init(XElement connconfig) {
		try {
			String hostname = connconfig.getAttribute("Host");
			String username = connconfig.getAttribute("User");
	    	String password = connconfig.getAttribute("Password");
			String keyfile = connconfig.getAttribute("KeyFile");
	    	String passphrase = connconfig.getAttribute("Passphrase");
			
			int port = (int) StringUtil.parseInt(connconfig.getAttribute("Port"), 22);

	    	if (StringUtil.isNotEmpty(password))
	    		password = Hub.instance.getClock().getObfuscator().decryptHexToString(password).toString();
	    	
	    	String passwordx = password;
	    	
	    	if (StringUtil.isNotEmpty(passphrase))
	    		passphrase = Hub.instance.getClock().getObfuscator().decryptHexToString(passphrase).toString();
			
			if (StringUtil.isNotEmpty(keyfile)) 
				this.jsch.addIdentity(keyfile, passphrase);
			
			this.session = this.jsch.getSession(username, hostname, port);
			
	    	if (StringUtil.isNotEmpty(password))
	    		this.session.setPassword(password);

			this.session.setUserInfo(new UserInfo() {
				@Override
				public void showMessage(String message) {
					System.out.println("SSH session message: " + message);
				}

				@Override
				public boolean promptYesNo(String message) {
					return true;
				}

				@Override
				public boolean promptPassword(String message) {
					return false;
				}

				@Override
				public boolean promptPassphrase(String message) {
					return false;
				}

				@Override
				public String getPassword() {
					return passwordx;
				}

				@Override
				public String getPassphrase() {
					return null;
				}
			});

			this.session.connect(30000); // making a connection with timeout.
			this.session.setTimeout(20000);   // 20 second read timeout
		} 
		catch (Exception x) {
			System.out.println("Error initializing SSH session: " + x);
			return false;
		}
		
		return true;
	}

	public void close() {
		this.session.disconnect();
	}
	
	public Session session() {
		return this.session;
	}
	
	// intended to have a ./ before path 
	public boolean makeDirSftp(ChannelSftp sftp, Path path) {
		System.out.println("mkdir: " + path +  "  ------   " + path.getNameCount());
		
		// path "." should be there
		if (path.getNameCount() < 2)
			return true;
		
		//System.out.println("checking");
		
		try {
		    sftp.stat(path.toString());
		    return true;		// path is there 
		} 
		catch (Exception x) {
		}
		
		this.makeDirSftp(sftp, path.getParent());
		
		try {
			sftp.mkdir(path.toString());
    		sftp.chmod(493, path.toString());		// 755 octal = 493 dec
		} 
		catch (Exception x) {
			System.out.println("Failed to create directory: " + x);
			return false;
		}
		
		return true;
	}
}