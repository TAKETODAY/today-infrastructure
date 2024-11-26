/*
 * Copyright 2017 - 2024 the original author or authors.
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
