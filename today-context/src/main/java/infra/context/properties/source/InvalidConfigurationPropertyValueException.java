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

package infra.context.properties.source;

import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * Exception thrown when a configuration property value is invalid.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class InvalidConfigurationPropertyValueException extends RuntimeException {

  private final String name;

  @Nullable
  private final Object value;

  @Nullable
  private final String reason;

  /**
   * Creates a new instance for the specified property {@code name} and {@code value},
   * including a {@code reason} why the value is invalid.
   *
   * @param name the name of the property in canonical format
   * @param value the value of the property, can be {@code null}
   * @param reason a human-readable text that describes why the reason is invalid.
   * Starts with an upper-case and ends with a dot. Several sentences and carriage
   * returns are allowed.
   */
  public InvalidConfigurationPropertyValueException(String name, Object value, String reason) {
    this(name, value, reason, null);
  }

  InvalidConfigurationPropertyValueException(String name, Object value, String reason, @Nullable Throwable cause) {
    super("Property " + name + " with value '" + value + "' is invalid: " + reason, cause);
    Assert.notNull(name, "Name is required");
    this.name = name;
    this.value = value;
    this.reason = reason;
  }

  /**
   * Return the name of the property.
   *
   * @return the property name
   */
  public String getName() {
    return this.name;
  }

  /**
   * Return the invalid value, can be {@code null}.
   *
   * @return the invalid value
   */
  @Nullable
  public Object getValue() {
    return this.value;
  }

  /**
   * Return the reason why the value is invalid.
   *
   * @return the reason
   */
  @Nullable
  public String getReason() {
    return this.reason;
  }

}
