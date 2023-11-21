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

package cn.taketoday.jdbc.support.incrementer;

import javax.sql.DataSource;

import cn.taketoday.lang.Assert;

/**
 * Abstract base class for {@link DataFieldMaxValueIncrementer} implementations that use
 * a column in a custom sequence table. Subclasses need to provide the specific handling
 * of that table in their {@link #getNextKey()} implementation.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class AbstractColumnMaxValueIncrementer extends AbstractDataFieldMaxValueIncrementer {

  /** The name of the column for this sequence. */
  private String columnName;

  /** The number of keys buffered in a cache. */
  private int cacheSize = 1;

  /**
   * Default constructor for bean property style usage.
   *
   * @see #setDataSource
   * @see #setIncrementerName
   * @see #setColumnName
   */
  public AbstractColumnMaxValueIncrementer() { }

  /**
   * Convenience constructor.
   *
   * @param dataSource the DataSource to use
   * @param incrementerName the name of the sequence/table to use
   * @param columnName the name of the column in the sequence table to use
   */
  public AbstractColumnMaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName) {
    super(dataSource, incrementerName);
    Assert.notNull(columnName, "Column name is required");
    this.columnName = columnName;
  }

  /**
   * Set the name of the column in the sequence table.
   */
  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  /**
   * Return the name of the column in the sequence table.
   */
  public String getColumnName() {
    return this.columnName;
  }

  /**
   * Set the number of buffered keys.
   */
  public void setCacheSize(int cacheSize) {
    this.cacheSize = cacheSize;
  }

  /**
   * Return the number of buffered keys.
   */
  public int getCacheSize() {
    return this.cacheSize;
  }

  @Override
  public void afterPropertiesSet() {
    super.afterPropertiesSet();
    if (this.columnName == null) {
      throw new IllegalArgumentException("Property 'columnName' is required");
    }
  }

}
