package de.soderer.utilities.console;

import java.util.ArrayList;
import java.util.List;

import de.soderer.utilities.Utilities;
import de.soderer.utilities.console.ConsoleUtilities.ConsoleColor;

public class ConsoleMenu {
	private final String mainTitle;
	private final String menuTitle;
	private final ConsoleMenu parentMenu;
	private final List<ConsoleMenu> subMenus = new ArrayList<>();

	private final List<String> messages = new ArrayList<>();
	private final List<String> warnings = new ArrayList<>();
	private final List<String> errors = new ArrayList<>();

	public ConsoleMenu(final String mainTitle) {
		this.mainTitle = mainTitle;
		menuTitle = "Main menu";
		parentMenu = null;
	}

	public ConsoleMenu(final ConsoleMenu parentMenu, final String menuTitle) throws Exception {
		if (parentMenu != null) {
			parentMenu.addSubMenu(this);
		} else {
			throw new Exception("Invalid empty parent menu");
		}
		this.parentMenu = parentMenu;
		mainTitle = parentMenu.getMainTitle();
		this.menuTitle = menuTitle;
	}

	public String getMainTitle() {
		return mainTitle;
	}

	public ConsoleMenu getParentMenu() {
		return parentMenu;
	}

	public String getTitle() {
		return menuTitle;
	}

	public List<String> getMessages() {
		return messages;
	}

	private ConsoleMenu addSubMenu(final ConsoleMenu subMenu) {
		subMenus.add(subMenu);
		return this;
	}

	public int show() throws Exception {
		while (true) {
			ConsoleUtilities.clearScreen();

			ConsoleUtilities.printBoxed(mainTitle);
			System.out.println();

			System.out.println(menuTitle);
			System.out.println();

			if (errors.size() > 0) {
				for (final String error : errors) {
					ConsoleUtilities.printColoredText(ConsoleColor.Light_red, "Error: " + error);
					System.out.println();
				}
				errors.clear();
			}

			if (warnings.size() > 0) {
				for (final String warning : warnings) {
					ConsoleUtilities.printColoredText(ConsoleColor.Yellow, "Warning: " + warning);
				}
				System.out.println();
				messages.clear();
			}

			if (messages.size() > 0) {
				for (final String message : messages) {
					ConsoleUtilities.printColoredText(ConsoleColor.Green, message);
				}
				System.out.println();
				messages.clear();
			}

			String defaultMenuTitle;
			if (parentMenu == null) {
				defaultMenuTitle = "Quit";
			} else {
				defaultMenuTitle = "Back to " + parentMenu.getTitle();
			}

			System.out.println("Please choose (Blank => " + defaultMenuTitle + "):");

			int subMenuIndex = 0;
			for (final ConsoleMenu subMenu : subMenus) {
				subMenuIndex++;
				System.out.println(" " + (subMenuIndex < 10 ? " " : "") + subMenuIndex + ") " + subMenu.getTitle());
			}

			System.out.println("  0) " + defaultMenuTitle);

			String choice = System.console().readLine(" > ");
			choice = choice == null ? "" : choice.trim();

			if (Utilities.isBlank(choice)) {
				return 0;
			} else if ("q".equalsIgnoreCase(choice) || "quit".equalsIgnoreCase(choice) || "exit".equalsIgnoreCase(choice)) {
				ConsoleUtilities.clearScreen();
				System.exit(0);
			} else {
				try {
					final int choiceInt = Integer.parseInt(choice);

					if (choiceInt == 0) {
						return 0;
					} else if (1 <= choiceInt && choiceInt <= subMenus.size()) {
						final int returnValue = subMenus.get(choiceInt - 1).show();
						if (returnValue < 0) {
							return returnValue;
						}
					} else {
						errors.add("Invalid input: " + choice);
					}
				} catch (@SuppressWarnings("unused") final Exception e) {
					errors.add("Invalid input: " + choice);
				}
			}
		}
	}
}
