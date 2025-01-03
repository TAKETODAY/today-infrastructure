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

package infra.context.properties.source;

import java.io.Serial;
import java.util.List;

/**
 * Exception thrown when {@link ConfigurationPropertyName} has invalid characters.
 *
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class InvalidConfigurationPropertyNameException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  private final CharSequence name;

  private final List<Character> invalidCharacters;

  public InvalidConfigurationPropertyNameException(CharSequence name, List<Character> invalidCharacters) {
    super("Configuration property name '" + name + "' is not valid");
    this.name = name;
    this.invalidCharacters = invalidCharacters;
  }

  public List<Character> getInvalidCharacters() {
    return this.invalidCharacters;
  }

  public CharSequence getName() {
    return this.name;
  }

  public static void throwIfHasInvalidChars(CharSequence name, List<Character> invalidCharacters) {
    if (!invalidCharacters.isEmpty()) {
      throw new InvalidConfigurationPropertyNameException(name, invalidCharacters);
    }
  }

}
