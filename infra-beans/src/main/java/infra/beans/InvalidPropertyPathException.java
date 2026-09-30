/*
 * Copyright 2002-present the original author or authors.
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

package infra.beans;

import org.jspecify.annotations.Nullable;

import java.beans.PropertyChangeEvent;
import java.util.Objects;

/**
 * Raised when a property path does not follow the grammar of {@link PropertyPath}.
 * A syntactically valid path that cannot be resolved against a bean instead raises
 * {@link NotReadablePropertyException} or {@link NotWritablePropertyException}.
 * <p>As a {@link PropertyAccessException}, this failure may be collected in a
 * {@link PropertyBatchUpdateException} alongside other binding errors.
 *
 * @author Brian Clozel
 * @see PropertyPath#parse(String)
 * @since 5.0
 */
@SuppressWarnings("serial")
public class InvalidPropertyPathException extends PropertyAccessException {

  /** Error code for malformed paths. */
  public static final String ERROR_CODE = "invalidPropertyPath";

  private final String propertyPath;

  /**
   * Create an exception for a malformed property path.
   *
   * @param propertyPath the offending path
   * @param reason the violated grammar rule
   */
  public InvalidPropertyPathException(String propertyPath, String reason) {
    super("Invalid property path '" + propertyPath + "': " + reason, null);
    this.propertyPath = propertyPath;
  }

  /**
   * Associate a parsing failure with a property change event.
   *
   * @param propertyChangeEvent the event for the property
   * @param cause the original parsing failure
   */
  public InvalidPropertyPathException(PropertyChangeEvent propertyChangeEvent, InvalidPropertyPathException cause) {
    super(propertyChangeEvent, Objects.requireNonNull(cause.getMessage()), cause);
    this.propertyPath = cause.propertyPath;
  }

  /**
   * Associate a parsing failure with an attempted change.
   *
   * @param source the bean that fired the event
   * @param propertyName the programmatic property name
   * @param newValue the attempted value
   * @param cause the original parsing failure
   */
  public InvalidPropertyPathException(Object source, String propertyName, @Nullable Object newValue,
          InvalidPropertyPathException cause) {
    this(new PropertyChangeEvent(source, propertyName, null, newValue), cause);
  }

  /** Return the offending property path. */
  public String getPropertyPath() {
    return this.propertyPath;
  }

  @Override
  public String getErrorCode() {
    return ERROR_CODE;
  }
}
