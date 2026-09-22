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
 * An immutable restriction that combines two restrictions with a
 * {@link LogicalOperator}.
 *
 * <p>The rendered expression is always enclosed in parentheses to preserve its
 * precedence when nested in another logical expression. The supplied
 * {@link Platform} is passed unchanged to both operands and to
 * {@link LogicalOperator#render(Platform, StringBuilder)}, which resolves the
 * dialect-specific operator token.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see Restriction
 * @since 5.0
 */
final class LogicalRestriction implements Restriction {

  private final Restriction left;

  private final LogicalOperator operator;

  private final Restriction right;

  /**
   * Create a logical combination of two restrictions.
   *
   * @param left the left operand
   * @param operator the logical operator joining the operands
   * @param right the right operand
   * @throws IllegalArgumentException if an operand or the operator is {@code null}
   */
  public LogicalRestriction(Restriction left, LogicalOperator operator, Restriction right) {
    Assert.notNull(left, "Left restriction is required");
    Assert.notNull(operator, "LogicalOperator is required");
    Assert.notNull(right, "Right restriction is required");
    this.left = left;
    this.operator = operator;
    this.right = right;
  }

  /**
   * Render both operands using the given platform and enclose the resulting
   * logical expression in parentheses.
   *
   * @param platform the database platform whose rendering rules apply
   * @param sqlBuffer the buffer to append to
   */
  @Override
  public void render(Platform platform, StringBuilder sqlBuffer) {
    sqlBuffer.append('(');
    left.render(platform, sqlBuffer);
    operator.render(platform, sqlBuffer);
    right.render(platform, sqlBuffer);
    sqlBuffer.append(')');
  }

}
