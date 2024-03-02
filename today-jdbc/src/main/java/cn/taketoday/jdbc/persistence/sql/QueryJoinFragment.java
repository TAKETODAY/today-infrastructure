/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.jdbc.persistence.sql;

import cn.taketoday.jdbc.persistence.dialect.Platform;
import cn.taketoday.util.StringUtils;

/**
 * A join that appears in a translated HQL query
 *
 * @author Gavin King
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class QueryJoinFragment extends JoinFragment {

  private StringBuilder afterFrom = new StringBuilder();
  private StringBuilder afterWhere = new StringBuilder();

  private final Platform platform;

  private final boolean useThetaStyleInnerJoins;

  public QueryJoinFragment(Platform platform, boolean useThetaStyleInnerJoins) {
    this.platform = platform;
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
      JoinFragment jf = platform.createOuterJoinFragment();
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
      JoinFragment jf = platform.createOuterJoinFragment();
      jf.addJoin(tableName, alias, fkColumns, pkColumns, joinType, on);
      addFragment(jf);
    }
    else {
      addCrossJoin(tableName, alias);
      addCondition(concreteAlias, fkColumns, pkColumns);
      addCondition(on);
    }
  }

  @Override
  public String toFromFragmentString() {
    return afterFrom.toString();
  }

  @Override
  public String toWhereFragmentString() {
    return afterWhere.toString();
  }

  @Override
  public void addJoins(String fromFragment, String whereFragment) {
    afterFrom.append(fromFragment);
    afterWhere.append(whereFragment);
  }

  @Override
  public JoinFragment copy() {
    QueryJoinFragment copy = new QueryJoinFragment(platform, useThetaStyleInnerJoins);
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

  @Override
  public void addCrossJoin(String tableName, String alias) {
    afterFrom.append(", ")
            .append(tableName)
            .append(' ')
            .append(alias);
  }

  @Override
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
   * @return true if the condition was added, false if it was already in the fragment.
   */
  @Override
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
