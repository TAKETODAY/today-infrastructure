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

package infra.persistence;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import infra.core.Pair;

/**
 * Fluent query builder for entity retrieval, returned by {@link EntityManager#search(Class)}.
 * <p>
 * Chained options configure filtering and sorting; a terminal operation
 * executes the query and returns results. After a terminal operation,
 * resources are released automatically.
 *
 * <pre>{@code
 * // all users
 * List<User> users = em.search(User.class).list();
 *
 * // by example
 * User user = em.search(User.class)
 *         .example(exampleUser)
 *         .first();
 *
 * // conditions + pagination + sort
 * Page<User> page = em.search(User.class)
 *         .where(q -> q.eq("status", "active"))
 *         .sortBy("createdAt", Order.DESC)
 *         .page(Pageable.of(1, 20));
 *
 * // iterate
 * try (var it = em.search(User.class).iterate()) { ... }
 *
 * // count
 * long n = em.search(User.class).where(condition).count();
 *
 * // to map
 * Map<Long, User> map = em.search(User.class).mapBy(User::getId);
 * }</pre>
 *
 * @param <T> the entity type
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public interface EntitySearch<T> extends AutoCloseable {

  /**
   * Filter results by the given example object. Non-null properties
   * are used as equality conditions.
   * <p>Mutually exclusive with {@link #where(QueryStatement)}
   * and {@link #where(ConditionStatement)}.
   */
  EntitySearch<T> example(Object example);

  /**
   * Apply a custom {@link QueryStatement} for full SQL-level control.
   * <p>Mutually exclusive with {@link #example(Object)}
   * and {@link #where(ConditionStatement)}.
   */
  EntitySearch<T> where(QueryStatement handler);

  /**
   * Apply a custom {@link ConditionStatement} for WHERE-clause-level control.
   * <p>Mutually exclusive with {@link #example(Object)}
   * and {@link #where(QueryStatement)}.
   */
  EntitySearch<T> where(ConditionStatement handler);

  /**
   * Sort by a single property.
   */
  EntitySearch<T> sortBy(String property, Order order);

  /**
   * Sort by multiple properties.
   */
  EntitySearch<T> sortBy(Map<String, Order> sortKeys);

  /**
   * Sort by one or more property-order pairs.
   */
  @SuppressWarnings("unchecked")
  EntitySearch<T> sortBy(Pair<String, Order>... sortKeys);

  // --- terminal operations ---

  /**
   * Execute and return all matching entities as a list.
   */
  List<T> list();

  /**
   * Execute and return a paginated result.
   */
  Page<T> page(Pageable pageable);

  /**
   * Return the first matching entity, or {@code null} if none found.
   */
  @Nullable T first();

  /**
   * Return the unique matching entity, or {@code null} if none found.
   *
   * @throws infra.dao.IncorrectResultSizeDataAccessException if more than one entity matches
   */
  @Nullable T unique();

  /**
   * Count matching entities.
   */
  Number count();

  /**
   * Return an {@link EntityIterator} for lazy iteration.
   * Caller is responsible for closing the iterator.
   */
  EntityIterator<T> iterate();

  /**
   * Execute, collect into a map keyed by the named property.
   */
  <K> Map<K, T> mapBy(String property);

  /**
   * Execute, collect into a map keyed by the given function.
   */
  <K> Map<K, T> mapBy(Function<T, K> keyMapper);

  /**
   * Execute and apply the consumer to each entity.
   */
  void forEach(Consumer<T> consumer);

  /**
   * Release any underlying resources.
   */
  @Override
  void close();

}
