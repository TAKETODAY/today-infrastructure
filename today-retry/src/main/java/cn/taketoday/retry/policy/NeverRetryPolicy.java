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

package cn.taketoday.retry.policy;

import cn.taketoday.retry.RetryPolicy;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.context.RetryContextSupport;

/**
 * A {@link RetryPolicy} that allows the first attempt but never permits a retry. Also be
 * used as a base class for other policies, e.g. for test purposes as a stub.
 *
 * @author Dave Syer
 */
@SuppressWarnings("serial")
public class NeverRetryPolicy implements RetryPolicy {

  /**
   * Returns false after the first exception. So there is always one try, and then the
   * retry is prevented.
   *
   * @see RetryPolicy#canRetry(RetryContext)
   */
  public boolean canRetry(RetryContext context) {
    return !((NeverRetryContext) context).isFinished();
  }

  /**
   * Do nothing.
   *
   * @see RetryPolicy#close(RetryContext)
   */
  public void close(RetryContext context) {
    // no-op
  }

  /**
   * Return a context that can respond to early termination requests, but does nothing
   * else.
   *
   * @see RetryPolicy#open(RetryContext)
   */
  public RetryContext open(RetryContext parent) {
    return new NeverRetryContext(parent);
  }

  /**
   * Make the throwable available for downstream use through the context.
   *
   * @see RetryPolicy#registerThrowable(RetryContext,
   * Throwable)
   */
  public void registerThrowable(RetryContext context, Throwable throwable) {
    ((NeverRetryContext) context).setFinished();
    ((RetryContextSupport) context).registerThrowable(throwable);
  }

  /**
   * Special context object for {@link NeverRetryPolicy}. Implements a flag with a
   * similar function to {@link RetryContext#isExhaustedOnly()}, but kept separate so
   * that if subclasses of {@link NeverRetryPolicy} need to they can modify the
   * behaviour of {@link NeverRetryPolicy#canRetry(RetryContext)} without affecting
   * {@link RetryContext#isExhaustedOnly()}.
   *
   * @author Dave Syer
   */
  private static class NeverRetryContext extends RetryContextSupport {

    private boolean finished = false;

    public NeverRetryContext(RetryContext parent) {
      super(parent);
    }

    public boolean isFinished() {
      return finished;
    }

    public void setFinished() {
      this.finished = true;
    }

  }

}
