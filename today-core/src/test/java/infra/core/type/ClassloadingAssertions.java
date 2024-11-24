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

package infra.core.type;

import java.lang.reflect.Method;

import infra.util.ClassUtils;
import infra.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ramnivas Laddad
 * @author Sam Brannen
 */
abstract class ClassloadingAssertions {

  private static boolean isClassLoaded(String className) {
    ClassLoader cl = ClassUtils.getDefaultClassLoader();
    Method findLoadedClassMethod = ReflectionUtils.findMethod(cl.getClass(), "findLoadedClass", String.class);
    ReflectionUtils.makeAccessible(findLoadedClassMethod);
    Class<?> loadedClass = (Class<?>) ReflectionUtils.invokeMethod(findLoadedClassMethod, cl, className);
    return loadedClass != null;
  }

  public static void assertClassNotLoaded(String className) {
    assertThat(isClassLoaded(className)).as("Class [" + className + "] should not have been loaded").isFalse();
  }

}
