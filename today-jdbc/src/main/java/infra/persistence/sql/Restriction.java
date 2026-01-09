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

import infra.util.CollectionUtils;

/**
 * Represents a restriction in SQL query generation. This interface provides
 * methods to render SQL fragments for various types of restrictions, such as
 * equality, comparison, nullness checks, and custom operators.
 *
 * <p>Restrictions can be combined to form complex SQL conditions. The
 * {@link #render(StringBuilder)} method is used to append the SQL representation
 * of the restriction to a given buffer.
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 *   // Create a restriction for equality
 *   Restriction eqRestriction = Restriction.equal("age", "30");
 *
 *   // Create a restriction for null check
 *   Restriction nullRestriction = Restriction.isNull("email");
 *
 *   // Combine restrictions into a list
 *   List<Restriction> restrictions = new ArrayList<>();
 *   restrictions.add(eqRestriction);
 *   restrictions.add(nullRestriction);
 *
 *   // Render the restrictions into an SQL buffer
 *   StringBuilder sqlBuffer = new StringBuilder();
 *   Restriction.render(restrictions, sqlBuffer);
 *
 *   // The sqlBuffer now contains: " WHERE `age` = 30 AND email is null"
 *   System.out.println(sqlBuffer.toString());
 * }</pre>
 *
 * <p>This interface also provides static factory methods to create common
 * types of restrictions, such as equality, comparison, and nullness checks.
 *
 * <p><strong>Static Factory Methods:</strong>
 * <ul>
 *   <li>{@link #equal(String, String)} - Creates an equality restriction.</li>
 *   <li>{@link #notEqual(String, String)} - Creates a non-equality restriction.</li>
 *   <li>{@link #isNull(String)} - Creates a nullness restriction (IS NULL).</li>
 *   <li>{@link #isNotNull(String)} - Creates a non-nullness restriction (IS NOT NULL).</li>
 *   <li>{@link #forOperator(String, String, String)} - Creates a custom operator restriction.</li>
 * </ul>
 *
 * <p><strong>Rendering Multiple Restrictions:</strong>
 * <pre>{@code
 *   // Example of rendering multiple restrictions with a WHERE clause
 *   List<Restriction> restrictions = Arrays.asList(
 *       Restriction.graterThan("salary", "50000"),
 *       Restriction.lessEqual("age", "40")
 *   );
 *
 *   StringBuilder sqlBuffer = Restriction.renderWhereClause(restrictions);
 *   if (sqlBuffer != null) {
 *     System.out.println(sqlBuffer.toString());
 *     // Output: "`salary` > 50000 AND `age` <= 40"
 *   }
 * }</pre>
 *
 * <p><strong>Implementation Notes:</strong>
 * Implementations of this interface should ensure that the {@link #render(StringBuilder)}
 * method appends valid SQL fragments to the provided buffer. The static utility methods
 * handle common use cases and provide a convenient way to construct SQL conditions.
 *
 * @author Steve Ebersole
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ComparisonRestriction
 * @see NullnessRestriction
 * @see Plain
 * @since 4.0
 */
public interface Restriction {

  /**
   * Renders the SQL representation of this restriction into the provided
   * {@code StringBuilder}. This method is typically used to append the
   * restriction's SQL fragment to a larger SQL query being constructed.
   *
   * <p>For example, this method can be used in the context of building a
   * {@code DELETE} or {@code SELECT} statement where restrictions are applied
   * to filter rows based on certain conditions.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   * Restriction restriction = Restriction.equal("age", "30");
   * StringBuilder sqlBuffer = new StringBuilder();
   *
   * restriction.render(sqlBuffer);
   *
   * // The resulting SQL fragment might look like:
   * // "age = 30"
   * System.out.println(sqlBuffer.toString());
   * }</pre>
   *
   * @param sqlBuffer the {@code StringBuilder} to which the SQL fragment
   * of this restriction will be appended. Must not be null.
   */
  void render(StringBuilder sqlBuffer);

