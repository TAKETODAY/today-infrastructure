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

package infra.context.properties.source;

import org.jspecify.annotations.Nullable;

import infra.lang.Assert;

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
