/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.logging.logback;

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