  /**
   * Performs a logical AND operation.
   * By default, this method returns {@code true}. It serves as a
   * basic implementation that can be overridden to provide custom
   * logical AND behavior.
   *
   * @return the result of the logical AND operation. By default,
   * this method always returns {@code true}.
   */
  default boolean logicalAnd() {
    return true;
  }

  // Static Factory Methods

  /**
   * Creates a plain SQL restriction using the provided character sequence.
   * This method allows you to directly include custom SQL fragments as restrictions
   * in your query. The resulting {@code Restriction} will render the given sequence
   * as-is when its SQL representation is generated.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   * // Create a plain restriction with a custom SQL fragment
   * Restriction restriction = Restriction.plain("status = 'ACTIVE'");
   *
   * // Use the restriction in a DELETE statement
   * Delete delete = new Delete("users");
   * delete.addColumnRestriction(restriction);
   *
   * // Render the SQL statement
   * String sql = delete.toStatementString(platform);
   *
   * // The resulting SQL might look like:
   * // DELETE FROM users WHERE status = 'ACTIVE'
   * }</pre>
   *
   * @param sequence the character sequence representing the SQL fragment to be used
   * as a restriction. Must not be null.
   * @return a new {@code Restriction} instance that renders the provided sequence
   * as its SQL representation.
   */
  static Restriction plain(CharSequence sequence) {
    return new Plain(sequence);
  }

  /**
   * Creates an equality restriction for the specified column name using a placeholder value (?).
   * This is typically used in prepared statements where the actual value will be provided later.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *   Restriction restriction = Restriction.equal("age");
   *   StringBuilder sqlBuffer = new StringBuilder();
   *   restriction.render(sqlBuffer);
   *   // The resulting SQL fragment might look like:
   *   // "age = ?"
   * }</pre>
   *
   * @param columnName the name of the column to apply the equality restriction to. Must not be null.
   * @return a new {@code Restriction} instance representing the equality condition.
   */
  static Restriction equal(String columnName) {
    return new ComparisonRestriction(columnName, " = ", "?");
  }

  /**
   * Creates an equality restriction between the left-hand side (LHS) and right-hand side (RHS) values.
   * This is used to directly compare two values in a query.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *   Restriction restriction = Restriction.equal("age", "30");
   *   StringBuilder sqlBuffer = new StringBuilder();
   *   restriction.render(sqlBuffer);
   *   // The resulting SQL fragment might look like:
   *   // "age = 30"
   * }</pre>
   *
   * @param lhs the left-hand side value (e.g., column name). Must not be null.
   * @param rhs the right-hand side value (e.g., constant or parameter). Must not be null.
   * @return a new {@code Restriction} instance representing the equality condition.
   */
  static Restriction equal(String lhs, String rhs) {
    return new ComparisonRestriction(lhs, " = ", rhs);
  }

  /**
   * Creates a non-equality restriction for the specified column name using a placeholder value (?).
   * This is typically used in prepared statements where the actual value will be provided later.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *   Restriction restriction = Restriction.notEqual("status");
   *   StringBuilder sqlBuffer = new StringBuilder();
   *   restriction.render(sqlBuffer);
   *   // The resulting SQL fragment might look like:
   *   // "status <> ?"
   * }</pre>
   *
   * @param columnName the name of the column to apply the non-equality restriction to. Must not be null.
   * @return a new {@code Restriction} instance representing the non-equality condition.
   */
  static Restriction notEqual(String columnName) {
    return new ComparisonRestriction(columnName, " <> ", "?");
  }

  /**
   * Creates a non-equality restriction between the left-hand side (LHS) and right-hand side (RHS) values.
   * This is used to directly compare two values in a query for inequality.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *   Restriction restriction = Restriction.notEqual("status", "'ACTIVE'");
   *   StringBuilder sqlBuffer = new StringBuilder();
   *   restriction.render(sqlBuffer);
   *   // The resulting SQL fragment might look like:
   *   // "status <> 'ACTIVE'"
   * }</pre>
   *
   * @param lhs the left-hand side value (e.g., column name). Must not be null.
   * @param rhs the right-hand side value (e.g., constant or parameter). Must not be null.
   * @return a new {@code Restriction} instance representing the non-equality condition.
   */
  static Restriction notEqual(String lhs, String rhs) {
    return new ComparisonRestriction(lhs, " <> ", rhs);
  }

