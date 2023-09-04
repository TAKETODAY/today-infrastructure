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

package cn.taketoday.classify.util;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.aop.framework.Advised;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * A strategy interface for invoking a method. Typically used by adapters.
 *
 * @author Mark Fisher
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface MethodInvoker {

  Object invokeMethod(Object... args);

  /**
   * Create {@link MethodInvoker} for the method with the provided annotation on the
   * provided object. Annotations that cannot be applied to methods (i.e. that aren't
   * annotated with an element type of METHOD) will cause an exception to be thrown.
   *
   * @param annotationType to be searched for
   * @param target to be invoked
   * @return MethodInvoker for the provided annotation, null if none is found.
   */
  static MethodInvoker forAnnotation(Class<? extends Annotation> annotationType, Object target) {
    Assert.notNull(target, "Target is required");
    Assert.notNull(annotationType, "AnnotationType is required");
    if (!ObjectUtils.containsElement(annotationType.getAnnotation(Target.class).value(), ElementType.METHOD)) {
      throw new IllegalArgumentException("Annotation [" + annotationType + "] is not a Method-level annotation.");
    }

    Class<?> targetClass = target instanceof Advised
                           ? ((Advised) target).getTargetSource().getTargetClass()
                           : target.getClass();
    if (targetClass == null) {
      // Proxy with no target cannot have annotations
      return null;
    }
    AtomicReference<Method> annotatedMethod = new AtomicReference<>();
    ReflectionUtils.doWithMethods(targetClass, method -> {
      Annotation annotation = AnnotationUtils.findAnnotation(method, annotationType);
      if (annotation != null) {
        if (annotatedMethod.get() != null) {
          throw new IllegalArgumentException(
                  "found more than one method on target class [" + targetClass.getSimpleName()
                          + "] with the annotation type [" + annotationType.getSimpleName() + "].");
        }
        annotatedMethod.set(method);
      }
    });
    Method method = annotatedMethod.get();
    if (method == null) {
      return null;
    }
    else {
      return new SimpleMethodInvoker(target, annotatedMethod.get());
    }
  }

  /**
   * Create a {@link MethodInvoker} for the delegate from a single public method.
   *
   * @param target an object to search for an appropriate method
   * @return a MethodInvoker that calls a method on the delegate
   * @throws IllegalStateException not found, no more than one non-void public method detected with single argument
   */
  static MethodInvoker forSingleArgument(Object target) {
    AtomicReference<Method> methodHolder = new AtomicReference<>();
    ReflectionUtils.doWithMethods(target.getClass(), method -> {
      if ((method.getModifiers() & Modifier.PUBLIC) == 0 || method.isBridge()) {
        return;
      }
      if (method.getParameterTypes().length != 1) {
        return;
      }
      if (method.getReturnType().equals(Void.TYPE) || ReflectionUtils.isEqualsMethod(method)) {
        return;
      }
      Assert.state(methodHolder.get() == null,
              "More than one non-void public method detected with single argument.");
      methodHolder.set(method);
    });
    Method method = methodHolder.get();
    return new SimpleMethodInvoker(target, method);
  }

}
