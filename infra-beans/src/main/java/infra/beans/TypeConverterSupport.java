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

import java.lang.reflect.Field;

import infra.core.MethodParameter;
import infra.core.TypeDescriptor;
import infra.core.conversion.ConversionException;
import infra.core.conversion.ConverterNotFoundException;

/**
 * Base implementation of the {@link TypeConverter} interface, using a package-private delegate.
 * Mainly serves as base class for {@link BeanWrapperImpl}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SimpleTypeConverter
 * @since 4.0 2022/2/17 17:46
 */
public abstract class TypeConverterSupport extends PropertyEditorRegistrySupport implements TypeConverter {

  @SuppressWarnings("NullAway")
  protected TypeConverterDelegate typeConverterDelegate;

  @Override
  @Nullable
  public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType) throws TypeMismatchException {
    return convertIfNecessary(null, value, requiredType, TypeDescriptor.valueOf(requiredType));
  }

  @Override
  @Nullable
  public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
          @Nullable MethodParameter methodParam) throws TypeMismatchException {

    return convertIfNecessary((methodParam != null ? methodParam.getParameterName() : null), value, requiredType,
            (methodParam != null ? new TypeDescriptor(methodParam) : TypeDescriptor.valueOf(requiredType)));
  }

  @Override
  @Nullable
  public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
          @Nullable Field field) throws TypeMismatchException {

    return convertIfNecessary((field != null ? field.getName() : null), value, requiredType,
            (field != null ? new TypeDescriptor(field) : TypeDescriptor.valueOf(requiredType)));
  }

  @Override
  @Nullable
  public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
          @Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {

    return convertIfNecessary(null, value, requiredType, typeDescriptor);
  }

  @Nullable
  private <T> T convertIfNecessary(@Nullable String propertyName, @Nullable Object value,
          @Nullable Class<T> requiredType, @Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {

    try {
      return typeConverterDelegate.convertIfNecessary(
              propertyName, null, value, requiredType, typeDescriptor);
    }
    catch (ConverterNotFoundException | IllegalStateException ex) {
      throw new ConversionNotSupportedException(value, requiredType, ex);
    }
    catch (ConversionException | IllegalArgumentException ex) {
      throw new TypeMismatchException(value, requiredType, ex);
    }
  }

}
