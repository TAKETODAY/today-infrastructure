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

package infra.aot.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/1 17:12
 */
class RecordedInvocationsPublisherTests {

  @Test
  void shouldAddAndRemoveListener() {
    RecordedInvocationsListener listener = invocation -> { };

    assertThatCode(() -> {
      RecordedInvocationsPublisher.addListener(listener);
      RecordedInvocationsPublisher.removeListener(listener);
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleNullListenerInAdd() {
    assertThatCode(() -> {
      RecordedInvocationsPublisher.addListener(null);
      RecordedInvocationsPublisher.removeListener(null);
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandlePublishWithNullInvocation() {
    assertThatCode(() -> {
      RecordedInvocationsPublisher.publish(null);
    }).doesNotThrowAnyException();
  }

}