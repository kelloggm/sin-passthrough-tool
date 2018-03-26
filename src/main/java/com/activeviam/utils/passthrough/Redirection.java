package com.activeviam.utils.passthrough;

import java.util.ArrayList;

public class Redirection {

	private int remotePort;
	private String destinationHost;
	private int destinationPort;

	public Redirection(String redirection) {
		final String[] redirections = redirection.split(":");
		int remotePort, localPort;
		String local = "localhost";
		switch (redirections.length) {
			case 2:
				remotePort = Integer.parseInt(redirections[0]);
				localPort = Integer.parseInt(redirections[1]);
				break;
			case 3:
				remotePort = Integer.parseInt(redirections[0]);
				local = redirections[1];
				localPort = Integer.parseInt(redirections[2]);
				break;
			default:
				throw new IllegalArgumentException("Redirection rule must be of size 2, 3 or 4");
		}
		setup(remotePort, local, localPort);
	}

	private void setup(int remotePort, String destinationHost, int destinationPort) {
		this.remotePort = remotePort;
		this.destinationHost = destinationHost;
		this.destinationPort = destinationPort;
	}

	public int getRemotePort() {
		return remotePort;
	}

	public String getDestinationHost() {
		return destinationHost;
	}

	public int getDestinationPort() {
		return destinationPort;
	}

	public static Redirection[] parse(String... rules) {
		ArrayList<Redirection> r = new ArrayList<>(rules.length);
		for (String rule : rules) {
			r.add(new Redirection(rule));
		}
		return r.toArray(new Redirection[rules.length]);
	}
}
