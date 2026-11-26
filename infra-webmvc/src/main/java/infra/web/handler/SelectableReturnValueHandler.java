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

import java.util.List;

import infra.core.ArraySizeTrimmer;
import infra.util.Assert;
import infra.util.CollectionUtils;
import infra.web.HttpContext;
import infra.web.ReturnValueHandler;

/**
 * Selects and delegates to the first matching {@link ReturnValueHandler}
 * in list order.
 *
 * <p>Use {@link #selectHandler(Object, Object)} to find a match without
 * invoking it, or {@link #handleSelectively(HttpContext, Object, Object)}
 * to delegate and find out which handler processed the result.
 *
 * @author TODAY 2021/9/3 23:09
 * @see ReturnValueHandler#select(Iterable, Object, Object)
 * @since 4.0
 */
public class SelectableReturnValueHandler implements ReturnValueHandler, ArraySizeTrimmer {

  private final List<ReturnValueHandler> internalHandlers;

  /**
   * Create a selectable handler backed by the supplied list without copying it.
   *
   * @param internalHandlers the candidates in selection order (never {@code null})
   */
  public SelectableReturnValueHandler(List<ReturnValueHandler> internalHandlers) {
    Assert.notNull(internalHandlers, "internalHandlers is required");
    this.internalHandlers = internalHandlers;
  }

  /**
   * {@inheritDoc}
   * <p>Checks only handler support, without a return value.
   */
  @Override
  public boolean supportsHandler(Object handler) {
    return selectHandler(handler, NONE_RETURN_VALUE) != null;
  }

  /**
   * {@inheritDoc}
   * <p>Checks only return value support, without a request handler.
   */
  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return selectHandler(null, returnValue) != null;
  }

  /**
   * Select the first matching handler in list order without invoking it.
   * A {@code null} return value participates in matching, whereas
   * {@link #NONE_RETURN_VALUE} disables return value checks.
   *
   * @param handler the request handler, or {@code null} to check only the return value
   * @param returnValue the result to check; may be {@code null} or {@link #NONE_RETURN_VALUE}
   * @return the first matching handler, or {@code null} if none matches or if
   * {@code handler} is {@code null} and {@code returnValue} is {@code NONE_RETURN_VALUE}
   * @see ReturnValueHandler#select(Iterable, Object, Object)
   */
  @Nullable
  public final ReturnValueHandler selectHandler(@Nullable Object handler, @Nullable Object returnValue) {
    return ReturnValueHandler.select(internalHandlers, handler, returnValue);
  }

  /**
   * Delegate processing to the first matching handler.
   *
   * @param context the current HTTP request and response context
   * @param handler the request handler, which may be {@code null}
   * @param returnValue the result to process; may be {@code null} or {@link #NONE_RETURN_VALUE}
   * @throws ReturnValueHandlerNotFoundException if no handler matches or the
   * selected handler is this instance
   * @throws Exception if the selected handler fails to process the return value
   * @see #handleSelectively(HttpContext, Object, Object)
   */
  @Override
  public void handleReturnValue(HttpContext context, @Nullable Object handler, @Nullable Object returnValue) throws Exception {
    if (handleSelectively(context, handler, returnValue) == null) {
      throw new ReturnValueHandlerNotFoundException(returnValue, handler);
    }
  }

  /**
   * Select and invoke the first matching handler. If that handler is this
   * instance, do not invoke it or try later candidates.
   *
   * @param context the current HTTP request and response context
   * @param handler the request handler, which may be {@code null}
   * @param returnValue the result to process; may be {@code null} or {@link #NONE_RETURN_VALUE}
   * @return the handler that processed the result, or {@code null} if no handler
   * matches or the selected handler is this instance
   * @throws Exception if the selected handler fails to process the return value
   * @see #selectHandler(Object, Object)
   */
  @Nullable
  public final ReturnValueHandler handleSelectively(
          HttpContext context, @Nullable Object handler, @Nullable Object returnValue) throws Exception {
    ReturnValueHandler selected = selectHandler(handler, returnValue);
    if (selected != null && selected != this) {
      selected.handleReturnValue(context, handler, returnValue);
      return selected;
    }
    // none one
    return null;
  }

  /**
   * Return the original list of candidates. Changes to that list, if mutable,
   * affect subsequent selection.
   *
   * @return the candidates in selection order
   */
  public List<ReturnValueHandler> getInternalHandlers() {
    return internalHandlers;
  }

  /**
   * Trim the backing list's capacity if it is an {@link java.util.ArrayList}
   * or implements {@link ArraySizeTrimmer}; otherwise do nothing.
   */
  @Override
  public void trimToSize() {
    CollectionUtils.trimToSize(internalHandlers);
  }

}
