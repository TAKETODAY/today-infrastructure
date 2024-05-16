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

/**
 * Thrown when we cannot determine anything more specific than "something went
 * wrong while processing an SQL script": for example, a {@link java.sql.SQLException}
 * from JDBC that we cannot pinpoint more precisely.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class UncategorizedScriptException extends ScriptException {

  /**
   * Create a new {@code UncategorizedScriptException}.
   *
   * @param message detailed message
   */
  public UncategorizedScriptException(String message) {
    super(message);
  }

  /**
   * Create a new {@code UncategorizedScriptException}.
   *
   * @param message detailed message
   * @param cause the root cause
   */
  public UncategorizedScriptException(String message, Throwable cause) {
    super(message, cause);
  }

}
