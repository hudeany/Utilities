package de.soderer.utilities.mail;

public enum MailerConnectionSecurity {
	None,
	STARTTLS,
	SSL_TLS;

	public static MailerConnectionSecurity getHttpContentTypeByName(final String mailerConnectionSecurityString) throws Exception {
		if (mailerConnectionSecurityString != null) {
			for (final MailerConnectionSecurity mailerConnectionSecurity : MailerConnectionSecurity.values()) {
				if (mailerConnectionSecurity.name().replace("_", "").equalsIgnoreCase(mailerConnectionSecurityString.replace("_", ""))) {
					return mailerConnectionSecurity;
				}
			}
		}
		throw new Exception("Unknown MailerConnectionSecurity: '" + mailerConnectionSecurityString + "'");
	}
}
