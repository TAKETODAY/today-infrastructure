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

/**
 * An ANSI-style join.
 *
 * @author Gavin King
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ANSIJoinFragment extends JoinFragment {
  private StringBuilder buffer = new StringBuilder();
  private final StringBuilder conditions = new StringBuilder();

  /**
   * Adds a join, represented by the given information, to the fragment.
   *
   * @param tableName The name of the table being joined.
   * @param alias The alias applied to the table being joined.
   * @param fkColumns The columns (from the table being joined) used to define the join-restriction (the ON)
   * @param pkColumns The columns (from the table being joined to) used to define the join-restriction (the ON)
   * @param joinType The type of join to produce (INNER, etc).
   */
  public void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, JoinType joinType) {
    addJoin(tableName, alias, fkColumns, pkColumns, joinType, null);
  }

  /**
   * Adds a join, represented by the given information, to the fragment.
   *
   * @param rhsTableName The name of the table being joined (the RHS table).
   * @param rhsAlias The alias applied to the table being joined (the alias for the RHS table).
   * @param lhsColumns The columns (from the table being joined) used to define the join-restriction (the ON).  These
   * are the LHS columns, and are expected to be qualified.
   * @param rhsColumns The columns (from the table being joined to) used to define the join-restriction (the ON).  These
   * are the RHS columns and are expected to *not* be qualified.
   * @param joinType The type of join to produce (INNER, etc).
   * @param on Any extra join restrictions
   */
  public void addJoin(String rhsTableName, String rhsAlias, String[] lhsColumns, String[] rhsColumns, JoinType joinType, @Nullable String on) {
    String joinString = switch (joinType) {
      case INNER_JOIN -> " inner join ";
      case LEFT_OUTER_JOIN -> " left outer join ";
      case RIGHT_OUTER_JOIN -> " right outer join ";
      case FULL_JOIN -> " full outer join ";
      default -> throw new IllegalArgumentException("undefined join type: " + joinType);
    };

    buffer.append(joinString)
            .append(rhsTableName)
            .append(' ')
            .append(rhsAlias)
            .append(" on ");

    for (int j = 0; j < lhsColumns.length; j++) {
      buffer.append(lhsColumns[j])
              .append('=')
              .append(rhsAlias)
              .append('.')
              .append(rhsColumns[j]);
      if (j < lhsColumns.length - 1) {
        buffer.append(" and ");
      }
    }

    addCondition(buffer, on);

  }

  public void addJoin(String rhsTableName, String rhsAlias,
          String[][] lhsColumns, String[] rhsColumns, JoinType joinType, @Nullable String on) {
    final String joinString = switch (joinType) {
      case INNER_JOIN -> " inner join ";
      case LEFT_OUTER_JOIN -> " left outer join ";
      case RIGHT_OUTER_JOIN -> " right outer join ";
      case FULL_JOIN -> " full outer join ";
      default -> throw new IllegalArgumentException("undefined join type: " + joinType);
    };

    this.buffer.append(joinString)
            .append(rhsTableName)
            .append(' ')
            .append(rhsAlias)
            .append(" on ");

    if (lhsColumns.length > 1) {
      this.buffer.append("(");
    }
    for (int i = 0; i < lhsColumns.length; i++) {
      for (int j = 0; j < lhsColumns[i].length; j++) {
        this.buffer.append(lhsColumns[i][j])
                .append('=')
                .append(rhsAlias)
                .append('.')
                .append(rhsColumns[j]);
        if (j < lhsColumns[i].length - 1) {
          this.buffer.append(" and ");
        }
      }
      if (i < lhsColumns.length - 1) {
        this.buffer.append(" or ");
      }
    }
    if (lhsColumns.length > 1) {
      this.buffer.append(")");
    }

    addCondition(buffer, on);
  }

  @Override
  public String toFromFragmentString() {
    return this.buffer.toString();
  }

  @Override
  public String toWhereFragmentString() {
    return this.conditions.toString();
  }

  @Override
  public void addJoins(String fromFragment, String whereFragment) {
    this.buffer.append(fromFragment);
    //where fragment must be empty!
  }

  @Override
  public JoinFragment copy() {
    final ANSIJoinFragment copy = new ANSIJoinFragment();
    copy.buffer = new StringBuilder(this.buffer.toString());
    return copy;
  }

  /**
   * Adds a condition to the join fragment.  For each given column a predicate is built in the form:
   * {@code [alias.[column] = [condition]}
   *
   * @param alias The alias to apply to column(s)
   * @param columns The columns to apply restriction
   * @param condition The restriction condition
   */
  public void addCondition(String alias, String[] columns, String condition) {
    for (String column : columns) {
      this.conditions.append(" and ")
              .append(alias)
              .append('.')
              .append(column)
              .append(condition);
    }
  }

  @Override
  public void addCrossJoin(String tableName, String alias) {
    this.buffer.append(", ")
            .append(tableName)
            .append(' ')
            .append(alias);
  }

  @Override
  public void addCondition(String alias, String[] fkColumns, String[] pkColumns) {
    throw new UnsupportedOperationException();

  }

  @Override
  public boolean addCondition(@Nullable String condition) {
    return addCondition(conditions, condition);
  }

  /**
   * Adds an externally built join fragment.
   *
   * @param fromFragmentString The join fragment string
   */
  public void addFromFragmentString(String fromFragmentString) {
    this.buffer.append(fromFragmentString);
  }

}
