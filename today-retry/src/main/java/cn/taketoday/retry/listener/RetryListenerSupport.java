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

package cn.taketoday.retry.listener;

import cn.taketoday.retry.RetryCallback;
import cn.taketoday.retry.RetryListener;
import cn.taketoday.retry.RetryContext;

/**
 * Empty method implementation of {@link RetryListener}.
 *
 * @author Dave Syer
 */
public class RetryListenerSupport implements RetryListener {

  public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
          Throwable throwable) {
  }

  public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
          Throwable throwable) {
  }

  public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
    return true;
  }

}
