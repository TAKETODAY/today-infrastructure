/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.jdbc.sql;

/**
 * Represents a selection criteria for rows in a JDBC {@link java.sql.ResultSet}
 *
 * @author Gavin King
 */
public final class RowSelection {
  private Integer firstRow;
  private Integer maxRows;
  private Integer timeout;
  private Integer fetchSize;

  public void setFirstRow(Integer firstRow) {
    if (firstRow != null && firstRow < 0) {
      throw new IllegalArgumentException("first-row value cannot be negative : " + firstRow);
    }
    this.firstRow = firstRow;
  }

  public void setFirstRow(int firstRow) {
    this.firstRow = firstRow;
  }

  public Integer getFirstRow() {
    return firstRow;
  }

  public void setMaxRows(Integer maxRows) {
    this.maxRows = maxRows;
  }

  public void setMaxRows(int maxRows) {
    this.maxRows = maxRows;
  }

  public Integer getMaxRows() {
    return maxRows;
  }

  public void setTimeout(Integer timeout) {
    this.timeout = timeout;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  public Integer getTimeout() {
    return timeout;
  }

  public Integer getFetchSize() {
    return fetchSize;
  }

  public void setFetchSize(Integer fetchSize) {
    this.fetchSize = fetchSize;
  }

  public void setFetchSize(int fetchSize) {
    this.fetchSize = fetchSize;
  }

  public boolean definesLimits() {
    return maxRows != null || (firstRow != null && firstRow <= 0);
  }

}
