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
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.lang.Nullable;

/**
 * Interface that defines type conversion methods. Typically (but not necessarily)
 * implemented in conjunction with the {@link PropertyEditorRegistry} interface.
 *
 * <p><b>Note:</b> Since TypeConverter implementations are typically based on
 * {@link java.beans.PropertyEditor PropertyEditors} which aren't thread-safe,
 * TypeConverters themselves are <em>not</em> to be considered as thread-safe either.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SimpleTypeConverter
 * @see BeanWrapperImpl
 * @since 4.0 2022/2/17 18:01
 */
public interface TypeConverter {

  /**
   * Convert the value to the required type (if necessary from a String).
   * <p>Conversions from String to any type will typically use the {@code setAsText}
   * method of the PropertyEditor class, or a Framework Converter in a ConversionService.
   *
   * @param value the value to convert
   * @param requiredType the type we must convert to
   * (or {@code null} if not known, for example in case of a collection element)
   * @return the new value, possibly the result of type conversion
   * @throws TypeMismatchException if type conversion failed
   * @see java.beans.PropertyEditor#setAsText(String)
   * @see java.beans.PropertyEditor#getValue()
   * @see ConversionService
   * @see Converter
   */
  @Nullable
  <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType) throws TypeMismatchException;

  /**
   * Convert the value to the required type (if necessary from a String).
   * <p>Conversions from String to any type will typically use the {@code setAsText}
   * method of the PropertyEditor class, or a Framework Converter in a ConversionService.
   *
   * @param value the value to convert
   * @param requiredType the type we must convert to
   * (or {@code null} if not known, for example in case of a collection element)
   * @param methodParam the method parameter that is the target of the conversion
   * (for analysis of generic types; may be {@code null})
   * @return the new value, possibly the result of type conversion
   * @throws TypeMismatchException if type conversion failed
   * @see java.beans.PropertyEditor#setAsText(String)
   * @see java.beans.PropertyEditor#getValue()
   * @see ConversionService
   * @see Converter
   */
  @Nullable
  <T> T convertIfNecessary(
          @Nullable Object value, @Nullable Class<T> requiredType,
          @Nullable MethodParameter methodParam) throws TypeMismatchException;

  /**
   * Convert the value to the required type (if necessary from a String).
   * <p>Conversions from String to any type will typically use the {@code setAsText}
   * method of the PropertyEditor class, or a Framework Converter in a ConversionService.
   *
   * @param value the value to convert
   * @param requiredType the type we must convert to
   * (or {@code null} if not known, for example in case of a collection element)
   * @param field the reflective field that is the target of the conversion
   * (for analysis of generic types; may be {@code null})
   * @return the new value, possibly the result of type conversion
   * @throws TypeMismatchException if type conversion failed
   * @see java.beans.PropertyEditor#setAsText(String)
   * @see java.beans.PropertyEditor#getValue()
   * @see ConversionService
   * @see Converter
   */
  @Nullable
  <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable Field field)
          throws TypeMismatchException;

  /**
   * Convert the value to the required type (if necessary from a String).
   * <p>Conversions from String to any type will typically use the {@code setAsText}
   * method of the PropertyEditor class, or a Framework Converter in a ConversionService.
   *
   * @param value the value to convert
   * @param requiredType the type we must convert to
   * (or {@code null} if not known, for example in case of a collection element)
   * @param typeDescriptor the type descriptor to use (may be {@code null}))
   * @return the new value, possibly the result of type conversion
   * @throws TypeMismatchException if type conversion failed
   * @see java.beans.PropertyEditor#setAsText(String)
   * @see java.beans.PropertyEditor#getValue()
   * @see ConversionService
   * @see Converter
   */
  @Nullable
  default <T> T convertIfNecessary(
          @Nullable Object value, @Nullable Class<T> requiredType,
          @Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {

    throw new UnsupportedOperationException("TypeDescriptor resolution not supported");
  }

}
