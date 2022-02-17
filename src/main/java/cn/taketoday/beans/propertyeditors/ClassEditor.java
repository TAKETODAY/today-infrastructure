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

package cn.taketoday.beans.propertyeditors;

import java.beans.PropertyEditorSupport;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * Property editor for {@link Class java.lang.Class}, to enable the direct
 * population of a {@code Class} property without recourse to having to use a
 * String class name property as bridge.
 *
 * <p>Also supports "java.lang.String[]"-style array class names, in contrast to the
 * standard {@link Class#forName(String)} method.
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 * @see Class#forName
 * @see cn.taketoday.util.ClassUtils#forName(String, ClassLoader)
 * @since 4.0
 */
public class ClassEditor extends PropertyEditorSupport {

  @Nullable
  private final ClassLoader classLoader;

  /**
   * Create a default ClassEditor, using the thread context ClassLoader.
   */
  public ClassEditor() {
    this(null);
  }

  /**
   * Create a default ClassEditor, using the given ClassLoader.
   *
   * @param classLoader the ClassLoader to use
   * (or {@code null} for the thread context ClassLoader)
   */
  public ClassEditor(@Nullable ClassLoader classLoader) {
    this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
  }

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    if (StringUtils.hasText(text)) {
      setValue(ClassUtils.resolveClassName(text.trim(), this.classLoader));
    }
    else {
      setValue(null);
    }
  }

  @Override
  public String getAsText() {
    Class<?> clazz = (Class<?>) getValue();
    if (clazz != null) {
      return ClassUtils.getQualifiedName(clazz);
    }
    else {
      return "";
    }
  }

}
