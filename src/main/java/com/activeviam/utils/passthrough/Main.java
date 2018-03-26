package com.activeviam.utils.passthrough;

import com.jcraft.jsch.JSchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	private static final Logger LOGGER = LoggerFactory.getLogger(SSHServer.class);

	public static void main(String... args) {
		final Redirection[] r = Redirection.parse(args);
		final String provider = ConfigurationProvider.get("cloud.provider");
		SSHServer server = null;

		if (provider != null) {
			final CloudProvider instance = CloudProvider.getInstance();
			if (instance.isAWS()) {
				server = new EC2SSHServer(r);
			}
		}

		if(server == null) {
			LOGGER.error("No SSH server available");
			System.exit(2);
		}

		try {
			server.redirect();
			System.out.println("Your services are available from " + server.getInstanceHost());
			System.out.println("Simply SIGINT this process to stop redirection");
		} catch (JSchException e) {
			LOGGER.error("Unable to setup redirections", e);
		}
	}
}
