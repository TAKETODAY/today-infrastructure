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

import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.HandlerInterceptor;
import infra.web.RedirectModel;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;
import infra.web.view.ModelAndView;

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
