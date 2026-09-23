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

import java.util.ArrayList;
import java.util.Objects;

import infra.jdbc.ParameterBinder;
import infra.persistence.Identifier;
import infra.persistence.platform.Platform;

/**
 * Assemble SQL text and collect binders in placeholder order for a single render.
 *
 * <p>{@link #append(CharSequence)} adds SQL literally, including any {@code ?}
 * characters; it does not register binders. Use {@link #parameter(ParameterBinder)}
 * for each placeholder whose binding should be collected. Existing SQL fragments
 * containing their own placeholders must manage those bindings separately.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public final class SqlBuilder {

  private final Platform platform;

  private final StringBuilder sql;

  private final ArrayList<ParameterBinder> binders = new ArrayList<>();

  /**
   * Create a builder for the given database platform.
   *
   * @param platform the platform used to render identifiers
   */
  public SqlBuilder(Platform platform) {
    this(platform, 16);
  }

  /**
   * Create a builder with an initial SQL buffer capacity.
   *
   * @param platform the platform used to render identifiers
   * @param capacity the initial SQL buffer capacity
   */
  public SqlBuilder(Platform platform, int capacity) {
    this.platform = Objects.requireNonNull(platform, "platform");
    this.sql = new StringBuilder(capacity);
  }

  /**
   * Append SQL text unchanged. No parameters are collected from the text.
   *
   * @param text the SQL text to append
   * @return this builder
   */
  public SqlBuilder append(CharSequence text) {
    sql.append(Objects.requireNonNull(text, "text"));
    return this;
  }

  /**
   * Append an SQL character unchanged.
   *
   * @param character the character to append
   * @return this builder
   */
  public SqlBuilder append(char character) {
    sql.append(character);
    return this;
  }

  /**
   * Append an identifier using the platform's quoting rules when required.
   *
   * @param identifier the identifier to render
   * @return this builder
   */
  public SqlBuilder identifier(Identifier identifier) {
    Objects.requireNonNull(identifier, "identifier").appendTo(sql, platform);
    return this;
  }

  /**
   * Append a placeholder and register its JDBC binder at the same position.
   * Each binder binds exactly one parameter.
   *
   * @param binder the binder for this placeholder
   * @return this builder
   */
  public SqlBuilder parameter(ParameterBinder binder) {
    binders.add(Objects.requireNonNull(binder, "binder"));
    sql.append('?');
    return this;
  }

  /**
   * Snapshot the SQL and binders assembled so far. Further changes to this builder
   * do not affect the returned statement.
   *
   * @return the rendered SQL and its ordered binders
   */
  public SqlStatement build() {
    return new SqlStatement(sql.toString(), binders);
  }

}
