/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.bind.resolver;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

import infra.core.ArraySizeTrimmer;
import infra.core.style.ToStringBuilder;
import infra.util.CollectionUtils;
import infra.web.RequestContext;
import infra.web.handler.method.ResolvableMethodParameter;

/**
 * A composite strategy class that manages a list of {@link ParameterResolvingStrategy}
 * instances. This class implements the {@link Iterable} interface, allowing it to
 * iterate over its strategies, and also acts as a {@link ParameterResolvingStrategy}
 * itself by delegating calls to the underlying strategies.
 *
 * <p>This class provides methods to add, remove, replace, and query strategies,
 * as well as methods to manage the internal list of strategies efficiently.
 * It also supports trimming the internal list capacity to its size via the
 * {@link ArraySizeTrimmer} interface.</p>
 *
 * <h3>Usage Examples</h3>
 *
 * Adding strategies:
 * <pre>{@code
 * ParameterResolvingStrategies strategies = new ParameterResolvingStrategies();
 * strategies.add(new CustomParameterResolvingStrategy());
 * strategies.add(new AnotherParameterResolvingStrategy());
 * }</pre>
 *
 * Resolving arguments using the first supporting strategy:
 * <pre>{@code
 * RequestContext context = ...;
 * ResolvableMethodParameter parameter = ...;
 * Object result = strategies.resolveArgument(context, parameter);
 * }</pre>
 *
 * Checking if a specific strategy is present:
 * <pre>{@code
 * boolean containsCustomStrategy = strategies.contains(CustomParameterResolvingStrategy.class);
 * }</pre>
 *
 * Replacing an existing strategy:
 * <pre>{@code
 * boolean replaced = strategies.replace(CustomParameterResolvingStrategy.class, new UpdatedStrategy());
 * }</pre>
 *
 * Iterating over all strategies:
 * <pre>{@code
 * for (ParameterResolvingStrategy strategy : strategies) {
 *   System.out.println(strategy);
 * }
 * }</pre>
 *
 * Trimming the internal list to optimize memory usage:
 * <pre>{@code
 * strategies.trimToSize();
 * }</pre>
 *
 * <h3>Implementation Details</h3>
 *
 * <p>This class delegates the {@link #supportsParameter(ResolvableMethodParameter)} and
 * {@link #resolveArgument(RequestContext, ResolvableMethodParameter)} methods to the
 * first strategy in the list that supports the given parameter. If no strategy supports
 * the parameter, the methods return {@code false} or {@code null}, respectively.</p>
 *
 * <p>The internal list of strategies can be modified using methods like {@link #add},
 * {@link #set}, and {@link #removeIf}. These methods ensure efficient management of
 * the strategies while maintaining the integrity of the list.</p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ParameterResolvingStrategy
 * @see ArraySizeTrimmer
 * @since 4.0 2021/9/26 21:07
 */
public class ParameterResolvingStrategies implements ArraySizeTrimmer, Iterable<ParameterResolvingStrategy>, ParameterResolvingStrategy {

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
  public void add(ParameterResolvingStrategy @Nullable ... resolver) {
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
  @SuppressWarnings("unchecked")
  public <T> T get(Class<T> strategyClass) {
    int idx = indexOf(strategyClass);
    if (idx != -1) {
      return (T) strategies.get(idx);
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
    return ToStringBuilder.forInstance(this)
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
