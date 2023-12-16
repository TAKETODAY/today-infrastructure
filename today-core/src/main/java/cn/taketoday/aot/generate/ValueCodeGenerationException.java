/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.aot.generate;

import cn.taketoday.lang.Nullable;

/**
 * Thrown when value code generation fails.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ValueCodeGenerationException extends RuntimeException {

  @Nullable
  private final Object value;

  protected ValueCodeGenerationException(String message, @Nullable Object value, @Nullable Throwable cause) {
    super(message, cause);
    this.value = value;
  }

  public ValueCodeGenerationException(@Nullable Object value, Throwable cause) {
    super(buildErrorMessage(value), cause);
    this.value = value;
  }

  private static String buildErrorMessage(@Nullable Object value) {
    StringBuilder message = new StringBuilder("Failed to generate code for '");
    message.append(value).append("'");
    if (value != null) {
      message.append(" with type ").append(value.getClass());
    }
    return message.toString();
  }

  /**
   * Return the value that failed to be generated.
   *
   * @return the value
   */
  @Nullable
  public Object getValue() {
    return this.value;
  }

}
