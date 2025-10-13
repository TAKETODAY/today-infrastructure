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