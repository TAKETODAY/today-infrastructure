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

package infra.persistence.sql;





/**
 * A programmatic source of an {@link OrderSpec}, allowing an entity to supply its
 * own ORDER BY clause instead of relying on {@code @OrderBy}/{@code @OrderByClause}
 * annotations.
 *
 * <p>An example object that implements this interface contributes its ordering to
 * the resolved query. The returned spec takes precedence over the declarative
 * ordering derived from the entity's annotations.
 *
 * <p>Implementations should return a non-empty spec only when ordering is required;
 * an empty spec (or one that {@linkplain OrderSpec#isEmpty() isEmpty()}) is treated
 * as "no ordering".
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see OrderSpec
 * @since 4.0 2024/3/31 19:28
 */
public interface OrderSpecSource {

  /**
   * Return the ORDER BY specification contributed by this source.
   *
   * @return the ordering spec, never {@code null}
   */
  OrderSpec orderSpec();
}
