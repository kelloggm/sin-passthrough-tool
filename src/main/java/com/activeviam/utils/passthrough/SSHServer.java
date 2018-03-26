package com.activeviam.utils.passthrough;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * SSH Server running in cloud.
 */
public abstract class SSHServer {

	private static final Logger LOGGER = LoggerFactory.getLogger(SSHServer.class);
	protected static final JSch jsch = new JSch();
	private final Redirection[] redirections;

	protected KeyPair keys;
	protected String publicKey;
	protected String publicIP;
	protected Session session;
	protected String user;
	protected String id = String.valueOf(UUID.randomUUID());

	public SSHServer(Redirection... redirections) {

		LOGGER.info("Setup a new SSH Cloud Server");

		this.redirections = redirections;

		this.generateSSHKeys();
		this.deploy();

	}

	protected abstract void deploy();

	protected abstract void shutdown();

	protected int[] getExposedPorts() {
		final int[] exposedPorts = new int[this.redirections.length + 1];
		final Redirection[] redirections1 = this.redirections;
		for (int i = 0; i < redirections1.length; i++) {
			Redirection r = redirections1[i];
			exposedPorts[i] = r.getRemotePort();
		}
		exposedPorts[this.redirections.length] = 22;
		return exposedPorts;
	}

	protected void setupSSHServer() {
		LOGGER.info("Connecting to server and setup...");

		if (!tryToConnect())
			return;

		LOGGER.debug("Connected");
		LOGGER.debug("Copying SSH keys");
		execCommand("sudo bash -c 'cp -R ~/.ssh/authorized_keys /root/.ssh/authorized_keys'");
		user = "root";
		LOGGER.debug("Editing SSH configuration");
		execCommand("sudo bash -c 'echo \"GatewayPorts clientspecified\" >> /etc/ssh/sshd_config'");
		execCommand("sudo bash -c 'echo \"PermitRootLogin yes\" >> /etc/ssh/sshd_config'");

		LOGGER.debug("Restarting SSH server to apply new configuration");
		execCommand("sudo service ssh reload");
		LOGGER.debug("Logout");
		logout();

		LOGGER.info("Node is ready");
	}

	private void logout() {
		session.disconnect();
		session = null;
	}

	protected int execCommand(String s) {
		try {
			final ChannelExec exec = (ChannelExec) session.openChannel("exec");
			exec.setCommand(s);

			if (LOGGER.isDebugEnabled())
				LOGGER.debug(session.getUserName() + "@" + session.getHost() + "# " + s);

			InputStream in = exec.getInputStream();

			exec.connect();

			byte[] tmp = new byte[1024];
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
				}
				if (exec.isClosed()) {
					if (in.available() > 0)
						continue;
					break;
				}
			}
			return exec.getExitStatus();
		} catch (JSchException | IOException e) {
			e.printStackTrace();
		}
		return -1;
	}

	protected boolean tryToConnect() {
		final boolean connect = tryToConnect(100);
		if (!connect && LOGGER.isInfoEnabled()) {
			LOGGER.info("Failed to connect to SSH server with more than 100 tries.");
		}
		return connect;
	}

	private boolean tryToConnect(int ttl) {
		if (ttl == -1)
			return false;
		try {
			this.connect();
			return true;
		} catch (JSchException e) {
			LOGGER.info("Server not ready, wait 2 seconds");
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e1) {
				LOGGER.error("Interrupted on setup");
			}
			return tryToConnect(ttl - 1);
		}

	}

	private void generateSSHKeys() {
		LOGGER.debug("Generating SSH Keys");

		try {
			keys = KeyPair.genKeyPair(new JSch(), KeyPair.RSA);
		} catch (JSchException e) {
			LOGGER.error("Unable to generate SSH keys", e);
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		keys.writePublicKey(baos, "ssh-client@local");
		publicKey = baos.toString();
	}

	public void connect() throws JSchException {
		jsch.getIdentityRepository().add(this.keys.forSSHAgent());
		session = jsch.getSession(getInstanceUsername(), getInstanceHost(), 22);
		session.setHostKeyRepository(new LazyKnownHostSSHClient());
		session.connect();
	}

	public void redirect() throws JSchException {

		if (session == null) {
			tryToConnect();
		}

		for (Redirection r : redirections) {
			final int remotePort = r.getRemotePort();
			if (remotePort > 0 && remotePort < 1024 && !getInstanceUsername().equals("root")) {
				LOGGER.error("Unable to open a root-only port with a standard user account");
				throw new IllegalArgumentException("Port " + remotePort + " is not allowed in that configuration");
			}
			session.setPortForwardingR("0.0.0.0", remotePort, r.getDestinationHost(), r.getDestinationPort());
		}
	}

	public String getInstanceUsername() {
		if (user != null)
			return user;
		return "ubuntu";
	}

	public String getInstanceHost() {
		return publicIP;
	}

	public boolean isConnected() {
		return session != null && session.isConnected();
	}
}
