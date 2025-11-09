/*
 * Copyright 2017 - 2025 the original author or authors.
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
