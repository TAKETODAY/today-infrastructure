/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc.persistence;

import cn.taketoday.jdbc.persistence.dialect.Dialect;
import cn.taketoday.util.StringUtils;

/**
 * A join that appears in a translated HQL query
 *
 * @author Gavin King
 */
public class QueryJoinFragment extends JoinFragment {

  private StringBuilder afterFrom = new StringBuilder();
  private StringBuilder afterWhere = new StringBuilder();
  private final Dialect dialect;
  private final boolean useThetaStyleInnerJoins;

  public QueryJoinFragment(Dialect dialect, boolean useThetaStyleInnerJoins) {
    this.dialect = dialect;
    this.useThetaStyleInnerJoins = useThetaStyleInnerJoins;
  }

  public void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, JoinType joinType) {
    addJoin(tableName, alias, alias, fkColumns, pkColumns, joinType, null);
  }

  public void addJoin(String tableName, String alias, String[] fkColumns, String[] pkColumns, JoinType joinType, String on) {
    addJoin(tableName, alias, alias, fkColumns, pkColumns, joinType, on);
  }

  public void addJoin(String tableName, String alias, String[][] fkColumns, String[] pkColumns, JoinType joinType) {
    addJoin(tableName, alias, alias, fkColumns, pkColumns, joinType, null);
  }

  public void addJoin(String tableName, String alias, String[][] fkColumns, String[] pkColumns, JoinType joinType, String on) {
    addJoin(tableName, alias, alias, fkColumns, pkColumns, joinType, on);
  }

  private void addJoin(String tableName, String alias, String concreteAlias, String[] fkColumns, String[] pkColumns, JoinType joinType, String on) {
    if (!useThetaStyleInnerJoins || joinType != JoinType.INNER_JOIN) {
      JoinFragment jf = dialect.createOuterJoinFragment();
      jf.addJoin(tableName, alias, fkColumns, pkColumns, joinType, on);
      addFragment(jf);
    }
    else {
      addCrossJoin(tableName, alias);
      addCondition(concreteAlias, fkColumns, pkColumns);
      addCondition(on);
    }
  }

  private void addJoin(String tableName, String alias, String concreteAlias, String[][] fkColumns, String[] pkColumns, JoinType joinType, String on) {
    if (!useThetaStyleInnerJoins || joinType != JoinType.INNER_JOIN) {
      JoinFragment jf = dialect.createOuterJoinFragment();
      jf.addJoin(tableName, alias, fkColumns, pkColumns, joinType, on);
      addFragment(jf);
    }
    else {
      addCrossJoin(tableName, alias);
      addCondition(concreteAlias, fkColumns, pkColumns);
      addCondition(on);
    }
  }

  public String toFromFragmentString() {
    return afterFrom.toString();
  }

  public String toWhereFragmentString() {
    return afterWhere.toString();
  }

  public void addJoins(String fromFragment, String whereFragment) {
    afterFrom.append(fromFragment);
    afterWhere.append(whereFragment);
  }

  public JoinFragment copy() {
    QueryJoinFragment copy = new QueryJoinFragment(dialect, useThetaStyleInnerJoins);
    copy.afterFrom = new StringBuilder(afterFrom.toString());
    copy.afterWhere = new StringBuilder(afterWhere.toString());
    return copy;
  }

  public void addCondition(String alias, String[] columns, String condition) {
    for (String column : columns) {
      afterWhere.append(" and ")
              .append(alias)
              .append('.')
              .append(column)
              .append(condition);
    }
  }

  public void addCrossJoin(String tableName, String alias) {
    afterFrom.append(", ")
            .append(tableName)
            .append(' ')
            .append(alias);
  }

  public void addCondition(String alias, String[] fkColumns, String[] pkColumns) {
    for (int j = 0; j < fkColumns.length; j++) {
      afterWhere.append(" and ")
              .append(fkColumns[j])
              .append('=')
              .append(alias)
              .append('.')
              .append(pkColumns[j]);
    }
  }

  public void addCondition(String alias, String[][] fkColumns, String[] pkColumns) {
    afterWhere.append(" and ");
    if (fkColumns.length > 1) {
      afterWhere.append("(");
    }
    for (int i = 0; i < fkColumns.length; i++) {
      for (int j = 0; j < fkColumns[i].length; j++) {
        afterWhere.append(fkColumns[i][j])
                .append('=')
                .append(alias)
                .append('.')
                .append(pkColumns[j]);
        if (j < fkColumns[i].length - 1) {
          afterWhere.append(" and ");
        }
      }
      if (i < fkColumns.length - 1) {
        afterWhere.append(" or ");
      }
    }
    if (fkColumns.length > 1) {
      afterWhere.append(")");
    }
  }

  /**
   * Add the condition string to the join fragment.
   *
   * @param condition
   * @return true if the condition was added, false if it was already in the fragment.
   */
  public boolean addCondition(String condition) {
    // if the condition is not already there...
    if (!StringUtils.isEmpty(condition)
            && !afterFrom.toString().contains(condition.trim())
            && !afterWhere.toString().contains(condition.trim())) {
      if (!condition.startsWith(" and ")) {
        afterWhere.append(" and ");
      }
      afterWhere.append(condition);
      return true;
    }
    else {
      return false;
    }
  }

  public void addFromFragmentString(String fromFragmentString) {
    afterFrom.append(fromFragmentString);
  }

  public void clearWherePart() {
    afterWhere.setLength(0);
  }
}
