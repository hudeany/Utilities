package de.soderer.utilities;

public interface CredentialsParent {
	public String aquireUserName() throws Exception;

	public char[] aquirePassword() throws Exception;
}
