/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.buildpack.platform.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonStream}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class JsonStreamTests extends AbstractJsonTests {

  private final JsonStream jsonStream;

  JsonStreamTests() {
    this.jsonStream = new JsonStream(getObjectMapper());
  }

  @Test
  void getWhenReadingObjectNodeReturnsNodes() throws Exception {
    List<ObjectNode> result = new ArrayList<>();
    this.jsonStream.get(getContent("stream.json"), result::add);
    assertThat(result).hasSize(595);
    assertThat(result.get(594).toString())
            .contains("Status: Downloaded newer image for paketo-buildpacks/cnb:base");
  }

  @Test
  void getWhenReadTypesReturnsTypes() throws Exception {
    List<TestEvent> result = new ArrayList<>();
    this.jsonStream.get(getContent("stream.json"), TestEvent.class, result::add);
    assertThat(result).hasSize(595);
    assertThat(result.get(1).getId()).isEqualTo("5667fdb72017");
    assertThat(result.get(594).getStatus())
            .isEqualTo("Status: Downloaded newer image for paketo-buildpacks/cnb:base");
  }

  /**
   * Event for type deserialization tests.
   */
  static class TestEvent {

    private final String id;

    private final String status;

    @JsonCreator
    TestEvent(String id, String status) {
      this.id = id;
      this.status = status;
    }

    String getId() {
      return this.id;
    }

    String getStatus() {
      return this.status;
    }

  }

}
