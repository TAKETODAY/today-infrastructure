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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.util.Assert;
import cn.taketoday.web.servlet.FlashMap;
import cn.taketoday.web.servlet.HandlerInterceptor;
import cn.taketoday.web.servlet.ModelAndView;
import cn.taketoday.web.servlet.support.RequestContextUtils;

/**
 * A simple implementation of {@link MvcResult} with setters.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 4.0
 */
class DefaultMvcResult implements MvcResult {

  private static final Object RESULT_NONE = new Object();

  private final MockHttpServletRequest mockRequest;

  private final MockHttpServletResponse mockResponse;

  @Nullable
  private Object handler;

  @Nullable
  private HandlerInterceptor[] interceptors;

  @Nullable
  private ModelAndView modelAndView;

  @Nullable
  private Exception resolvedException;

  private final AtomicReference<Object> asyncResult = new AtomicReference<>(RESULT_NONE);

  @Nullable
  private CountDownLatch asyncDispatchLatch;

  /**
   * Create a new instance with the given request and response.
   */
  public DefaultMvcResult(MockHttpServletRequest request, MockHttpServletResponse response) {
    this.mockRequest = request;
    this.mockResponse = response;
  }

  @Override
  public MockHttpServletRequest getRequest() {
    return this.mockRequest;
  }

  @Override
  public MockHttpServletResponse getResponse() {
    return this.mockResponse;
  }

  public void setHandler(@Nullable Object handler) {
    this.handler = handler;
  }

  @Override
  @Nullable
  public Object getHandler() {
    return this.handler;
  }

  public void setInterceptors(@Nullable HandlerInterceptor... interceptors) {
    this.interceptors = interceptors;
  }

  @Override
  @Nullable
  public HandlerInterceptor[] getInterceptors() {
    return this.interceptors;
  }

  public void setResolvedException(Exception resolvedException) {
    this.resolvedException = resolvedException;
  }

  @Override
  @Nullable
  public Exception getResolvedException() {
    return this.resolvedException;
  }

  public void setModelAndView(@Nullable ModelAndView mav) {
    this.modelAndView = mav;
  }

  @Override
  @Nullable
  public ModelAndView getModelAndView() {
    return this.modelAndView;
  }

  @Override
  public FlashMap getFlashMap() {
    return RequestContextUtils.getOutputFlashMap(this.mockRequest);
  }

  public void setAsyncResult(Object asyncResult) {
    this.asyncResult.set(asyncResult);
  }

  @Override
  public Object getAsyncResult() {
    return getAsyncResult(-1);
  }

  @Override
  public Object getAsyncResult(long timeToWait) {
    if (this.mockRequest.getAsyncContext() != null && timeToWait == -1) {
      long requestTimeout = this.mockRequest.getAsyncContext().getTimeout();
      timeToWait = requestTimeout == -1 ? Long.MAX_VALUE : requestTimeout;
    }
    if (!awaitAsyncDispatch(timeToWait)) {
      throw new IllegalStateException("Async result for handler [" + this.handler + "]" +
              " was not set during the specified timeToWait=" + timeToWait);
    }
    Object result = this.asyncResult.get();
    Assert.state(result != RESULT_NONE, () -> "Async result for handler [" + this.handler + "] was not set");
    return this.asyncResult.get();
  }

  /**
   * True if the latch count reached 0 within the specified timeout.
   */
  private boolean awaitAsyncDispatch(long timeout) {
    Assert.state(this.asyncDispatchLatch != null,
            "The asyncDispatch CountDownLatch was not set by the TestDispatcherServlet.");
    try {
      return this.asyncDispatchLatch.await(timeout, TimeUnit.MILLISECONDS);
    }
    catch (InterruptedException ex) {
      return false;
    }
  }

  void setAsyncDispatchLatch(CountDownLatch asyncDispatchLatch) {
    this.asyncDispatchLatch = asyncDispatchLatch;
  }

}
