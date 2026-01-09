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
