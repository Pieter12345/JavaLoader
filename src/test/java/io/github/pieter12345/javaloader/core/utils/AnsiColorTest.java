package io.github.pieter12345.javaloader.core.utils;

import static io.github.pieter12345.javaloader.core.utils.AnsiColor.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the {@link AnsiColor} class.
 * @author P.J.S. Kools
 */
class AnsiColorTest {
	
	@ParameterizedTest
	@MethodSource("defaultColorizeSource")
	void testDefaultColorize(String input, String expected) {
		assertThat(AnsiColor.colorize(input)).isEqualTo(expected);
	}
	
	static Stream<Arguments> defaultColorizeSource() {
		return Stream.of(
				
				// No colors.
				() -> new Object[] {"Some string without colors.", "Some string without colors."},
				
				// Only a colorcode.
				() -> new Object[] {"&4", RED},
				
				// One colorcode in the middle.
				() -> new Object[] {"normal&4red", "normal" + RED + "red"},
				
				// One colorcode in front.
				() -> new Object[] {"&4red", RED + "red"},
				
				// One colorcode behind.
				() -> new Object[] {"normal&4", "normal" + RED},
				
				// Multiple colorcodes in a row.
				() -> new Object[] {"&0&1&2&3", BLACK + BLUE + GREEN + CYAN},
				
				// Escape color char.
				() -> new Object[] {"&&", "&"},
				() -> new Object[] {"&&Meow&&Meow&&", "&Meow&Meow&"},
				
				// Test all color characters.
				() -> new Object[] {"&r&0&1&2&3&4&5&6&7&8&9&a&b&c&d&e&f", RESET + BLACK + BLUE + GREEN + CYAN + RED
						+ MAGENTA + YELLOW + LIGHT_GRAY + DARK_GRAY + BLUE_BRIGHT + GREEN_BRIGHT + CYAN_BRIGHT
						+ RED_BRIGHT + MAGENTA_BRIGHT + YELLOW_BRIGHT + WHITE}
			);
	}
	
	@Test
	void testDefaultColorizeNull() {
		assertThat(AnsiColor.colorize(null)).isNull();
	}
	
	@Test
	void testDefaultColorizeInvalidChar() {
		assertThrows(FormatException.class, () -> AnsiColor.colorize("&g"));
	}
	
	@Test
	void testDefaultColorizeEndingUnescapedColorizeChar() {
		assertThrows(FormatException.class, () -> AnsiColor.colorize("Meow&"));
	}
	
	@ParameterizedTest
	@MethodSource("stripColorsSource")
	void testStripColor(String input, String expected) {
		assertThat(AnsiColor.stripColors(input)).isEqualTo(expected);
	}
	
	static Stream<Arguments> stripColorsSource() {
		return Stream.of(
				
				// No colors.
				() -> new Object[] {"Some string without colors.", "Some string without colors."},
				
				// Only one color.
				() -> new Object[] {RED, ""},
				
				// One color in the middle.
				() -> new Object[] {"normal" + RED + "red", "normalred"},
				
				// One color in front.
				() -> new Object[] {RED + "red", "red"},
				
				// One colorcode behind.
				() -> new Object[] {"normal" + RED, "normal"},
				
				// Test all colors in a row.
				() -> new Object[] {RESET + BLACK + BLUE + GREEN + CYAN + RED + MAGENTA + YELLOW + LIGHT_GRAY
						+ DARK_GRAY + BLUE_BRIGHT + GREEN_BRIGHT + CYAN_BRIGHT + RED_BRIGHT + MAGENTA_BRIGHT
						+ YELLOW_BRIGHT + WHITE, ""}
			);
	}
	
	@Test
	void testStripColorsNull() {
		assertThat(AnsiColor.stripColors(null)).isNull();
	}
}
