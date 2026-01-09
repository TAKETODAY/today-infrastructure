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

import java.util.Objects;

/**
 * A binary-comparison restriction
 *
 * @author Steve Ebersole
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ComparisonRestriction implements Restriction {

  private final String lhs;

  private final String operator;

  private final String rhs;

  public ComparisonRestriction(String lhs, String operator, String rhs) {
    this.lhs = lhs;
    this.operator = operator;
    this.rhs = rhs;
  }

  @Override
  public void render(StringBuilder sqlBuffer) {
    sqlBuffer.append('`')
            .append(lhs)
            .append('`')
            .append(operator)
            .append(rhs);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ComparisonRestriction that = (ComparisonRestriction) o;
    return Objects.equals(lhs, that.lhs)
            && Objects.equals(operator, that.operator)
            && Objects.equals(rhs, that.rhs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lhs, operator, rhs);
  }

  @Override
  public String toString() {
    return "%s %s %s".formatted(lhs, operator, rhs);
  }

}
