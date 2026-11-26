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

package infra.web.handler.method;

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
import infra.web.ServerHttpExchange;

/**
 * A composite strategy that manages an ordered list of
 * {@link ParameterResolvingStrategy} instances. This class implements
 * {@link Iterable} so that its strategies can be iterated over, and also acts as
 * a {@link ParameterResolvingStrategy} by delegating argument resolution to the
 * first matching strategy.
 *
 * <p>This class provides methods to add, remove, replace, and query strategies,
 * as well as methods to manage the internal list efficiently. It also supports
 * trimming the internal list capacity to its current size via the
 * {@link ArraySizeTrimmer} interface.
 *
 * <h3>Usage examples</h3>
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
 * ServerHttpExchange exchange = ...;
 * HandlerParameter parameter = ...;
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
 * <h3>Implementation details</h3>
 *
 * <p>This class delegates the {@link #supportsParameter(HandlerParameter)} and
 * {@link #resolveArgument(ServerHttpExchange, HandlerParameter)} methods to the
 * first strategy in the list that supports the given parameter. If no strategy supports
 * the parameter, the methods return {@code false} or {@code null}, respectively.
 *
 * <p>The internal list of strategies can be modified using methods such as
 * {@link #add(ParameterResolvingStrategy...)}, {@link #set(List)}, and
 * {@link #removeIf(Predicate)}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ParameterResolvingStrategy
 * @see ArraySizeTrimmer
 * @since 4.0 2021/9/26 21:07
 */
public class ParameterResolvingStrategies implements ArraySizeTrimmer, Iterable<ParameterResolvingStrategy>, ParameterResolvingStrategy {

  private final ArrayList<ParameterResolvingStrategy> strategies;

  /**
   * Create an empty collection of parameter resolving strategies.
   */
  public ParameterResolvingStrategies() {
    this(new ArrayList<>());
  }

  /**
   * Create an empty collection with the specified initial capacity.
   *
   * @param size the initial capacity of the collection
   * @throws IllegalArgumentException if {@code size} is negative
   */
  public ParameterResolvingStrategies(int size) {
    this(new ArrayList<>(size));
  }

  ParameterResolvingStrategies(ArrayList<ParameterResolvingStrategy> strategies) {
    this.strategies = strategies;
  }

  //---------------------------------------------------------------------
  // Implementation of ParameterResolvingStrategy interface
  //---------------------------------------------------------------------

