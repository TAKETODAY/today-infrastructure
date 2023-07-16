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

package cn.taketoday.buildpack.platform.docker;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LogUpdateEvent}.
 *
 * @author Phillip Webb
 */
class LogUpdateEventTests {

  @Test
  void readAllWhenSimpleStreamReturnsEvents() throws Exception {
    List<LogUpdateEvent> events = readAll("log-update-event.stream");
    assertThat(events).hasSize(7);
    assertThat(events.get(0))
            .hasToString("Analyzing image '307c032c4ceaa6330b6c02af945a1fe56a8c3c27c28268574b217c1d38b093cf'");
    assertThat(events.get(1))
            .hasToString("Writing metadata for uncached layer 'org.cloudfoundry.openjdk:openjdk-jre'");
    assertThat(events.get(2))
            .hasToString("Using cached launch layer 'org.cloudfoundry.jvmapplication:executable-jar'");
  }

  @Test
  void readAllWhenAnsiStreamReturnsEvents() throws Exception {
    List<LogUpdateEvent> events = readAll("log-update-event-ansi.stream");
    assertThat(events).hasSize(20);
    assertThat(events.get(0).toString()).isEmpty();
    assertThat(events.get(1)).hasToString("Cloud Foundry OpenJDK Buildpack v1.0.64");
    assertThat(events.get(2)).hasToString("  OpenJDK JRE 11.0.5: Reusing cached layer");
  }

  @Test
  void readSucceedsWhenStreamTypeIsInvalid() throws IOException {
    List<LogUpdateEvent> events = readAll("log-update-event-invalid-stream-type.stream");
    assertThat(events).hasSize(1);
    assertThat(events.get(0)).hasToString("Stream type is out of bounds. Must be >= 0 and < 3, but was 3");
  }

  private List<LogUpdateEvent> readAll(String name) throws IOException {
    List<LogUpdateEvent> events = new ArrayList<>();
    try (InputStream inputStream = getClass().getResourceAsStream(name)) {
      LogUpdateEvent.readAll(inputStream, events::add);
    }
    return events;
  }

}
