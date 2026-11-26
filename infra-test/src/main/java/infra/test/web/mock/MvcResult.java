/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.web.mock;

import org.jspecify.annotations.Nullable;

import infra.core.Conventions;
import infra.web.HandlerExceptionHandler;
import infra.web.HandlerInterceptor;
import infra.web.ServerHttpExchange;
import infra.web.RedirectModel;
import infra.web.mock.MockAsyncContext;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;
import infra.web.view.ModelAndView;

/**
 * Provides access to the result of an executed request.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface MvcResult {

  String VIEW_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          MvcResult.class, "view");

  String VIEW_NAME_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          MvcResult.class, "view-name");

  String MODEL_AND_VIEW_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          MvcResult.class, "model-view");

  /**
   * Return the performed request.
   *
   * @return the request, never {@code null}
   */
  MockRequest getRequest();

  /**
   * Return the resulting response.
   *
   * @return the response, never {@code null}
   */
  MockResponse getResponse();

  ServerHttpExchange getExchange();

  /**
   * Return the executed handler.
   *
   * @return the handler, possibly {@code null} if none were executed
   */
  @Nullable
  Object getHandler();

  /**
   * Return interceptors around the handler.
   *
   * @return interceptors, or {@code null} if none were selected
   */
  HandlerInterceptor @Nullable [] getInterceptors();

  /**
   * Return the {@code ModelAndView} prepared by the handler.
   *
   * @return a {@code ModelAndView}, or {@code null} if none
   */
  @Nullable
  ModelAndView getModelAndView();

  /**
   * Return any exception raised by a handler and successfully resolved
   * through a {@link HandlerExceptionHandler}.
   *
   * @return an exception, or {@code null} if none
   */
  @Nullable
  Throwable getResolvedException();

  /**
   * Return the exception that was thrown unexpectedly while processing the
   * request, if any.
   */
  @Nullable
  Throwable getUnresolvedException();

  /**
   * Return the output redirect model populated during request processing.
   *
   * @return the {@code RedirectModel}, possibly empty
   */
  RedirectModel getOutputRedirectModel();

  /**
   * Get the result of async execution.
   * <p>This method will wait for the async result to be set within the
   * timeout value associated with the async request, see
   * {@link MockAsyncContext#setTimeout
   * MockAsyncContext#setTimeout}. Alternatively, use
   * {@link #getAsyncResult(long)} to specify the amount of time to wait.
   * <p>An async request timeout of zero or less specifies no timeout, in which
   * case this method waits indefinitely.
   *
   * @throws IllegalStateException if the async result was not set
   */
  Object getAsyncResult();

  /**
   * Get the result of async execution and wait if necessary.
   * <p>This wait limit applies only to the calling test thread. It does not
   * change the timeout configured for the async request.
   *
   * @param timeToWait the wait limit in milliseconds, {@code 0} to check
   * without waiting, or {@code -1} to use the request timeout as in
   * {@link #getAsyncResult()}
   * @throws IllegalStateException if the async result was not set
   */
  Object getAsyncResult(long timeToWait);

}
