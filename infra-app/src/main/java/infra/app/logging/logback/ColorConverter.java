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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import infra.core.ansi.AnsiColor;
import infra.core.ansi.AnsiBackground;
import infra.core.ansi.AnsiElement;
import infra.core.ansi.AnsiOutput;
import infra.core.ansi.AnsiStyle;

/**
 * Logback {@link CompositeConverter} to color output using the {@link AnsiOutput} class.
 * Multiple ANSI color, background and style options may be provided. If no recognized
 * options are specified, the color is picked based on the logging level.
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

  private static final Map<String, AnsiElement> ELEMENTS_BY_NAME;

  static {
    Map<String, AnsiElement> elements = new HashMap<>(ELEMENTS);
    Arrays.stream(AnsiStyle.values())
            .forEach(style -> elements.put(style.name().toLowerCase(Locale.ROOT), style));
    Arrays.stream(AnsiBackground.values())
            .filter(background -> background != AnsiBackground.DEFAULT)
            .forEach(background -> elements.put("bg_" + background.name().toLowerCase(Locale.ROOT), background));
    ELEMENTS_BY_NAME = Collections.unmodifiableMap(elements);
  }

  private AnsiElement[] elements = {};

  @Override
  public void start() {
    List<String> options = getOptionList();
    if (options != null && !options.isEmpty()) {
      List<AnsiElement> elements = new ArrayList<>();
      for (String option : options) {
        for (String optionPart : option.split(",")) {
          AnsiElement element = ELEMENTS_BY_NAME.get(optionPart.trim().toLowerCase(Locale.ROOT));
          if (element != null) {
            elements.add(element);
          }
        }
      }
      this.elements = elements.toArray(AnsiElement[]::new);
    }
    super.start();
  }

  @Override
  protected String transform(ILoggingEvent event, String in) {
    if (this.elements.length == 0) {
      // Assume highlighting
      AnsiElement element = LEVELS.get(event.getLevel().toInteger());
      return toAnsiString(in, element != null ? element : AnsiColor.GREEN);
    }
    return toAnsiString(in, this.elements);
  }

  protected String toAnsiString(String in, AnsiElement element) {
    return AnsiOutput.toString(element, in);
  }

  protected String toAnsiString(String in, AnsiElement... elements) {
    Object[] ansiParams = new Object[elements.length + 1];
    System.arraycopy(elements, 0, ansiParams, 0, elements.length);
    ansiParams[elements.length] = in;
    return AnsiOutput.toString(ansiParams);
  }

  static String getName(AnsiElement element) {
    return ELEMENTS_BY_NAME.entrySet()
            .stream()
            .filter((entry) -> entry.getValue().equals(element))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElseThrow();
  }

}
