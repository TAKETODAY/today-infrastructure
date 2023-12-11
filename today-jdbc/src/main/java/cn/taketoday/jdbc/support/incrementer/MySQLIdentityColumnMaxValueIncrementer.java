/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.jdbc.support.incrementer;

import javax.sql.DataSource;

/**
 * {@link DataFieldMaxValueIncrementer} that increments the maximum counter value of an
 * auto-increment column of a given MySQL table.
 *
 * <p>The sequence is kept in a table. The storage engine used by the sequence table must be
 * InnoDB in MySQL 8.0 or later since the current maximum auto-increment counter is required to be
 * persisted across restarts of the database server.
 *
 * <p>Example:
 *
 * <pre class="code">
 * create table tab_sequence (`id` bigint unsigned primary key auto_increment);</pre>
 *
 * <p>If {@code cacheSize} is set, the intermediate values are served without querying the
 * database. If the server or your application is stopped or crashes or a transaction
 * is rolled back, the unused values will never be served. The maximum hole size in
 * numbering is consequently the value of {@code cacheSize}.
 *
 * @author Henning Pöttker
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MySQLIdentityColumnMaxValueIncrementer extends AbstractIdentityColumnMaxValueIncrementer {

  /**
   * Default constructor for bean property style usage.
   *
   * @see #setDataSource
   * @see #setIncrementerName
   * @see #setColumnName
   */
  public MySQLIdentityColumnMaxValueIncrementer() { }

  /**
   * Convenience constructor.
   *
   * @param dataSource the DataSource to use
   * @param incrementerName the name of the sequence table to use
   * @param columnName the name of the column in the sequence table to use
   */
  public MySQLIdentityColumnMaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName) {
    super(dataSource, incrementerName, columnName);
  }

  @Override
  protected String getIncrementStatement() {
    return "insert into " + getIncrementerName() + " () values ()";
  }

  @Override
  protected String getIdentityStatement() {
    return "select last_insert_id()";
  }

}
