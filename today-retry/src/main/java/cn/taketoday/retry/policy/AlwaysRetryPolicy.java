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

/**
 * A {@link RetryPolicy} that always permits a retry. Can also be used as a base class for
 * other policies, e.g. for test purposes as a stub.
 *
 * @author Dave Syer
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
