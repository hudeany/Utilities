package de.soderer.utilities;

public class UnknownHostKeyException extends Exception {
	private static final long serialVersionUID = -7464906738157348128L;

	String hostnameOrIp;
	String hostKeyFingerPrint;

	public UnknownHostKeyException(final String message, final String hostnameOrIp, final String hostKeyFingerPrint) {
		super(message);
		this.hostnameOrIp = hostnameOrIp;
		this.hostKeyFingerPrint = hostKeyFingerPrint;
	}

	public String getHostnameOrIp() {
		return hostnameOrIp;
	}

	public String getHostKeyFingerPrint() {
		return hostKeyFingerPrint;
	}
}
