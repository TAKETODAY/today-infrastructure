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

package cn.taketoday.web.handler;

import java.io.IOException;
import java.util.List;

import cn.taketoday.core.ArraySizeTrimmer;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.handler.result.SmartReturnValueHandler;

/**
 * select {@link ReturnValueHandler} handler in list
 *
 * @author TODAY 2021/9/3 23:09
 * @since 4.0
 */
public class SelectableReturnValueHandler implements ReturnValueHandler, ArraySizeTrimmer {
  private final List<ReturnValueHandler> internalHandlers;

  public SelectableReturnValueHandler(List<ReturnValueHandler> internalHandlers) {
    Assert.notNull(internalHandlers, "internalHandlers must not be null");
    this.internalHandlers = internalHandlers;
  }

  @Override
  public boolean supportsHandler(Object handler) {
    return selectHandler(handler, NONE_RETURN_VALUE) != null;
  }

  @Override
  public boolean supportsReturnValue(Object returnValue) {
    return selectHandler(null, returnValue) != null;
  }

  /**
   * @param returnValue if returnValue is {@link #NONE_RETURN_VALUE} match handler only
   * @return null if returnValue is {@link #NONE_RETURN_VALUE} or no one matched
   */
  @Nullable
  public final ReturnValueHandler selectHandler(@Nullable Object handler, @Nullable Object returnValue) {
    if (returnValue != NONE_RETURN_VALUE) {
      if (handler != null) {
        // match handler and return-value
        for (ReturnValueHandler returnValueHandler : internalHandlers) {
          if (returnValueHandler instanceof SmartReturnValueHandler smartHandler) {
            // smart handler
            if (smartHandler.supportsHandler(handler, returnValue)) {
              return returnValueHandler;
            }
          }
          else {
            if (returnValueHandler.supportsHandler(handler)
                    || returnValueHandler.supportsReturnValue(returnValue)) {
              return returnValueHandler;
            }
          }
        }
      }
      else {
        // match return-value only
        for (ReturnValueHandler returnValueHandler : internalHandlers) {
          if (returnValueHandler.supportsReturnValue(returnValue)) {
            return returnValueHandler;
          }
        }
      }
    }
    else if (handler != null) {
      // match handler only
      for (ReturnValueHandler returnValueHandler : internalHandlers) {
        if (returnValueHandler.supportsHandler(handler)) {
          return returnValueHandler;
        }
      }
    }
    return null;
  }

  /**
   * @param context Current HTTP request context
   * @param handler Target HTTP handler
   * @param returnValue Handler execution result
   * @throws ReturnValueHandlerNotFoundException not found ReturnValueHandler
   * @throws Exception throws when write data to response
   */
  @Override
  public void handleReturnValue(RequestContext context, Object handler, Object returnValue) throws Exception {
    if (handleSelectively(context, handler, returnValue) == null) {
      throw new ReturnValueHandlerNotFoundException(returnValue, handler);
    }
  }

  /**
   * select a handler and handle return-value with selected handler
   *
   * @param context current request context
   * @param handler web request handler
   * @param returnValue handler execution result
   * @return selected handler or which handler handled this result(return-value)
   * @throws IOException throws when write data to response
   */
  @Nullable
  public final ReturnValueHandler handleSelectively(
          RequestContext context, @Nullable Object handler, @Nullable Object returnValue) throws Exception {
    ReturnValueHandler selected = selectHandler(handler, returnValue);
    if (selected != null && selected != this) {
      selected.handleReturnValue(context, handler, returnValue);
      return selected;
    }
    // none one
    return null;
  }

  public List<ReturnValueHandler> getInternalHandlers() {
    return internalHandlers;
  }

  @Override
  public void trimToSize() {
    CollectionUtils.trimToSize(internalHandlers);
  }

}
