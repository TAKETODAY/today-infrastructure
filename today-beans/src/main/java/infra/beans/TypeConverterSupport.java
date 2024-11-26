/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.beans;

import java.lang.reflect.Field;

import infra.core.MethodParameter;
import infra.core.TypeDescriptor;
import infra.core.conversion.ConversionException;
import infra.core.conversion.ConverterNotFoundException;
import infra.lang.Nullable;

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
