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

package infra.persistence;

import org.jspecify.annotations.Nullable;

/**
 * Factory interface for creating query statements based on example objects.
 *
 * <p>This interface provides methods to create both general {@link QueryStatement} and
 * {@link ConditionStatement} instances from example objects. Implementations of this
 * interface are responsible for analyzing the provided example object and generating
 * appropriate query representations.</p>
 *
 * <p>Typically used in persistence frameworks to convert domain objects or criteria
 * into database query representations that can be executed against a data store.</p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see QueryStatement
 * @see ConditionStatement
 * @since 4.0 2024/4/10 13:54
 */
public interface QueryStatementFactory {

  /**
   * Creates a {@link QueryStatement} based on the provided example object.
   *
   * <p>The example object is analyzed to determine the appropriate query structure
   * and conditions. The returned query statement can be used to execute queries
   * against a data store.</p>
   *
   * @param example the example object to base the query on
   * @return a QueryStatement representing the query derived from the example,
   * or null if this factory cannot create a query for the given example
   */
  @Nullable
  QueryStatement createQuery(Object example);

  /**
   * Creates a {@link ConditionStatement} based on the provided example object.
   *
   * <p>The example object is analyzed to determine the appropriate conditions
   * for filtering data. The returned condition statement can be used to add
   * WHERE clauses or other filtering criteria to queries.</p>
   *
   * @param example the example object to base the conditions on
   * @return a ConditionStatement representing the conditions derived from the example,
   * or null if this factory cannot create conditions for the given example
   */
  @Nullable
  ConditionStatement createCondition(Object example);

}
