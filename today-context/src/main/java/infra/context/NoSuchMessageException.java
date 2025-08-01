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

package infra.context;

import java.util.Locale;

import infra.lang.Nullable;

/**
 * Exception thrown when a message can't be resolved.
 *
 * @author Rod Johnson
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class NoSuchMessageException extends RuntimeException {

  /**
   * Create a new exception.
   *
   * @param code the code that could not be resolved for given locale
   * @param locale the locale that was used to search for the code within
   */
  public NoSuchMessageException(String code, @Nullable Locale locale) {
    super("No message found under code '" + code + "' for locale '" + locale + "'.");
  }

  /**
   * Create a new exception.
   *
   * @param code the code that could not be resolved for given locale
   */
  public NoSuchMessageException(String code) {
    super("No message found under code '" + code + "' for locale '" + Locale.getDefault() + "'.");
  }

}

