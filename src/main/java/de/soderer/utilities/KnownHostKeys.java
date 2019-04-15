package de.soderer.utilities;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;

public class KnownHostKeys {
	private static void printKnownHostKeys(final JSch jsch) {
		final HostKeyRepository hkr = jsch.getHostKeyRepository();
		final HostKey[] hks = hkr.getHostKey();
		if (hks != null) {
			System.out.println("Host keys in " + hkr.getKnownHostsRepositoryID());
			for (final HostKey hk : hks) {
				System.out.println(hk.getHost() + " " + hk.getType() + " " + hk.getFingerPrint(jsch));
			}
			System.out.println("");
		}
	}
}
