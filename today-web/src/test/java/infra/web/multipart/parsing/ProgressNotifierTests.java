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

package infra.web.multipart.parsing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/12/5 14:14
 */
class ProgressNotifierTests {

  @Test
  void testOnBytesRead() {
    ProgressListener listener = mock(ProgressListener.class);
    ProgressNotifier notifier = new ProgressNotifier(listener, 100L);

    notifier.onBytesRead(10);
    verify(listener).update(10L, 100L, 0);

    notifier.onBytesRead(20);
    verify(listener).update(30L, 100L, 0);
  }

  @Test
  void testOnItem() {
    ProgressListener listener = mock(ProgressListener.class);
    ProgressNotifier notifier = new ProgressNotifier(listener, -1L);

    notifier.onItem();
    verify(listener).update(0L, -1L, 1);

    notifier.onItem();
    verify(listener).update(0L, -1L, 2);
  }

  @Test
  void testNotifyListenerWithNullListener() {
    ProgressNotifier notifier = new ProgressNotifier(null, 50L);

    // Should not throw exception
    assertThatNoException().isThrownBy(() -> {
      notifier.onBytesRead(10);
      notifier.onItem();
    });
  }

  @Test
  void testConstructorWithValidListener() {
    ProgressListener listener = mock(ProgressListener.class);
    ProgressNotifier notifier = new ProgressNotifier(listener, 200L);

    notifier.onBytesRead(50);
    notifier.onItem();

    verify(listener).update(50L, 200L, 1);
  }

}