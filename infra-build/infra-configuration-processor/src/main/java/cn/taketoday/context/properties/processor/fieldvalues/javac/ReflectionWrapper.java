/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.properties.processor.fieldvalues.javac;

import java.lang.reflect.Method;

/**
 * Base class for reflection based wrappers. Used to access internal Java classes without
 * needing tools.jar on the classpath.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ReflectionWrapper {

  private final Class<?> type;

  private final Object instance;

  ReflectionWrapper(String type, Object instance) {
    this.type = findClass(instance.getClass().getClassLoader(), type);
    this.instance = this.type.cast(instance);
  }

  protected final Object getInstance() {
    return this.instance;
  }

  @Override
  public String toString() {
    return this.instance.toString();
  }

  protected Class<?> findClass(String name) {
    return findClass(getInstance().getClass().getClassLoader(), name);
  }

  protected Method findMethod(String name, Class<?>... parameterTypes) {
    return findMethod(this.type, name, parameterTypes);
  }

  protected static Class<?> findClass(ClassLoader classLoader, String name) {
    try {
      return Class.forName(name, false, classLoader);
    }
    catch (ClassNotFoundException ex) {
      throw new IllegalStateException(ex);
    }
  }

  protected static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
    try {
      return type.getMethod(name, parameterTypes);
    }
    catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

}
