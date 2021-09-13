/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.Assert;
import cn.taketoday.core.Constant;
import cn.taketoday.core.DecoratingProxy;
import cn.taketoday.core.Nullable;
import cn.taketoday.core.Order;
import cn.taketoday.core.Ordered;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ConcurrentReferenceHashMap;

/**
 * General utility for determining the order of an object based on its type declaration.
 * Handles {@link Order} annotation as well as {@link javax.annotation.Priority}.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author TODAY 2018-11-08 19:02
 */
public abstract class OrderUtils {

  /** Cache marker for a non-annotated Class. */
  private static final Object NOT_ANNOTATED = new Object();

  private static final Class<? extends Annotation>
          JAVAX_PRIORITY_ANNOTATION = ClassUtils.loadClass("javax.annotation.Priority");

  /** Cache for @Order value (or NOT_ANNOTATED marker) per Class. */
  private static final ConcurrentReferenceHashMap<AnnotatedElement, Object>
          orderCache = new ConcurrentReferenceHashMap<>(64);

  /**
   * Get the order of the {@link AnnotatedElement}
   *
   * @param annotated
   *         {@link AnnotatedElement}
   *
   * @return The order
   */
  public static int getOrderOrLowest(final AnnotatedElement annotated) {
    return getOrder(annotated, Ordered.LOWEST_PRECEDENCE);
  }

  /**
   * Get the order of the object
   *
   * @param obj
   *         object
   *
   * @return The order
   */
  public static int getOrderOrLowest(final Object obj) {
    if (obj instanceof Ordered) {
      return ((Ordered) obj).getOrder();
    }
    if (obj instanceof AnnotatedElement) {
      return getOrderOrLowest((AnnotatedElement) obj);
    }

    if (obj instanceof DecoratingProxy) {
      return getOrderOrLowest(((DecoratingProxy) obj).getDecoratedClass());
    }
    return getOrderOrLowest(ClassUtils.getUserClass(obj));
  }

  //

  /**
   * Return the order on the specified {@code type}, or the specified
   * default value if none can be found.
   * <p>Takes care of {@link Order @Order} and {@code @javax.annotation.Priority}.
   *
   * @param element
   *         the annotated element (e.g. type or method)
   *
   * @return the priority value, or the specified default order if none can be found
   *
   * @see #getPriority(AnnotatedElement)
   */
  public static int getOrder(AnnotatedElement element, int defaultOrder) {
    Integer order = getOrder(element);
    return (order != null ? order : defaultOrder);
  }

  /**
   * Return the order on the specified {@code type}, or the specified
   * default value if none can be found.
   * <p>Takes care of {@link Order @Order} and {@code @javax.annotation.Priority}.
   *
   * @param element
   *         the annotated element (e.g. type or method)
   *
   * @return the priority value, or the specified default order if none can be found
   *
   * @see #getPriority(AnnotatedElement)
   */
  @Nullable
  public static Integer getOrder(AnnotatedElement element, @Nullable Integer defaultOrder) {
    Integer order = getOrder(element);
    return (order != null ? order : defaultOrder);
  }

  /**
   * Return the order declared on the specified {@code element}.
   * <p>Takes care of {@link Order @Order} and {@code @javax.annotation.Priority}.
   *
   * @param element
   *         the annotated element (e.g. type or method)
   *
   * @return the order value, or {@code null} if none can be found
   */
  @Nullable
  public static Integer getOrder(AnnotatedElement element) {
    Assert.notNull(element, "AnnotatedElement must not be null");
    Object cached = orderCache.get(element);
    if (cached != null) {
      return (cached instanceof Integer ? (Integer) cached : null);
    }
    Integer result;
    AnnotationAttributes attributes = AnnotationUtils.getAttributes(Order.class, element);
    if (attributes != null) {
      result = attributes.getNumber(Constant.VALUE);
    }
    else {
      result = getPriority(element);
    }
    orderCache.put(element, result != null ? result : NOT_ANNOTATED);
    return result;
  }

  /**
   * Return the value of the {@code javax.annotation.Priority} annotation
   * declared on the specified type, or {@code null} if none.
   *
   * @param element
   *         the annotated element (e.g. type or method)
   *
   * @return the priority value if the annotation is declared, or {@code null} if none
   */
  @Nullable
  public static Integer getPriority(AnnotatedElement element) {
    AnnotationAttributes attributes = AnnotationUtils.getAttributes(JAVAX_PRIORITY_ANNOTATION, element);
    if (attributes != null) {
      return attributes.getNumber(Constant.VALUE);
    }
    return null;
  }

}
