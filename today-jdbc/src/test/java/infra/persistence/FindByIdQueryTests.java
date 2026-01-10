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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/13 21:49
 */
class FindByIdQueryTests {

  @Test
  void shouldCreateFindByIdQueryWithValidId() {
    Long id = 1L;
    FindByIdQuery query = new FindByIdQuery(id);

    assertThat(query).isNotNull();
  }

  @Test
  void shouldGetDescription() {
    Long id = 1L;
    FindByIdQuery query = new FindByIdQuery(id);

    String description = query.getDescription();

    assertThat(description).isEqualTo("Fetch entity By ID");
  }

  @Test
  void shouldGetDebugLogMessage() {
    Long id = 1L;
    FindByIdQuery query = new FindByIdQuery(id);

    Object logMessage = query.getDebugLogMessage();

    assertThat(logMessage).isNotNull();
    assertThat(logMessage.toString()).contains("Query entity using ID: '1'");
  }

  @Test
  void shouldHandleNullId() {
    FindByIdQuery query = new FindByIdQuery(null);

    String description = query.getDescription();
    Object logMessage = query.getDebugLogMessage();

    assertThat(description).isEqualTo("Fetch entity By ID");
    assertThat(logMessage.toString()).contains("Query entity using ID: 'null'");
  }

  @Test
  void shouldHandleDifferentIdTypes() {
    // Test with String ID
    FindByIdQuery stringQuery = new FindByIdQuery("test-id");
    assertThat(stringQuery.getDebugLogMessage().toString()).contains("Query entity using ID: 'test-id'");

    // Test with Integer ID
    FindByIdQuery intQuery = new FindByIdQuery(42);
    assertThat(intQuery.getDebugLogMessage().toString()).contains("Query entity using ID: '42'");
  }

}