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

/**
 * Thrown by {@link ScriptUtils} if an SQL script cannot be read.
 *
 * @author Keith Donald
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class CannotReadScriptException extends ScriptException {

  /**
   * Create a new {@code CannotReadScriptException}.
   *
   * @param resource the resource that cannot be read from
   * @param cause the underlying cause of the resource access failure
   */
  public CannotReadScriptException(EncodedResource resource, Throwable cause) {
    super("Cannot read SQL script from " + resource, cause);
  }

}
