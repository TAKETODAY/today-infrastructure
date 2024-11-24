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

package infra.retry.backoff;

import infra.lang.Nullable;
import infra.retry.RetryContext;
import infra.retry.support.RetryTemplate;

/**
 * Strategy interface to control back off between attempts in a single
 * {@link RetryTemplate retry operation}.
 *
 * Implementations are expected to be thread-safe and should be designed for concurrent
 * access. Configuration for each implementation is also expected to be thread-safe but
 * need not be suitable for high load concurrent access.
 *
 * For each block of retry operations the {@link #start} method is called and
 * implementations can return an implementation-specific
 *
 * {@link BackOffContext} that can be used to track state through subsequent back off
 * invocations. Each back off process is handled via a call to {@link #backOff}.
 *
 * The {@link RetryTemplate} will pass in the
 * corresponding {@link BackOffContext} object created by the call to {@link #start}.
 *
 * @author Rob Harrop
 * @author Dave Syer
 * @since 4.0
 */
public interface BackOffPolicy {

  /**
   * Start a new block of back off operations. Implementations can choose to pause when
   * this method is called, but normally it returns immediately.
   *
   * @param context the {@link RetryContext} context, which might contain information
   * that we can use to decide how to proceed.
   * @return the implementation-specific {@link BackOffContext} or '<code>null</code>'.
   */
  @Nullable
  BackOffContext start(RetryContext context);

  /**
   * Back off/pause in an implementation-specific fashion. The passed in
   * {@link BackOffContext} corresponds to the one created by the call to {@link #start}
   * for a given retry operation set.
   *
   * @param backOffContext the {@link BackOffContext}
   * @throws BackOffInterruptedException if the attempt at back off is interrupted.
   */
  void backOff(BackOffContext backOffContext) throws BackOffInterruptedException;

}
