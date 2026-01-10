/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web.handler;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.List;

import infra.core.ArraySizeTrimmer;
import infra.lang.Assert;
import infra.util.CollectionUtils;
import infra.web.RequestContext;
import infra.web.ReturnValueHandler;

/**
 * select {@link ReturnValueHandler} handler in list
 *
 * @author TODAY 2021/9/3 23:09
 * @since 4.0
 */
public class SelectableReturnValueHandler implements ReturnValueHandler, ArraySizeTrimmer {

  private final List<ReturnValueHandler> internalHandlers;

  public SelectableReturnValueHandler(List<ReturnValueHandler> internalHandlers) {
    Assert.notNull(internalHandlers, "internalHandlers is required");
    this.internalHandlers = internalHandlers;
  }

  @Override
  public boolean supportsHandler(Object handler) {
    return selectHandler(handler, NONE_RETURN_VALUE) != null;
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return selectHandler(null, returnValue) != null;
  }

  /**
   * @param returnValue if returnValue is {@link #NONE_RETURN_VALUE} match handler only
   * @return null if returnValue is {@link #NONE_RETURN_VALUE} or no one matched
   */
  @Nullable
  public final ReturnValueHandler selectHandler(@Nullable Object handler, @Nullable Object returnValue) {
    return ReturnValueHandler.select(internalHandlers, handler, returnValue);
  }

  /**
   * @param context Current HTTP request context
   * @param handler Target HTTP handler
   * @param returnValue Handler execution result
   * @throws ReturnValueHandlerNotFoundException not found ReturnValueHandler
   * @throws Exception throws when write data to response
   */
  @Override
  public void handleReturnValue(RequestContext context, @Nullable Object handler, @Nullable Object returnValue) throws Exception {
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