  /**
   * Determine whether any registered strategy supports the given parameter.
   *
   * @param parameter the parameter to inspect
   * @return {@code true} if at least one registered strategy supports the
   * parameter, {@code false} otherwise
   */
  @Override
  public boolean supportsParameter(HandlerParameter parameter) {
    for (ParameterResolvingStrategy strategy : strategies) {
      if (strategy.supportsParameter(parameter)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Resolve the given parameter using the first registered strategy that
   * supports it.
   *
   * @param exchange the current HTTP exchange
   * @param parameter the parameter to resolve
   * @return the resolved argument value, or {@code null} if no registered
   * strategy supports the parameter or the selected strategy returns
   * {@code null}
   * @throws Exception if the selected strategy cannot resolve the argument
   */
  @Override
  public @Nullable Object resolveArgument(ServerHttpExchange exchange, HandlerParameter parameter) throws Exception {
    for (ParameterResolvingStrategy strategy : strategies) {
      if (strategy.supportsParameter(parameter)) {
        return strategy.resolveArgument(exchange, parameter);
      }
    }
    return null;
  }

  /**
   * Add a strategy to the end of this collection.
   *
   * <p>A {@code null} strategy is ignored.
   *
   * @param strategy the strategy to add, or {@code null}
   */
  public void add(@Nullable ParameterResolvingStrategy strategy) {
    if (strategy != null) {
      strategies.add(strategy);
    }
  }

  /**
   * Add the specified strategies to the end of this collection.
   *
   * @param strategies the strategies to add
   */
  public void add(ParameterResolvingStrategy @Nullable ... strategies) {
    CollectionUtils.addAll(this.strategies, strategies);
  }

  /**
   * Add all strategies in the specified list to the end of this collection.
   *
   * @param strategies the strategies to add, or {@code null}
   */
  public void add(@Nullable List<ParameterResolvingStrategy> strategies) {
    if (CollectionUtils.isNotEmpty(strategies)) {
      this.strategies.addAll(strategies);
      trimToSize();
    }
  }

  /**
   * Replace the current strategies with the strategies in the specified list.
   * Passing {@code null} or an empty list clears this collection.
   *
   * @param strategies the replacement strategies, or {@code null}
   */
  public void set(@Nullable List<ParameterResolvingStrategy> strategies) {
    this.strategies.clear();
    if (CollectionUtils.isNotEmpty(strategies)) {
      this.strategies.addAll(strategies);
      trimToSize();
    }
  }

  /**
   * Return the first strategy whose concrete class exactly matches the specified
   * class.
   *
   * @param strategyClass the concrete strategy class to search for
   * @param <T> the strategy type
   * @return the first matching strategy, or {@code null} if no strategy with the
   * specified concrete class is present
   */
  @SuppressWarnings("unchecked")
  public <T> @Nullable T get(Class<T> strategyClass) {
    int idx = indexOf(strategyClass);
    if (idx != -1) {
      return (T) strategies.get(idx);
    }
    return null;
  }

  /**
   * Return the index of the first strategy whose concrete class exactly matches
   * the specified class, or {@code -1} if no such strategy is present.
   *
   * @param strategyClass the concrete strategy class to search for
   * @return the index of the first matching strategy, or {@code -1}
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
   * Return the index of the last strategy whose concrete class exactly matches
   * the specified class, or {@code -1} if no such strategy is present.
   *
   * @param strategyClass the concrete strategy class to search for
   * @return the index of the last matching strategy, or {@code -1}
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
   * Replace the strategy at the specified position.
   *
   * @param idx the index of the strategy to replace
   * @param strategy the replacement strategy
   * @return the strategy previously at the specified position
   * @throws IndexOutOfBoundsException if the index is out of range
   * ({@code idx < 0 || idx >= size()})
   */
  public ParameterResolvingStrategy set(int idx, ParameterResolvingStrategy strategy) {
    return strategies.set(idx, strategy);
  }

  /**
   * Replace the first strategy whose concrete class exactly matches the
   * specified class.
   *
   * @param strategyClass the concrete strategy class to search for
   * @param strategy the replacement strategy
   * @return {@code true} if a matching strategy was replaced, {@code false} if
   * no matching strategy was found
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
   * Remove all strategies that satisfy the given predicate.
   *
   * @param filter a predicate that returns {@code true} for strategies to be
   * removed
   * @return {@code true} if at least one strategy was removed
   * @throws NullPointerException if the specified filter is null
   */
  public boolean removeIf(Predicate<ParameterResolvingStrategy> filter) {
    return strategies.removeIf(filter);
  }

  /**
   * Determine whether this collection contains a strategy whose concrete class
   * exactly matches the specified class.
   *
   * @param strategyClass the concrete strategy class to search for
   * @return {@code true} if a matching strategy is present, {@code false}
   * otherwise
   */
  public boolean contains(Class<?> strategyClass) {
    return indexOf(strategyClass) != -1;
  }

  /**
   * Trim the internal storage to the current number of strategies.
   *
   * @since 4.0
   */
  @Override
  public void trimToSize() {
    strategies.trimToSize();
  }

  /**
   * Return the internal mutable list of strategies.
   *
   * <p>Changes made to the returned list are reflected in this collection.
   *
   * @return the internal list of strategies
   */
  public ArrayList<ParameterResolvingStrategy> getStrategies() {
    return strategies;
  }

  /**
   * Return an iterator over the registered strategies in list order.
   *
   * @return an iterator over the registered strategies
   */
  @Override
  public Iterator<ParameterResolvingStrategy> iterator() {
    return strategies.iterator();
  }

  /**
   * Perform the given action for each registered strategy in list order.
   *
   * @param action the action to perform for each strategy
   */
  @Override
  public void forEach(Consumer<? super ParameterResolvingStrategy> action) {
    strategies.forEach(action);
  }

  /**
   * Return a late-binding spliterator over the registered strategies.
   *
   * @return a spliterator over the registered strategies
   */
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
   * Return the number of registered strategies.
   *
   * @return the number of registered strategies
   */
  public int size() {
    return strategies.size();
  }

  /**
   * Determine whether no strategies are registered.
   *
   * @return {@code true} if no strategies are registered, otherwise
   * {@code false}
   */
  public boolean isEmpty() {
    return strategies.isEmpty();
  }

}
