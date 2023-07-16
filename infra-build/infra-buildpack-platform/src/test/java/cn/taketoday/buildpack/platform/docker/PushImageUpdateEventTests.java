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

import cn.taketoday.buildpack.platform.docker.ProgressUpdateEvent.ProgressDetail;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PushImageUpdateEvent}.
 *
 * @author Scott Frederick
 */
class PushImageUpdateEventTests extends ProgressUpdateEventTests<PushImageUpdateEvent> {

  @Test
  void getIdReturnsId() {
    PushImageUpdateEvent event = createEvent();
    assertThat(event.getId()).isEqualTo("id");
  }

  @Test
  void getErrorReturnsErrorDetail() {
    PushImageUpdateEvent event = new PushImageUpdateEvent(null, null, null, null,
            new PushImageUpdateEvent.ErrorDetail("test message"));
    assertThat(event.getErrorDetail().getMessage()).isEqualTo("test message");
  }

  @Override
  protected PushImageUpdateEvent createEvent(String status, ProgressDetail progressDetail, String progress) {
    return new PushImageUpdateEvent("id", status, progressDetail, progress, null);
  }

}
