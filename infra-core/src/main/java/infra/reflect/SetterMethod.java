/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.reflect;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import infra.util.ReflectionUtils;

/**
 * Fast invocation of bean's setter method {@link java.lang.reflect.Method Method}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2020-08-14 00:29
 */
public interface SetterMethod {

  /**
   * Sets the property value.
   *
   * @param obj the target object
   * @param value the property value to set
   * @throws ReflectionException if the property is read-only
   */
  void set(Object obj, @Nullable Object value);

  /**
   * Returns the write method for this property.
   *
   * @return the write method, or {@code null} if this property is read-only
   * @throws ReflectionException if the property is read-only
   * @since 3.0
   */
  @Nullable
  default Method getWriteMethod() {
    return null;
  }

  // static factory

  /**
   * Create a new SetterMethod from a Java reflection Field.
   *
   * @param field the given Java reflection Field
   * @return a SetterMethod instance
   */
  static SetterMethod forField(final Field field) {
    final Method writeMethod = ReflectionUtils.getWriteMethod(field);
    if (writeMethod == null) {
      return forReflective(field); // fallback to Reflective
    }
    return forMethod(writeMethod);
  }

  /**
   * Create a SetterMethod using fast invocation technology {@link MethodInvoker}.
   *
   * @param method the Java reflection {@link Method}
   * @return a SetterMethod instance
   * @see MethodInvoker#forMethod(Method)
   */
  static SetterMethod forMethod(final Method method) {
    final MethodInvoker accessor = MethodInvoker.forMethod(method);
    return forMethod(accessor);
  }

  /**
   * Uses fast invocation technology {@link MethodInvoker}
   *
   * @param invoker Fast MethodInvoker
   * @return SetterMethod
   * @see MethodInvoker#forMethod(Method)
   */
  static SetterMethod forMethod(final MethodInvoker invoker) {
    return new MethodAccessorSetterMethod(invoker);
  }

  /**
   * Uses Java reflection {@link Field} technology
   *
   * @param field Field
   * @return Reflective SetterMethod
   * @see Field#set(Object, Object)
   * @see ReflectionUtils#setField(Field, Object, Object)
   */
  static SetterMethod forReflective(final Field field) {
    ReflectionUtils.makeAccessible(field);
    return new ReflectiveSetterMethod(field);
  }

}
