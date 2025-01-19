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

package infra.core.annotation;

import java.lang.reflect.AnnotatedElement;

import infra.core.annotation.MergedAnnotations.SearchStrategy;
import infra.lang.NullValue;
import infra.lang.Nullable;
import infra.util.ConcurrentReferenceHashMap;

/**
 * General utility for determining the order of an object based on its type declaration.
 * Handles {@link Order} annotation as well as {@link jakarta.annotation.Priority}.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-11-08 19:02
 */
public abstract class OrderUtils {

  private static final String PRIORITY_ANNOTATION = "jakarta.annotation.Priority";

  /** Cache for @Order value (or NOT_ANNOTATED marker) per Class. */
  static final ConcurrentReferenceHashMap<AnnotatedElement, Object>
          orderCache = new ConcurrentReferenceHashMap<>(64);

  /**
   * Return the order on the specified {@code type}, or the specified
   * default value if none can be found.
   * <p>Takes care of {@link Order @Order} and {@code @jakarta.annotation.Priority}.
   *
   * @param type the type to handle
   * @return the priority value, or the specified default order if none can be found
   * @see #getPriority(Class)
   */
  public static int getOrder(Class<?> type, int defaultOrder) {
    Integer order = getOrder(type);
    return order != null ? order : defaultOrder;
  }

  /**
   * Return the order on the specified {@code type}, or the specified
   * default value if none can be found.
   * <p>Takes care of {@link Order @Order} and {@code @jakarta.annotation.Priority}.
   *
   * @param type the type to handle
   * @return the priority value, or the specified default order if none can be found
   * @see #getPriority(Class)
   */
  @Nullable
  public static Integer getOrder(Class<?> type, @Nullable Integer defaultOrder) {
    Integer order = getOrder(type);
    return order != null ? order : defaultOrder;
  }

  /**
   * Return the order on the specified {@code type}.
   * <p>Takes care of {@link Order @Order} and {@code @jakarta.annotation.Priority}.
   *
   * @param type the type to handle
   * @return the order value, or {@code null} if none can be found
   * @see #getPriority(Class)
   */
  @Nullable
  public static Integer getOrder(Class<?> type) {
    return getOrder((AnnotatedElement) type);
  }

  /**
   * Return the order declared on the specified {@code element}.
   * <p>Takes care of {@link Order @Order} and {@code @jakarta.annotation.Priority}.
   *
   * @param element the annotated element (e.g. type or method)
   * @return the order value, or {@code null} if none can be found
   */
  @Nullable
  public static Integer getOrder(AnnotatedElement element) {
    return getOrderFromAnnotations(element, MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY));
  }

  /**
   * Return the order from the specified annotation collection.
   * <p>Takes care of {@link Order @Order} and
   * {@code @jakarta.annotation.Priority}.
   *
   * @param element the source element
   * @param annotations the annotation to consider
   * @return the order value, or {@code null} if none can be found
   */
  @Nullable
  static Integer getOrderFromAnnotations(AnnotatedElement element, MergedAnnotations annotations) {
    if (!(element instanceof Class)) {
      return findOrder(annotations);
    }
    Object cached = orderCache.get(element);
    if (cached != null) {
      return cached instanceof Integer ? (Integer) cached : null;
    }
    Integer result = findOrder(annotations);
    orderCache.put(element, result != null ? result : NullValue.INSTANCE);
    return result;
  }

  @Nullable
  private static Integer findOrder(MergedAnnotations annotations) {
    MergedAnnotation<Order> orderAnnotation = annotations.get(Order.class);
    if (orderAnnotation.isPresent()) {
      return orderAnnotation.getIntValue();
    }
    MergedAnnotation<?> priorityAnnotation = annotations.get(PRIORITY_ANNOTATION);
    if (priorityAnnotation.isPresent()) {
      return priorityAnnotation.getIntValue();
    }
    return null;
  }

  /**
   * Return the value of the {@code jakarta.annotation.Priority} annotation
   * declared on the specified type, or {@code null} if none.
   *
   * @param type the type to handle
   * @return the priority value if the annotation is declared, or {@code null} if none
   */
  @Nullable
  public static Integer getPriority(Class<?> type) {
    return MergedAnnotations.from(type, SearchStrategy.TYPE_HIERARCHY)
            .get(PRIORITY_ANNOTATION)
            .getValue(Integer.class);
  }

}
