package com.activeviam.utils.passthrough;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.UserInfo;

/**
 * Lazy known host server.
 */
public class LazyKnownHostSSHClient implements HostKeyRepository {

	public int check(String host, byte[] key) {
		return HostKeyRepository.OK;
	}

	public void add(HostKey hostkey, UserInfo ui) {

	}

	public void remove(String host, String type) {

	}

	public void remove(String host, String type, byte[] key) {

	}

	public String getKnownHostsRepositoryID() {
		return "OK-FOR-ALL";
	}

	public HostKey[] getHostKey() {
		return new HostKey[0];
	}

	public HostKey[] getHostKey(String host, String type) {
		return new HostKey[0];
	}
}
