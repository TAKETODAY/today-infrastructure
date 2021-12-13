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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import cn.taketoday.jdbc.CannotGetJdbcConnectionException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Mock object based tests for {@code DatabaseStartupValidator}.
 *
 * @author Marten Deinum
 */
class DatabaseStartupValidatorTests {

  private final DataSource dataSource = mock(DataSource.class);

  private final Connection connection = mock(Connection.class);

  private final DatabaseStartupValidator validator = new DatabaseStartupValidator();

  @BeforeEach
  void setUp() throws Exception {
    given(dataSource.getConnection()).willReturn(connection);
    validator.setDataSource(dataSource);
    validator.setTimeout(3); // ensure tests don't accidentally run too long
  }

  @Test
  void exceededTimeoutThrowsException() {
    validator.setTimeout(1);
    assertThatExceptionOfType(CannotGetJdbcConnectionException.class)
            .isThrownBy(validator::afterPropertiesSet);
  }

  @Test
  void properSetupForDataSource() {
    validator.setDataSource(null);

    assertThatIllegalArgumentException().isThrownBy(validator::afterPropertiesSet);
  }

  @Test
  void shouldUseJdbc4IsValidByDefault() throws Exception {
    given(connection.isValid(1)).willReturn(true);

    validator.afterPropertiesSet();

    verify(connection, times(1)).isValid(1);
    verify(connection, times(1)).close();
  }

  @Test
  void shouldCallValidatonTwiceWhenNotValid() throws Exception {
    given(connection.isValid(1)).willReturn(false, true);

    validator.afterPropertiesSet();

    verify(connection, times(2)).isValid(1);
    verify(connection, times(2)).close();
  }

  @Test
  void shouldCallValidatonTwiceInCaseOfException() throws Exception {
    given(connection.isValid(1)).willThrow(new SQLException("Test")).willReturn(true);

    validator.afterPropertiesSet();

    verify(connection, times(2)).isValid(1);
    verify(connection, times(2)).close();
  }

  @Test
  @SuppressWarnings("deprecation")
  void useValidationQueryInsteadOfIsValid() throws Exception {
    String validationQuery = "SELECT NOW() FROM DUAL";
    Statement statement = mock(Statement.class);
    given(connection.createStatement()).willReturn(statement);
    given(statement.execute(validationQuery)).willReturn(true);

    validator.setValidationQuery(validationQuery);
    validator.afterPropertiesSet();

    verify(connection, times(1)).createStatement();
    verify(statement, times(1)).execute(validationQuery);
    verify(connection, times(1)).close();
    verify(statement, times(1)).close();
  }

  @Test
  @SuppressWarnings("deprecation")
  void shouldExecuteValidatonTwiceOnError() throws Exception {
    String validationQuery = "SELECT NOW() FROM DUAL";
    Statement statement = mock(Statement.class);
    given(connection.createStatement()).willReturn(statement);
    given(statement.execute(validationQuery))
            .willThrow(new SQLException("Test"))
            .willReturn(true);

    validator.setValidationQuery(validationQuery);
    validator.afterPropertiesSet();

    verify(connection, times(2)).createStatement();
    verify(statement, times(2)).execute(validationQuery);
    verify(connection, times(2)).close();
    verify(statement, times(2)).close();
  }

}
