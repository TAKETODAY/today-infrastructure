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

package infra.retry.policy;

import infra.retry.RetryContext;
import infra.retry.RetryPolicy;

/**
 * A {@link RetryPolicy} that always permits a retry. Can also be used as a base class for
 * other policies, e.g. for test purposes as a stub.
 *
 * @author Dave Syer
 * @since 4.0
 */
@SuppressWarnings("serial")
public class AlwaysRetryPolicy extends NeverRetryPolicy {

  /**
   * Always returns true.
   *
   * @see RetryPolicy#canRetry(RetryContext)
   */
  public boolean canRetry(RetryContext context) {
    return true;
  }

}
