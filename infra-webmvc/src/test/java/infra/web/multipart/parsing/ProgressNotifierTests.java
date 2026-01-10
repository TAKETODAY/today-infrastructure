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