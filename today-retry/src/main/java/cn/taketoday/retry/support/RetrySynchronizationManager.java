/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.retry.support;

import cn.taketoday.retry.RetryCallback;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryOperations;

/**
 * Global variable support for retry clients. Normally it is not necessary for clients to
 * be aware of the surrounding environment because a {@link RetryCallback} can always use
 * the context it is passed by the enclosing {@link RetryOperations}. But occasionally it
 * might be helpful to have lower level access to the ongoing {@link RetryContext} so we
 * provide a global accessor here. The mutator methods ({@link #clear()} and
 * {@link #register(RetryContext)} should not be used except internally by
 * {@link RetryOperations} implementations.
 *
 * @author Dave Syer
 */
public final class RetrySynchronizationManager {

  private RetrySynchronizationManager() {
  }

  private static final ThreadLocal<RetryContext> context = new ThreadLocal<RetryContext>();

  /**
   * Public accessor for the locally enclosing {@link RetryContext}.
   *
   * @return the current retry context, or null if there isn't one
   */
  public static RetryContext getContext() {
    RetryContext result = context.get();
    return result;
  }

  /**
   * Method for registering a context - should only be used by {@link RetryOperations}
   * implementations to ensure that {@link #getContext()} always returns the correct
   * value.
   *
   * @param context the new context to register
   * @return the old context if there was one
   */
  public static RetryContext register(RetryContext context) {
    RetryContext oldContext = getContext();
    RetrySynchronizationManager.context.set(context);
    return oldContext;
  }

  /**
   * Clear the current context at the end of a batch - should only be used by
   * {@link RetryOperations} implementations.
   *
   * @return the old value if there was one.
   */
  public static RetryContext clear() {
    RetryContext value = getContext();
    RetryContext parent = value == null ? null : value.getParent();
    RetrySynchronizationManager.context.set(parent);
    return value;
  }

}
