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
import infra.util.Assert;

/**
 * A restriction that renders an {@code EXISTS (subquery)} or
 * {@code NOT EXISTS (subquery)} predicate.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Restrictions#exists(CharSequence)
 * @see Restrictions#notExists(CharSequence)
 * @since 5.0
 */
final class ExistsRestriction implements Restriction {

  private final CharSequence subquery;

  private final boolean affirmative;

  ExistsRestriction(CharSequence subquery, boolean affirmative) {
    Assert.notNull(subquery, "Subquery is required");
    this.subquery = subquery;
    this.affirmative = affirmative;
  }

  @Override
  public void render(Platform platform, StringBuilder sqlBuffer) {
    if (affirmative) {
      sqlBuffer.append("EXISTS (");
    }
    else {
      sqlBuffer.append("NOT EXISTS (");
    }
    sqlBuffer.append(subquery).append(')');
  }

}
