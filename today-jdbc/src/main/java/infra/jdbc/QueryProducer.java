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
