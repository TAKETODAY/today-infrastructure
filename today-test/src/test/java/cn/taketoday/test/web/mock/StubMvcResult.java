/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.test.web.mock;

import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mock.MockRequestContext;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.web.RedirectModel;

/**
 * A stub implementation of the {@link MvcResult} contract.
 *
 * @author Rossen Stoyanchev
 */
public class StubMvcResult implements MvcResult {

  private HttpMockRequestImpl request;

  private Object handler;

  private HandlerInterceptor[] interceptors;

  private Exception resolvedException;

  private ModelAndView mav;

  private RedirectModel flashMap;

  private MockHttpResponseImpl response;
  final RequestContext requestContext;

  public StubMvcResult(HttpMockRequestImpl request,
          Object handler,
          HandlerInterceptor[] interceptors,
          Exception resolvedException,
          ModelAndView mav,
          RedirectModel flashMap,
          MockHttpResponseImpl response) {
    this.request = request;
    this.handler = handler;
    this.interceptors = interceptors;
    this.resolvedException = resolvedException;
    this.mav = mav;
    this.flashMap = flashMap;
    this.response = response;

    this.requestContext = new MockRequestContext(null, request, response);
  }

  @Override
  public HttpMockRequestImpl getRequest() {
    return request;
  }

  @Override
  public Object getHandler() {
    return handler;
  }

  @Override
  public HandlerInterceptor[] getInterceptors() {
    return interceptors;
  }

  @Override
  public Exception getResolvedException() {
    return resolvedException;
  }

  @Override
  public ModelAndView getModelAndView() {
    return mav;
  }

  @Override
  public RedirectModel getFlashMap() {
    return flashMap;
  }

  @Override
  public MockHttpResponseImpl getResponse() {
    return response;
  }

  @Override
  public RequestContext getRequestContext() {
    return requestContext;
  }

  public ModelAndView getMav() {
    return mav;
  }

  public void setMav(ModelAndView mav) {
    this.mav = mav;
  }

  public void setRequest(HttpMockRequestImpl request) {
    this.request = request;
  }

  public void setHandler(Object handler) {
    this.handler = handler;
  }

  public void setInterceptors(HandlerInterceptor[] interceptors) {
    this.interceptors = interceptors;
  }

  public void setResolvedException(Exception resolvedException) {
    this.resolvedException = resolvedException;
  }

  public void setFlashMap(RedirectModel flashMap) {
    this.flashMap = flashMap;
  }

  public void setResponse(MockHttpResponseImpl response) {
    this.response = response;
  }

  @Override
  public Object getAsyncResult() {
    return null;
  }

  @Override
  public Object getAsyncResult(long timeToWait) {
    return null;
  }

}
