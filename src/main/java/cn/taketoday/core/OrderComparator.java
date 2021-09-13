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

package cn.taketoday.core;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import cn.taketoday.util.ObjectUtils;

/**
 * {@link Comparator} implementation for {@link Ordered} objects, sorting
 * by order value ascending, respectively by priority descending.
 *
 * <h3>{@code PriorityOrdered} Objects</h3>
 * <p>{@link PriorityOrdered} objects will be sorted with higher priority than
 * <em>plain</em> {@code Ordered} objects.
 *
 * <h3>Same Order Objects</h3>
 * <p>Objects that have the same order value will be sorted with arbitrary
 * ordering with respect to other objects with the same order value.
 *
 * <h3>Non-ordered Objects</h3>
 * <p>Any object that does not provide its own order value is implicitly
 * assigned a value of {@link Ordered#LOWEST_PRECEDENCE}, thus ending up
 * at the end of a sorted collection in arbitrary order with respect to
 * other objects with the same order value.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author TODAY 2021/9/12 11:32
 * @see Ordered
 * @see PriorityOrdered
 * @see java.util.List#sort(java.util.Comparator)
 * @see java.util.Arrays#sort(Object[], java.util.Comparator)
 * @since 4.0
 */
public class OrderComparator implements Comparator<Object> {

  /**
   * Shared default instance of {@code OrderComparator}.
   */
  public static final OrderComparator INSTANCE = new OrderComparator();

  /**
   * Build an adapted order comparator with the given source provider.
   *
   * @param sourceProvider
   *         the order source provider to use
   *
   * @return the adapted comparator
   */
  public <T> Comparator<T> withSourceProvider(OrderSourceProvider sourceProvider) {
    return (o1, o2) -> doCompare(o1, o2, sourceProvider);
  }

  @Override
  public int compare(@Nullable Object o1, @Nullable Object o2) {
    return doCompare(o1, o2, null);
  }

  private int doCompare(
          @Nullable Object o1, @Nullable Object o2, @Nullable OrderSourceProvider sourceProvider) {
    boolean p1 = (o1 instanceof PriorityOrdered);
    boolean p2 = (o2 instanceof PriorityOrdered);
    if (p1 && !p2) {
      return -1;
    }
    else if (p2 && !p1) {
      return 1;
    }

    int i1 = getOrder(o1, sourceProvider);
    int i2 = getOrder(o2, sourceProvider);
    return Integer.compare(i1, i2);
  }

  /**
   * Determine the order value for the given object.
   * <p>The default implementation checks against the given {@link OrderSourceProvider}
   * using {@link #findOrder} and falls back to a regular {@link #getOrder(Object)} call.
   *
   * @param obj
   *         the object to check
   *
   * @return the order value, or {@code Ordered.LOWEST_PRECEDENCE} as fallback
   */
  private int getOrder(@Nullable Object obj, @Nullable OrderSourceProvider sourceProvider) {
    Integer order = null;
    if (obj != null && sourceProvider != null) {
      Object orderSource = sourceProvider.getOrderSource(obj);
      if (orderSource != null) {
        if (orderSource.getClass().isArray()) {
          for (Object source : ObjectUtils.toObjectArray(orderSource)) {
            order = findOrder(source);
            if (order != null) {
              break;
            }
          }
        }
        else {
          order = findOrder(orderSource);
        }
      }
    }
    return (order != null ? order : getOrder(obj));
  }

  /**
   * Determine the order value for the given object.
   * <p>The default implementation checks against the {@link Ordered} interface
   * through delegating to {@link #findOrder}. Can be overridden in subclasses.
   *
   * @param obj
   *         the object to check
   *
   * @return the order value, or {@code Ordered.LOWEST_PRECEDENCE} as fallback
   */
  protected int getOrder(@Nullable Object obj) {
    if (obj != null) {
      Integer order = findOrder(obj);
      if (order != null) {
        return order;
      }
    }
    return Ordered.LOWEST_PRECEDENCE;
  }

  /**
   * Find an order value indicated by the given object.
   * <p>The default implementation checks against the {@link Ordered} interface.
   * Can be overridden in subclasses.
   *
   * @param obj
   *         the object to check
   *
   * @return the order value, or {@code null} if none found
   */
  @Nullable
  protected Integer findOrder(Object obj) {
    return (obj instanceof Ordered ? ((Ordered) obj).getOrder() : null);
  }

  /**
   * Determine a priority value for the given object, if any.
   * <p>The default implementation always returns {@code null}.
   * Subclasses may override this to give specific kinds of values a
   * 'priority' characteristic, in addition to their 'order' semantics.
   * A priority indicates that it may be used for selecting one object over
   * another, in addition to serving for ordering purposes in a list/array.
   *
   * @param obj
   *         the object to check
   *
   * @return the priority value, or {@code null} if none
   */
  @Nullable
  public Integer getPriority(Object obj) {
    return null;
  }

  /**
   * Sort the given List with a default OrderComparator.
   * <p>Optimized to skip sorting for lists with size 0 or 1,
   * in order to avoid unnecessary array extraction.
   *
   * @param list
   *         the List to sort
   *
   * @see java.util.List#sort(java.util.Comparator)
   */
  public static void sort(List<?> list) {
    if (list.size() > 1) {
      list.sort(INSTANCE);
    }
  }

  /**
   * Sort the given array with a default OrderComparator.
   * <p>Optimized to skip sorting for lists with size 0 or 1,
   * in order to avoid unnecessary array extraction.
   *
   * @param array
   *         the array to sort
   *
   * @see java.util.Arrays#sort(Object[], java.util.Comparator)
   */
  public static void sort(Object[] array) {
    if (array.length > 1) {
      Arrays.sort(array, INSTANCE);
    }
  }

  /**
   * Sort the given array or List with a default OrderComparator,
   * if necessary. Simply skips sorting when given any other value.
   * <p>Optimized to skip sorting for lists with size 0 or 1,
   * in order to avoid unnecessary array extraction.
   *
   * @param value
   *         the array or List to sort
   *
   * @see java.util.Arrays#sort(Object[], java.util.Comparator)
   */
  public static void sortIfNecessary(Object value) {
    if (value instanceof Object[]) {
      sort((Object[]) value);
    }
    else if (value instanceof List) {
      sort((List<?>) value);
    }
  }

}
