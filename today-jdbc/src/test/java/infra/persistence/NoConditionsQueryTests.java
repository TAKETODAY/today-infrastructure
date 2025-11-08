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
import java.util.List;

import infra.persistence.sql.Restriction;
import infra.persistence.sql.Select;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/8 20:14
 */
class NoConditionsQueryTests {

  @Test
  void shouldReturnInstance() {
    NoConditionsQuery instance1 = NoConditionsQuery.instance;
    NoConditionsQuery instance2 = NoConditionsQuery.instance;

    assertThat(instance1).isNotNull();
    assertThat(instance1).isSameAs(instance2);
  }

  @Test
  void shouldRenderInternalDoNothing() {
    NoConditionsQuery noConditionsQuery = NoConditionsQuery.instance;
    EntityMetadata metadata = mock(EntityMetadata.class);
    Select select = mock(Select.class);

    // Should not throw any exception
    noConditionsQuery.renderInternal(metadata, select);

    // Verify no interactions with mocks
    verifyNoInteractions(metadata, select);
  }

  @Test
  void shouldSetParameterDoNothing() throws SQLException {
    NoConditionsQuery noConditionsQuery = NoConditionsQuery.instance;
    EntityMetadata metadata = mock(EntityMetadata.class);
    PreparedStatement statement = mock(PreparedStatement.class);

    // Should not throw any exception
    noConditionsQuery.setParameter(metadata, statement);

    // Verify no interactions with mocks
    verifyNoInteractions(metadata, statement);
  }

  @Test
  void shouldGetDescription() {
    NoConditionsQuery noConditionsQuery = NoConditionsQuery.instance;

    String description = noConditionsQuery.getDescription();

    assertThat(description).isEqualTo("Query entities without conditions");
  }

  @Test
  void shouldGetDebugLogMessage() {
    NoConditionsQuery noConditionsQuery = NoConditionsQuery.instance;

    Object debugLogMessage = noConditionsQuery.getDebugLogMessage();

    assertThat(debugLogMessage).isNotNull();
    assertThat(debugLogMessage.toString()).contains("Query entities without conditions");
  }

  @Test
  void shouldRenderWhereClauseDoNothing() {
    NoConditionsQuery noConditionsQuery = NoConditionsQuery.instance;
    EntityMetadata metadata = mock(EntityMetadata.class);
    List<Restriction> restrictions = mock();

    // Should not throw any exception
    noConditionsQuery.renderWhereClause(metadata, restrictions);

    // Verify no interactions with mocks
    verifyNoInteractions(metadata, restrictions);
  }

  @Test
  void shouldImplementConditionStatement() {
    NoConditionsQuery noConditionsQuery = NoConditionsQuery.instance;

    assertThat(noConditionsQuery).isInstanceOf(ConditionStatement.class);
  }

  @Test
  void shouldImplementDebugDescriptive() {
    NoConditionsQuery noConditionsQuery = NoConditionsQuery.instance;

    assertThat(noConditionsQuery).isInstanceOf(DebugDescriptive.class);
  }

}