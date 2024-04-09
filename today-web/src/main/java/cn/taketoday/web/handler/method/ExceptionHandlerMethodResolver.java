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

package cn.taketoday.web.handler.method;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.MethodIntrospector;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.ReflectionUtils.MethodFilter;
import cn.taketoday.util.comparator.ExceptionDepthComparator;
import cn.taketoday.web.annotation.ExceptionHandler;

/**
 * Discovers {@linkplain ExceptionHandler @ExceptionHandler} methods in a given class,
 * including all of its superclasses, and helps to resolve a given {@link Exception}
 * to the exception types supported by a given {@link Method}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 22:12
 */
public class ExceptionHandlerMethodResolver {

  /**
   * A filter for selecting {@code @ExceptionHandler} methods.
   */
  public static final MethodFilter EXCEPTION_HANDLER_METHODS = method ->
          AnnotatedElementUtils.hasAnnotation(method, ExceptionHandler.class);

  private static final Method NO_MATCHING_EXCEPTION_HANDLER_METHOD;

  static {
    try {
      NO_MATCHING_EXCEPTION_HANDLER_METHOD =
              ExceptionHandlerMethodResolver.class.getDeclaredMethod("noMatchingExceptionHandler");
    }
    catch (NoSuchMethodException ex) {
      throw new IllegalStateException("Expected method not found: " + ex);
    }
  }

  private final Map<Class<? extends Throwable>, Method> mappedMethods = new HashMap<>(16);
  private final Map<Class<? extends Throwable>, Method> exceptionLookupCache = new ConcurrentReferenceHashMap<>(16);

  /**
   * A constructor that finds {@link ExceptionHandler} methods in the given type.
   *
   * @param handlerType the type to introspect
   */
  public ExceptionHandlerMethodResolver(Class<?> handlerType) {
    for (Method method : MethodIntrospector.filterMethods(handlerType, EXCEPTION_HANDLER_METHODS)) {
      for (Class<? extends Throwable> exceptionType : detectExceptionMappings(method)) {
        addExceptionMapping(exceptionType, method);
      }
    }
  }

  /**
   * Extract exception mappings from the {@code @ExceptionHandler} annotation first,
   * and then as a fallback from the method signature itself.
   */
  @SuppressWarnings("unchecked")
  private List<Class<? extends Throwable>> detectExceptionMappings(Method method) {
    ArrayList<Class<? extends Throwable>> result = new ArrayList<>();
    detectAnnotationExceptionMappings(method, result);
    if (result.isEmpty()) {
      for (Class<?> paramType : method.getParameterTypes()) {
        if (Throwable.class.isAssignableFrom(paramType)) {
          result.add((Class<? extends Throwable>) paramType);
        }
      }
    }
    if (result.isEmpty()) {
      throw new IllegalStateException("No exception types mapped to " + method);
    }
    return result;
  }

  private void detectAnnotationExceptionMappings(Method method, List<Class<? extends Throwable>> result) {
    ExceptionHandler ann = AnnotatedElementUtils.findMergedAnnotation(method, ExceptionHandler.class);
    Assert.state(ann != null, "No ExceptionHandler annotation");
    CollectionUtils.addAll(result, ann.value());
  }

  private void addExceptionMapping(Class<? extends Throwable> exceptionType, Method method) {
    Method oldMethod = mappedMethods.put(exceptionType, method);
    if (oldMethod != null && !oldMethod.equals(method)) {
      throw new IllegalStateException("Ambiguous @ExceptionHandler method mapped for [%s]: {%s, %s}"
              .formatted(exceptionType, oldMethod, method));
    }
  }

  /**
   * Whether the contained type has any exception mappings.
   */
  public boolean hasExceptionMappings() {
    return !this.mappedMethods.isEmpty();
  }

  /**
   * Find a {@link Method} to handle the given exception.
   * <p>Uses {@link ExceptionDepthComparator} if more than one match is found.
   *
   * @param exception the exception
   * @return a Method to handle the exception, or {@code null} if none found
   */
  @Nullable
  public Method resolveMethod(Throwable exception) {
    return resolveMethodByThrowable(exception);
  }

  /**
   * Find a {@link Method} to handle the given Throwable.
   * <p>Uses {@link ExceptionDepthComparator} if more than one match is found.
   *
   * @param exception the exception
   * @return a Method to handle the exception, or {@code null} if none found
   */
  @Nullable
  public Method resolveMethodByThrowable(Throwable exception) {
    Method method = resolveMethodByExceptionType(exception.getClass());
    if (method == null) {
      Throwable cause = exception.getCause();
      if (cause != null) {
        method = resolveMethodByThrowable(cause);
      }
    }
    return method;
  }

  /**
   * Find a {@link Method} to handle the given exception type. This can be
   * useful if an {@link Exception} instance is not available (e.g. for tools).
   * <p>Uses {@link ExceptionDepthComparator} if more than one match is found.
   *
   * @param exceptionType the exception type
   * @return a Method to handle the exception, or {@code null} if none found
   */
  @Nullable
  public Method resolveMethodByExceptionType(Class<? extends Throwable> exceptionType) {
    Method method = exceptionLookupCache.get(exceptionType);
    if (method == null) {
      method = getMappedMethod(exceptionType);
      exceptionLookupCache.put(exceptionType, method);
    }
    return method != NO_MATCHING_EXCEPTION_HANDLER_METHOD ? method : null;
  }

  /**
   * Return the {@link Method} mapped to the given exception type, or
   * {@link #NO_MATCHING_EXCEPTION_HANDLER_METHOD} if none.
   */
  private Method getMappedMethod(Class<? extends Throwable> exceptionType) {
    ArrayList<Class<? extends Throwable>> matches = new ArrayList<>();
    for (Class<? extends Throwable> mappedException : mappedMethods.keySet()) {
      if (mappedException.isAssignableFrom(exceptionType)) {
        matches.add(mappedException);
      }
    }
    if (!matches.isEmpty()) {
      if (matches.size() > 1) {
        matches.sort(new ExceptionDepthComparator(exceptionType));
      }
      return this.mappedMethods.get(matches.get(0));
    }
    else {
      return NO_MATCHING_EXCEPTION_HANDLER_METHOD;
    }
  }

  /**
   * For the {@link #NO_MATCHING_EXCEPTION_HANDLER_METHOD} constant.
   */
  @SuppressWarnings("unused")
  private void noMatchingExceptionHandler() { }

}
