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

package cn.taketoday.jdbc.datasource.init;

import cn.taketoday.core.io.EncodedResource;
import cn.taketoday.lang.Nullable;

/**
 * Thrown by {@link ScriptUtils} if an SQL script cannot be properly parsed.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ScriptParseException extends ScriptException {

  /**
   * Create a new {@code ScriptParseException}.
   *
   * @param message detailed message
   * @param resource the resource from which the SQL script was read
   */
  public ScriptParseException(String message, @Nullable EncodedResource resource) {
    super(buildMessage(message, resource));
  }

  /**
   * Create a new {@code ScriptParseException}.
   *
   * @param message detailed message
   * @param resource the resource from which the SQL script was read
   * @param cause the underlying cause of the failure
   */
  public ScriptParseException(String message, @Nullable EncodedResource resource, @Nullable Throwable cause) {
    super(buildMessage(message, resource), cause);
  }

  private static String buildMessage(String message, @Nullable EncodedResource resource) {
    return String.format("Failed to parse SQL script from resource [%s]: %s",
            (resource == null ? "<unknown>" : resource), message);
  }

}
