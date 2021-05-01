package de.soderer.utilities.mail.dkim;

import java.util.Arrays;
import java.util.List;

public enum DkimKeyPairType {
	RSA("rsa", "RSA", DkimSigningAlgorithm.SHA256_WITH_RSA, new DkimSigningAlgorithm[] { DkimSigningAlgorithm.SHA256_WITH_RSA, DkimSigningAlgorithm.SHA1_WITH_RSA }) {};

	private final String dkimNotation;

	private final String javaNotation;

	private final DkimSigningAlgorithm defaultSigningAlgorithm;

	private final List<DkimSigningAlgorithm> supportedSigningAlgorithms;

	DkimKeyPairType(final String dkimNotation, final String javaNotation, final DkimSigningAlgorithm defaultSigningAlgorithm, final DkimSigningAlgorithm[] supportedSigningAlgorithms) {
		this.dkimNotation = dkimNotation;
		this.javaNotation = javaNotation;
		this.defaultSigningAlgorithm = defaultSigningAlgorithm;
		this.supportedSigningAlgorithms = Arrays.asList(supportedSigningAlgorithms);
	}

	public String getDkimNotation() {
		return dkimNotation;
	}

	public String getJavaNotation() {
		return javaNotation;
	}

	public DkimSigningAlgorithm getDefaultSigningAlgorithm() {
		return defaultSigningAlgorithm;
	}

	public boolean supportsSigningAlgorithm(final DkimSigningAlgorithm signingAlgorithm) {
		return supportedSigningAlgorithms.contains(signingAlgorithm);
	}
}
