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

package cn.taketoday.jdbc.support;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import cn.taketoday.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import cn.taketoday.jdbc.support.incrementer.HanaSequenceMaxValueIncrementer;
import cn.taketoday.jdbc.support.incrementer.HsqlMaxValueIncrementer;
import cn.taketoday.jdbc.support.incrementer.MySQLMaxValueIncrementer;
import cn.taketoday.jdbc.support.incrementer.OracleSequenceMaxValueIncrementer;
import cn.taketoday.jdbc.support.incrementer.PostgresSequenceMaxValueIncrementer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link DataFieldMaxValueIncrementer} implementations.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class DataFieldMaxValueIncrementerTests {

  private final DataSource dataSource = mock(DataSource.class);

  private final Connection connection = mock(Connection.class);

  private final Statement statement = mock(Statement.class);

  private final ResultSet resultSet = mock(ResultSet.class);

  @Test
  void hanaSequenceMaxValueIncrementer() throws SQLException {
    given(dataSource.getConnection()).willReturn(connection);
    given(connection.createStatement()).willReturn(statement);
    given(statement.executeQuery("select myseq.nextval from dummy")).willReturn(resultSet);
    given(resultSet.next()).willReturn(true);
    given(resultSet.getLong(1)).willReturn(10L, 12L);

    HanaSequenceMaxValueIncrementer incrementer = new HanaSequenceMaxValueIncrementer();
    incrementer.setDataSource(dataSource);
    incrementer.setIncrementerName("myseq");
    incrementer.setPaddingLength(2);
    incrementer.afterPropertiesSet();

    assertThat(incrementer.nextLongValue()).isEqualTo(10);
    assertThat(incrementer.nextStringValue()).isEqualTo("12");

    verify(resultSet, times(2)).close();
    verify(statement, times(2)).close();
    verify(connection, times(2)).close();
  }

  @Test
  void hsqlMaxValueIncrementer() throws SQLException {
    given(dataSource.getConnection()).willReturn(connection);
    given(connection.createStatement()).willReturn(statement);
    given(statement.executeQuery("select max(identity()) from myseq")).willReturn(resultSet);
    given(resultSet.next()).willReturn(true);
    given(resultSet.getLong(1)).willReturn(0L, 1L, 2L, 3L, 4L, 5L);

    HsqlMaxValueIncrementer incrementer = new HsqlMaxValueIncrementer();
    incrementer.setDataSource(dataSource);
    incrementer.setIncrementerName("myseq");
    incrementer.setColumnName("seq");
    incrementer.setCacheSize(3);
    incrementer.setPaddingLength(3);
    incrementer.afterPropertiesSet();

    assertThat(incrementer.nextIntValue()).isEqualTo(0);
    assertThat(incrementer.nextLongValue()).isEqualTo(1);
    assertThat(incrementer.nextStringValue()).isEqualTo("002");
    assertThat(incrementer.nextIntValue()).isEqualTo(3);
    assertThat(incrementer.nextLongValue()).isEqualTo(4);

    verify(statement, times(6)).executeUpdate("insert into myseq values(null)");
    verify(statement).executeUpdate("delete from myseq where seq < 2");
    verify(statement).executeUpdate("delete from myseq where seq < 5");
    verify(resultSet, times(6)).close();
    verify(statement, times(2)).close();
    verify(connection, times(2)).close();
  }

  @Test
  void hsqlMaxValueIncrementerWithDeleteSpecificValues() throws SQLException {
    given(dataSource.getConnection()).willReturn(connection);
    given(connection.createStatement()).willReturn(statement);
    given(statement.executeQuery("select max(identity()) from myseq")).willReturn(resultSet);
    given(resultSet.next()).willReturn(true);
    given(resultSet.getLong(1)).willReturn(0L, 1L, 2L, 3L, 4L, 5L);

    HsqlMaxValueIncrementer incrementer = new HsqlMaxValueIncrementer();
    incrementer.setDataSource(dataSource);
    incrementer.setIncrementerName("myseq");
    incrementer.setColumnName("seq");
    incrementer.setCacheSize(3);
    incrementer.setPaddingLength(3);
    incrementer.setDeleteSpecificValues(true);
    incrementer.afterPropertiesSet();

    assertThat(incrementer.nextIntValue()).isEqualTo(0);
    assertThat(incrementer.nextLongValue()).isEqualTo(1);
    assertThat(incrementer.nextStringValue()).isEqualTo("002");
    assertThat(incrementer.nextIntValue()).isEqualTo(3);
    assertThat(incrementer.nextLongValue()).isEqualTo(4);

    verify(statement, times(6)).executeUpdate("insert into myseq values(null)");
    verify(statement).executeUpdate("delete from myseq where seq in (-1, 0, 1)");
    verify(statement).executeUpdate("delete from myseq where seq in (2, 3, 4)");
    verify(resultSet, times(6)).close();
    verify(statement, times(2)).close();
    verify(connection, times(2)).close();
  }

  @Test
  void mySQLMaxValueIncrementer() throws SQLException {
    given(dataSource.getConnection()).willReturn(connection);
    given(connection.createStatement()).willReturn(statement);
    given(statement.executeQuery("select last_insert_id()")).willReturn(resultSet);
    given(resultSet.next()).willReturn(true);
    given(resultSet.getLong(1)).willReturn(2L, 4L);

    MySQLMaxValueIncrementer incrementer = new MySQLMaxValueIncrementer();
    incrementer.setDataSource(dataSource);
    incrementer.setIncrementerName("myseq");
    incrementer.setColumnName("seq");
    incrementer.setCacheSize(2);
    incrementer.setPaddingLength(1);
    incrementer.afterPropertiesSet();

    assertThat(incrementer.nextIntValue()).isEqualTo(1);
    assertThat(incrementer.nextLongValue()).isEqualTo(2);
    assertThat(incrementer.nextStringValue()).isEqualTo("3");
    assertThat(incrementer.nextLongValue()).isEqualTo(4);

    verify(statement, times(2)).executeUpdate("update myseq set seq = last_insert_id(seq + 2) limit 1");
    verify(resultSet, times(2)).close();
    verify(statement, times(2)).close();
    verify(connection, times(2)).close();
  }

  @Test
  void oracleSequenceMaxValueIncrementer() throws SQLException {
    given(dataSource.getConnection()).willReturn(connection);
    given(connection.createStatement()).willReturn(statement);
    given(statement.executeQuery("select myseq.nextval from dual")).willReturn(resultSet);
    given(resultSet.next()).willReturn(true);
    given(resultSet.getLong(1)).willReturn(10L, 12L);

    OracleSequenceMaxValueIncrementer incrementer = new OracleSequenceMaxValueIncrementer();
    incrementer.setDataSource(dataSource);
    incrementer.setIncrementerName("myseq");
    incrementer.setPaddingLength(2);
    incrementer.afterPropertiesSet();

    assertThat(incrementer.nextLongValue()).isEqualTo(10);
    assertThat(incrementer.nextStringValue()).isEqualTo("12");

    verify(resultSet, times(2)).close();
    verify(statement, times(2)).close();
    verify(connection, times(2)).close();
  }

  @Test
  void postgresSequenceMaxValueIncrementer() throws SQLException {
    given(dataSource.getConnection()).willReturn(connection);
    given(connection.createStatement()).willReturn(statement);
    given(statement.executeQuery("select nextval('myseq')")).willReturn(resultSet);
    given(resultSet.next()).willReturn(true);
    given(resultSet.getLong(1)).willReturn(10L, 12L);

    PostgresSequenceMaxValueIncrementer incrementer = new PostgresSequenceMaxValueIncrementer();
    incrementer.setDataSource(dataSource);
    incrementer.setIncrementerName("myseq");
    incrementer.setPaddingLength(5);
    incrementer.afterPropertiesSet();

    assertThat(incrementer.nextStringValue()).isEqualTo("00010");
    assertThat(incrementer.nextIntValue()).isEqualTo(12);

    verify(resultSet, times(2)).close();
    verify(statement, times(2)).close();
    verify(connection, times(2)).close();
  }

}
