package de.soderer.utilities.console;

import java.awt.Toolkit;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.soderer.utilities.Utilities;

public class SimpleConsoleInput {
	private boolean silent = false;
	private Character hideCharacter = null;
	private String prompt = "";
	private String presetContent = "";
	protected List<String> autoCompletionStrings = null;

	/**
	 * When silent = false a system beep occurs on input error
	 *
	 * @param silent
	 */
	public boolean isSilent() {
		return silent;
	}

	/**
	 * When silent = false a system beep occurs on input error
	 *
	 * @param silent
	 */
	public void setSilent(final boolean silent) {
		this.silent = silent;
	}

	/**
	 * For password inputs
	 *
	 * @return
	 */
	public Character getHideCharacter() {
		return hideCharacter;
	}

	/**
	 * For password inputs
	 *
	 * @param hideCharacter
	 * @return
	 */
	public SimpleConsoleInput setHideCharacter(final Character hideCharacter) {
		this.hideCharacter = hideCharacter;
		return this;
	}

	public String getPrompt() {
		return prompt;
	}

	public SimpleConsoleInput setPrompt(final String prompt) {
		this.prompt = (prompt == null ? "" : prompt);
		return this;
	}

	public String getPresetContent() {
		return presetContent;
	}

	public SimpleConsoleInput setPresetContent(final String presetContent) {
		this.presetContent = (presetContent == null ? "" : presetContent);
		return this;
	}

	public List<String> getAutoCompletionStrings() {
		return autoCompletionStrings;
	}

	public SimpleConsoleInput setAutoCompletionStrings(final List<String> autoCompletionStrings) {
		this.autoCompletionStrings = autoCompletionStrings;
		return this;
	}

	public SimpleConsoleInput addAutoCompletionString(final String autoCompletionString) {
		if (autoCompletionStrings == null) {
			autoCompletionStrings = new ArrayList<>();
		}
		autoCompletionStrings.add(autoCompletionString);
		return this;
	}

