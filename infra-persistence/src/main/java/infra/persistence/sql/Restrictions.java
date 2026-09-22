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



import infra.persistence.Identifier;
import infra.persistence.platform.Platform;
import infra.util.Assert;
import infra.util.CollectionUtils;
import java.util.Collection;
import org.jspecify.annotations.Nullable;

/**
 * Factory and rendering support for {@link Restriction WHERE restrictions}.
 *
 * <p>A {@link Restriction} renders only its own predicate; the {@code WHERE}
 * keyword and the connectors between several restrictions are supplied here.
 * Grouping is explicit and parenthesized: use
 * {@link #and(Restriction, Restriction)}, {@link #or(Restriction, Restriction)}
 * or {@link #xor(Restriction, Restriction)} to nest expressions. A plain
 * collection, by contrast, is rendered with a single connector threaded between
 * its elements, which keeps the result associative and free of precedence
 * surprises.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Restriction
 * @since 5.0
 */
public abstract class Restrictions {

  private Restrictions() {
  }

  // Static Factory Methods

  /**
   * Create a restriction that appends the given SQL fragment unchanged.
   *
   * <p>No identifier quoting, validation, escaping, or parameterization is
   * performed. Prefer the structured factory methods for externally supplied
   * names or values.
   *
   * @param sequence the SQL predicate fragment
   * @return a restriction that renders the fragment as-is
   */
  public static Restriction plain(CharSequence sequence) {
    return new PlainRestriction(sequence);
  }

