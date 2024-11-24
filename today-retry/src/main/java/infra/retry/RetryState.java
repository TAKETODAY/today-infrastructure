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
package infra.retry;

import infra.lang.Nullable;

/**
 * Stateful retry is characterised by having to recognise the items that are being
 * processed, so this interface is used primarily to provide a cache key in between failed
 * attempts. It also provides a hints to the {@link RetryOperations} for optimisations to
 * do with avoidable cache hits and switching to stateless retry if a rollback is not
 * needed.
 *
 * @author Dave Syer
 * @since 4.0
 */
public interface RetryState {

  /**
   * Key representing the state for a retry attempt. Stateful retry is characterised by
   * having to recognise the items that are being processed, so this value is used as a
   * cache key in between failed attempts.
   *
   * @return the key that this state represents
   */
  @Nullable
  Object getKey();

  /**
   * Indicate whether a cache lookup can be avoided. If the key is known ahead of the
   * retry attempt to be fresh (i.e. has never been seen before) then a cache lookup can
   * be avoided if this flag is true.
   *
   * @return true if the state does not require an explicit check for the key
   */
  boolean isForceRefresh();

  /**
   * Check whether this exception requires a rollback. The default is always true, which
   * is conservative, so this method provides an optimisation for switching to stateless
   * retry if there is an exception for which rollback is unnecessary. Example usage
   * would be for a stateful retry to specify a validation exception as not for
   * rollback.
   *
   * @param exception the exception that caused a retry attempt to fail
   * @return true if this exception should cause a rollback
   */
  boolean rollbackFor(Throwable exception);

}
