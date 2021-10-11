/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core.conversion.support;

import cn.taketoday.lang.Assert;
import cn.taketoday.core.conversion.ConversionException;
import cn.taketoday.util.ClassUtils;

/**
 * @author TODAY 2021/3/21 11:09
 * @since 3.0
 */
public class ClassConverter extends StringSourceConverter<Class<?>> {
  private ClassLoader classLoader = ClassUtils.getClassLoader();

  @Override
  public Class<?> convert(String source) {
    try {
      return loadClass(source);
    }
    catch (ClassNotFoundException e) {
      return fallback(source, e);
    }
  }

  protected Class<?> fallback(String source, ClassNotFoundException e) {
    throw new ConversionException(e);
  }

  protected Class<?> loadClass(String source) throws ClassNotFoundException {
    return obtainClassLoader().loadClass(source);
  }

  public void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  public ClassLoader getClassLoader() {
    return classLoader;
  }

  final ClassLoader obtainClassLoader() {
    final ClassLoader classLoader = getClassLoader();
    Assert.state(classLoader != null, "No ClassLoader.");
    return classLoader;
  }
}
