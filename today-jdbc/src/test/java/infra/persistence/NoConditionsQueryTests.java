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