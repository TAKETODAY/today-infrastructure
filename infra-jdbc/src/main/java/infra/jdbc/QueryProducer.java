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

package infra.jdbc;

/**
 * An interface responsible for producing SQL queries and named queries.
 * <p>
 * This interface provides methods to create instances of {@link Query} and {@link NamedQuery}.
 * It is recommended to use these methods in conjunction with the {@link JdbcConnection} class,
 * ensuring proper resource management using try-with-resources blocks.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * try (Connection con = repositoryManager.open()) {
 *   // Create a query and fetch results
 *   return repositoryManager.createQuery("SELECT * FROM users", true)
 *                            .fetch(User.class);
 * }
 * }</pre>
 *
 * <p>
 * Another example with named queries:
 * </p>
 * <pre>{@code
 * try (Connection con = repositoryManager.open()) {
 *   // Create a named query and fetch results
 *   return repositoryManager.createNamedQuery("SELECT * FROM products WHERE id = :id")
 *                            .fetch(Product.class);
 * }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Query
 * @see NamedQuery
 * @see JdbcConnection
 * @since 4.0 2023/1/22 14:00
 */
public interface QueryProducer {

  /**
   * Creates a {@link Query}
   * <p>
   * better to use :
   * create queries with {@link JdbcConnection} class instead,
   * using try-with-resource blocks
   * <pre>{@code
   * try (Connection con = repositoryManager.open()) {
   *    return repositoryManager.createQuery(query, name, returnGeneratedKeys)
   *                .fetch(Pojo.class);
   * }
   * }</pre>
   * </p>
   *
   * @param query the sql query string
   * @param returnGeneratedKeys boolean value indicating if the database should return any
   * generated keys.
   * @return the {@link NamedQuery} instance
   */
  Query createQuery(String query, boolean returnGeneratedKeys);

  /**
   * Creates a {@link Query}
   *
   * better to use :
   * create queries with {@link JdbcConnection} class instead,
   * using try-with-resource blocks
   * <pre>{@code
   *     try (Connection con = repositoryManager.open()) {
   *         return repositoryManager.createQuery(query, name)
   *                      .fetch(Pojo.class);
   *     }
   *  }</pre>
   *
   * @param query the sql query string
   * @return the {@link NamedQuery} instance
   */
  Query createQuery(String query);

  /**
   * Creates a {@link NamedQuery}
   * <p>
   * better to use :
   * create queries with {@link JdbcConnection} class instead,
   * using try-with-resource blocks
   * <pre>{@code
   * try (Connection con = repositoryManager.open()) {
   *    return repositoryManager.createNamedQuery(query, name, returnGeneratedKeys)
   *                .fetch(Pojo.class);
   * }
   * }</pre>
   * </p>
   *
   * @param query the sql query string
   * @param returnGeneratedKeys boolean value indicating if the database should return any
   * generated keys.
   * @return the {@link NamedQuery} instance
   */
  NamedQuery createNamedQuery(String query, boolean returnGeneratedKeys);

  /**
   * Creates a {@link NamedQuery}
   *
   * better to use :
   * create queries with {@link JdbcConnection} class instead,
   * using try-with-resource blocks
   * <pre>{@code
   *     try (Connection con = repositoryManager.open()) {
   *         return repositoryManager.createNamedQuery(query, name)
   *                      .fetch(Pojo.class);
   *     }
   *  }</pre>
   *
   * @param query the sql query string
   * @return the {@link NamedQuery} instance
   */
  NamedQuery createNamedQuery(String query);

}
