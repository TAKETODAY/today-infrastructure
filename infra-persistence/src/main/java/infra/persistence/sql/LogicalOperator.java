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
 * A binary logical operator that joins two predicates in a {@code WHERE} clause.
 *
 * <p>SQL natively supports {@link #AND} and {@link #OR}. {@link #XOR} is a
 * MySQL extension; whether it can be rendered is decided by the
 * {@link Platform}, so a database without native support rejects it instead of
 * silently producing a broken statement. Rendering is always delegated to the
 * platform, which keeps dialect differences out of the enum.
 *
 * <p>{@code NOT} is deliberately not part of this type: it is a unary operator
 * that applies to a single predicate, not a connector between two. Model it as
 * a {@link Restriction} wrapper instead.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Restriction#connector()
 * @see Restriction#and(Restriction, Restriction)
 * @see Restriction#or(Restriction, Restriction)
 * @see Restriction#xor(Restriction, Restriction)
 * @since 5.0
 */
public enum LogicalOperator {

  /**
   * The {@code AND} conjunction: both operands must hold.
   */
  AND,

  /**
   * The {@code OR} disjunction: at least one operand must hold.
   */
  OR,

  /**
   * The {@code XOR} exclusive disjunction: exactly one operand must hold.
   *
   * <p>Rendered only by platforms that support it natively (for example MySQL).
   */
  XOR;

  /**
   * Render this operator, surrounded by single spaces, using the token the
   * given platform accepts.
   *
   * @param platform the platform whose dialect applies
   * @param sqlBuffer the buffer to append to
   * @throws UnsupportedOperationException if the platform cannot render this operator
   */
  public void render(Platform platform, StringBuilder sqlBuffer) {
    sqlBuffer.append(' ').append(platform.getLogicalOperator(this)).append(' ');
  }

  /**
   * Resolve the operator matching a boolean {@code logicalAnd} flag.
   *
   * @param logicalAnd {@code true} for {@link #AND}, {@code false} for {@link #OR}
   * @return the matching operator
   */
  public static LogicalOperator of(boolean logicalAnd) {
    return logicalAnd ? AND : OR;
  }

}
