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

import org.jspecify.annotations.Nullable;

import java.util.Collection;

import infra.persistence.Identifier;
import infra.persistence.platform.Platform;
import infra.util.Assert;
import infra.util.CollectionUtils;

/**
 * A predicate fragment used in the {@code WHERE} clause of a SQL statement.
 *
 * <p>A restriction renders only its predicate, without a leading {@code WHERE},
 * {@code AND}, or {@code OR}. Identifier-bearing implementations retain
 * {@link infra.persistence.Identifier Identifier} metadata and use the supplied
 * {@link Platform} when rendering database-specific quote characters.
 *
 * <p>Factory methods create comparison, nullness and plain SQL restrictions.
 * Restrictions can be grouped with {@link #and(Restriction, Restriction)},
 * {@link #or(Restriction, Restriction)} and {@link #xor(Restriction, Restriction)},
 * or rendered as a collection with
 * {@link #append(Platform, Collection, StringBuilder)}:
 * <pre>{@code
 * List<Restriction> restrictions = List.of(
 *     Restriction.equal("age", "?"),
 *     Restriction.isNotNull("email"));
 *
 * StringBuilder sql = new StringBuilder("SELECT * FROM users");
 * Restriction.append(platform, restrictions, sql);
 * // SELECT * FROM users WHERE age = ? AND email is not null
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ComparisonRestriction
 * @see NullnessRestriction
 * @see Plain
 * @since 4.0
 */
public interface Restriction {

  /**
   * Render this restriction as a platform-specific SQL fragment and append it to
   * the supplied buffer.
   *
   * <p>The given {@link Platform} determines dialect-specific syntax such as the
   * quote characters used for identifiers. Implementations should append only the
   * restriction itself; they must not add a {@code WHERE} keyword or a leading
   * logical operator. Those are supplied by the statement or collection rendering
   * methods that compose restrictions.
   *
   * @param platform the database platform whose SQL rendering rules apply
   * @param sqlBuffer the buffer to which the rendered restriction is appended
   * @since 5.0
   */
  void render(Platform platform, StringBuilder sqlBuffer);

  /**
   * Return the logical operator that joins this restriction to the preceding
   * one when rendered as part of a collection.
   *
   * <p>The connector belongs to collection rendering, not to the fragment
   * itself: a restriction rendered on its own never emits it, and the first
   * element of a collection has its connector ignored.
   *
   * @return the connector, never {@code null}
   * @since 5.0
   */
  default LogicalOperator connector() {
    return LogicalOperator.AND;
  }

