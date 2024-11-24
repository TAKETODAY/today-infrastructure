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

import infra.retry.RetryContext;

/**
 * Simple base class for {@link BackOffPolicy} implementations that maintain no state
 * across invocations.
 *
 * @author Rob Harrop
 * @author Dave Syer
 * @since 4.0
 */
public abstract class StatelessBackOffPolicy implements BackOffPolicy {

  /**
   * Delegates directly to the {@link #doBackOff()} method without passing on the
   * {@link BackOffContext} argument which is not needed for stateless implementations.
   */
  @Override
  public final void backOff(BackOffContext backOffContext) throws BackOffInterruptedException {
    doBackOff();
  }

  /**
   * Returns '<code>null</code>'. Subclasses can add behaviour, e.g. initial sleep
   * before first attempt.
   */
  @Override
  public BackOffContext start(RetryContext status) {
    return null;
  }

  /**
   * Sub-classes should implement this method to perform the actual back off.
   *
   * @throws BackOffInterruptedException if the backoff is interrupted
   */
  protected abstract void doBackOff() throws BackOffInterruptedException;

}
