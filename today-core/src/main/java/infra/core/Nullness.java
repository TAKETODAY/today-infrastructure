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

package infra.core;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;

/**
 * Constants that indicate nullness, as well as related utility methods.
 *
 * <p>Nullness applies to type usage, a field, a method return type, or a parameter.
 * <a href="https://jspecify.dev/docs/user-guide/">JSpecify annotations</a> are
 * fully supported
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public enum Nullness {

  /**
   * Unspecified nullness (Java default for non-primitive types and JSpecify
   * {@code @NullUnmarked} code).
   */
  UNSPECIFIED,

  /**
   * Can include null (typically specified with a {@code @Nullable} annotation).
   */
  NULLABLE,

  /**
   * Will not include null (JSpecify {@code @NullMarked} code).
   */
  NON_NULL;

  /**
   * Return the nullness of the return type for the given method.
   *
   * @param method the source for the method return type
   * @return the corresponding nullness
   */
  public static Nullness forMethodReturnType(Method method) {
    return hasNullableAnnotation(method) ? Nullness.NULLABLE :
            jSpecifyNullness(method, method.getDeclaringClass(), method.getAnnotatedReturnType());
  }

  /**
   * Return the nullness of the given parameter.
   *
   * @param parameter the parameter descriptor
   * @return the corresponding nullness
   */
  public static Nullness forParameter(Parameter parameter) {
    Executable executable = parameter.getDeclaringExecutable();
    return hasNullableAnnotation(parameter) ? Nullness.NULLABLE :
            jSpecifyNullness(executable, executable.getDeclaringClass(), parameter.getAnnotatedType());
  }

  /**
   * Return the nullness of the given method parameter.
   *
   * @param mp the method parameter descriptor
   * @return the corresponding nullness
   */
  public static Nullness forMethodParameter(MethodParameter mp) {
    return mp.getParameterIndex() < 0 ?
            forMethodReturnType(Objects.requireNonNull(mp.getMethod())) :
            forParameter(mp.getParameter());
  }

  /**
   * Return the nullness of the given field.
   *
   * @param field the field descriptor
   * @return the corresponding nullness
   */
  public static Nullness forField(Field field) {
    return hasNullableAnnotation(field) ? Nullness.NULLABLE :
            jSpecifyNullness(field, field.getDeclaringClass(), field.getAnnotatedType());
  }

  // Check method and parameter level @Nullable annotations regardless of the package
  // (including JSR 305 annotations)
  private static boolean hasNullableAnnotation(AnnotatedElement element) {
    for (Annotation annotation : element.getDeclaredAnnotations()) {
      if ("Nullable".equals(annotation.annotationType().getSimpleName())) {
        return true;
      }
    }
    return false;
  }

  private static Nullness jSpecifyNullness(AnnotatedElement annotatedElement, Class<?> declaringClass, AnnotatedType annotatedType) {
    if (annotatedType.getType() instanceof Class<?> clazz && clazz.isPrimitive()) {
      return clazz != void.class ? Nullness.NON_NULL : Nullness.UNSPECIFIED;
    }
    if (annotatedType.isAnnotationPresent(Nullable.class)) {
      return Nullness.NULLABLE;
    }
    if (annotatedType.isAnnotationPresent(NonNull.class)) {
      return Nullness.NON_NULL;
    }
    Nullness nullness = Nullness.UNSPECIFIED;
    // Package level
    Package declaringPackage = declaringClass.getPackage();
    if (declaringPackage.isAnnotationPresent(NullMarked.class)) {
      nullness = Nullness.NON_NULL;
    }
    // Class level
    if (declaringClass.isAnnotationPresent(NullMarked.class)) {
      nullness = Nullness.NON_NULL;
    }
    else if (declaringClass.isAnnotationPresent(NullUnmarked.class)) {
      nullness = Nullness.UNSPECIFIED;
    }
    // Annotated element level
    if (annotatedElement.isAnnotationPresent(NullMarked.class)) {
      nullness = Nullness.NON_NULL;
    }
    else if (annotatedElement.isAnnotationPresent(NullUnmarked.class)) {
      nullness = Nullness.UNSPECIFIED;
    }
    return nullness;
  }

}
