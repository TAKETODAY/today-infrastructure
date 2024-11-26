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

import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.core.simple.SimpleJdbcInsert;
import infra.jdbc.support.KeyHolder;

/**
 * {@link DataFieldMaxValueIncrementer} that increments the maximum value of a given Sybase table
 * with the equivalent of an auto-increment column. Note: If you use this class, your table key
 * column should <i>NOT</i> be defined as an IDENTITY column, as the sequence table does the job.
 *
 * <p>This class is intended to be used with Sybase Adaptive Server.
 *
 * <p>The sequence is kept in a table. There should be one sequence table per
 * table that needs an auto-generated key.
 *
 * <p>Example:
 *
 * <pre class="code">create table tab (id int not null primary key, text varchar(100))
 * create table tab_sequence (id bigint identity)
 * insert into tab_sequence values()</pre>
 *
 * If "cacheSize" is set, the intermediate values are served without querying the
 * database. If the server or your application is stopped or crashes or a transaction
 * is rolled back, the unused values will never be served. The maximum hole size in
 * numbering is consequently the value of cacheSize.
 *
 * <b>HINT:</b> Since Sybase Adaptive Server supports the JDBC 3.0 {@code getGeneratedKeys}
 * method, it is recommended to use IDENTITY columns directly in the tables and then
 * using a {@link SimpleJdbcInsert} or utilizing
 * a {@link KeyHolder} when calling the with the
 * {@code update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder)}
 * method of the {@link JdbcTemplate}.
 *
 * <p>Thanks to Yinwei Liu for the suggestion!
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 4.0
 */
public class SybaseMaxValueIncrementer extends AbstractIdentityColumnMaxValueIncrementer {

  /**
   * Default constructor for bean property style usage.
   *
   * @see #setDataSource
   * @see #setIncrementerName
   * @see #setColumnName
   */
  public SybaseMaxValueIncrementer() {
  }

  /**
   * Convenience constructor.
   *
   * @param dataSource the DataSource to use
   * @param incrementerName the name of the sequence/table to use
   * @param columnName the name of the column in the sequence table to use
   */
  public SybaseMaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName) {
    super(dataSource, incrementerName, columnName);
  }

  @Override
  protected String getIncrementStatement() {
    return "insert into " + getIncrementerName() + " values()";
  }

  @Override
  protected String getIdentityStatement() {
    return "select @@identity";
  }

}