	public String readInput() throws IOException {
		boolean insertMode = true;
		String content = presetContent == null ? "" : presetContent;
		int previousContentLength = content.length();
		int cursorIndex = content.length();

		while (true) {
			if (hideCharacter == null) {
				System.out.print("\r" + prompt + content);
			} else {
				System.out.print("\r" + prompt + Utilities.repeat(hideCharacter, content.length()));
			}

			if (previousContentLength > content.length()) {
				// Cleanup after deletion of a character or autocomplete with shorter content
				System.out.print(Utilities.repeat(" ", previousContentLength - content.length()) + Utilities.repeat("\b", previousContentLength - content.length()));
			}

			// Put cursor to its index
			System.out.print(Utilities.repeat("\b", content.length() - cursorIndex));

			previousContentLength = content.length();

			final int keyCode = JnaKeyStrokeReader.readKey(true);

			if (keyCode == JnaKeyStrokeReader.KeyCode_CtrlC) {
				System.exit(1);
			} else if (keyCode == JnaKeyStrokeReader.KeyCode_Enter) {
				return content;
			} else if (keyCode == JnaKeyStrokeReader.KeyCode_Backspace) {
				if (cursorIndex > 0) {
					content = content.substring(0, cursorIndex - 1) + content.substring(cursorIndex);
					cursorIndex--;
				} else if (!silent) {
					Toolkit.getDefaultToolkit().beep();
				}
			} else if (keyCode == JnaKeyStrokeReader.KeyCode_Delete) {
				if (cursorIndex < content.length()) {
					content = content.substring(0, cursorIndex) + content.substring(cursorIndex + 1);
				} else if (!silent) {
					Toolkit.getDefaultToolkit().beep();
				}
			} else if (keyCode == JnaKeyStrokeReader.KeyCode_Left) {
				if (cursorIndex > 0) {
					cursorIndex--;
				}
			} else if (keyCode == JnaKeyStrokeReader.KeyCode_Right) {
				if (cursorIndex < content.length()) {
					cursorIndex++;
				}
			} else if (keyCode == JnaKeyStrokeReader.KeyCode_Down) {
				// do nothing
			} else if (keyCode == JnaKeyStrokeReader.KeyCode_Up) {
				// do nothing
			} else if (keyCode == JnaKeyStrokeReader.KeyCode_PageDown) {
				final List<String> autoCompletionStringsToCheck = getAutoCompletionStrings(content, keyCode);
				if (autoCompletionStringsToCheck != null) {
					int autoCompletionIndex = -1;
					for (int i = 0; i < autoCompletionStringsToCheck.size(); i++) {
						if (content.equals(autoCompletionStringsToCheck.get(i))) {
							autoCompletionIndex = i;
							break;
						}
					}
					if (autoCompletionIndex == autoCompletionStringsToCheck.size() - 1) {
						content = autoCompletionStringsToCheck.get(0);
						cursorIndex = content.length();
					} else if (autoCompletionIndex >= 0) {
						content = autoCompletionStringsToCheck.get(autoCompletionIndex + 1);
						cursorIndex = content.length();
					} else if (!silent) {
						Toolkit.getDefaultToolkit().beep();
					}
				} else if (!silent) {
					Toolkit.getDefaultToolkit().beep();
				}
			} else if (keyCode == JnaKeyStrokeReader.KeyCode_PageUp) {
				final List<String> autoCompletionStringsToCheck = getAutoCompletionStrings(content, keyCode);
				if (autoCompletionStringsToCheck != null) {
					int autoCompletionIndex = -1;
					for (int i = 0; i < autoCompletionStringsToCheck.size(); i++) {
						if (content.equals(autoCompletionStringsToCheck.get(i))) {
							autoCompletionIndex = i;
							break;
						}
					}
					if (autoCompletionIndex == 0) {
						content = autoCompletionStringsToCheck.get(autoCompletionStringsToCheck.size() - 1);
						cursorIndex = content.length();
					} else if (autoCompletionIndex > 0) {
						content = autoCompletionStringsToCheck.get(autoCompletionIndex - 1);
						cursorIndex = content.length();
					} else if (!silent) {
						Toolkit.getDefaultToolkit().beep();
					}
				} else if (!silent) {
					Toolkit.getDefaultToolkit().beep();
				}
			} else if (keyCode == JnaKeyStrokeReader.KeyCode_Home) {
				cursorIndex = 0;
			} else if (keyCode == JnaKeyStrokeReader.KeyCode_End) {
				cursorIndex = content.length();
			} else if (keyCode == JnaKeyStrokeReader.KeyCode_Insert) {
				insertMode = !insertMode;
			} else if (keyCode == JnaKeyStrokeReader.KeyCode_Tab) {
				final String prefix = content.substring(0, cursorIndex);
				final List<String> autoCompletionStringsToCheck = getAutoCompletionStrings(prefix, keyCode);
				if (autoCompletionStringsToCheck != null) {
					String autoCompletionStringCandidate = null;
					for (final String autoCompletionString : autoCompletionStringsToCheck) {
						if (autoCompletionString != null && autoCompletionString.startsWith(prefix)) {
							if (autoCompletionStringCandidate == null) {
								autoCompletionStringCandidate = autoCompletionString;
							} else {
								autoCompletionStringCandidate = null;
								if (!silent) {
									Toolkit.getDefaultToolkit().beep();
								}
								break;
							}
						}
					}
					if (autoCompletionStringCandidate != null) {
						content = autoCompletionStringCandidate + content.substring(cursorIndex);
						cursorIndex = autoCompletionStringCandidate.length();
					} else if (!silent) {
						Toolkit.getDefaultToolkit().beep();
					}
				} else if (!silent) {
					Toolkit.getDefaultToolkit().beep();
				}
			} else {
				if (insertMode || cursorIndex == content.length()) {
					content = content.substring(0, cursorIndex) + ((char) keyCode) + content.substring(cursorIndex);
					cursorIndex++;
				} else {
					content = content.substring(0, cursorIndex) + ((char) keyCode) + content.substring(cursorIndex + 1);
					cursorIndex++;
				}
			}
		}
	}

	/**
	 * Override hook for other autoComplete functions
	 *
	 * @param checkContent
	 * @param keyCode
	 */
	protected List<String> getAutoCompletionStrings(final String checkContent, final int keyCode) {
		return autoCompletionStrings;
	}
}