  /**
   * Creates a "greater than" restriction for the specified column name using a placeholder value (?).
   * This is typically used in prepared statements where the actual value will be provided later.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *   Restriction restriction = Restriction.graterThan("salary");
   *   StringBuilder sqlBuffer = new StringBuilder();
   *   restriction.render(sqlBuffer);
   *   // The resulting SQL fragment might look like:
   *   // "salary > ?"
   * }</pre>
   *
   * @param columnName the name of the column to apply the "greater than" restriction to. Must not be null.
   * @return a new {@code Restriction} instance representing the "greater than" condition.
   */
  static Restriction graterThan(String columnName) {
    return graterThan(columnName, "?");
  }

  /**
   * Creates a "greater than" restriction between the left-hand side (LHS) and right-hand side (RHS) values.
   * This is used to directly compare two values in a query.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *   Restriction restriction = Restriction.graterThan("salary", "50000");
   *   StringBuilder sqlBuffer = new StringBuilder();
   *   restriction.render(sqlBuffer);
   *   // The resulting SQL fragment might look like:
   *   // "salary > 50000"
   * }</pre>
   *
   * @param lhs the left-hand side value (e.g., column name). Must not be null.
   * @param rhs the right-hand side value (e.g., constant or parameter). Must not be null.
   * @return a new {@code Restriction} instance representing the "greater than" condition.
   */
  static Restriction graterThan(String lhs, String rhs) {
    return new ComparisonRestriction(lhs, " > ", rhs);
  }

  /**
   * Creates a "greater than or equal to" restriction for the specified column name using a placeholder value (?).
   * This is typically used in prepared statements where the actual value will be provided later.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *   Restriction restriction = Restriction.graterEqual("age");
   *   StringBuilder sqlBuffer = new StringBuilder();
   *   restriction.render(sqlBuffer);
   *   // The resulting SQL fragment might look like:
   *   // "age >= ?"
   * }</pre>
   *
   * @param columnName the name of the column to apply the "greater than or equal to" restriction to. Must not be null.
   * @return a new {@code Restriction} instance representing the "greater than or equal to" condition.
   */
  static Restriction graterEqual(String columnName) {
    return graterEqual(columnName, "?");
  }

  /**
   * Creates a "greater than or equal to" restriction between the left-hand side (LHS) and right-hand side (RHS) values.
   * This is used to directly compare two values in a query.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *   Restriction restriction = Restriction.graterEqual("age", "18");
   *   StringBuilder sqlBuffer = new StringBuilder();
   *   restriction.render(sqlBuffer);
   *   // The resulting SQL fragment might look like:
   *   // "age >= 18"
   * }</pre>
   *
   * @param lhs the left-hand side value (e.g., column name). Must not be null.
   * @param rhs the right-hand side value (e.g., constant or parameter). Must not be null.
   * @return a new {@code Restriction} instance representing the "greater than or equal to" condition.
   */
  static Restriction graterEqual(String lhs, String rhs) {
    return new ComparisonRestriction(lhs, " >= ", rhs);
  }

  /**
   * Creates a "less than" restriction for the specified column name using a placeholder value (?).
   * This is typically used in prepared statements where the actual value will be provided later.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *   Restriction restriction = Restriction.lessThan("price");
   *   StringBuilder sqlBuffer = new StringBuilder();
   *   restriction.render(sqlBuffer);
   *   // The resulting SQL fragment might look like:
   *   // "price < ?"
   * }</pre>
   *
   * @param columnName the name of the column to apply the "less than" restriction to. Must not be null.
   * @return a new {@code Restriction} instance representing the "less than" condition.
   */
  static Restriction lessThan(String columnName) {
    return lessThan(columnName, "?");
  }

