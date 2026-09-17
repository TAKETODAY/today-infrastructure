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
 * Restrictions can be grouped with {@link #and(Restriction, Restriction)} and
 * {@link #or(Restriction, Restriction)}, or rendered as a collection with
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
   * Return how this restriction should be joined to the preceding restriction
   * when rendered as part of a collection.
   *
   * @return {@code true} for {@code AND}, or {@code false} for {@code OR}
   */
  default boolean logicalAnd() {
    return true;
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
  static Restriction graterThan(String columnName) {
    return graterThan(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column > ?} restriction.
   *
   * @param columnName the column identifier
   * @return the greater-than restriction
   */
  static Restriction graterThan(Identifier columnName) {
    return graterThan(columnName, "?");
  }

  /**
   * Create a {@code column > expression} restriction.
   *
   * @param lhs the column name to parse as an identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the greater-than restriction
   */
  static Restriction graterThan(String lhs, String rhs) {
    return graterThan(Identifier.parse(lhs), rhs);
  }

  /**
   * Create a {@code column > expression} restriction.
   *
   * @param lhs the column identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the greater-than restriction
   */
  static Restriction graterThan(Identifier lhs, String rhs) {
    return new ComparisonRestriction(lhs, " > ", rhs);
  }

  /**
   * Create a {@code column >= ?} restriction.
   *
   * @param columnName the column name to parse as an identifier
   * @return the greater-than-or-equal restriction
   */
  static Restriction graterEqual(String columnName) {
    return graterEqual(Identifier.parse(columnName));
  }

  /**
   * Create a {@code column >= ?} restriction.
   *
   * @param columnName the column identifier
   * @return the greater-than-or-equal restriction
   */
  static Restriction graterEqual(Identifier columnName) {
    return graterEqual(columnName, "?");
  }

  /**
   * Create a {@code column >= expression} restriction.
   *
   * @param lhs the column name to parse as an identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the greater-than-or-equal restriction
   */
  static Restriction graterEqual(String lhs, String rhs) {
    return graterEqual(Identifier.parse(lhs), rhs);
  }

  /**
   * Create a {@code column >= expression} restriction.
   *
   * @param lhs the column identifier
   * @param rhs the SQL expression on the right-hand side
   * @return the greater-than-or-equal restriction
   */
  static Restriction graterEqual(Identifier lhs, String rhs) {
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
    return new LogicalRestriction(lhs, true, rhs);
  }

  /**
   * Mark a restriction to be joined to the preceding collection element with
   * {@code OR} instead of the default {@code AND}.
   *
   * <p>Rendering the returned restriction by itself does not prepend an
   * {@code OR} token; collection rendering supplies the connector.
   *
   * @param rhs the restriction to mark
   * @return a delegating restriction whose {@link #logicalAnd()} is {@code false}
   * @since 5.0
   */
  static Restriction or(Restriction rhs) {
    return new Restriction() {

      @Override
      public void render(Platform platform, StringBuilder sqlBuffer) {
        rhs.render(platform, sqlBuffer);
      }

      @Override
      public boolean logicalAnd() {
        return false;
      }

    };
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
    return new LogicalRestriction(lhs, false, rhs);
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
   * {@link #logicalAnd()} value, without a leading {@code WHERE} keyword.
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
        buf.append(restriction.logicalAnd() ? " AND " : " OR ");
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
