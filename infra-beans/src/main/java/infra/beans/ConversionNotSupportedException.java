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

/**
 * Exception thrown when no suitable editor or converter can be found for a bean property.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/17 18:06
 */
public class ConversionNotSupportedException extends TypeMismatchException {

  /**
   * Create a new ConversionNotSupportedException.
   *
   * @param propertyChangeEvent the PropertyChangeEvent that resulted in the problem
   * @param requiredType the required target type (or {@code null} if not known)
   * @param cause the root cause (may be {@code null})
   */
  public ConversionNotSupportedException(PropertyChangeEvent propertyChangeEvent, @Nullable Class<?> requiredType, @Nullable Throwable cause) {
    super(propertyChangeEvent, requiredType, cause);
  }

  /**
   * Create a new ConversionNotSupportedException.
   *
   * @param value the offending value that couldn't be converted (may be {@code null})
   * @param requiredType the required target type (or {@code null} if not known)
   * @param cause the root cause (may be {@code null})
   */
  public ConversionNotSupportedException(@Nullable Object value, @Nullable Class<?> requiredType, @Nullable Throwable cause) {
    super(value, requiredType, cause);
  }

}