  /**
   * Creates a "less than" restriction between the left-hand side (LHS) and right-hand side (RHS) values.
   * This is used to directly compare two values in a query.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *   Restriction restriction = Restriction.lessThan("price", "100");
   *   StringBuilder sqlBuffer = new StringBuilder();
   *   restriction.render(sqlBuffer);
   *   // The resulting SQL fragment might look like:
   *   // "price < 100"
   * }</pre>
   *
   * @param lhs the left-hand side value (e.g., column name). Must not be null.
   * @param rhs the right-hand side value (e.g., constant or parameter). Must not be null.
   * @return a new {@code Restriction} instance representing the "less than" condition.
   */
  static Restriction lessThan(String lhs, String rhs) {
    return new ComparisonRestriction(lhs, " < ", rhs);
  }

  /**
   * Creates a "less than or equal to" restriction for the specified column name using a placeholder value (?).
   * This is typically used in prepared statements where the actual value will be provided later.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *   Restriction restriction = Restriction.lessEqual("quantity");
   *   StringBuilder sqlBuffer = new StringBuilder();
   *   restriction.render(sqlBuffer);
   *   // The resulting SQL fragment might look like:
   *   // "quantity <= ?"
   * }</pre>
   *
   * @param columnName the name of the column to apply the "less than or equal to" restriction to. Must not be null.
   * @return a new {@code Restriction} instance representing the "less than or equal to" condition.
   */
  static Restriction lessEqual(String columnName) {
    return lessEqual(columnName, "?");
  }

  /**
   * Creates a "less than or equal to" restriction between the left-hand side (LHS) and right-hand side (RHS) values.
   * This is used to directly compare two values in a query.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *   Restriction restriction = Restriction.lessEqual("quantity", "50");
   *   StringBuilder sqlBuffer = new StringBuilder();
   *   restriction.render(sqlBuffer);
   *   // The resulting SQL fragment might look like:
   *   // "quantity <= 50"
   * }</pre>
   *
   * @param lhs the left-hand side value (e.g., column name). Must not be null.
   * @param rhs the right-hand side value (e.g., constant or parameter). Must not be null.
   * @return a new {@code Restriction} instance representing the "less than or equal to" condition.
   */
  static Restriction lessEqual(String lhs, String rhs) {
    return new ComparisonRestriction(lhs, " <= ", rhs);
  }

  /**
   * Creates a custom restriction using the specified operator between the left-hand side (LHS) and right-hand side (RHS) values.
   * This allows for flexible query generation with custom operators such as LIKE, IN, or BETWEEN.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *   Restriction restriction = Restriction.forOperator("name", "LIKE", "'%John%'");
   *   StringBuilder sqlBuffer = new StringBuilder();
   *   restriction.render(sqlBuffer);
   *   // The resulting SQL fragment might look like:
   *   // "name LIKE '%John%'"
   * }</pre>
   *
   * @param lhs the left-hand side value (e.g., column name). Must not be null.
   * @param operator the custom SQL operator to use (e.g., LIKE, IN, BETWEEN). Must not be null.
   * @param rhs the right-hand side value (e.g., constant or parameter). Must not be null.
   * @return a new {@code Restriction} instance representing the custom condition.
   */
  static Restriction forOperator(String lhs, String operator, String rhs) {
    return new ComparisonRestriction(lhs, operator, rhs);
  }

  /**
   * Creates a restriction to check if the specified column is null.
   * This is used to generate SQL conditions such as "IS NULL".
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *   Restriction restriction = Restriction.isNull("email");
   *   StringBuilder sqlBuffer = new StringBuilder();
   *   restriction.render(sqlBuffer);
   *   // The resulting SQL fragment might look like:
   *   // "email IS NULL"
   * }</pre>
   *
   * @param columnName the name of the column to apply the "IS NULL" restriction to. Must not be null.
   * @return a new {@code Restriction} instance representing the "IS NULL" condition.
   */
  static Restriction isNull(String columnName) {
    return new NullnessRestriction(columnName, true);
  }

