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

package cn.taketoday.jdbc.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.sql.DataSource;

import cn.taketoday.dao.IncorrectResultSizeDataAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Rob Winch
 * @since 19.12.2004
 */
public class JdbcTemplateQueryTests {

  private Connection connection;

  private DataSource dataSource;

  private Statement statement;

  private PreparedStatement preparedStatement;

  private ResultSet resultSet;

  private ResultSetMetaData resultSetMetaData;

  private JdbcTemplate template;

  @BeforeEach
  public void setUp() throws Exception {
    this.connection = mock(Connection.class);
    this.dataSource = mock(DataSource.class);
    this.statement = mock(Statement.class);
    this.preparedStatement = mock(PreparedStatement.class);
    this.resultSet = mock(ResultSet.class);
    this.resultSetMetaData = mock(ResultSetMetaData.class);
    this.template = new JdbcTemplate(this.dataSource);
    given(this.dataSource.getConnection()).willReturn(this.connection);
    given(this.resultSet.getMetaData()).willReturn(this.resultSetMetaData);
    given(this.resultSetMetaData.getColumnCount()).willReturn(1);
    given(this.resultSetMetaData.getColumnLabel(1)).willReturn("age");
    given(this.connection.createStatement()).willReturn(this.statement);
    given(this.connection.prepareStatement(ArgumentMatchers.anyString())).willReturn(this.preparedStatement);
    given(this.preparedStatement.executeQuery()).willReturn(this.resultSet);
    given(this.statement.executeQuery(ArgumentMatchers.anyString())).willReturn(this.resultSet);
  }

