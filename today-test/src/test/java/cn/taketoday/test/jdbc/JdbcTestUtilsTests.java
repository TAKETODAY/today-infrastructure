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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.jdbc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;

import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.jdbc.core.PreparedStatementCreator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link JdbcTestUtils}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class JdbcTestUtilsTests {

  @Mock
  Connection connection;

  @Mock
  PreparedStatement preparedStatement;

  @Mock
  JdbcTemplate jdbcTemplate;

  @Test
  void deleteWithoutWhereClause() throws Exception {
    given(connection.prepareStatement("DELETE FROM person")).willReturn(preparedStatement);
    PreparedStatementCreator preparedStatementCreator = assertArg(psc ->
            assertThat(psc.createPreparedStatement(connection)).isSameAs(preparedStatement));
    given(jdbcTemplate.update(preparedStatementCreator)).willReturn(10);

    int deleted = JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "person", null);
    assertThat(deleted).isEqualTo(10);
  }

  @Test
  void deleteWithWhereClause() throws Exception {
    given(connection.prepareStatement("DELETE FROM person WHERE name = 'Bob' and age > 25")).willReturn(preparedStatement);
    PreparedStatementCreator preparedStatementCreator = assertArg(psc ->
            assertThat(psc.createPreparedStatement(connection)).isSameAs(preparedStatement));
    given(jdbcTemplate.update(preparedStatementCreator)).willReturn(10);

    int deleted = JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "person", "name = 'Bob' and age > 25");
    assertThat(deleted).isEqualTo(10);
  }

  @Test
  void deleteWithWhereClauseAndArguments() throws Exception {
    given(connection.prepareStatement("DELETE FROM person WHERE name = ? and age > ?")).willReturn(preparedStatement);
    PreparedStatementCreator preparedStatementCreator = assertArg(psc -> {
      assertThat(psc.createPreparedStatement(connection)).isSameAs(preparedStatement);
      verify(preparedStatement).setString(1, "Bob");
      verify(preparedStatement).setObject(2, 25);
    });
    given(jdbcTemplate.update(preparedStatementCreator)).willReturn(10);

    int deleted = JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "person", "name = ? and age > ?", "Bob", 25);
    assertThat(deleted).isEqualTo(10);
  }

}
