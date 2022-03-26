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
package cn.taketoday.retry;

/**
 * Callback for stateful retry after all tries are exhausted.
 *
 * @param <T> the type that is returned from the recovery
 * @author Dave Syer
 * @since 4.0
 */
public interface RecoveryCallback<T> {

  /**
   * @param context the current retry context
   * @return an Object that can be used to replace the callback result that failed
   * @throws Exception when something goes wrong
   */
  T recover(RetryContext context) throws Exception;

}
