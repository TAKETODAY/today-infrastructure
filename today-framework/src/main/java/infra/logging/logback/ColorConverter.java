/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.logging.logback;

import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import infra.core.ansi.AnsiColor;
import infra.core.ansi.AnsiElement;
import infra.core.ansi.AnsiOutput;
import infra.core.ansi.AnsiStyle;

/**
 * Logback {@link CompositeConverter} colors output using the {@link AnsiOutput} class. A
 * single 'color' option can be provided to the converter, or if not specified color will
 * be picked based on the logging level.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ColorConverter extends CompositeConverter<ILoggingEvent> {

  private static final Map<String, AnsiElement> ELEMENTS = Map.ofEntries(
          Map.entry("black", AnsiColor.BLACK),
          Map.entry("white", AnsiColor.WHITE),
          Map.entry("faint", AnsiStyle.FAINT),
          Map.entry("red", AnsiColor.RED),
          Map.entry("green", AnsiColor.GREEN),
          Map.entry("yellow", AnsiColor.YELLOW),
          Map.entry("blue", AnsiColor.BLUE),
          Map.entry("magenta", AnsiColor.MAGENTA),
          Map.entry("cyan", AnsiColor.CYAN),
          Map.entry("bright_black", AnsiColor.BRIGHT_BLACK),
          Map.entry("bright_white", AnsiColor.BRIGHT_WHITE),
          Map.entry("bright_red", AnsiColor.BRIGHT_RED),
          Map.entry("bright_green", AnsiColor.BRIGHT_GREEN),
          Map.entry("bright_yellow", AnsiColor.BRIGHT_YELLOW),
          Map.entry("bright_blue", AnsiColor.BRIGHT_BLUE),
          Map.entry("bright_magenta", AnsiColor.BRIGHT_MAGENTA),
          Map.entry("bright_cyan", AnsiColor.BRIGHT_CYAN)
  );

  private static final Map<Integer, AnsiElement> LEVELS = Map.of(
          Level.ERROR_INTEGER, AnsiColor.RED,
          Level.WARN_INTEGER, AnsiColor.YELLOW
  );

  @Override
  protected String transform(ILoggingEvent event, String in) {
    String firstOption = getFirstOption();
    AnsiElement element = firstOption == null ? null : ELEMENTS.get(firstOption);
    if (element == null) {
      // Assume highlighting
      element = LEVELS.get(event.getLevel().toInteger());
      if (element == null) {
        element = AnsiColor.GREEN;
      }
    }
    return toAnsiString(in, element);
  }

  protected String toAnsiString(String in, AnsiElement element) {
    return AnsiOutput.toString(element, in);
  }

  static String getName(AnsiElement element) {
    return ELEMENTS.entrySet()
            .stream()
            .filter((entry) -> entry.getValue().equals(element))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElseThrow();
  }

}
