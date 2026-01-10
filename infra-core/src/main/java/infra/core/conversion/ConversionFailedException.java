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

package infra.core.conversion;

import org.jspecify.annotations.Nullable;

import infra.core.TypeDescriptor;
import infra.util.ObjectUtils;

/**
 * Exception to be thrown when an actual type conversion attempt fails.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author TODAY 2021/3/22 12:11
 * @since 3.0
 */
public class ConversionFailedException extends ConversionException {

  @Nullable
  private final TypeDescriptor sourceType;

  private final TypeDescriptor targetType;

  @Nullable
  private final Object value;

  /**
   * Create a new conversion exception.
   *
   * @param sourceType the value's original type
   * @param targetType the value's target type
   * @param value the value we tried to convert
   * @param cause the cause of the conversion failure
   */
  public ConversionFailedException(@Nullable TypeDescriptor sourceType,
          TypeDescriptor targetType, @Nullable Object value, Throwable cause) {

    super("Failed to convert from type [%s] to type [%s] for value [%s]".formatted(sourceType, targetType, ObjectUtils.nullSafeConciseToString(value)), cause);
    this.sourceType = sourceType;
    this.targetType = targetType;
    this.value = value;
  }

  /**
   * Return the source type we tried to convert the value from.
   */
  @Nullable
  public TypeDescriptor getSourceType() {
    return this.sourceType;
  }

  /**
   * Return the target type we tried to convert the value to.
   */
  public TypeDescriptor getTargetType() {
    return this.targetType;
  }

  /**
   * Return the offending value.
   */
  @Nullable
  public Object getValue() {
    return this.value;
  }

}
