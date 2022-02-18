/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.beans;

import java.lang.reflect.Field;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionException;
import cn.taketoday.core.conversion.ConverterNotFoundException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

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

  @Nullable
  protected TypeConverterDelegate typeConverterDelegate;

  @Override
  @Nullable
  public <T> T convertIfNecessary(
          @Nullable Object value, @Nullable Class<T> requiredType) throws TypeMismatchException {
    return convertIfNecessary(value, requiredType, TypeDescriptor.valueOf(requiredType));
  }

  @Override
  @Nullable
  public <T> T convertIfNecessary(
          @Nullable Object value, @Nullable Class<T> requiredType,
          @Nullable MethodParameter methodParam) throws TypeMismatchException {

    return convertIfNecessary(value, requiredType,
            (methodParam != null ? new TypeDescriptor(methodParam) : TypeDescriptor.valueOf(requiredType)));
  }

  @Override
  @Nullable
  public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable Field field)
          throws TypeMismatchException {

    return convertIfNecessary(value, requiredType,
            (field != null ? new TypeDescriptor(field) : TypeDescriptor.valueOf(requiredType)));
  }

  @Nullable
  @Override
  public <T> T convertIfNecessary(
          @Nullable Object value, @Nullable Class<T> requiredType,
          @Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {

    Assert.state(this.typeConverterDelegate != null, "No TypeConverterDelegate");
    try {
      return this.typeConverterDelegate.convertIfNecessary(null, null, value, requiredType, typeDescriptor);
    }
    catch (ConverterNotFoundException | IllegalStateException ex) {
      throw new ConversionNotSupportedException(value, requiredType, ex);
    }
    catch (ConversionException | IllegalArgumentException ex) {
      throw new TypeMismatchException(value, requiredType, ex);
    }
  }

}
