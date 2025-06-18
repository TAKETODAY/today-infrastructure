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

package infra.persistence.sql;

import infra.lang.Assert;

/**
 * Represents a composite logical restriction that combines two
 * {@link Restriction} instances using either a logical AND or OR operator.
 * This class is immutable and implements the {@link Restriction} interface
 * to render SQL-like logical expressions.
 *
 * <p>This class ensures that both the left and right restrictions are non-null
 * during construction. The rendered output is always enclosed in parentheses
 * to ensure proper grouping of logical expressions.</p>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see Restriction
 * @since 5.0
 */
final class LogicalRestriction implements Restriction {

  private final Restriction left;

  private final boolean logicalAnd;

  private final Restriction right;

  /**
   * Constructs a new {@code LogicalRestriction} that combines two
   * {@link Restriction} instances using either a logical AND or OR operator.
   *
   * <p>This constructor ensures that both the left and right restrictions are
   * non-null. If either restriction is null, an exception is thrown.</p>
   *
   * <p>Example usage:</p>
   *
   * Creating a logical AND restriction:
   * <pre>{@code
   * Restriction left = new SimpleRestriction("age > 18");
   * Restriction right = new SimpleRestriction("age < 65");
   * LogicalRestriction andRestriction = new LogicalRestriction(left, true, right);
   * }</pre>
   *
   * Creating a logical OR restriction:
   * <pre>{@code
   * Restriction left = new SimpleRestriction("name = 'John'");
   * Restriction right = new SimpleRestriction("name = 'Jane'");
   * LogicalRestriction orRestriction = new LogicalRestriction(left, false, right);
   * }</pre>
   *
   * @param left the left-hand side {@link Restriction} (required)
   * @param logicalAnd whether to use a logical AND operator ({@code true})
   * or a logical OR operator ({@code false})
   * @param right the right-hand side {@link Restriction} (required)
   * @throws IllegalArgumentException if either {@code left} or {@code right}
   * is {@code null}
   */
  public LogicalRestriction(Restriction left, boolean logicalAnd, Restriction right) {
    Assert.notNull(left, "Left restriction is required");
    Assert.notNull(right, "Right restriction is required");
    this.left = left;
    this.right = right;
    this.logicalAnd = logicalAnd;
  }

  /**
   * Renders the SQL representation of this logical restriction into the provided
   * {@code StringBuilder}. The output is a composite SQL expression that combines
   * the left and right restrictions using either a logical AND or OR operator,
   * enclosed in parentheses to ensure proper grouping.
   *
   * <p>This method appends the SQL fragment of the left restriction, followed by
   * the logical operator (either "AND" or "OR"), and then the SQL fragment of the
   * right restriction. The entire expression is wrapped in parentheses to maintain
   * precedence in SQL queries.</p>
   *
   * <p><b>Usage Example:</b></p>
   *
   * Combining two restrictions with a logical AND:
   * <pre>{@code
   * Restriction left = new SimpleRestriction("age > 18");
   * Restriction right = new SimpleRestriction("age < 65");
   * LogicalRestriction andRestriction = new LogicalRestriction(left, true, right);
   *
   * StringBuilder sqlBuffer = new StringBuilder();
   * andRestriction.render(sqlBuffer);
   *
   * // The resulting SQL fragment:
   * // "(age > 18 AND age < 65)"
   * System.out.println(sqlBuffer.toString());
   * }</pre>
   *
   * Combining two restrictions with a logical OR:
   * <pre>{@code
   * Restriction left = new SimpleRestriction("name = 'John'");
   * Restriction right = new SimpleRestriction("name = 'Jane'");
   * LogicalRestriction orRestriction = new LogicalRestriction(left, false, right);
   *
   * StringBuilder sqlBuffer = new StringBuilder();
   * orRestriction.render(sqlBuffer);
   *
   * // The resulting SQL fragment:
   * // "(name = 'John' OR name = 'Jane')"
   * System.out.println(sqlBuffer.toString());
   * }</pre>
   *
   * @param sqlBuffer the {@code StringBuilder} to which the SQL fragment of this
   * logical restriction will be appended. Must not be null.
   */
  @Override
  public void render(StringBuilder sqlBuffer) {
    sqlBuffer.append('(');
    left.render(sqlBuffer);
    sqlBuffer.append(' ').append(logicalAnd ? "AND" : "OR").append(' ');
    right.render(sqlBuffer);
    sqlBuffer.append(')');
  }

}