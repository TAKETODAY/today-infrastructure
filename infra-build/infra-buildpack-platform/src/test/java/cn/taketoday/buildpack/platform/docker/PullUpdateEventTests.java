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

import cn.taketoday.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PullImageUpdateEvent}.
 *
 * @author Phillip Webb
 */
class PullUpdateEventTests extends AbstractJsonTests {

  @Test
  void readValueWhenFullDeserializesJson() throws Exception {
    PullImageUpdateEvent event = getObjectMapper().readValue(getContent("pull-update-full.json"),
            PullImageUpdateEvent.class);
    assertThat(event.getId()).isEqualTo("4f4fb700ef54");
    assertThat(event.getStatus()).isEqualTo("Extracting");
    assertThat(event.getProgressDetail().getCurrent()).isEqualTo(16);
    assertThat(event.getProgressDetail().getTotal()).isEqualTo(32);
    assertThat(event.getProgress()).isEqualTo("[==================================================>]      32B/32B");
  }

  @Test
  void readValueWhenMinimalDeserializesJson() throws Exception {
    PullImageUpdateEvent event = getObjectMapper().readValue(getContent("pull-update-minimal.json"),
            PullImageUpdateEvent.class);
    assertThat(event.getId()).isNull();
    assertThat(event.getStatus()).isEqualTo("Status: Downloaded newer image for paketo-buildpacks/cnb:base");
    assertThat(event.getProgressDetail()).isNull();
    assertThat(event.getProgress()).isNull();
  }

  @Test
  void readValueWhenEmptyDetailsDeserializesJson() throws Exception {
    PullImageUpdateEvent event = getObjectMapper().readValue(getContent("pull-with-empty-details.json"),
            PullImageUpdateEvent.class);
    assertThat(event.getId()).isEqualTo("d837a2a1365e");
    assertThat(event.getStatus()).isEqualTo("Pulling fs layer");
    assertThat(event.getProgressDetail()).isNull();
    assertThat(event.getProgress()).isNull();
  }

}
