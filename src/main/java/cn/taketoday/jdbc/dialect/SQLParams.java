/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.jdbc.dialect;

import java.util.List;
import java.util.Map;

/**
 * @author TODAY 2021/10/10 13:16
 * @since 4.0
 */
public class SQLParams {

  private Class<?> modelClass;

  private Object model;

  private String selectColumns;
  private String tableName;
  private String pkName;
  private StringBuilder conditionSQL;

  private Map<String, Object> updateColumns;

  private List<String> excludedColumns;

  private PageRow pageRow;
  private String orderBy;
  private boolean isSQLLimit;

  private String customSQL;

  public Class<?> getModelClass() {
    return modelClass;
  }

  public void setModelClass(Class<?> modelClass) {
    this.modelClass = modelClass;
  }

  public Object getModel() {
    return model;
  }

  public void setModel(Object model) {
    this.model = model;
  }

  public String getSelectColumns() {
    return selectColumns;
  }

  public void setSelectColumns(String selectColumns) {
    this.selectColumns = selectColumns;
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getPkName() {
    return pkName;
  }

  public void setPkName(String pkName) {
    this.pkName = pkName;
  }

  public StringBuilder getConditionSQL() {
    return conditionSQL;
  }

  public void setConditionSQL(StringBuilder conditionSQL) {
    this.conditionSQL = conditionSQL;
  }

  public Map<String, Object> getUpdateColumns() {
    return updateColumns;
  }

  public void setUpdateColumns(Map<String, Object> updateColumns) {
    this.updateColumns = updateColumns;
  }

  public List<String> getExcludedColumns() {
    return excludedColumns;
  }

  public void setExcludedColumns(List<String> excludedColumns) {
    this.excludedColumns = excludedColumns;
  }

  public PageRow getPageRow() {
    return pageRow;
  }

  public void setPageRow(PageRow pageRow) {
    this.pageRow = pageRow;
  }

  public String getOrderBy() {
    return orderBy;
  }

  public void setOrderBy(String orderBy) {
    this.orderBy = orderBy;
  }

  public boolean isSQLLimit() {
    return isSQLLimit;
  }

  public void setSQLLimit(boolean SQLLimit) {
    isSQLLimit = SQLLimit;
  }

  public String getCustomSQL() {
    return customSQL;
  }

  public void setCustomSQL(String customSQL) {
    this.customSQL = customSQL;
  }
}
