package de.soderer.utilities.remote;

/**
 * Result of a remote command execution (e.g. via {@link SftpConnection#executeCommandWithResult(String)}),
 * keeping stdout, stderr and the exit code separately available instead of collapsing them into a
 * single string or deciding success/failure purely by whether stderr received any output.
 */
public class CommandResult {
	private final String stdOut;
	private final String stdErr;
	private final int exitCode;

	public CommandResult(final String stdOut, final String stdErr, final int exitCode) {
		this.stdOut = stdOut;
		this.stdErr = stdErr;
		this.exitCode = exitCode;
	}

	public String getStdOut() {
		return stdOut;
	}

	public String getStdErr() {
		return stdErr;
	}

	public int getExitCode() {
		return exitCode;
	}

	public boolean isSuccessful() {
		return exitCode == 0;
	}

	@Override
	public String toString() {
		return "exitCode=" + exitCode + ", stdOut='" + stdOut + "', stdErr='" + stdErr + "'";
	}
}
