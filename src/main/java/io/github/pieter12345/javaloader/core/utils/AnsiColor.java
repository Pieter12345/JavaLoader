package io.github.pieter12345.javaloader.core.utils;

/**
 * ANSI color support.
 * @author P.J.S. Kools
 */
public abstract class AnsiColor {
	public static final String RESET          = "\u001B[m";
	public static final String BLACK          = "\u001B[30m";
	public static final String RED            = "\u001B[31m";
	public static final String GREEN          = "\u001B[32m";
	public static final String YELLOW         = "\u001B[33m";
	public static final String BLUE           = "\u001B[34m";
	public static final String MAGENTA        = "\u001B[35m";
	public static final String CYAN           = "\u001B[36m";
	public static final String LIGHT_GRAY     = "\u001B[37m"; // White.
	public static final String DARK_GRAY      = "\u001B[30;1m"; // Black bright.
	public static final String RED_BRIGHT     = "\u001B[31;1m";
	public static final String GREEN_BRIGHT   = "\u001B[32;1m";
	public static final String YELLOW_BRIGHT  = "\u001B[33;1m";
	public static final String BLUE_BRIGHT    = "\u001B[34;1m";
	public static final String MAGENTA_BRIGHT = "\u001B[35;1m";
	public static final String CYAN_BRIGHT    = "\u001B[36;1m";
	public static final String WHITE          = "\u001B[37;1m"; // White bright.
	
	/**
	 * Colorizes the given string, using '&' as colorize character. Accepted input is &1, &2, ..., &9, &a, ..., &f
	 * and &r to reset all colors. To print color character '&', type '&&'.
	 * @param str - The string to colorize.
	 * @return The colorized string or null when 'str' is null.
	 * @throws FormatException When the colorize character is found in combination with an invalid color character.
	 */
	public static String colorize(String str) throws FormatException {
		return colorize(str, '&');
	}
	
	/**
	 * Colorizes the given string, using the given colorize character. If the colorize character is '&', then accepted
	 * input is &1, &2, ..., &9, &a, ..., &f and &r to reset all colors. To print color character '&', type '&&'.
	 * @param str - The string to colorize.
	 * @param colorizeChar - The colorize character.
	 * @return The colorized string or null when 'str' is null.
	 * @throws FormatException When the colorize character is found in combination with an invalid color character.
	 */
	public static String colorize(String str, char colorizeChar) throws FormatException {
		if(str == null) {
			return null;
		}
		for(int i = 0; i < str.length(); i++) {
			if(str.charAt(i) == colorizeChar) {
				if(i == str.length() - 1) {
					throw new FormatException("Input ends with an unescaped colorize character.");
				}
				char colorChar = Character.toLowerCase(str.charAt(i + 1));
				String colorStr;
				if(colorChar == colorizeChar) {
					colorStr = "" + colorizeChar;
				} else {
					switch(colorChar) {
						case 'r': colorStr = RESET; break;
						case '0': colorStr = BLACK; break;
						case '1': colorStr = BLUE; break;
						case '2': colorStr = GREEN; break;
						case '3': colorStr = CYAN; break;
						case '4': colorStr = RED; break;
						case '5': colorStr = MAGENTA; break;
						case '6': colorStr = YELLOW; break;
						case '7': colorStr = LIGHT_GRAY; break;
						case '8': colorStr = DARK_GRAY; break;
						case '9': colorStr = BLUE_BRIGHT; break;
						case 'a': colorStr = GREEN_BRIGHT; break;
						case 'b': colorStr = CYAN_BRIGHT; break;
						case 'c': colorStr = RED_BRIGHT; break;
						case 'd': colorStr = MAGENTA_BRIGHT; break;
						case 'e': colorStr = YELLOW_BRIGHT; break;
						case 'f': colorStr = WHITE; break;
						default: throw new FormatException("Unknown color char found: '" + colorChar + "'.");
					}
				}
				str = str.substring(0, i) + colorStr + str.substring(i + 2);
				i += colorStr.length() - 1; // Skip the newly placed color string.
			}
		}
		return str;
	}
	
	/**
	 * stripColors method.
	 * Strips all ANSI colors from the given string.
	 * @param str - The String to strip.
	 * @return The String without ANSI color codes or null if 'str' is null.
	 */
	public static String stripColors(String str) {
		return (str == null ? null : str.replaceAll("\u001B\\[(\\d*(\\;\\d+)*)m", ""));
	}
	
	/**
	 * Thrown when some passed argument does not match the required format.
	 * @author P.J.S. Kools
	 */
	@SuppressWarnings("serial")
	public static class FormatException extends RuntimeException {
		public FormatException(String message) {
			super(message);
		}
	}
}
