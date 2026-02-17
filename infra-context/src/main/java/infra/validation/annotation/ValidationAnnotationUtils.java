/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.validation.annotation;

import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import infra.aop.framework.AopProxyUtils;
import infra.aop.support.AopUtils;
import infra.core.annotation.AnnotationUtils;
import infra.lang.Constant;

/**
 * Utility class for handling validation annotations.
 * Mainly for internal use within the framework.
 *
 * @author Christoph Dreis
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class ValidationAnnotationUtils {

  /**
   * Determine any validation hints by the given annotation.
   * <p>This implementation checks for Infra
   * {@link Validated},
   * {@code @jakarta.validation.Valid}, and custom annotations whose
   * name starts with "Valid" which may optionally declare validation
   * hints through the "value" attribute.
   *
   * @param ann the annotation (potentially a validation annotation)
   * @return the validation hints to apply (possibly an empty array),
   * or {@code null} if this annotation does not trigger any validation
   */
  public static Object @Nullable [] determineValidationHints(Annotation ann) {
    // Direct presence of @Validated ?
    if (ann instanceof Validated validated) {
      return validated.value();
    }
    // Direct presence of @Valid ?
    Class<? extends Annotation> annotationType = ann.annotationType();
    if ("jakarta.validation.Valid".equals(annotationType.getName())) {
      return Constant.EMPTY_OBJECTS;
    }
    // Meta presence of @Validated ?
    Validated validatedAnn = AnnotationUtils.getAnnotation(ann, Validated.class);
    if (validatedAnn != null) {
      return validatedAnn.value();
    }
    // Custom validation annotation ?
    if (annotationType.getSimpleName().startsWith("Valid")) {
      return convertValidationHints(AnnotationUtils.getValue(ann));
    }
    // No validation triggered
    return null;
  }

  private static Object[] convertValidationHints(@Nullable Object hints) {
    if (hints == null) {
      return Constant.EMPTY_OBJECTS;
    }
    return (hints instanceof Object[] ? (Object[]) hints : new Object[] { hints });
  }

  /**
   * Determine the applicable validation groups from an
   * {@link infra.validation.annotation.Validated @Validated}
   * annotation either on the method, or on the containing target class of
   * the method, or for an AOP proxy without a target (with all behavior in
   * advisors), also check on proxied interfaces.
   *
   * @since 5.0
   */
  public static Class<?>[] determineValidationGroups(Object target, Method method) {
    Validated validatedAnn = AnnotationUtils.findAnnotation(method, Validated.class);
    if (validatedAnn == null) {
      if (AopUtils.isAopProxy(target)) {
        for (Class<?> type : AopProxyUtils.proxiedUserInterfaces(target)) {
          validatedAnn = AnnotationUtils.findAnnotation(type, Validated.class);
          if (validatedAnn != null) {
            break;
          }
        }
      }
      else {
        validatedAnn = AnnotationUtils.findAnnotation(target.getClass(), Validated.class);
      }
    }
    return (validatedAnn != null ? validatedAnn.value() : Constant.EMPTY_CLASSES);
  }
}
