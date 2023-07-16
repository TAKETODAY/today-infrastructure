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
 * Tests for {@link ProgressUpdateEvent}.
 *
 * @param <E> The event type
 * @author Phillip Webb
 * @author Scott Frederick
 */
abstract class ProgressUpdateEventTests<E extends ProgressUpdateEvent> {

  @Test
  void getStatusReturnsStatus() {
    ProgressUpdateEvent event = createEvent();
    assertThat(event.getStatus()).isEqualTo("status");
  }

  @Test
  void getProgressDetailsReturnsProgressDetails() {
    ProgressUpdateEvent event = createEvent();
    assertThat(event.getProgressDetail().getCurrent()).isOne();
    assertThat(event.getProgressDetail().getTotal()).isEqualTo(2);
  }

  @Test
  void getProgressReturnsProgress() {
    ProgressUpdateEvent event = createEvent();
    assertThat(event.getProgress()).isEqualTo("progress");
  }

  @Test
  void progressDetailIsEmptyWhenCurrentIsNullReturnsTrue() {
    ProgressDetail detail = new ProgressDetail(null, 2);
    assertThat(ProgressDetail.isEmpty(detail)).isTrue();
  }

  @Test
  void progressDetailIsEmptyWhenTotalIsNullReturnsTrue() {
    ProgressDetail detail = new ProgressDetail(1, null);
    assertThat(ProgressDetail.isEmpty(detail)).isTrue();
  }

  @Test
  void progressDetailIsEmptyWhenTotalAndCurrentAreNotNullReturnsFalse() {
    ProgressDetail detail = new ProgressDetail(1, 2);
    assertThat(ProgressDetail.isEmpty(detail)).isFalse();
  }

  protected E createEvent() {
    return createEvent("status", new ProgressDetail(1, 2), "progress");
  }

  protected abstract E createEvent(String status, ProgressDetail progressDetail, String progress);

}
