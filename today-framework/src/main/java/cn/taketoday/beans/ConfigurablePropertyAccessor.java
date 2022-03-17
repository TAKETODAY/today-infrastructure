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

import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.lang.Nullable;

/**
 * Interface that encapsulates configuration methods for a PropertyAccessor.
 * Also extends the PropertyEditorRegistry interface, which defines methods
 * for PropertyEditor management.
 *
 * <p>Serves as base interface for {@link BeanWrapper}.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/17 17:37
 */
public interface ConfigurablePropertyAccessor extends PropertyAccessor, PropertyEditorRegistry, TypeConverter {

  /**
   * Specify a ConversionService to use for converting
   * property values, as an alternative to JavaBeans PropertyEditors.
   */
  void setConversionService(@Nullable ConversionService conversionService);

  /**
   * Return the associated ConversionService, if any.
   */
  @Nullable
  ConversionService getConversionService();

  /**
   * Set whether to extract the old property value when applying a
   * property editor to a new value for a property.
   */
  void setExtractOldValueForEditor(boolean extractOldValueForEditor);

  /**
   * Return whether to extract the old property value when applying a
   * property editor to a new value for a property.
   */
  boolean isExtractOldValueForEditor();

  /**
   * Set whether this instance should attempt to "auto-grow" a
   * nested path that contains a {@code null} value.
   * <p>If {@code true}, a {@code null} path location will be populated
   * with a default object value and traversed instead of resulting in a
   * {@link NullValueInNestedPathException}.
   * <p>Default is {@code false} on a plain PropertyAccessor instance.
   */
  void setAutoGrowNestedPaths(boolean autoGrowNestedPaths);

  /**
   * Return whether "auto-growing" of nested paths has been activated.
   */
  boolean isAutoGrowNestedPaths();

}
