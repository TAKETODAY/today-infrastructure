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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import cn.taketoday.jdbc.datasource.SingleConnectionDataSource;
import cn.taketoday.jdbc.support.SQLStateSQLExceptionTranslator;
import cn.taketoday.jdbc.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 02.08.2004
 */
public class RowMapperTests {

  private final Connection connection = mock(Connection.class);

  private final Statement statement = mock(Statement.class);

  private final PreparedStatement preparedStatement = mock(PreparedStatement.class);

  private final ResultSet resultSet = mock(ResultSet.class);

  private final JdbcTemplate template = new JdbcTemplate();

  private final RowMapper<TestBean> testRowMapper =
          (rs, rowNum) -> new TestBean(rs.getString(1), rs.getInt(2));

  private List<TestBean> result;

  @BeforeEach
  public void setUp() throws SQLException {
    given(connection.createStatement()).willReturn(statement);
    given(connection.prepareStatement(ArgumentMatchers.anyString())).willReturn(preparedStatement);
    given(statement.executeQuery(ArgumentMatchers.anyString())).willReturn(resultSet);
    given(preparedStatement.executeQuery()).willReturn(resultSet);
    given(resultSet.next()).willReturn(true, true, false);
    given(resultSet.getString(1)).willReturn("tb1", "tb2");
    given(resultSet.getInt(2)).willReturn(1, 2);

    template.setDataSource(new SingleConnectionDataSource(connection, false));
    template.setExceptionTranslator(new SQLStateSQLExceptionTranslator());
    template.afterPropertiesSet();
  }

  @AfterEach
  public void verifyClosed() throws Exception {
    verify(resultSet).close();
  }

  @AfterEach
  public void verifyResults() {
    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(2);
    TestBean testBean1 = result.get(0);
    TestBean testBean2 = result.get(1);
    assertThat(testBean1.getName()).isEqualTo("tb1");
    assertThat(testBean2.getName()).isEqualTo("tb2");
    assertThat(testBean1.getAge()).isEqualTo(1);
    assertThat(testBean2.getAge()).isEqualTo(2);
  }

  @Test
  public void staticQueryWithRowMapper() throws SQLException {
    result = template.query("some SQL", testRowMapper);
    verify(statement).close();
  }

  @Test
  public void preparedStatementCreatorWithRowMapper() throws SQLException {
    result = template.query(con -> preparedStatement, testRowMapper);
    verify(preparedStatement).close();
  }

  @Test
  public void preparedStatementSetterWithRowMapper() throws SQLException {
    result = template.query("some SQL", ps -> ps.setString(1, "test"), testRowMapper);
    verify(preparedStatement).setString(1, "test");
    verify(preparedStatement).close();
  }

  @Test
  @SuppressWarnings("deprecation")
  public void queryWithArgsAndRowMapper() throws SQLException {
    result = template.query("some SQL", new Object[] { "test1", "test2" }, testRowMapper);
    preparedStatement.setString(1, "test1");
    preparedStatement.setString(2, "test2");
    preparedStatement.close();
  }

  @Test
  public void queryWithArgsAndTypesAndRowMapper() throws SQLException {
    result = template.query("some SQL",
            new Object[] { "test1", "test2" },
            new int[] { Types.VARCHAR, Types.VARCHAR },
            testRowMapper);
    verify(preparedStatement).setString(1, "test1");
    verify(preparedStatement).setString(2, "test2");
    verify(preparedStatement).close();
  }

}
