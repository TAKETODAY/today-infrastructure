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

import cn.taketoday.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Unit tests for {@link JdbcTestUtils}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class JdbcTestUtilsTests {

  @Mock
  JdbcTemplate jdbcTemplate;

  @Test
  void deleteWithoutWhereClause() throws Exception {
    given(jdbcTemplate.update("DELETE FROM person", new Object[0])).willReturn(10);
    int deleted = JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "person", null);
    assertThat(deleted).isEqualTo(10);
  }

  @Test
  void deleteWithWhereClause() throws Exception {
    given(jdbcTemplate.update("DELETE FROM person WHERE name = 'Bob' and age > 25", new Object[0])).willReturn(10);
    int deleted = JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "person", "name = 'Bob' and age > 25");
    assertThat(deleted).isEqualTo(10);
  }

  @Test
  void deleteWithWhereClauseAndArguments() throws Exception {
    given(jdbcTemplate.update("DELETE FROM person WHERE name = ? and age > ?", "Bob", 25)).willReturn(10);
    int deleted = JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "person", "name = ? and age > ?", "Bob", 25);
    assertThat(deleted).isEqualTo(10);
  }

}
