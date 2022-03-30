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

package cn.taketoday.test.web.servlet;

import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.view.ModelAndView;

/**
 * Provides access to the result of an executed request.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface MvcResult {

  /**
   * Return the performed request.
   *
   * @return the request, never {@code null}
   */
  MockHttpServletRequest getRequest();

  /**
   * Return the resulting response.
   *
   * @return the response, never {@code null}
   */
  MockHttpServletResponse getResponse();

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
  @Nullable
  HandlerInterceptor[] getInterceptors();

  /**
   * Return the {@code ModelAndView} prepared by the handler.
   *
   * @return a {@code ModelAndView}, or {@code null} if none
   */
  @Nullable
  ModelAndView getModelAndView();

  /**
   * Return any exception raised by a handler and successfully resolved
   * through a {@link cn.taketoday.web.handler.HandlerExceptionHandler}.
   *
   * @return an exception, or {@code null} if none
   */
  @Nullable
  Exception getResolvedException();

  /**
   * Return the "output" flash attributes saved during request processing.
   *
   * @return the {@code FlashMap}, possibly empty
   */
  FlashMap getFlashMap();

  /**
   * Get the result of async execution.
   * <p>This method will wait for the async result to be set within the
   * timeout value associated with the async request, see
   * {@link cn.taketoday.mock.web.MockAsyncContext#setTimeout
   * MockAsyncContext#setTimeout}. Alternatively, use
   * {@link #getAsyncResult(long)} to specify the amount of time to wait.
   *
   * @throws IllegalStateException if the async result was not set
   */
  Object getAsyncResult();

  /**
   * Get the result of async execution and wait if necessary.
   *
   * @param timeToWait how long to wait for the async result to be set, in
   * milliseconds; if -1, then fall back on the timeout value associated with
   * the async request, see
   * {@link cn.taketoday.mock.web.MockAsyncContext#setTimeout
   * MockAsyncContext#setTimeout} for more details.
   * @throws IllegalStateException if the async result was not set
   */
  Object getAsyncResult(long timeToWait);

}
