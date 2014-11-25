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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.Session;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthPassword;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;

import divconq.api.ApiSession;
import divconq.log.Logger;
import divconq.mod.ModuleBase;

public class SshdModule extends ModuleBase {
    protected SshServer sshd = null;
    protected Map<Session, ApiSession> apimap = new HashMap<>(); 

    public ApiSession getApiSession(Session session) {
    	return this.apimap.get(session);
    }

	@Override
	public void start() {
		this.sshd = SshServer.setUpDefaultServer();
		this.sshd.setPort(8022);
		this.sshd.setHost("192.168.0.13");
		this.sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(
				"/Work/Keys/testsshdkeys"));

		List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<NamedFactory<UserAuth>>();
		// userAuthFactories.add(new UserAuthPublicKey.Factory());
		userAuthFactories.add(new UserAuthPassword.Factory());
		this.sshd.setUserAuthFactories(userAuthFactories);
		/*
		 * this.sshd.setPublickeyAuthenticator(new PublickeyAuthenticator() {
		 * 
		 * @Override public boolean authenticate(String user, PublicKey key,
		 * ServerSession session) { //session. return "jack".equals(user); } });
		 */

		this.sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
			@Override
			public boolean authenticate(String user, String password,
					ServerSession sess) {
				boolean auth = "jack".equals(user);

				if (auth) {
					ApiSession apisess = SshdModule.this.apimap.get(sess);

					if (apisess == null) {
						apisess = ApiSession.createLocalSession("root");
						SshdModule.this.apimap.put(sess, apisess);
						System.out.println("Adding ssh session: " + sess.getId());
					} else
						System.out.println("Reusing ssh session: " + sess.getId());
				}

				return auth;
			}
		});

		// this.sshd.setCommandFactory(new ScpCommandFactory(new CommonCommandFactory()));
		this.sshd.setCommandFactory(new ScpCommandFactory());

		List<NamedFactory<Command>> namedFactoryList = new ArrayList<NamedFactory<Command>>();
		namedFactoryList.add(new SftpSubsystem.Factory());
		this.sshd.setSubsystemFactories(namedFactoryList);

		this.sshd.setFileSystemFactory(new FileSystemFactoryImpl(this));

		try {
			this.sshd.start();
		} catch (Exception x) {
			Logger.error("Error starting sshd module: " + x);
		}
	}
	
	@Override
	public void stop() {
        try {
        	this.sshd.stop();
        } 
        catch (Exception x) {
            Logger.error("Error stopping sshd module: " + x);
        }
	}
}