  /**
   * Creates a restriction to check if the specified column is not null.
   * This is used to generate SQL conditions such as "IS NOT NULL".
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *   Restriction restriction = Restriction.isNotNull("email");
   *   StringBuilder sqlBuffer = new StringBuilder();
   *   restriction.render(sqlBuffer);
   *   // The resulting SQL fragment might look like:
   *   // "email IS NOT NULL"
   * }</pre>
   *
   * @param columnName the name of the column to apply the "IS NOT NULL" restriction to. Must not be null.
   * @return a new {@code Restriction} instance representing the "IS NOT NULL" condition.
   */
  static Restriction isNotNull(String columnName) {
    return new NullnessRestriction(columnName, false);
  }

  /**
   * Creates a custom BETWEEN restriction.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *   Restriction restriction = Restriction.between("age");
   *   StringBuilder sqlBuffer = new StringBuilder();
   *   restriction.render(sqlBuffer);
   *   // The resulting SQL fragment might look like:
   *   // "`age` BETWEEN ? AND ?"
   * }</pre>
   *
   * @param columnName the name of the column to apply the "between" restriction to. Must not be null.
   * @return a new {@code Restriction} instance representing the custom condition.
   */
  static Restriction between(String columnName) {
    return forOperator(columnName, " BETWEEN", " ? AND ?");
  }

  /**
   * Creates a custom NOT-BETWEEN restriction.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *   Restriction restriction = Restriction.notBetween("age");
   *   StringBuilder sqlBuffer = new StringBuilder();
   *   restriction.render(sqlBuffer);
   *   // The resulting SQL fragment might look like:
   *   // "`age` NOT BETWEEN ? AND ?"
   * }</pre>
   *
   * @param columnName the name of the column to apply the "between" restriction to. Must not be null.
   * @return a new {@code Restriction} instance representing the custom condition.
   */
  static Restriction notBetween(String columnName) {
    return forOperator(columnName, " NOT BETWEEN", " ? AND ?");
  }

  /**
   * Combines two {@code Restriction} objects using a logical AND operation.
   * This method creates a new {@code LogicalRestriction} that represents the
   * conjunction of the two input restrictions.
   *
   * <p>Example usage:
   * <pre>{@code
   * Restriction restriction1 = ...;
   * Restriction restriction2 = ...;
   * Restriction combined = Restriction.and(restriction1, restriction2);
   *
   * // The resulting `combined` restriction can be used in further operations
   * }</pre>
   *
   * @param lhs the left-hand side {@code Restriction} to be combined
   * @param rhs the right-hand side {@code Restriction} to be combined
   * @return a new {@code Restriction} object representing the logical AND
   * of the two input restrictions
   * @since 5.0
   */
  static Restriction and(Restriction lhs, Restriction rhs) {
    return new LogicalRestriction(lhs, true, rhs);
  }

  /**
   * Creates a new {@code Restriction} that represents a logical OR operation
   * with the given {@code Restriction}. This method is typically used in the
   * context of building dynamic SQL queries where conditions need to be combined.
   *
   * <p>Example usage:
   * <pre>{@code
   *
   *  Restriction orCondition = Restriction.or(Restriction.plain("column1 = 'value1'"));
   * StringBuilder sql = new StringBuilder();
   * orCondition.render(sql);
   * System.out.println(sql.toString()); // OR column1 = 'value1'
   * }</pre>
   *
   * @param rhs the right-hand side {@code Restriction} to be combined with the current one
   * using a logical OR operation. Must not be null.
   * @return a new {@code Restriction} instance that, when rendered, applies the logical OR
   * operation between the current restriction and the provided one.
   * @since 5.0
   */
  static Restriction or(Restriction rhs) {
    return new Restriction() {

      @Override
      public void render(StringBuilder sqlBuffer) {
        rhs.render(sqlBuffer);
      }

      @Override
      public boolean logicalAnd() {
        return false;
      }

    };
  }

