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

import infra.origin.OriginProvider;

/**
 * {@link ConfigDataNotFoundException} thrown when a {@link ConfigData} cannot be found.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public abstract class ConfigDataNotFoundException extends ConfigDataException implements OriginProvider {

  /**
   * Create a new {@link ConfigDataNotFoundException} instance.
   *
   * @param message the exception message
   * @param cause the exception cause
   */
  ConfigDataNotFoundException(@Nullable String message, @Nullable Throwable cause) {
    super(message, cause);
  }

  /**
   * Return a description of actual referenced item that could not be found.
   *
   * @return a description of the referenced items
   */
  public abstract String getReferenceDescription();

}
