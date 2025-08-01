package de.soderer.utilities.remote;

public class FilePermissions {
	boolean userRead = false;
	boolean userWrite = false;
	boolean userExecute = false;

	boolean groupRead = false;
	boolean groupWrite = false;
	boolean groupExecute = false;

	boolean othersRead = false;
	boolean othersWrite = false;
	boolean othersExecute = false;

	public FilePermissions() {
	}

	public FilePermissions(final boolean userRead, final boolean userWrite, final boolean userExecute) {
		this.userRead = userRead;
		this.userWrite = userWrite;
		this.userExecute = userExecute;
	}

	public FilePermissions(final boolean userRead, final boolean userWrite, final boolean userExecute, final boolean groupRead, final boolean groupWrite, final boolean groupExecute) {
		this.userRead = userRead;
		this.userWrite = userWrite;
		this.userExecute = userExecute;

		this.groupRead = groupRead;
		this.groupWrite = groupWrite;
		this.groupExecute = groupExecute;
	}

	public FilePermissions(final boolean userRead, final boolean userWrite, final boolean userExecute, final boolean groupRead, final boolean groupWrite, final boolean groupExecute, final boolean othersRead, final boolean othersWrite, final boolean othersExecute) {
		this.userRead = userRead;
		this.userWrite = userWrite;
		this.userExecute = userExecute;

		this.groupRead = groupRead;
		this.groupWrite = groupWrite;
		this.groupExecute = groupExecute;

		this.othersRead = othersRead;
		this.othersWrite = othersWrite;
		this.othersExecute = othersExecute;
	}

	public boolean isUserRead() {
		return userRead;
	}

	public FilePermissions setUserRead(final boolean userRead) {
		this.userRead = userRead;
		return this;
	}

	public boolean isUserWrite() {
		return userWrite;
	}

	public FilePermissions setUserWrite(final boolean userWrite) {
		this.userWrite = userWrite;
		return this;
	}

	public boolean isUserExecute() {
		return userExecute;
	}

	public FilePermissions setUserExecute(final boolean userExecute) {
		this.userExecute = userExecute;
		return this;
	}

	public boolean isGroupRead() {
		return groupRead;
	}

	public FilePermissions setGroupRead(final boolean groupRead) {
		this.groupRead = groupRead;
		return this;
	}

	public boolean isGroupWrite() {
		return groupWrite;
	}

	public FilePermissions setGroupWrite(final boolean groupWrite) {
		this.groupWrite = groupWrite;
		return this;
	}

	public boolean isGroupExecute() {
		return groupExecute;
	}

	public FilePermissions setGroupExecute(final boolean groupExecute) {
		this.groupExecute = groupExecute;
		return this;
	}

	public boolean isOthersRead() {
		return othersRead;
	}

	public FilePermissions setOthersRead(final boolean othersRead) {
		this.othersRead = othersRead;
		return this;
	}

	public boolean isOthersWrite() {
		return othersWrite;
	}

	public FilePermissions setOthersWrite(final boolean othersWrite) {
		this.othersWrite = othersWrite;
		return this;
	}

	public boolean isOthersExecute() {
		return othersExecute;
	}

	public FilePermissions setOthersExecute(final boolean othersExecute) {
		this.othersExecute = othersExecute;
		return this;
	}

	public String getPermissionMapString() {
		int userNumber = 0;
		if (userRead) {
			userNumber += 4;
		}
		if (userWrite) {
			userNumber += 2;
		}
		if (userExecute) {
			userNumber += 1;
		}

		int groupNumber = 0;
		if (groupRead) {
			groupNumber += 4;
		}
		if (groupWrite) {
			groupNumber += 2;
		}
		if (groupExecute) {
			groupNumber += 1;
		}

		int othersNumber = 0;
		if (othersRead) {
			othersNumber += 4;
		}
		if (othersWrite) {
			othersNumber += 2;
		}
		if (othersExecute) {
			othersNumber += 1;
		}

		return "" + userNumber + groupNumber + othersNumber;
	}
}