  /**
   * Return a restriction that carries the given logical connector for
   * collection rendering.
   *
   * <p>The connector is metadata consumed by
   * {@link #appendWhereClause(Platform, Collection, StringBuilder)}; it is not
   * part of the fragment. Rendering the returned restriction on its own emits
   * only this restriction and never the connector. Collection rendering still
   * ignores the first element's connector.
   *
   * <p>{@link LogicalOperator#AND} is the default connector, so passing it
   * returns this instance unchanged instead of wrapping it.
   *
   * @param connector the operator joining this restriction to the preceding
   * collection element, never {@code null}
   * @return this restriction when {@code connector} is {@link LogicalOperator#AND},
   * otherwise a delegating restriction whose {@link #connector()} is {@code connector}
   * @throws IllegalArgumentException if {@code connector} is {@code null}
   * @see #connector()
   * @since 5.0
   */
  default Restriction withConnector(LogicalOperator connector) {
    Assert.notNull(connector, "LogicalOperator is required");
    if (connector == LogicalOperator.AND) {
      return this;
    }
    Restriction self = this;
    return new Restriction() {

      @Override
      public void render(Platform platform, StringBuilder sqlBuffer) {
        self.render(platform, sqlBuffer);
      }

      @Override
      public LogicalOperator connector() {
        return connector;
      }

    };
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
  static Restriction plain(CharSequence sequence) {
    return new Plain(sequence);
  }

  /**
   * Create a {@code column = ?} restriction.
   *
   * @param columnName the column name to parse as an identifier
   * @return the equality restriction
   */
  static Restriction equal(String columnName) {
    return equal(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column = ?} restriction.
   *
   * @param columnName the column identifier
   * @return the equality restriction
   * @since 5.0
   */
  static Restriction equal(Identifier columnName) {
    return new ComparisonRestriction(columnName, " = ", "?");
  }

  /**
   * Create a {@code column = expression} restriction.
   *
   * @param lhs the column name to parse as an identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the equality restriction
   */
  static Restriction equal(String lhs, String rhs) {
    return equal(Identifier.parse(lhs), rhs);
  }

  /**
   * Create a {@code column = expression} restriction.
   *
   * @param lhs the column identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the equality restriction
   * @since 5.0
   */
  static Restriction equal(Identifier lhs, String rhs) {
    return new ComparisonRestriction(lhs, " = ", rhs);
  }

  /**
   * Create a {@code column <> ?} restriction.
   *
   * @param columnName the column name to parse as an identifier
   * @return the inequality restriction
   */
  static Restriction notEqual(String columnName) {
    return notEqual(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column <> ?} restriction.
   *
   * @param columnName the column identifier
   * @return the inequality restriction
   * @since 5.0
   */
  static Restriction notEqual(Identifier columnName) {
    return new ComparisonRestriction(columnName, " <> ", "?");
  }

  /**
   * Create a {@code column <> expression} restriction.
   *
   * @param lhs the column name to parse as an identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the inequality restriction
   */
  static Restriction notEqual(String lhs, String rhs) {
    return notEqual(Identifier.parse(lhs), rhs);
  }

  /**
   * Create a {@code column <> expression} restriction.
   *
   * @param lhs the column identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the inequality restriction
   * @since 5.0
   */
  static Restriction notEqual(Identifier lhs, String rhs) {
    return new ComparisonRestriction(lhs, " <> ", rhs);
  }

  /**
   * Create a {@code column > ?} restriction.
   *
   * @param columnName the column name to parse as an identifier
   * @return the greater-than restriction
   */
  static Restriction greaterThan(String columnName) {
    return greaterThan(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column > ?} restriction.
   *
   * @param columnName the column identifier
   * @return the greater-than restriction
   * @since 5.0
   */
  static Restriction greaterThan(Identifier columnName) {
    return greaterThan(columnName, "?");
  }

  /**
   * Create a {@code column > expression} restriction.
   *
   * @param lhs the column name to parse as an identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the greater-than restriction
   */
  static Restriction greaterThan(String lhs, String rhs) {
    return greaterThan(Identifier.parse(lhs), rhs);
  }

  /**
   * Create a {@code column > expression} restriction.
   *
   * @param lhs the column identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the greater-than restriction
   * @since 5.0
   */
  static Restriction greaterThan(Identifier lhs, String rhs) {
    return new ComparisonRestriction(lhs, " > ", rhs);
  }

  /**
   * Create a {@code column >= ?} restriction.
   *
   * @param columnName the column name to parse as an identifier
   * @return the greater-than-or-equal restriction
   */
  static Restriction greaterEqual(String columnName) {
    return greaterEqual(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column >= ?} restriction.
   *
   * @param columnName the column identifier
   * @return the greater-than-or-equal restriction
   * @since 5.0
   */
  static Restriction greaterEqual(Identifier columnName) {
    return greaterEqual(columnName, "?");
  }

  /**
   * Create a {@code column >= expression} restriction.
   *
   * @param lhs the column name to parse as an identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the greater-than-or-equal restriction
   */
  static Restriction greaterEqual(String lhs, String rhs) {
    return greaterEqual(Identifier.parse(lhs), rhs);
  }

  /**
   * Create a {@code column >= expression} restriction.
   *
   * @param lhs the column identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the greater-than-or-equal restriction
   * @since 5.0
   */
  static Restriction greaterEqual(Identifier lhs, String rhs) {
    return new ComparisonRestriction(lhs, " >= ", rhs);
  }

  /**
   * Create a {@code column < ?} restriction.
   *
   * @param columnName the column name to parse as an identifier
   * @return the less-than restriction
   */
  static Restriction lessThan(String columnName) {
    return lessThan(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column < ?} restriction.
   *
   * @param columnName the column identifier
   * @return the less-than restriction
   * @since 5.0
   */
  static Restriction lessThan(Identifier columnName) {
    return lessThan(columnName, "?");
  }

  /**
   * Create a {@code column < expression} restriction.
   *
   * @param lhs the column name to parse as an identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the less-than restriction
   */
  static Restriction lessThan(String lhs, String rhs) {
    return lessThan(Identifier.parse(lhs), rhs);
  }

  /**
   * Create a {@code column < expression} restriction.
   *
   * @param lhs the column identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the less-than restriction
   * @since 5.0
   */
  static Restriction lessThan(Identifier lhs, String rhs) {
    return new ComparisonRestriction(lhs, " < ", rhs);
  }

  /**
   * Create a {@code column <= ?} restriction.
   *
   * @param columnName the column name to parse as an identifier
   * @return the less-than-or-equal restriction
   */
  static Restriction lessEqual(String columnName) {
    return lessEqual(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column <= ?} restriction.
   *
   * @param columnName the column identifier
   * @return the less-than-or-equal restriction
   * @since 5.0
   */
  static Restriction lessEqual(Identifier columnName) {
    return lessEqual(columnName, "?");
  }

  /**
   * Create a {@code column <= expression} restriction.
   *
   * @param lhs the column name to parse as an identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the less-than-or-equal restriction
   */
  static Restriction lessEqual(String lhs, String rhs) {
    return lessEqual(Identifier.parse(lhs), rhs);
  }

  /**
   * Create a {@code column <= expression} restriction.
   *
   * @param lhs the column identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the less-than-or-equal restriction
   * @since 5.0
   */
  static Restriction lessEqual(Identifier lhs, String rhs) {
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
  static Restriction forOperator(String lhs, String operator, String rhs) {
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
   * @since 5.0
   */
  static Restriction forOperator(Identifier lhs, String operator, String rhs) {
    return new ComparisonRestriction(lhs, operator, rhs);
  }

  /**
   * Create a {@code column is null} restriction.
   *
   * @param columnName the column name to parse as an identifier
   * @return the nullness restriction
   */
  static Restriction isNull(String columnName) {
    return isNull(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column is null} restriction.
   *
   * @param columnName the column identifier
   * @return the nullness restriction
   * @since 5.0
   */
  static Restriction isNull(Identifier columnName) {
    return new NullnessRestriction(columnName, true);
  }

  /**
   * Create a {@code column is not null} restriction.
   *
   * @param columnName the column name to parse as an identifier
   * @return the non-nullness restriction
   */
  static Restriction isNotNull(String columnName) {
    return isNotNull(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column is not null} restriction.
   *
   * @param columnName the column identifier
   * @return the non-nullness restriction
   * @since 5.0
   */
  static Restriction isNotNull(Identifier columnName) {
    return new NullnessRestriction(columnName, false);
  }

  /**
   * Create a {@code column BETWEEN ? AND ?} restriction.
   *
   * @param columnName the column name to parse as an identifier
   * @return the between restriction
   */
  static Restriction between(String columnName) {
    return between(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column BETWEEN ? AND ?} restriction.
   *
   * @param columnName the column identifier
   * @return the between restriction
   * @since 5.0
   */
  static Restriction between(Identifier columnName) {
    return forOperator(columnName, " BETWEEN", " ? AND ?");
  }

  /**
   * Create a {@code column NOT BETWEEN ? AND ?} restriction.
   *
   * @param columnName the column name to parse as an identifier
   * @return the not-between restriction
   */
  static Restriction notBetween(String columnName) {
    return notBetween(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column NOT BETWEEN ? AND ?} restriction.
   *
   * @param columnName the column identifier
   * @return the not-between restriction
   * @since 5.0
   */
  static Restriction notBetween(Identifier columnName) {
    return forOperator(columnName, " NOT BETWEEN", " ? AND ?");
  }

  /**
   * Group two restrictions with {@code AND}.
   *
   * @param lhs the left operand
   * @param rhs the right operand
   * @return a parenthesized logical restriction
   * @since 5.0
   */
  static Restriction and(Restriction lhs, Restriction rhs) {
    return new LogicalRestriction(lhs, LogicalOperator.AND, rhs);
  }

  /**
   * Return {@code restriction} marked to be joined to the preceding collection
   * element with {@code OR}.
   *
   * <p>Equivalent to {@code restriction.withConnector(LogicalOperator.OR)}.
   * Rendering the result on its own emits only the wrapped restriction; the
   * {@code OR} token is supplied by collection rendering.
   *
   * @param restriction the restriction to mark, never {@code null}
   * @return a restriction whose {@link #connector()} is {@link LogicalOperator#OR}
   * @throws IllegalArgumentException if {@code restriction} is {@code null}
   * @see #withConnector(LogicalOperator)
   * @since 5.0
   */
  static Restriction or(Restriction restriction) {
    return restriction.withConnector(LogicalOperator.OR);
  }

  /**
   * Return {@code restriction} marked to be joined to the preceding collection
   * element with {@code XOR}.
   *
   * <p>Equivalent to {@code restriction.withConnector(LogicalOperator.XOR)}.
   * The connector is emitted only while rendering a collection, and only on a
   * {@link Platform} that supports XOR natively.
   *
   * @param restriction the restriction to mark, never {@code null}
   * @return a restriction whose {@link #connector()} is {@link LogicalOperator#XOR}
   * @throws IllegalArgumentException if {@code restriction} is {@code null}
   * @see #withConnector(LogicalOperator)
   * @see LogicalOperator#XOR
   * @since 5.0
   */
  static Restriction xor(Restriction restriction) {
    return restriction.withConnector(LogicalOperator.XOR);
  }

  /**
   * Group two restrictions with {@code OR}.
   *
   * @param lhs the left operand
   * @param rhs the right operand
   * @return a parenthesized logical restriction
   * @since 5.0
   */
  static Restriction or(Restriction lhs, Restriction rhs) {
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
   * @since 5.0
   */
  static Restriction xor(Restriction lhs, Restriction rhs) {
    return new LogicalRestriction(lhs, LogicalOperator.XOR, rhs);
  }

  /**
   * Append a {@code WHERE} clause when restrictions are present.
   *
   * @param platform the database platform whose rendering rules apply
   * @param restrictions the restrictions to render, possibly {@code null}
   * @param buf the buffer to append to; left unchanged for no restrictions
   */
  static void append(Platform platform, @Nullable Collection<? extends Restriction> restrictions, StringBuilder buf) {
    if (CollectionUtils.isNotEmpty(restrictions)) {
      buf.append(" WHERE ");
      appendWhereClause(platform, restrictions, buf);
    }
  }

  /**
   * Render restrictions without a leading {@code WHERE} keyword.
   *
   * @param platform the database platform whose rendering rules apply
   * @param restrictions the restrictions to render, possibly {@code null}
   * @return a new buffer containing the predicate list, or {@code null} for a
   * {@code null} or empty collection
   */
  static @Nullable StringBuilder renderWhereClause(Platform platform, @Nullable Collection<? extends Restriction> restrictions) {
    if (CollectionUtils.isNotEmpty(restrictions)) {
      StringBuilder buf = new StringBuilder(restrictions.size() * 10);
      appendWhereClause(platform, restrictions, buf);
      return buf;
    }
    return null;
  }

  /**
   * Append restrictions separated according to each element's
   * {@link #connector()} value, without a leading {@code WHERE} keyword.
   * The first element's connector is ignored.
   *
   * @param platform the database platform whose rendering rules apply
   * @param restrictions the restrictions to render
   * @param buf the buffer to append to
   */
  static void appendWhereClause(Platform platform, Collection<? extends Restriction> restrictions, StringBuilder buf) {
    boolean appended = false;
    for (Restriction restriction : restrictions) {
      if (appended) {
        restriction.connector().render(platform, buf);
      }
      else {
        appended = true;
      }
      restriction.render(platform, buf);
    }
  }

  /**
   * A restriction that renders a caller-provided SQL fragment unchanged.
   */
  final class Plain implements Restriction {

    private final CharSequence sequence;

    Plain(CharSequence sequence) {
      this.sequence = sequence;
    }

    @Override
    public void render(Platform platform, StringBuilder sqlBuffer) {
      sqlBuffer.append(sequence);
    }
  }

}
