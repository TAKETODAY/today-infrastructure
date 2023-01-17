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

package cn.taketoday.jdbc.persistence.dialect;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.jdbc.persistence.ANSICaseFragment;
import cn.taketoday.jdbc.persistence.ANSIJoinFragment;
import cn.taketoday.jdbc.persistence.CaseFragment;
import cn.taketoday.jdbc.persistence.JoinFragment;
import cn.taketoday.jdbc.result.JdbcBeanMetadata;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * SQL generate strategy
 *
 * @author TODAY 2021/10/10 13:11
 * @since 4.0
 */
public abstract class Dialect {
  /**
   * Defines a default batch size constant
   */
  public static final String DEFAULT_BATCH_SIZE = "15";

  /**
   * Defines a "no batching" batch size constant
   */
  public static final String NO_BATCH = "0";

  /**
   * Characters used as opening for quoting SQL identifiers
   */
  public static final String QUOTE = "`\"[";

  /**
   * Characters used as closing for quoting SQL identifiers
   */
  public static final String CLOSED_QUOTE = "`\"]";
  private static final Pattern SINGLE_QUOTE_PATTERN = Pattern.compile(
          "'",
          Pattern.LITERAL
  );

  private static final Pattern ESCAPE_CLOSING_COMMENT_PATTERN = Pattern.compile("\\*/");
  private static final Pattern ESCAPE_OPENING_COMMENT_PATTERN = Pattern.compile("/\\*");

  public static String escapeComment(String comment) {
    if (StringUtils.isNotEmpty(comment)) {
      final String escaped = ESCAPE_CLOSING_COMMENT_PATTERN.matcher(comment).replaceAll("*\\\\/");
      return ESCAPE_OPENING_COMMENT_PATTERN.matcher(escaped).replaceAll("/\\\\*");
    }
    return comment;
  }

  /**
   * determine the appropriate for update fragment to use.
   *
   * @return The appropriate for update fragment.
   */
  public String getForUpdateString() {
    return " for update";
  }

  public String select(SQLParams sqlParams) {
    StringBuilder sql = new StringBuilder();
    if (StringUtils.isNotEmpty(sqlParams.getCustomSQL())) {
      sql.append(sqlParams.getCustomSQL());
    }
    else {
      sql.append("SELECT");
      if (StringUtils.isNotEmpty(sqlParams.getSelectColumns())) {
        sql.append(' ').append(sqlParams.getSelectColumns()).append(' ');
      }
      else if (CollectionUtils.isNotEmpty(sqlParams.getExcludedColumns())) {
        sql.append(' ').append(buildColumns(sqlParams.getExcludedColumns(), sqlParams.getModelClass())).append(' ');
      }
      else {
        sql.append(" * ");
      }
      sql.append("FROM ").append(sqlParams.getTableName());
      if (sqlParams.getConditionSQL().length() > 0) {
        sql.append(" WHERE ").append(sqlParams.getConditionSQL().substring(5));
      }
    }

    if (StringUtils.isNotEmpty(sqlParams.getOrderBy())) {
      sql.append(" ORDER BY").append(sqlParams.getOrderBy());
    }
    if (sqlParams.isSQLLimit()) {
      sql.append(" LIMIT ?");
    }
    return sql.toString();
  }

  public String count(SQLParams sqlParams) {
    StringBuilder sql = new StringBuilder();
    sql.append("SELECT COUNT(*) FROM ").append(sqlParams.getTableName());
    if (sqlParams.getConditionSQL().length() > 0) {
      sql.append(" WHERE ").append(sqlParams.getConditionSQL().substring(5));
    }
    return sql.toString();
  }

  public String update(SQLParams sqlParams) {
    StringBuilder sql = new StringBuilder();
    sql.append("UPDATE ").append(sqlParams.getTableName()).append(" SET ");

    StringBuilder setSQL = new StringBuilder();

    Map<String, Object> updateColumns = sqlParams.getUpdateColumns();
    if (CollectionUtils.isNotEmpty(updateColumns)) {
      updateColumns.forEach((key, value) -> setSQL.append(key).append(" = ?, "));
    }
    else {
      Object model = sqlParams.getModel();
      if (model != null) {
        JdbcBeanMetadata beanMetadata = new JdbcBeanMetadata(sqlParams.getModelClass());
        for (BeanProperty property : beanMetadata) {
          Object value = property.getValue(model);
          if (value == null) {
            continue;
          }
          setSQL.append(property.getName()).append(" = ?, ");
        }
      }
    }
    sql.append(setSQL.substring(0, setSQL.length() - 2));
    if (sqlParams.getConditionSQL().length() > 0) {
      sql.append(" WHERE ").append(sqlParams.getConditionSQL().substring(5));
    }
    return sql.toString();
  }

  public String delete(SQLParams sqlParams) {
    StringBuilder sql = new StringBuilder();
    sql.append("DELETE FROM ").append(sqlParams.getTableName());

    if (sqlParams.getConditionSQL().length() > 0) {
      sql.append(" WHERE ").append(sqlParams.getConditionSQL().substring(5));
    }
    else {
      if (null != sqlParams.getModel()) {
        StringBuilder columnNames = new StringBuilder();
        JdbcBeanMetadata beanMetadata = new JdbcBeanMetadata(sqlParams.getModelClass());
        for (BeanProperty property : beanMetadata) {
          Object value = property.getValue(sqlParams.getModel());
          if (value == null) {
            continue;
          }
          columnNames.append(property.getName()).append(" = ? and ");
        }
        if (columnNames.length() > 0) {
          sql.append(" WHERE ").append(columnNames.substring(0, columnNames.length() - 5));
        }
      }
    }
    return sql.toString();
  }

  protected abstract String pagination(SQLParams sqlParams);

  public static String buildColumns(List<String> excludedColumns, Class<?> model) {
    StringBuilder sql = new StringBuilder();
    JdbcBeanMetadata beanMetadata = new JdbcBeanMetadata(model);
    for (BeanProperty property : beanMetadata) {
      String alias = property.getName();
      if (excludedColumns.contains(alias)) {
        sql.append(alias).append(',');
      }
    }
    if (sql.length() > 0) {
      return sql.substring(0, sql.length() - 1);
    }
    return "*";
  }

  /**
   * The fragment used to insert a row without specifying any column values.
   * This is not possible on some databases.
   *
   * @return The appropriate empty values clause.
   */
  public String getNoColumnsInsertString() {
    return "values ( )";
  }

  /**
   * Create a {@link JoinFragment} strategy responsible
   * for handling this dialect's variations in how joins are handled.
   *
   * @return This dialect's {@link JoinFragment} strategy.
   */
  public JoinFragment createOuterJoinFragment() {
    return new ANSIJoinFragment();
  }

  /**
   * Create a {@link CaseFragment} strategy responsible
   * for handling this dialect's variations in how CASE statements are
   * handled.
   *
   * @return This dialect's {@link CaseFragment} strategy.
   */
  public CaseFragment createCaseFragment() {
    return new ANSICaseFragment();
  }

}
