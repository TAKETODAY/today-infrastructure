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
 * @since 5.0 2025/11/8 20:04
 */
class DebugDescriptiveTests {

  @Test
  void shouldReturnCorrectDescription() {
    String expectedDescription = "Test description";
    DebugDescriptive debugDescriptive = () -> expectedDescription;

    String description = debugDescriptive.getDescription();

    assertThat(description).isEqualTo(expectedDescription);
  }

  @Test
  void shouldReturnCorrectDebugLogMessage() {
    String expectedDescription = "Test description";
    DebugDescriptive debugDescriptive = () -> expectedDescription;

    Object debugLogMessage = debugDescriptive.getDebugLogMessage();

    assertThat(debugLogMessage).isNotNull();
    assertThat(debugLogMessage.toString()).contains(expectedDescription);
  }

  @Test
  void shouldHandleEmptyDescription() {
    String expectedDescription = "";
    DebugDescriptive debugDescriptive = () -> expectedDescription;

    String description = debugDescriptive.getDescription();
    Object debugLogMessage = debugDescriptive.getDebugLogMessage();

    assertThat(description).isEmpty();
    assertThat(debugLogMessage).isNotNull();
  }

  @Test
  void shouldHandleSpecialCharactersInDescription() {
    String expectedDescription = "Test description with special characters: \n\t\"'";
    DebugDescriptive debugDescriptive = () -> expectedDescription;

    String description = debugDescriptive.getDescription();
    Object debugLogMessage = debugDescriptive.getDebugLogMessage();

    assertThat(description).isEqualTo(expectedDescription);
    assertThat(debugLogMessage).isNotNull();
    assertThat(debugLogMessage.toString()).contains(expectedDescription);
  }

}