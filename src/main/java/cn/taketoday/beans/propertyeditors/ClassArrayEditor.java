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
import java.util.StringJoiner;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Property editor for an array of {@link Class Classes}, to enable
 * the direct population of a {@code Class[]} property without having to
 * use a {@code String} class name property as bridge.
 *
 * <p>Also supports "java.lang.String[]"-style array class names, in contrast
 * to the standard {@link Class#forName(String)} method.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class ClassArrayEditor extends PropertyEditorSupport {

  @Nullable
  private final ClassLoader classLoader;

  /**
   * Create a default {@code ClassEditor}, using the thread
   * context {@code ClassLoader}.
   */
  public ClassArrayEditor() {
    this(null);
  }

  /**
   * Create a default {@code ClassArrayEditor}, using the given
   * {@code ClassLoader}.
   *
   * @param classLoader the {@code ClassLoader} to use
   * (or pass {@code null} for the thread context {@code ClassLoader})
   */
  public ClassArrayEditor(@Nullable ClassLoader classLoader) {
    this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
  }

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    if (StringUtils.hasText(text)) {
      String[] classNames = StringUtils.commaDelimitedListToStringArray(text);
      Class<?>[] classes = new Class<?>[classNames.length];
      for (int i = 0; i < classNames.length; i++) {
        String className = classNames[i].trim();
        classes[i] = ClassUtils.resolveClassName(className, this.classLoader);
      }
      setValue(classes);
    }
    else {
      setValue(null);
    }
  }

  @Override
  public String getAsText() {
    Class<?>[] classes = (Class[]) getValue();
    if (ObjectUtils.isEmpty(classes)) {
      return "";
    }
    StringJoiner sj = new StringJoiner(",");
    for (Class<?> klass : classes) {
      sj.add(ClassUtils.getQualifiedName(klass));
    }
    return sj.toString();
  }

}
