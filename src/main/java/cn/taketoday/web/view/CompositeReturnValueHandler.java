/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.view;

import java.io.IOException;
import java.util.List;

import cn.taketoday.core.Assert;
import cn.taketoday.core.Nullable;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY 2021/9/3 23:09
 */
public class CompositeReturnValueHandler implements RuntimeReturnValueHandler {

  private final ReturnValueHandler[] returnValueHandlers;
  private final RuntimeReturnValueHandler[] runtimeHandlers;

  public CompositeReturnValueHandler(List<ReturnValueHandler> returnValueHandlers) {
    Assert.notNull(returnValueHandlers, "returnValueHandlers");
    this.returnValueHandlers = returnValueHandlers.toArray(new ReturnValueHandler[0]);
    this.runtimeHandlers = RuntimeReturnValueHandler.filterArray(returnValueHandlers);
  }

  @Override
  public boolean supportsHandler(Object handler) {
    return selectHandler(handler, NONE_RETURN_VALUE) != null;
  }

  @Override
  public boolean supportsReturnValue(Object returnValue) {
    return selectHandler(null, returnValue) != null;
  }

  @Nullable
  public final ReturnValueHandler selectHandler(@Nullable Object handler, @Nullable Object returnValue) {
    if (handler != null) {
      for (final ReturnValueHandler returnValueHandler : returnValueHandlers) {
        if (returnValueHandler.supportsHandler(handler)) {
          return returnValueHandler;
        }
      }
    }
    // test return-value
    if (returnValue != NONE_RETURN_VALUE) {
      for (final RuntimeReturnValueHandler returnValueHandler : runtimeHandlers) {
        if (returnValueHandler.supportsReturnValue(returnValue)) {
          return returnValueHandler;
        }
      }
    }
    return null;
  }

  /**
   * @param context
   *         Current HTTP request context
   * @param handler
   *         Target HTTP handler
   * @param returnValue
   *         Handler execution result
   *
   * @throws ReturnValueHandlerNotFoundException
   *         not found ReturnValueHandler
   */
  @Override
  public void handleReturnValue(RequestContext context, Object handler, Object returnValue) throws IOException {
    ReturnValueHandler selected = selectHandler(handler, returnValue);
    if (selected != null && selected != this) {
      selected.handleReturnValue(context, handler, returnValue);
    }
    else {
      throw new ReturnValueHandlerNotFoundException(returnValue, handler);
    }
  }

  public final void handleSelected(
          RequestContext context, @Nullable Object handler, @Nullable Object returnValue) throws IOException {
    ReturnValueHandler selected = selectHandler(handler, returnValue);
    if (selected != null && selected != this) {
      selected.handleReturnValue(context, handler, returnValue);
    }
  }

}
