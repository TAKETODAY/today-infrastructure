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
 * Nullness restriction - IS (NOT)? NULL
 *
 * @author Steve Ebersole
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class NullnessRestriction implements Restriction {

  private final String columnName;

  private final boolean affirmative;

  NullnessRestriction(String columnName, boolean affirmative) {
    this.columnName = columnName;
    this.affirmative = affirmative;
  }

  @Override
  public void render(StringBuilder sqlBuffer) {
    sqlBuffer.append('`');
    sqlBuffer.append(columnName);
    sqlBuffer.append('`');
    if (affirmative) {
      sqlBuffer.append(" is null");
    }
    else {
      sqlBuffer.append(" is not null");
    }
  }
}
