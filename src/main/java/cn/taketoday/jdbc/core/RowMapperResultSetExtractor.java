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

package cn.taketoday.jdbc.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.lang.Assert;

/**
 * Adapter implementation of the ResultSetExtractor interface that delegates
 * to a RowMapper which is supposed to create an object for each row.
 * Each object is added to the results List of this ResultSetExtractor.
 *
 * <p>Useful for the typical case of one object per row in the database table.
 * The number of entries in the results list will match the number of rows.
 *
 * <p>Note that a RowMapper object is typically stateless and thus reusable;
 * just the RowMapperResultSetExtractor adapter is stateful.
 *
 * <p>A usage example with JdbcTemplate:
 *
 * <pre class="code">JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);  // reusable object
 * RowMapper rowMapper = new UserRowMapper();  // reusable object
 *
 * List allUsers = (List) jdbcTemplate.query(
 *     "select * from user",
 *     new RowMapperResultSetExtractor(rowMapper, 10));
 *
 * User user = (User) jdbcTemplate.queryForObject(
 *     "select * from user where id=?", new Object[] {id},
 *     new RowMapperResultSetExtractor(rowMapper, 1));</pre>
 *
 * <p>Alternatively, consider subclassing MappingSqlQuery from the {@code jdbc.object}
 * package: Instead of working with separate JdbcTemplate and RowMapper objects,
 * you can have executable query objects (containing row-mapping logic) there.
 *
 * @param <T> the result element type
 * @author Juergen Hoeller
 * @see RowMapper
 * @see JdbcTemplate
 * @see cn.taketoday.jdbc.object.MappingSqlQuery
 * @since 4.0
 */
public class RowMapperResultSetExtractor<T> implements ResultSetExtractor<List<T>> {

  private final RowMapper<T> rowMapper;

  private final int rowsExpected;

  /**
   * Create a new RowMapperResultSetExtractor.
   *
   * @param rowMapper the RowMapper which creates an object for each row
   */
  public RowMapperResultSetExtractor(RowMapper<T> rowMapper) {
    this(rowMapper, 0);
  }

  /**
   * Create a new RowMapperResultSetExtractor.
   *
   * @param rowMapper the RowMapper which creates an object for each row
   * @param rowsExpected the number of expected rows
   * (just used for optimized collection handling)
   */
  public RowMapperResultSetExtractor(RowMapper<T> rowMapper, int rowsExpected) {
    Assert.notNull(rowMapper, "RowMapper is required");
    this.rowMapper = rowMapper;
    this.rowsExpected = rowsExpected;
  }

  @Override
  public List<T> extractData(ResultSet rs) throws SQLException {
    List<T> results = (this.rowsExpected > 0 ? new ArrayList<>(this.rowsExpected) : new ArrayList<>());
    int rowNum = 0;
    while (rs.next()) {
      results.add(this.rowMapper.mapRow(rs, rowNum++));
    }
    return results;
  }

}
