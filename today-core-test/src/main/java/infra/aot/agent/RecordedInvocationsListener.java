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

package infra.aot.agent;

/**
 * Listener for {@link RecordedInvocation invocations recorded} by the {@link RuntimeHintsAgent}.
 *
 * @author Brian Clozel
 * @since 4.0
 */
@FunctionalInterface
public interface RecordedInvocationsListener {

  /**
   * Called when an {@link RecordedInvocation invocation has been recorded} by the Java Agent.
   *
   * @param invocation the recorded invocation.
   */
  void onInvocation(RecordedInvocation invocation);

}
