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

package cn.taketoday.web.bind.resolver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

import cn.taketoday.core.ArraySizeTrimmer;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * Composite ParameterResolvingStrategy
 *
 * @author TODAY 2021/9/26 21:07
 * @since 4.0
 */
public class ParameterResolvingStrategies
        implements ArraySizeTrimmer, Iterable<ParameterResolvingStrategy>, ParameterResolvingStrategy {
  private final ArrayList<ParameterResolvingStrategy> strategies;

  public ParameterResolvingStrategies() {
    this(new ArrayList<>());
  }

  public ParameterResolvingStrategies(int size) {
    this(new ArrayList<>(size));
  }

  ParameterResolvingStrategies(ArrayList<ParameterResolvingStrategy> strategies) {
    this.strategies = strategies;
  }

  //---------------------------------------------------------------------
  // Implementation of ParameterResolvingStrategy interface
  //---------------------------------------------------------------------

  @Override
  public boolean supportsParameter(ResolvableMethodParameter parameter) {
    for (ParameterResolvingStrategy strategy : strategies) {
      if (strategy.supportsParameter(parameter)) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    for (ParameterResolvingStrategy strategy : strategies) {
      if (strategy.supportsParameter(resolvable)) {
        return strategy.resolveArgument(context, resolvable);
      }
    }
    return null;
  }

  public void add(@Nullable ParameterResolvingStrategy resolver) {
    if (resolver != null) {
      strategies.add(resolver);
    }
  }

  /**
   * add resolvers or resolving-strategies
   *
   * @param resolver resolvers or resolving-strategies
   */
  public void add(@Nullable ParameterResolvingStrategy... resolver) {
    CollectionUtils.addAll(strategies, resolver);
  }

  /**
   * add resolvers or resolving-strategies
   *
   * @param resolvers resolvers or resolving-strategies
   */
  public void add(@Nullable List<ParameterResolvingStrategy> resolvers) {
    if (CollectionUtils.isNotEmpty(resolvers)) {
      this.strategies.addAll(resolvers);
      trimToSize();
    }
  }

  /**
   * set or clear resolvers
   *
   * @param resolver can be null
   */
  public void set(@Nullable List<ParameterResolvingStrategy> resolver) {
    strategies.clear();
    if (CollectionUtils.isNotEmpty(resolver)) {
      strategies.addAll(resolver);
      trimToSize();
    }
  }

  /**
   * Returns the instance of the first occurrence of the specified strategy-class
   *
   * @param strategyClass strategy-class to search for
   * @return the instance of the first occurrence of the specified strategy-class
   */
  @Nullable
  public ParameterResolvingStrategy get(Class<?> strategyClass) {
    int idx = indexOf(strategyClass);
    if (idx != -1) {
      return strategies.get(idx);
    }
    return null;
  }

  /**
   * Returns the index of the first occurrence of the specified strategy-class
   * in this list, or -1 if this list does not contain the strategy-class.
   * More formally, returns the lowest index {@code i} such that
   * -1 if there is no such index.
   *
   * @param strategyClass strategy-class to search for
   * @return the index of the first occurrence of the specified strategy-class in
   * this list, or -1 if this list does not contain the strategy-class
   */
  public int indexOf(Class<?> strategyClass) {
    int idx = 0;
    for (ParameterResolvingStrategy resolver : strategies) {
      if (strategyClass == resolver.getClass()) {
        return idx;
      }
      idx++;
    }
    return -1;
  }

  /**
   * Returns the index of the last occurrence of the specified strategy-class
   * in this list, or -1 if this list does not contain the strategy-class.
   * More formally, returns the highest index {@code i} such that -1 if
   * there is no such index.
   *
   * @param strategyClass strategy-class to search for
   * @return the index of the last occurrence of the specified strategy-class in
   * this list, or -1 if this list does not contain the strategy-class
   */
  public int lastIndexOf(Class<?> strategyClass) {
    int idx = strategies.size() - 1;
    for (ParameterResolvingStrategy resolver : strategies) {
      if (strategyClass == resolver.getClass()) {
        return idx;
      }
      idx--;
    }
    return -1;
  }

  /**
   * Replaces the element at the specified position in this list with
   * the specified element.
   *
   * @param idx index of the element to replace
   * @param strategy strategy to be stored at the specified position
   * @return the element previously at the specified position
   * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index >= size())}
   */
  public ParameterResolvingStrategy set(int idx, ParameterResolvingStrategy strategy) {
    return strategies.set(idx, strategy);
  }

  /**
   * Replaces the element at the specified position in this list with
   * the specified strategy-class.
   *
   * @param strategyClass strategy-class to search for
   * @param strategy new strategy
   * @return if replaced
   */
  public boolean replace(Class<?> strategyClass, ParameterResolvingStrategy strategy) {
    int idx = indexOf(strategyClass);
    if (idx != -1) {
      strategies.set(idx, strategy);
      return true;
    }
    return false;
  }

  /**
   * Removes all the elements of this collection that satisfy the given
   * predicate.  Errors or runtime exceptions thrown during iteration or by
   * the predicate are relayed to the caller.
   *
   * <p>
   * The default implementation traverses all elements of the collection using
   * its {@link List#iterator()}.  Each matching element is removed using
   * {@link Iterator#remove()}.  If the collection's iterator does not
   * support removal then an {@code UnsupportedOperationException} will be
   * thrown on the first matching element.
   *
   * @param filter a predicate which returns {@code true} for elements to be
   * removed
   * @return {@code true} if any elements were removed
   * @throws NullPointerException if the specified filter is null
   * @throws UnsupportedOperationException if elements cannot be removed
   * from this collection.  Implementations may throw this exception if a
   * matching element cannot be removed or if, in general, removal is not
   * supported.
   */
  public boolean removeIf(Predicate<ParameterResolvingStrategy> filter) {
    return strategies.removeIf(filter);
  }

  /**
   * Returns <tt>true</tt> if resolvers list contains the specified {@code resolverClass}.
   * More formally, returns <tt>true</tt> if and only if all resolvers contains
   * at least one element <tt>e</tt> such that
   * <tt>(resolverClass == resolver.getClass())</tt>.
   *
   * @param strategyClass element whose presence in this defaultResolvers or customizedResolvers is to be tested
   * @return <tt>true</tt> if resolvers contains the specified {@code resolverClass}
   */
  public boolean contains(Class<?> strategyClass) {
    return indexOf(strategyClass) != -1;
  }

  /**
   * @since 4.0
   */
  @Override
  public void trimToSize() {
    strategies.trimToSize();
  }

  public ArrayList<ParameterResolvingStrategy> getStrategies() {
    return strategies;
  }

  @Override
  public Iterator<ParameterResolvingStrategy> iterator() {
    return strategies.iterator();
  }

  @Override
  public void forEach(Consumer<? super ParameterResolvingStrategy> action) {
    strategies.forEach(action);
  }

  @Override
  public Spliterator<ParameterResolvingStrategy> spliterator() {
    return strategies.spliterator();
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("strategies", strategies.size())
            .toString();
  }

  /**
   * Returns the number of strategies in this list.
   *
   * @return the number of strategies in this list
   */
  public int size() {
    return strategies.size();
  }

  /**
   * Returns {@code true} if this strategies list contains no strategy.
   *
   * @return {@code true} if this strategies list contains no strategy
   */
  public boolean isEmpty() {
    return strategies.isEmpty();
  }

}
