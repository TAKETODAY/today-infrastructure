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

package infra.jdbc.support.incrementer;

import javax.sql.DataSource;

/**
 * {@link DataFieldMaxValueIncrementer} that increments the maximum value of a given HSQL table
 * with the equivalent of an auto-increment column. Note: If you use this class, your HSQL
 * key column should <i>NOT</i> be auto-increment, as the sequence table does the job.
 *
 * <p>The sequence is kept in a table. There should be one sequence table per
 * table that needs an auto-generated key.
 *
 * <p>Example:
 *
 * <pre class="code">create table tab (id int not null primary key, text varchar(100));
 * create table tab_sequence (value identity);
 * insert into tab_sequence values(0);</pre>
 *
 * If "cacheSize" is set, the intermediate values are served without querying the
 * database. If the server or your application is stopped or crashes or a transaction
 * is rolled back, the unused values will never be served. The maximum hole size in
 * numbering is consequently the value of cacheSize.
 *
 * <p><b>NOTE:</b> HSQL now supports sequences and you should consider using them instead:
 * {@link HsqlSequenceMaxValueIncrementer}
 *
 * @author Jean-Pierre Pawlak
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see HsqlSequenceMaxValueIncrementer
 */
public class HsqlMaxValueIncrementer extends AbstractIdentityColumnMaxValueIncrementer {

  /**
   * Default constructor for bean property style usage.
   *
   * @see #setDataSource
   * @see #setIncrementerName
   * @see #setColumnName
   */
  public HsqlMaxValueIncrementer() {
  }

  /**
   * Convenience constructor.
   *
   * @param dataSource the DataSource to use
   * @param incrementerName the name of the sequence/table to use
   * @param columnName the name of the column in the sequence table to use
   */
  public HsqlMaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName) {
    super(dataSource, incrementerName, columnName);
  }

  @Override
  protected String getIncrementStatement() {
    return "insert into " + getIncrementerName() + " values(null)";
  }

  @Override
  protected String getIdentityStatement() {
    return "select max(identity()) from " + getIncrementerName();
  }

}