  /**
   * Combines two restrictions into a logical OR operation.
   *
   * This method creates a new {@code LogicalRestriction} that represents the logical
   * OR of the given left-hand side (lhs) and right-hand side (rhs) restrictions.
   * It can be used to build complex restriction logic in a fluent manner.
   *
   * Example usage:
   * <pre>
   *   Restriction restriction1 = ...;
   *   Restriction restriction2 = ...;
   *
   *   Restriction combined = Restriction.or(restriction1, restriction2);
   *
   *   // The resulting 'combined' restriction will evaluate to true if either
   *   // restriction1 or restriction2 evaluates to true.
   * </pre>
   *
   * @param lhs the left-hand side restriction to be combined
   * @param rhs the right-hand side restriction to be combined
   * @return a new {@code Restriction} instance representing the logical OR
   * of the two input restrictions
   * @since 5.0
   */
  static Restriction or(Restriction lhs, Restriction rhs) {
    return new LogicalRestriction(lhs, false, rhs);
  }

  /**
   * Renders a collection of restrictions into the provided SQL buffer, prefixing them with "WHERE" if the collection is not empty.
   * This is useful for constructing SQL queries with multiple conditions.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *   List<Restriction> restrictions = Arrays.asList(
   *       Restriction.equal("age", "30"),
   *       Restriction.isNull("email")
   *   );
   *   StringBuilder sqlBuffer = new StringBuilder();
   *   Restriction.render(restrictions, sqlBuffer);
   *   // The resulting SQL fragment might look like:
   *   // " WHERE age = 30 AND email IS NULL"
   * }</pre>
   *
   * @param restrictions the collection of restrictions to render. May be null or empty.
   * @param buf the {@code StringBuilder} to which the SQL fragment will be appended. Must not be null.
   */
  static void render(@Nullable Collection<? extends Restriction> restrictions, StringBuilder buf) {
    if (CollectionUtils.isNotEmpty(restrictions)) {
      buf.append(" WHERE ");
      renderWhereClause(restrictions, buf);
    }
  }

  /**
   * Renders a collection of restrictions into a new {@code StringBuilder}, returning it if the collection is not empty.
   * This is useful for generating standalone SQL fragments for multiple conditions.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *   List<Restriction> restrictions = Arrays.asList(
   *       Restriction.equal("age", "30"),
   *       Restriction.isNull("email")
   *   );
   *   StringBuilder sqlBuffer = Restriction.renderWhereClause(restrictions);
   *   if (sqlBuffer != null) {
   *     System.out.println(sqlBuffer.toString());
   *     // Output: "age = 30 AND email IS NULL"
   *   }
   * }</pre>
   *
   * @param restrictions the collection of restrictions to render. May be null or empty.
   * @return a new {@code StringBuilder} containing the rendered SQL fragment, or null if the collection is empty.
   */
  @Nullable
  static StringBuilder renderWhereClause(@Nullable Collection<? extends Restriction> restrictions) {
    if (CollectionUtils.isNotEmpty(restrictions)) {
      StringBuilder buf = new StringBuilder(restrictions.size() * 10);
      renderWhereClause(restrictions, buf);
      return buf;
    }
    return null;
  }

  /**
   * Renders a collection of restrictions into the provided SQL buffer, separating them with "AND".
   * This is useful for combining multiple conditions into a single SQL fragment.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *   List<Restriction> restrictions = Arrays.asList(
   *       Restriction.equal("age", "30"),
   *       Restriction.isNull("email")
   *   );
   *   StringBuilder sqlBuffer = new StringBuilder();
   *   Restriction.renderWhereClause(restrictions, sqlBuffer);
   *   // The resulting SQL fragment might look like:
   *   // "age = 30 AND email IS NULL"
   * }</pre>
   *
   * @param restrictions the collection of restrictions to render. Must not be null or empty.
   * @param buf the {@code StringBuilder} to which the SQL fragment will be appended. Must not be null.
   */
  static void renderWhereClause(Collection<? extends Restriction> restrictions, StringBuilder buf) {
    boolean appended = false;
    for (Restriction restriction : restrictions) {
      if (appended) {
        buf.append(restriction.logicalAnd() ? " AND " : " OR ");
      }
      else {
        appended = true;
      }
      restriction.render(buf);
    }
  }

  class Plain implements Restriction {

    private final CharSequence sequence;

    Plain(CharSequence sequence) {
      this.sequence = sequence;
    }

    @Override
    public void render(StringBuilder sqlBuffer) {
      sqlBuffer.append(sequence);
    }

  }

}
