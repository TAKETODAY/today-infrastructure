/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.beans.factory.support;

import java.lang.reflect.Method;

import cn.taketoday.beans.factory.BeanDefinitionValidationException;
import cn.taketoday.util.ClassUtils;

/**
 * Descriptor for a {@link java.lang.reflect.Method Method} which holds a
 * reference to the method's {@linkplain #declaringClass declaring class},
 * {@linkplain #methodName name}, and {@linkplain #parameterTypes parameter types}.
 *
 * @param declaringClass the method's declaring class
 * @param methodName the name of the method
 * @param parameterTypes the types of parameters accepted by the method
 * @author Sam Brannen
 * @since 4.0
 */
record MethodDescriptor(Class<?> declaringClass, String methodName, Class<?>... parameterTypes) {

  /**
   * Create a {@link MethodDescriptor} for the supplied bean class and method name.
   * <p>The supplied {@code methodName} may be a {@linkplain Method#getName()
   * simple method name} or a
   * {@linkplain cn.taketoday.util.ClassUtils#getQualifiedMethodName(Method)
   * qualified method name}.
   * <p>If the method name is fully qualified, this utility will parse the
   * method name and its declaring class from the qualified method name and then
   * attempt to load the method's declaring class using the {@link ClassLoader}
   * of the supplied {@code beanClass}. Otherwise, the returned descriptor will
   * reference the supplied {@code beanClass} and {@code methodName}.
   *
   * @param beanName the bean name in the factory (for debugging purposes)
   * @param beanClass the bean class
   * @param methodName the name of the method
   * @return a new {@code MethodDescriptor}; never {@code null}
   */
  static MethodDescriptor create(String beanName, Class<?> beanClass, String methodName) {
    try {
      Class<?> declaringClass = beanClass;
      String methodNameToUse = methodName;

      // Parse fully-qualified method name if necessary.
      int indexOfDot = methodName.lastIndexOf('.');
      if (indexOfDot > 0) {
        String className = methodName.substring(0, indexOfDot);
        methodNameToUse = methodName.substring(indexOfDot + 1);
        if (!beanClass.getName().equals(className)) {
          declaringClass = ClassUtils.forName(className, beanClass.getClassLoader());
        }
      }
      return new MethodDescriptor(declaringClass, methodNameToUse);
    }
    catch (Exception | LinkageError ex) {
      throw new BeanDefinitionValidationException(
              "Could not create MethodDescriptor for method '%s' on bean with name '%s': %s"
                      .formatted(methodName, beanName, ex.getMessage()));
    }
  }

}
