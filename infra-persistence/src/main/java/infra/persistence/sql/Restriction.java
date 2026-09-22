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

import infra.persistence.platform.Platform;

/**
 * A predicate fragment used in the {@code WHERE} clause of a SQL statement.
 *
 * <p>A restriction renders only its own predicate, without a leading
 * {@code WHERE} keyword and without any logical connector. Identifier-bearing
 * implementations retain {@link infra.persistence.Identifier Identifier}
 * metadata and use the supplied {@link Platform} when rendering
 * database-specific quote characters.
 *
 * <p>Restrictions are created and composed through {@link Restrictions}, which
 * also supplies the {@code WHERE} keyword and the connectors between several
 * restrictions. Keeping those concerns out of this interface lets a restriction
 * stay a small, context-free SQL fragment.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Restrictions
 * @see ComparisonRestriction
 * @see NullnessRestriction
 * @see PlainRestriction
 * @since 4.0
 */
public interface Restriction {

  /**
   * Render this restriction as a platform-specific SQL fragment and append it to
   * the supplied buffer.
   *
   * <p>The given {@link Platform} determines dialect-specific syntax such as the
   * quote characters used for identifiers. Implementations should append only the
   * restriction itself; they must not add a {@code WHERE} keyword or a logical
   * operator. Those are supplied by {@link Restrictions} when it composes
   * restrictions.
   *
   * @param platform the database platform whose SQL rendering rules apply
   * @param sqlBuffer the buffer to which the rendered restriction is appended
   * @since 5.0
   */
  void render(Platform platform, StringBuilder sqlBuffer);

}
