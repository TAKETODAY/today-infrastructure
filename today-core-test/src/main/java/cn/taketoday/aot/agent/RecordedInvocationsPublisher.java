/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.aot.agent;

import java.util.ArrayDeque;
import java.util.Deque;

import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.aot.hint.RuntimeHints;

/**
 * Publishes invocations on method relevant to {@link RuntimeHints},
 * as they are recorded by the {@link RuntimeHintsAgent}.
 * <p>Components interested in this can {@link #addListener(RecordedInvocationsListener) register}
 * and {@link #removeListener(RecordedInvocationsListener) deregister} themselves at any point at runtime.
 *
 * @author Brian Clozel
 * @since 4.0
 */
public abstract class RecordedInvocationsPublisher {

  private static final Deque<RecordedInvocationsListener> LISTENERS = new ArrayDeque<>();

  private RecordedInvocationsPublisher() {

  }

  /**
   * Register the given invocations listener.
   *
   * @param listener the listener to be notified about recorded invocations
   */
  public static void addListener(RecordedInvocationsListener listener) {
    LISTENERS.addLast(listener);
  }

  /**
   * Deregister the given invocations listener.
   *
   * @param listener the listener that was notified about recorded invocations
   */
  public static void removeListener(RecordedInvocationsListener listener) {
    LISTENERS.remove(listener);
  }

  /**
   * Record an invocation on reflection methods covered by {@link ReflectionHints}.
   */
  static void publish(RecordedInvocation invocation) {
    LISTENERS.forEach(listener -> listener.onInvocation(invocation));
  }

}
