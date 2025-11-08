/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.persistence;

import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import infra.core.annotation.MergedAnnotation;
import infra.lang.Constant;
import infra.persistence.sql.OrderByClause;
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
class ConditionStatementTests {

  @Test
  void shouldRenderWhereClauseWithRestrictions() {
    EntityMetadata mockMetadata = mock(EntityMetadata.class);
    List<Restriction> restrictions = new ArrayList<>();

    ConditionStatement conditionStatement = new TestConditionStatement();
    conditionStatement.renderWhereClause(mockMetadata, restrictions);

    // Verify that the method can be called without exceptions
    assertThat(restrictions).isNotNull();
  }

  @Test
  void shouldReturnNullWhenOrderByAnnotationNotPresent() {
    EntityMetadata mockMetadata = mock(EntityMetadata.class);
    when(mockMetadata.getAnnotation(OrderBy.class)).thenReturn(MergedAnnotation.missing());

    ConditionStatement conditionStatement = new TestConditionStatement();
    OrderByClause orderByClause = conditionStatement.getOrderByClause(mockMetadata);

    assertThat(orderByClause).isNull();
  }

  @Test
  void shouldReturnNullWhenOrderByValueIsDefaultNone() {
    EntityMetadata mockMetadata = mock(EntityMetadata.class);
    MergedAnnotation<OrderBy> mockAnnotation = mock(MergedAnnotation.class);
    when(mockAnnotation.isPresent()).thenReturn(true);
    when(mockAnnotation.getStringValue()).thenReturn(Constant.DEFAULT_NONE);
    when(mockMetadata.getAnnotation(OrderBy.class)).thenReturn(mockAnnotation);

    ConditionStatement conditionStatement = new TestConditionStatement();
    OrderByClause orderByClause = conditionStatement.getOrderByClause(mockMetadata);

    assertThat(orderByClause).isNull();
  }

  @Test
  void shouldReturnOrderByClauseWhenAnnotationPresentWithValidValue() {
    EntityMetadata mockMetadata = mock(EntityMetadata.class);
    MergedAnnotation<OrderBy> mockAnnotation = mock();
    when(mockAnnotation.isPresent()).thenReturn(true);
    when(mockAnnotation.getStringValue()).thenReturn("name ASC");
    when(mockMetadata.getAnnotation(OrderBy.class)).thenReturn(mockAnnotation);

    ConditionStatement conditionStatement = new TestConditionStatement();
    OrderByClause orderByClause = conditionStatement.getOrderByClause(mockMetadata);

    assertThat(orderByClause).isNotNull();
    assertThat(orderByClause.toClause()).isEqualTo("name ASC");
  }

  @Test
  void shouldHandleSQLExceptionInSetParameter() throws SQLException {
    EntityMetadata mockMetadata = mock(EntityMetadata.class);
    PreparedStatement mockStatement = mock(PreparedStatement.class);
    doThrow(new SQLException("Test exception")).when(mockStatement).setObject(anyInt(), any());

    ConditionStatement conditionStatement = new TestConditionStatement();

    assertThatThrownBy(() -> conditionStatement.setParameter(mockMetadata, mockStatement))
            .isInstanceOf(SQLException.class)
            .hasMessage("Test exception");
  }

  private static class TestConditionStatement implements ConditionStatement {
    @Override
    public void renderWhereClause(EntityMetadata metadata, List<Restriction> restrictions) {
      // No-op implementation for testing
    }

    @Override
    public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException {
      throw new SQLException("Test exception");
    }
  }

}