  @Test
  public void testQueryForList() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID < 3";
    given(this.resultSet.next()).willReturn(true, true, false);
    given(this.resultSet.getObject(1)).willReturn(11, 12);
    List<Map<String, Object>> li = this.template.queryForList(sql);
    assertThat(li.size()).as("All rows returned").isEqualTo(2);
    assertThat(((Integer) li.get(0).get("age")).intValue()).as("First row is Integer").isEqualTo(11);
    assertThat(((Integer) li.get(1).get("age")).intValue()).as("Second row is Integer").isEqualTo(12);
    verify(this.resultSet).close();
    verify(this.statement).close();
  }

  @Test
  public void testQueryForListWithEmptyResult() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID < 3";
    given(this.resultSet.next()).willReturn(false);
    List<Map<String, Object>> li = this.template.queryForList(sql);
    assertThat(li.size()).as("All rows returned").isEqualTo(0);
    verify(this.resultSet).close();
    verify(this.statement).close();
  }

  @Test
  public void testQueryForListWithSingleRowAndColumn() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID < 3";
    given(this.resultSet.next()).willReturn(true, false);
    given(this.resultSet.getObject(1)).willReturn(11);
    List<Map<String, Object>> li = this.template.queryForList(sql);
    assertThat(li.size()).as("All rows returned").isEqualTo(1);
    assertThat(((Integer) li.get(0).get("age")).intValue()).as("First row is Integer").isEqualTo(11);
    verify(this.resultSet).close();
    verify(this.statement).close();
  }

  @Test
  public void testQueryForListWithIntegerElement() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID < 3";
    given(this.resultSet.next()).willReturn(true, false);
    given(this.resultSet.getInt(1)).willReturn(11);
    List<Integer> li = this.template.queryForList(sql, Integer.class);
    assertThat(li.size()).as("All rows returned").isEqualTo(1);
    assertThat(li.get(0).intValue()).as("Element is Integer").isEqualTo(11);
    verify(this.resultSet).close();
    verify(this.statement).close();
  }

  @Test
  public void testQueryForMapWithSingleRowAndColumn() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID < 3";
    given(this.resultSet.next()).willReturn(true, false);
    given(this.resultSet.getObject(1)).willReturn(11);
    Map<String, Object> map = this.template.queryForMap(sql);
    assertThat(((Integer) map.get("age")).intValue()).as("Wow is Integer").isEqualTo(11);
    verify(this.resultSet).close();
    verify(this.statement).close();
  }

  @Test
  public void testQueryForObjectThrowsIncorrectResultSizeForMoreThanOneRow() throws Exception {
    String sql = "select pass from t_account where first_name='Alef'";
    given(this.resultSet.next()).willReturn(true, true, false);
    given(this.resultSet.getString(1)).willReturn("pass");
    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
            this.template.queryForObject(sql, String.class));
    verify(this.resultSet).close();
    verify(this.statement).close();
  }

  @Test
  public void testQueryForObjectWithRowMapper() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
    given(this.resultSet.next()).willReturn(true, false);
    given(this.resultSet.getInt(1)).willReturn(22);
    Object o = this.template.queryForObject(sql, new RowMapper<Integer>() {
      @Override
      public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getInt(1);
      }
    });
    assertThat(o instanceof Integer).as("Correct result type").isTrue();
    verify(this.resultSet).close();
    verify(this.statement).close();
  }

  @Test
  public void testQueryForStreamWithRowMapper() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
    given(this.resultSet.next()).willReturn(true, false);
    given(this.resultSet.getInt(1)).willReturn(22);
    AtomicInteger count = new AtomicInteger();
    try (Stream<Integer> s = this.template.queryForStream(sql, (rs, rowNum) -> rs.getInt(1))) {
      s.forEach(val -> {
        count.incrementAndGet();
        assertThat(val).isEqualTo(22);
      });
    }
    assertThat(count.get()).isEqualTo(1);
    verify(this.resultSet).close();
    verify(this.statement).close();
  }

  @Test
  public void testQueryForObjectWithString() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
    given(this.resultSet.next()).willReturn(true, false);
    given(this.resultSet.getString(1)).willReturn("myvalue");
    assertThat(this.template.queryForObject(sql, String.class)).isEqualTo("myvalue");
    verify(this.resultSet).close();
    verify(this.statement).close();
  }

  @Test
  public void testQueryForObjectWithBigInteger() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
    given(this.resultSet.next()).willReturn(true, false);
    given(this.resultSet.getObject(1, BigInteger.class)).willReturn(new BigInteger("22"));
    assertThat(this.template.queryForObject(sql, BigInteger.class)).isEqualTo(new BigInteger("22"));
    verify(this.resultSet).close();
    verify(this.statement).close();
  }

  @Test
  public void testQueryForObjectWithBigDecimal() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
    given(this.resultSet.next()).willReturn(true, false);
    given(this.resultSet.getBigDecimal(1)).willReturn(new BigDecimal("22.5"));
    assertThat(this.template.queryForObject(sql, BigDecimal.class)).isEqualTo(new BigDecimal("22.5"));
    verify(this.resultSet).close();
    verify(this.statement).close();
  }

  @Test
  public void testQueryForObjectWithInteger() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
    given(this.resultSet.next()).willReturn(true, false);
    given(this.resultSet.getInt(1)).willReturn(22);
    assertThat(this.template.queryForObject(sql, Integer.class)).isEqualTo(Integer.valueOf(22));
    verify(this.resultSet).close();
    verify(this.statement).close();
  }

  @Test
  public void testQueryForObjectWithIntegerAndNull() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
    given(this.resultSet.next()).willReturn(true, false);
    given(this.resultSet.getInt(1)).willReturn(0);
    given(this.resultSet.wasNull()).willReturn(true);
    assertThat(this.template.queryForObject(sql, Integer.class)).isNull();
    verify(this.resultSet).close();
    verify(this.statement).close();
  }

  @Test
  public void testQueryForInt() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
    given(this.resultSet.next()).willReturn(true, false);
    given(this.resultSet.getInt(1)).willReturn(22);
    int i = this.template.queryForObject(sql, Integer.class).intValue();
    assertThat(i).as("Return of an int").isEqualTo(22);
    verify(this.resultSet).close();
    verify(this.statement).close();
  }

  @Test
  public void testQueryForIntPrimitive() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
    given(this.resultSet.next()).willReturn(true, false);
    given(this.resultSet.getInt(1)).willReturn(22);
    int i = this.template.queryForObject(sql, int.class);
    assertThat(i).as("Return of an int").isEqualTo(22);
    verify(this.resultSet).close();
    verify(this.statement).close();
  }

  @Test
  public void testQueryForLong() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
    given(this.resultSet.next()).willReturn(true, false);
    given(this.resultSet.getLong(1)).willReturn(87L);
    long l = this.template.queryForObject(sql, Long.class).longValue();
    assertThat(l).as("Return of a long").isEqualTo(87);
    verify(this.resultSet).close();
    verify(this.statement).close();
  }

  @Test
  public void testQueryForLongPrimitive() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
    given(this.resultSet.next()).willReturn(true, false);
    given(this.resultSet.getLong(1)).willReturn(87L);
    long l = this.template.queryForObject(sql, long.class);
    assertThat(l).as("Return of a long").isEqualTo(87);
    verify(this.resultSet).close();
    verify(this.statement).close();
  }

  @Test
  public void testQueryForListWithArgs() throws Exception {
    doTestQueryForListWithArgs("SELECT AGE FROM CUSTMR WHERE ID < ?");
  }

  @Test
  public void testQueryForListIsNotConfusedByNamedParameterPrefix() throws Exception {
    doTestQueryForListWithArgs("SELECT AGE FROM PREFIX:CUSTMR WHERE ID < ?");
  }

  private void doTestQueryForListWithArgs(String sql) throws Exception {
    given(this.resultSet.next()).willReturn(true, true, false);
    given(this.resultSet.getObject(1)).willReturn(11, 12);
    List<Map<String, Object>> li = this.template.queryForList(sql, 3);
    assertThat(li.size()).as("All rows returned").isEqualTo(2);
    assertThat(((Integer) li.get(0).get("age")).intValue()).as("First row is Integer").isEqualTo(11);
    assertThat(((Integer) li.get(1).get("age")).intValue()).as("Second row is Integer").isEqualTo(12);
    verify(this.preparedStatement).setObject(1, 3);
    verify(this.resultSet).close();
    verify(this.preparedStatement).close();
  }

  @Test
  public void testQueryForListWithArgsAndEmptyResult() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID < ?";
    given(this.resultSet.next()).willReturn(false);
    List<Map<String, Object>> li = this.template.queryForList(sql, 3);
    assertThat(li.size()).as("All rows returned").isEqualTo(0);
    verify(this.preparedStatement).setObject(1, 3);
    verify(this.resultSet).close();
    verify(this.preparedStatement).close();
  }

  @Test
  public void testQueryForListWithArgsAndSingleRowAndColumn() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID < ?";
    given(this.resultSet.next()).willReturn(true, false);
    given(this.resultSet.getObject(1)).willReturn(11);
    List<Map<String, Object>> li = this.template.queryForList(sql, 3);
    assertThat(li.size()).as("All rows returned").isEqualTo(1);
    assertThat(((Integer) li.get(0).get("age")).intValue()).as("First row is Integer").isEqualTo(11);
    verify(this.preparedStatement).setObject(1, 3);
    verify(this.resultSet).close();
    verify(this.preparedStatement).close();
  }

  @Test
  public void testQueryForListWithArgsAndIntegerElementAndSingleRowAndColumn() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID < ?";
    given(this.resultSet.next()).willReturn(true, false);
    given(this.resultSet.getInt(1)).willReturn(11);
    List<Integer> li = this.template.queryForList(sql, Integer.class, 3);
    assertThat(li.size()).as("All rows returned").isEqualTo(1);
    assertThat(li.get(0).intValue()).as("First row is Integer").isEqualTo(11);
    verify(this.preparedStatement).setObject(1, 3);
    verify(this.resultSet).close();
    verify(this.preparedStatement).close();
  }

  @Test
  public void testQueryForMapWithArgsAndSingleRowAndColumn() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID < ?";
    given(this.resultSet.next()).willReturn(true, false);
    given(this.resultSet.getObject(1)).willReturn(11);
    Map<String, Object> map = this.template.queryForMap(sql, 3);
    assertThat(((Integer) map.get("age")).intValue()).as("Row is Integer").isEqualTo(11);
    verify(this.preparedStatement).setObject(1, 3);
    verify(this.resultSet).close();
    verify(this.preparedStatement).close();
  }

  @Test
  public void testQueryForObjectWithArgsAndRowMapper() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID = ?";
    given(this.resultSet.next()).willReturn(true, false);
    given(this.resultSet.getInt(1)).willReturn(22);
    Object o = this.template.queryForObject(sql, (rs, rowNum) -> rs.getInt(1), 3);
    assertThat(o instanceof Integer).as("Correct result type").isTrue();
    verify(this.preparedStatement).setObject(1, 3);
    verify(this.resultSet).close();
    verify(this.preparedStatement).close();
  }

  @Test
  public void testQueryForStreamWithArgsAndRowMapper() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID = ?";
    given(this.resultSet.next()).willReturn(true, false);
    given(this.resultSet.getInt(1)).willReturn(22);
    AtomicInteger count = new AtomicInteger();
    try (Stream<Integer> s = this.template.queryForStream(sql, (rs, rowNum) -> rs.getInt(1), 3)) {
      s.forEach(val -> {
        count.incrementAndGet();
        assertThat(val).isEqualTo(22);
      });
    }
    assertThat(count.get()).isEqualTo(1);
    verify(this.preparedStatement).setObject(1, 3);
    verify(this.resultSet).close();
    verify(this.preparedStatement).close();
  }

  @Test
  public void testQueryForObjectWithArgsAndInteger() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID = ?";
    given(this.resultSet.next()).willReturn(true, false);
    given(this.resultSet.getInt(1)).willReturn(22);
    Object o = this.template.queryForObject(sql, Integer.class, 3);
    assertThat(o instanceof Integer).as("Correct result type").isTrue();
    verify(this.preparedStatement).setObject(1, 3);
    verify(this.resultSet).close();
    verify(this.preparedStatement).close();
  }

  @Test
  public void testQueryForIntWithArgs() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID = ?";
    given(this.resultSet.next()).willReturn(true, false);
    given(this.resultSet.getInt(1)).willReturn(22);
    int i = this.template.queryForObject(sql, Integer.class, 3).intValue();
    assertThat(i).as("Return of an int").isEqualTo(22);
    verify(this.preparedStatement).setObject(1, 3);
    verify(this.resultSet).close();
    verify(this.preparedStatement).close();
  }

  @Test
  public void testQueryForLongWithArgs() throws Exception {
    String sql = "SELECT AGE FROM CUSTMR WHERE ID = ?";
    given(this.resultSet.next()).willReturn(true, false);
    given(this.resultSet.getLong(1)).willReturn(87L);
    long l = this.template.queryForObject(sql, Long.class, 3).longValue();
    assertThat(l).as("Return of a long").isEqualTo(87);
    verify(this.preparedStatement).setObject(1, 3);
    verify(this.resultSet).close();
    verify(this.preparedStatement).close();
  }

}
