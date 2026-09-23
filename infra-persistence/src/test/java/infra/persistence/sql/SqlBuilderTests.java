/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.persistence.sql;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.sql.PreparedStatement;

import infra.jdbc.ParameterBinder;
import infra.persistence.Identifier;
import infra.persistence.platform.Platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class SqlBuilderTests {

  @Test
  void rendersIdentifiersAndBindsParametersInSqlOrder() throws Exception {
    SqlBuilder builder = new SqlBuilder(Platform.mysql())
            .append("SELECT ").identifier(Identifier.parse("`name`"))
            .append(" FROM ").identifier(Identifier.parse("users"))
            .append(" WHERE ").identifier(Identifier.parse("`age`"))
            .append(" > ").parameter(ParameterBinder.forInt(18))
            .append(" AND ").identifier(Identifier.parse("status"))
            .append(" = ").parameter(ParameterBinder.forString("active"));
    SqlStatement result = builder.build();
    PreparedStatement statement = mock(PreparedStatement.class);

    assertThat(result.sql()).isEqualTo("SELECT `name` FROM users WHERE `age` > ? AND status = ?");
    assertThat(result.parameterCount()).isEqualTo(2);
    assertThat(result.bind(statement, 3)).isEqualTo(5);
    InOrder order = inOrder(statement);
    order.verify(statement).setInt(3, 18);
    order.verify(statement).setString(4, "active");
    order.verifyNoMoreInteractions();
  }

  @Test
  void buildSnapshotsSqlAndBinders() throws Exception {
    SqlBuilder builder = new SqlBuilder(Platform.generic()).append("WHERE id = ")
            .parameter(ParameterBinder.forInt(7));
    SqlStatement first = builder.build();
    builder.append(" OR id = ").parameter(ParameterBinder.forInt(8));
    SqlStatement second = builder.build();
    PreparedStatement statement = mock(PreparedStatement.class);

    assertThat(first.sql()).isEqualTo("WHERE id = ?");
    assertThat(first.parameterCount()).isEqualTo(1);
    first.bind(statement);
    verify(statement).setInt(1, 7);
    verifyNoMoreInteractions(statement);
    assertThat(second.sql()).isEqualTo("WHERE id = ? OR id = ?");
    assertThat(second.parameterCount()).isEqualTo(2);
  }

  @Test
  void rawSqlIsNotTreatedAsACollectedParameter() {
    SqlStatement result = new SqlBuilder(Platform.generic())
            .append("SELECT '?', ").append('?').append(", ")
            .parameter(ParameterBinder.forInt(1)).build();

    assertThat(result.sql()).isEqualTo("SELECT '?', ?, ?");
    assertThat(result.parameterCount()).isEqualTo(1);
  }

  @Test
  void respectsPlatformIdentifierRenderingAndChecksIndices() {
    Platform custom = new Platform() {
      @Override
      public void appendQuotedIdentifier(StringBuilder sql, String name) {
        sql.append('[').append(name).append(']');
      }
    };
    SqlStatement result = new SqlBuilder(custom, 32)
            .identifier(Identifier.parse("`user`"))
            .append('.').identifier(Identifier.parse("name")).build();

    assertThat(result.sql()).isEqualTo("[user].name");
    assertThatIllegalArgumentException().isThrownBy(() -> result.bind(mock(PreparedStatement.class), 0));
  }

}
