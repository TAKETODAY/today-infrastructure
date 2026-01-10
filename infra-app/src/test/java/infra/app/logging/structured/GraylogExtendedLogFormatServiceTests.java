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

package infra.app.logging.structured;

import org.junit.jupiter.api.Test;

import infra.app.json.JsonWriter;
import infra.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GraylogExtendedLogFormatService}.
 *
 * @author Samuel Lissner
 */
class GraylogExtendedLogFormatServiceTests {

  @Test
  void getBindsFromEnvironment() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("logging.structured.gelf.service.name", "infra");
    environment.setProperty("logging.structured.gelf.service.version", "1.2.3");
    GraylogExtendedLogFormatService service = GraylogExtendedLogFormatService.get(environment);
    assertThat(service).isEqualTo(new GraylogExtendedLogFormatService("infra", "1.2.3"));
  }

  @Test
  void getWhenNoServiceNameUsesApplicationName() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("app.name", "infra");
    GraylogExtendedLogFormatService service = GraylogExtendedLogFormatService.get(environment);
    assertThat(service).isEqualTo(new GraylogExtendedLogFormatService("infra", null));
  }

  @Test
  void getWhenNoServiceVersionUsesApplicationVersion() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("app.version", "1.2.3");
    GraylogExtendedLogFormatService service = GraylogExtendedLogFormatService.get(environment);
    assertThat(service).isEqualTo(new GraylogExtendedLogFormatService(null, "1.2.3"));
  }

  @Test
  void getWhenNoPropertiesToBind() {
    MockEnvironment environment = new MockEnvironment();
    GraylogExtendedLogFormatService service = GraylogExtendedLogFormatService.get(environment);
    assertThat(service).isEqualTo(new GraylogExtendedLogFormatService(null, null));
  }

  @Test
  void addToJsonMembersCreatesValidJson() {
    GraylogExtendedLogFormatService service = new GraylogExtendedLogFormatService("infra", "1.2.3");
    JsonWriter<GraylogExtendedLogFormatService> writer = JsonWriter.of(service::jsonMembers);
    assertThat(writer.writeToString(service)).isEqualTo("{\"host\":\"infra\",\"_service_version\":\"1.2.3\"}");
  }

}
