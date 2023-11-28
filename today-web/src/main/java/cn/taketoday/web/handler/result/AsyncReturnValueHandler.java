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

package cn.taketoday.web.handler.result;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.HandlerExceptionHandler;
import cn.taketoday.web.RequestContext;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/23 11:08
 */
public interface AsyncReturnValueHandler {

  /**
   * Handle result of the handler
   *
   * @param context Current HTTP request context
   * @param returnValue Handler execution result
   * Or {@link HandlerExceptionHandler} return value
   * @throws Exception return-value handled failed
   */
  void handleAsyncReturnValue(RequestContext context, @Nullable Object returnValue)
          throws Exception;

}
