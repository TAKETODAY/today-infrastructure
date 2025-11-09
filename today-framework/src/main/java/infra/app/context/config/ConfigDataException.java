/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.app.context.config;

import org.jspecify.annotations.Nullable;

/**
 * Abstract base class for configuration data exceptions.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0
 */
public abstract class ConfigDataException extends RuntimeException {

  /**
   * Create a new {@link ConfigDataException} instance.
   *
   * @param message the exception message
   * @param cause the exception cause
   */
  protected ConfigDataException(@Nullable String message, @Nullable Throwable cause) {
    super(message, cause);
  }

}
