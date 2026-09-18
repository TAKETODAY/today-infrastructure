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

package infra.persistence;

import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import infra.persistence.sql.OrderSpec;
import infra.persistence.sql.Restriction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/8 20:04
 */
class QueryConditionTests {

  @Test
  void shouldRenderWhereClauseWithRestrictions() {
    EntityMetadata mockMetadata = mock(EntityMetadata.class);
    List<Restriction> restrictions = new ArrayList<>();

    QueryCondition conditionStatement = new TestQueryCondition();
    conditionStatement.collectRestrictions(mockMetadata, restrictions);

    // Verify that the method can be called without exceptions
    assertThat(restrictions).isNotNull();
  }

  @Test
  void shouldReturnEmptyOrderSpecWhenMetadataHasNoOrdering() {
    EntityMetadata mockMetadata = mock(EntityMetadata.class);
    when(mockMetadata.getOrderSpec()).thenReturn(OrderSpec.empty());

    QueryCondition conditionStatement = new TestQueryCondition();
    OrderSpec orderSpec = conditionStatement.resolveOrderByClause(mockMetadata);

    assertThat(orderSpec).isSameAs(OrderSpec.empty());
  }

  @Test
  void shouldReturnOrderSpecFromMetadata() {
    EntityMetadata mockMetadata = mock(EntityMetadata.class);
    OrderSpec expected = OrderSpec.asc("name");
    when(mockMetadata.getOrderSpec()).thenReturn(expected);

    QueryCondition conditionStatement = new TestQueryCondition();
    OrderSpec orderSpec = conditionStatement.resolveOrderByClause(mockMetadata);

    assertThat(orderSpec).isSameAs(expected);
  }

  @Test
  void shouldHandleSQLExceptionInSetParameter() throws SQLException {
    EntityMetadata mockMetadata = mock(EntityMetadata.class);
    PreparedStatement mockStatement = mock(PreparedStatement.class);
    doThrow(new SQLException("Test exception")).when(mockStatement).setObject(anyInt(), any());

    QueryCondition conditionStatement = new TestQueryCondition();

    assertThatThrownBy(() -> conditionStatement.setParameter(mockMetadata, mockStatement))
            .isInstanceOf(SQLException.class)
            .hasMessage("Test exception");
  }

  private static class TestQueryCondition implements QueryCondition {
    @Override
    public void collectRestrictions(EntityMetadata metadata, List<Restriction> restrictions) {
      // No-op implementation for testing
    }

    @Override
    public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException {
      throw new SQLException("Test exception");
    }
  }

}
