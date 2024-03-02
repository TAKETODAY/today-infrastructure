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

package cn.taketoday.util.concurrent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/27 23:38
 */
class FutureListenersTests {

  @Test
  void add() {
    ProgressiveListener second = new ProgressiveListener();
    FutureListeners futureListeners = new FutureListeners(new Listener(), second);
    assertThat(futureListeners.listeners).hasSize(2);
    assertThat(futureListeners.progressiveListeners).isEqualTo(second);
    assertThat(futureListeners.size).isEqualTo(2);

    futureListeners.add(new Listener());

    assertThat(futureListeners.listeners).hasSize(4);
    assertThat(futureListeners.progressiveListeners).isEqualTo(second);
    assertThat(futureListeners.size).isEqualTo(3);

    //
    futureListeners.add(new ProgressiveListener());

    assertThat(futureListeners.listeners).hasSize(4);
    assertThat(futureListeners.progressiveListeners).isInstanceOf(ProgressiveFutureListener[].class);
    assertThat(futureListeners.size).isEqualTo(4);

    futureListeners = new FutureListeners(new ProgressiveListener(), second);

    assertThat(futureListeners.listeners).hasSize(2);
    assertThat(futureListeners.progressiveListeners).isInstanceOf(ProgressiveFutureListener[].class);
    assertThat(futureListeners.size).isEqualTo(2);

    futureListeners = new FutureListeners(new ProgressiveListener(), new Listener());

    assertThat(futureListeners.listeners).hasSize(2);
    assertThat(futureListeners.progressiveListeners).isInstanceOf(ProgressiveFutureListener.class);
    assertThat(futureListeners.size).isEqualTo(2);

  }

  @Test
  void remove() {
    ProgressiveListener second = new ProgressiveListener();
    Listener first = new Listener();
    FutureListeners futureListeners = new FutureListeners(first, second);
    assertThat(futureListeners.listeners).hasSize(2);
    assertThat(futureListeners.progressiveListeners).isEqualTo(second);
    assertThat(futureListeners.size).isEqualTo(2);

    futureListeners.remove(first);

    assertThat(futureListeners.listeners).hasSize(2);
    assertThat(futureListeners.progressiveListeners).isEqualTo(second);
    assertThat(futureListeners.size).isEqualTo(1);

    futureListeners.remove(second);

    assertThat(futureListeners.listeners).hasSize(2);
    assertThat(futureListeners.progressiveListeners).isNull();
    assertThat(futureListeners.size).isEqualTo(0);

    ProgressiveListener progressiveListener = new ProgressiveListener();
    ProgressiveListener progressiveListener1 = new ProgressiveListener();

    futureListeners.add(first);
    futureListeners.add(second);
    futureListeners.add(new Listener());
    futureListeners.add(new Listener());

    futureListeners.add(progressiveListener);
    futureListeners.add(progressiveListener1);

    assertThat(futureListeners.listeners).hasSize(8);
    assertThat(futureListeners.progressiveListeners).isInstanceOf(ProgressiveFutureListener[].class);
    assertThat(futureListeners.size).isEqualTo(6);

    assertThat((ProgressiveFutureListener<?>[]) futureListeners.progressiveListeners).hasSize(3)
            .containsExactly(second, progressiveListener, progressiveListener1);

    futureListeners.remove(progressiveListener);

    assertThat(futureListeners.listeners).hasSize(8);
    assertThat(futureListeners.size).isEqualTo(5);
    assertThat((ProgressiveFutureListener<?>[]) futureListeners.progressiveListeners).hasSize(2)
            .containsExactly(second, progressiveListener1);

    futureListeners.remove(progressiveListener1);
    assertThat(futureListeners.listeners).hasSize(8);
    assertThat(futureListeners.size).isEqualTo(4);
    assertThat(futureListeners.progressiveListeners).isEqualTo(second);

    futureListeners.remove(new Listener());

    assertThat(futureListeners.listeners).hasSize(8);
    assertThat(futureListeners.size).isEqualTo(4);
    assertThat(futureListeners.progressiveListeners).isEqualTo(second);
  }

  static class Listener implements FutureListener<ListenableFuture<String>> {

    @Override
    public void operationComplete(ListenableFuture<String> future) throws Exception {

    }
  }

  static class ProgressiveListener implements ProgressiveFutureListener<ProgressiveFuture<String>> {

    @Override
    public void operationComplete(ProgressiveFuture<String> future) throws Exception {

    }

    @Override
    public void operationProgressed(ProgressiveFuture<String> future, long progress, long total) throws Exception {

    }
  }

}