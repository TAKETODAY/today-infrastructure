/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * @author TODAY 2021/10/2 20:44
 * @since 4.0
 */
public class CompositeStrategies<T> implements ArraySizeTrimmer, Iterable<T> {
  private final ArrayList<T> strategies;

  public CompositeStrategies() {
    this(new ArrayList<>());
  }

  public CompositeStrategies(int size) {
    this(new ArrayList<>(size));
  }

  CompositeStrategies(ArrayList<T> strategies) {
    this.strategies = strategies;
  }

  public void add(T resolver) {
    strategies.add(resolver);
  }

  /**
   * add resolvers or resolving-strategies
   *
   * @param resolver resolvers or resolving-strategies
   */
  @SafeVarargs
  public final void add(T... resolver) {
    Collections.addAll(strategies, resolver);
  }

  /**
   * add resolvers or resolving-strategies
   *
   * @param resolvers resolvers or resolving-strategies
   */
  public void add(List<T> resolvers) {
    this.strategies.addAll(resolvers);
    trimToSize();
  }

  /**
   * set or clear resolvers
   *
   * @param resolver can be null
   */
  public void set(@Nullable List<T> resolver) {
    strategies.clear();
    if (CollectionUtils.isNotEmpty(resolver)) {
      strategies.addAll(resolver);
      trimToSize();
    }
  }

  /**
   * Removes all the elements of this collection that satisfy the given
   * predicate.  Errors or runtime exceptions thrown during iteration or by
   * the predicate are relayed to the caller.
   *
   * @param filter a predicate which returns {@code true} for elements to be
   * removed
   * @return {@code true} if any elements were removed
   * @throws NullPointerException if the specified filter is null
   * @throws UnsupportedOperationException if elements cannot be removed
   * from this collection.  Implementations may throw this exception if a
   * matching element cannot be removed or if, in general, removal is not
   * supported.
   * @implSpec The default implementation traverses all elements of the collection using
   * its {@link List#iterator()}.  Each matching element is removed using
   * {@link Iterator#remove()}.  If the collection's iterator does not
   * support removal then an {@code UnsupportedOperationException} will be
   * thrown on the first matching element.
   */
  public boolean removeIf(Predicate<T> filter) {
    return strategies.removeIf(filter);
  }

  /**
   * Returns <tt>true</tt> if resolvers list contains the specified {@code resolverClass}.
   * More formally, returns <tt>true</tt> if and only if all resolvers contains
   * at least one element <tt>e</tt> such that
   * <tt>(resolverClass == resolver.getClass())</tt>.
   *
   * @param strategy element whose presence in this strategies
   * @return <tt>true</tt> if resolvers contains the specified {@code resolverClass}
   */
  public boolean contains(Class<?> strategy) {
    for (final T resolver : strategies) {
      if (strategy == resolver.getClass()) {
        return true;
      }
    }
    return false;
  }

  public ArrayList<T> getStrategies() {
    return strategies;
  }

  @Override
  public void trimToSize() {
    strategies.trimToSize();
  }

  @Override
  public Iterator<T> iterator() {
    return strategies.iterator();
  }

  @Override
  public void forEach(Consumer<? super T> action) {
    strategies.forEach(action);
  }

  @Override
  public Spliterator<T> spliterator() {
    return strategies.spliterator();
  }
}