  /**
   * Create a {@code column = ?} restriction.
   *
   * @param columnName the column name to parse as an identifier
   * @return the equality restriction
   */
  public static Restriction equal(String columnName) {
    return equal(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column = ?} restriction.
   *
   * @param columnName the column identifier
   * @return the equality restriction
   */
  public static Restriction equal(Identifier columnName) {
    return new ComparisonRestriction(columnName, " = ", "?");
  }

  /**
   * Create a {@code column = expression} restriction.
   *
   * @param lhs the column name to parse as an identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the equality restriction
   */
  public static Restriction equal(String lhs, String rhs) {
    return equal(Identifier.parse(lhs), rhs);
  }

  /**
   * Create a {@code column = expression} restriction.
   *
   * @param lhs the column identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the equality restriction
   */
  public static Restriction equal(Identifier lhs, String rhs) {
    return new ComparisonRestriction(lhs, " = ", rhs);
  }

  /**
   * Create a {@code column <> ?} restriction.
   *
   * @param columnName the column name to parse as an identifier
   * @return the inequality restriction
   */
  public static Restriction notEqual(String columnName) {
    return notEqual(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column <> ?} restriction.
   *
   * @param columnName the column identifier
   * @return the inequality restriction
   */
  public static Restriction notEqual(Identifier columnName) {
    return new ComparisonRestriction(columnName, " <> ", "?");
  }

  /**
   * Create a {@code column <> expression} restriction.
   *
   * @param lhs the column name to parse as an identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the inequality restriction
   */
  public static Restriction notEqual(String lhs, String rhs) {
    return notEqual(Identifier.parse(lhs), rhs);
  }

  /**
   * Create a {@code column <> expression} restriction.
   *
   * @param lhs the column identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the inequality restriction
   */
  public static Restriction notEqual(Identifier lhs, String rhs) {
    return new ComparisonRestriction(lhs, " <> ", rhs);
  }

  /**
   * Create a {@code column > ?} restriction.
   *
   * @param columnName the column name to parse as an identifier
   * @return the greater-than restriction
   */
  public static Restriction greaterThan(String columnName) {
    return greaterThan(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column > ?} restriction.
   *
   * @param columnName the column identifier
   * @return the greater-than restriction
   */
  public static Restriction greaterThan(Identifier columnName) {
    return greaterThan(columnName, "?");
  }

  /**
   * Create a {@code column > expression} restriction.
   *
   * @param lhs the column name to parse as an identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the greater-than restriction
   */
  public static Restriction greaterThan(String lhs, String rhs) {
    return greaterThan(Identifier.parse(lhs), rhs);
  }

  /**
   * Create a {@code column > expression} restriction.
   *
   * @param lhs the column identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the greater-than restriction
   */
  public static Restriction greaterThan(Identifier lhs, String rhs) {
    return new ComparisonRestriction(lhs, " > ", rhs);
  }

  /**
   * Create a {@code column >= ?} restriction.
   *
   * @param columnName the column name to parse as an identifier
   * @return the greater-than-or-equal restriction
   */
  public static Restriction greaterEqual(String columnName) {
    return greaterEqual(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column >= ?} restriction.
   *
   * @param columnName the column identifier
   * @return the greater-than-or-equal restriction
   */
  public static Restriction greaterEqual(Identifier columnName) {
    return greaterEqual(columnName, "?");
  }

  /**
   * Create a {@code column >= expression} restriction.
   *
   * @param lhs the column name to parse as an identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the greater-than-or-equal restriction
   */
  public static Restriction greaterEqual(String lhs, String rhs) {
    return greaterEqual(Identifier.parse(lhs), rhs);
  }

  /**
   * Create a {@code column >= expression} restriction.
   *
   * @param lhs the column identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the greater-than-or-equal restriction
   */
  public static Restriction greaterEqual(Identifier lhs, String rhs) {
    return new ComparisonRestriction(lhs, " >= ", rhs);
  }

  /**
   * Create a {@code column < ?} restriction.
   *
   * @param columnName the column name to parse as an identifier
   * @return the less-than restriction
   */
  public static Restriction lessThan(String columnName) {
    return lessThan(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column < ?} restriction.
   *
   * @param columnName the column identifier
   * @return the less-than restriction
   */
  public static Restriction lessThan(Identifier columnName) {
    return lessThan(columnName, "?");
  }

  /**
   * Create a {@code column < expression} restriction.
   *
   * @param lhs the column name to parse as an identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the less-than restriction
   */
  public static Restriction lessThan(String lhs, String rhs) {
    return lessThan(Identifier.parse(lhs), rhs);
  }

  /**
   * Create a {@code column < expression} restriction.
   *
   * @param lhs the column identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the less-than restriction
   */
  public static Restriction lessThan(Identifier lhs, String rhs) {
    return new ComparisonRestriction(lhs, " < ", rhs);
  }

  /**
   * Create a {@code column <= ?} restriction.
   *
   * @param columnName the column name to parse as an identifier
   * @return the less-than-or-equal restriction
   */
  public static Restriction lessEqual(String columnName) {
    return lessEqual(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column <= ?} restriction.
   *
   * @param columnName the column identifier
   * @return the less-than-or-equal restriction
   */
  public static Restriction lessEqual(Identifier columnName) {
    return lessEqual(columnName, "?");
  }

  /**
   * Create a {@code column <= expression} restriction.
   *
   * @param lhs the column name to parse as an identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the less-than-or-equal restriction
   */
  public static Restriction lessEqual(String lhs, String rhs) {
    return lessEqual(Identifier.parse(lhs), rhs);
  }

  /**
   * Create a {@code column <= expression} restriction.
   *
   * @param lhs the column identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the less-than-or-equal restriction
   */
  public static Restriction lessEqual(Identifier lhs, String rhs) {
    return new ComparisonRestriction(lhs, " <= ", rhs);
  }

  /**
   * Create a comparison using a caller-supplied SQL operator and expression.
   *
   * <p>The operator and right-hand expression are appended unchanged.
   *
   * @param lhs the column name to parse as an identifier
   * @param operator the SQL operator, including any required surrounding spaces
   * @param rhs the SQL expression on the right-hand side
   * @return the custom comparison restriction
   */
  public static Restriction forOperator(String lhs, String operator, String rhs) {
    return forOperator(Identifier.parse(lhs), operator, rhs);
  }

  /**
   * Create a comparison using a caller-supplied SQL operator and expression.
   *
   * <p>The operator and right-hand expression are appended unchanged.
   *
   * @param lhs the column identifier
   * @param operator the SQL operator, including any required surrounding spaces
   * @param rhs the SQL expression on the right-hand side
   * @return the custom comparison restriction
   */
  public static Restriction forOperator(Identifier lhs, String operator, String rhs) {
    return new ComparisonRestriction(lhs, operator, rhs);
  }

  /**
   * Create a {@code column is null} restriction.
   *
   * @param columnName the column name to parse as an identifier
   * @return the nullness restriction
   */
  public static Restriction isNull(String columnName) {
    return isNull(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column is null} restriction.
   *
   * @param columnName the column identifier
   * @return the nullness restriction
   */
  public static Restriction isNull(Identifier columnName) {
    return new NullnessRestriction(columnName, true);
  }

  /**
   * Create a {@code column is not null} restriction.
   *
   * @param columnName the column name to parse as an identifier
   * @return the non-nullness restriction
   */
  public static Restriction isNotNull(String columnName) {
    return isNotNull(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column is not null} restriction.
   *
   * @param columnName the column identifier
   * @return the non-nullness restriction
   */
  public static Restriction isNotNull(Identifier columnName) {
    return new NullnessRestriction(columnName, false);
  }

  /**
   * Create a {@code column BETWEEN ? AND ?} restriction.
   *
   * @param columnName the column name to parse as an identifier
   * @return the between restriction
   */
  public static Restriction between(String columnName) {
    return between(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column BETWEEN ? AND ?} restriction.
   *
   * @param columnName the column identifier
   * @return the between restriction
   */
  public static Restriction between(Identifier columnName) {
    return forOperator(columnName, " BETWEEN", " ? AND ?");
  }

  /**
   * Create a {@code column NOT BETWEEN ? AND ?} restriction.
   *
   * @param columnName the column name to parse as an identifier
   * @return the not-between restriction
   */
  public static Restriction notBetween(String columnName) {
    return notBetween(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column NOT BETWEEN ? AND ?} restriction.
   *
   * @param columnName the column identifier
   * @return the not-between restriction
   */
  public static Restriction notBetween(Identifier columnName) {
    return forOperator(columnName, " NOT BETWEEN", " ? AND ?");
  }

  /**
   * Create a {@code column LIKE ?} restriction.
   *
   * @param columnName the column name to parse as an identifier
   * @return the like restriction
   */
  public static Restriction like(String columnName) {
    return like(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column LIKE ?} restriction.
   *
   * @param columnName the column identifier
   * @return the like restriction
   */
  public static Restriction like(Identifier columnName) {
    return new LikeRestriction(columnName, true);
  }

  /**
   * Create a {@code column NOT LIKE ?} restriction.
   *
   * @param columnName the column name to parse as an identifier
   * @return the not-like restriction
   */
  public static Restriction notLike(String columnName) {
    return notLike(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column NOT LIKE ?} restriction.
   *
   * @param columnName the column identifier
   * @return the not-like restriction
   */
  public static Restriction notLike(Identifier columnName) {
    return new LikeRestriction(columnName, false);
  }

  /**
   * Create a {@code column IN (?, ?, ...)} restriction with the given count of
   * placeholders.
   *
   * @param columnName the column name to parse as an identifier
   * @param count the number of placeholders; must be at least 1
   * @return the in restriction
   */
  public static Restriction in(String columnName, int count) {
    return in(Identifier.parse(columnName), count);
  }

  /**
   * Create a {@code column IN (?, ?, ...)} restriction with the given count of
   * placeholders.
   *
   * @param columnName the column identifier
   * @param count the number of placeholders; must be at least 1
   * @return the in restriction
   */
  public static Restriction in(Identifier columnName, int count) {
    Assert.isTrue(count > 0, "IN requires at least one placeholder");
    return new InRestriction(columnName, placeholders(count), true);
  }

  /**
   * Create a {@code column NOT IN (?, ?, ...)} restriction with the given count
   * of placeholders.
   *
   * @param columnName the column name to parse as an identifier
   * @param count the number of placeholders; must be at least 1
   * @return the not-in restriction
   */
  public static Restriction notIn(String columnName, int count) {
    return notIn(Identifier.parse(columnName), count);
  }

  /**
   * Create a {@code column NOT IN (?, ?, ...)} restriction with the given count
   * of placeholders.
   *
   * @param columnName the column identifier
   * @param count the number of placeholders; must be at least 1
   * @return the not-in restriction
   */
  public static Restriction notIn(Identifier columnName, int count) {
    Assert.isTrue(count > 0, "NOT IN requires at least one placeholder");
    return new InRestriction(columnName, placeholders(count), false);
  }

  /**
   * Create an {@code EXISTS (subquery)} restriction.
   *
   * @param subquery the subquery SQL fragment
   * @return the exists restriction
   */
  public static Restriction exists(CharSequence subquery) {
    return new ExistsRestriction(subquery, true);
  }

  /**
   * Create a {@code NOT EXISTS (subquery)} restriction.
   *
   * @param subquery the subquery SQL fragment
   * @return the not-exists restriction
   */
  public static Restriction notExists(CharSequence subquery) {
    return new ExistsRestriction(subquery, false);
  }

  private static CharSequence placeholders(int count) {
    StringBuilder buf = new StringBuilder(count * 2 - 1);
    for (int i = 0; i < count; i++) {
      if (i > 0) {
        buf.append(", ");
      }
      buf.append('?');
    }
    return buf;
  }

  /**
   * Group two restrictions with {@code AND}.
   *
   * @param lhs the left operand
   * @param rhs the right operand
   * @return a parenthesized logical restriction
   */
  public static Restriction and(Restriction lhs, Restriction rhs) {
    return new LogicalRestriction(lhs, LogicalOperator.AND, rhs);
  }

  /**
   * Group two restrictions with {@code OR}.
   *
   * @param lhs the left operand
   * @param rhs the right operand
   * @return a parenthesized logical restriction
   */
  public static Restriction or(Restriction lhs, Restriction rhs) {
    return new LogicalRestriction(lhs, LogicalOperator.OR, rhs);
  }

  /**
   * Group two restrictions with {@code XOR}.
   *
   * <p>{@code XOR} is not part of ANSI SQL. The returned restriction renders
   * successfully only on a {@link Platform} that supports it natively, such as
   * MySQL; elsewhere rendering fails fast rather than emitting an invalid
   * statement.
   *
   * @param lhs the left operand
   * @param rhs the right operand
   * @return a parenthesized logical restriction
   * @throws UnsupportedOperationException when rendered on a platform without native XOR
   * @see LogicalOperator#XOR
   */
  public static Restriction xor(Restriction lhs, Restriction rhs) {
    return new LogicalRestriction(lhs, LogicalOperator.XOR, rhs);
  }

  // Rendering

  /**
   * Append a {@code WHERE} clause, joining every restriction with {@code AND}.
   *
   * @param platform the database platform whose rendering rules apply
   * @param restrictions the restrictions to render, possibly {@code null}
   * @param buf the buffer to append to; left unchanged for no restrictions
   */
  public static void append(Platform platform, @Nullable Collection<? extends Restriction> restrictions, StringBuilder buf) {
    append(platform, restrictions, LogicalOperator.AND, buf);
  }

  /**
   * Append a {@code WHERE} clause, joining every restriction with the given
   * connector.
   *
   * @param platform the database platform whose rendering rules apply
   * @param restrictions the restrictions to render, possibly {@code null}
   * @param connector the operator threaded between the restrictions, never {@code null}
   * @param buf the buffer to append to; left unchanged for no restrictions
   */
  public static void append(Platform platform, @Nullable Collection<? extends Restriction> restrictions,
          LogicalOperator connector, StringBuilder buf) {
    if (CollectionUtils.isNotEmpty(restrictions)) {
      buf.append(" WHERE ");
      appendWhereClause(platform, restrictions, connector, buf);
    }
  }

  /**
   * Render restrictions, joined with {@code AND}, without a leading
   * {@code WHERE} keyword.
   *
   * @param platform the database platform whose rendering rules apply
   * @param restrictions the restrictions to render, possibly {@code null}
   * @return a new buffer containing the predicate list, or {@code null} for a
   * {@code null} or empty collection
   */
  public static @Nullable StringBuilder renderWhereClause(Platform platform,
          @Nullable Collection<? extends Restriction> restrictions) {
    return renderWhereClause(platform, restrictions, LogicalOperator.AND);
  }

  /**
   * Render restrictions, joined with the given connector, without a leading
   * {@code WHERE} keyword.
   *
   * @param platform the database platform whose rendering rules apply
   * @param restrictions the restrictions to render, possibly {@code null}
   * @param connector the operator threaded between the restrictions, never {@code null}
   * @return a new buffer containing the predicate list, or {@code null} for a
   * {@code null} or empty collection
   */
  public static @Nullable StringBuilder renderWhereClause(Platform platform,
          @Nullable Collection<? extends Restriction> restrictions, LogicalOperator connector) {
    if (CollectionUtils.isNotEmpty(restrictions)) {
      StringBuilder buf = new StringBuilder(restrictions.size() * 10);
      appendWhereClause(platform, restrictions, connector, buf);
      return buf;
    }
    return null;
  }

  /**
   * Append restrictions joined with {@code AND}, without a leading
   * {@code WHERE} keyword.
   *
   * @param platform the database platform whose rendering rules apply
   * @param restrictions the restrictions to render
   * @param buf the buffer to append to
   */
  public static void appendWhereClause(Platform platform,
          Collection<? extends Restriction> restrictions, StringBuilder buf) {
    appendWhereClause(platform, restrictions, LogicalOperator.AND, buf);
  }

  /**
   * Append restrictions joined with the given connector, without a leading
   * {@code WHERE} keyword.
   *
   * <p>A single connector is threaded between all elements, so the rendered
   * list is associative and carries no precedence ambiguity. To mix connectors,
   * nest the operands with {@link #and(Restriction, Restriction)},
   * {@link #or(Restriction, Restriction)} or
   * {@link #xor(Restriction, Restriction)}.
   *
   * @param platform the database platform whose rendering rules apply
   * @param restrictions the restrictions to render
   * @param connector the operator threaded between the restrictions, never {@code null}
   * @param buf the buffer to append to
   * @throws IllegalArgumentException if {@code connector} is {@code null}
   */
  public static void appendWhereClause(Platform platform,
          Collection<? extends Restriction> restrictions, LogicalOperator connector, StringBuilder buf) {
    Assert.notNull(connector, "LogicalOperator is required");
    boolean appended = false;
    for (Restriction restriction : restrictions) {
      if (appended) {
        connector.render(platform, buf);
      }
      else {
        appended = true;
      }
      restriction.render(platform, buf);
    }
  }

}
