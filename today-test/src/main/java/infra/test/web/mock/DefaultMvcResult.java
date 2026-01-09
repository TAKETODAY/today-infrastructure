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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import infra.core.Conventions;
import infra.lang.Assert;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.ui.ModelMap;
import infra.web.BindingContext;
import infra.web.HandlerInterceptor;
import infra.web.RedirectModel;
import infra.web.RequestContext;
import infra.web.RequestContextUtils;
import infra.web.view.ModelAndView;
import infra.web.view.View;

/**
 * A simple implementation of {@link MvcResult} with setters.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 4.0
 */
class DefaultMvcResult implements MvcResult {

  static final String MVC_RESULT_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          MvcResult.class, "mvc-result");

  private static final Object RESULT_NONE = new Object();

  private final HttpMockRequestImpl mockRequest;
  private final MockHttpResponseImpl mockResponse;

  private RequestContext requestContext;

  @Nullable
  private Object handler;

  @Nullable
  private HandlerInterceptor[] interceptors;

  @Nullable
  private ModelAndView modelAndView;

  @Nullable
  private Throwable resolvedException;

  private final AtomicReference<Object> asyncResult = new AtomicReference<>(RESULT_NONE);

  @Nullable
  private CountDownLatch asyncDispatchLatch;

  /**
   * Create a new instance with the given request and response.
   */
  public DefaultMvcResult(HttpMockRequestImpl request,
          MockHttpResponseImpl response, RequestContext requestContext) {
    this.mockRequest = request;
    this.mockResponse = response;
    this.requestContext = requestContext;
    request.setAttribute(MVC_RESULT_ATTRIBUTE, this);
  }

  @Override
  public HttpMockRequestImpl getRequest() {
    return this.mockRequest;
  }

  @Override
  public MockHttpResponseImpl getResponse() {
    return this.mockResponse;
  }

  @Override
  public RequestContext getRequestContext() {
    return requestContext;
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

  public void setResolvedException(Throwable resolvedException) {
    this.resolvedException = resolvedException;
  }

  @Override
  @Nullable
  public Throwable getResolvedException() {
    return this.resolvedException;
  }

  public void setModelAndView(@Nullable ModelAndView mav) {
    this.modelAndView = mav;
  }

  @Override
  @Nullable
  public ModelAndView getModelAndView() {
    if (modelAndView == null) {
      Object attribute = requestContext.getAttribute(MODEL_AND_VIEW_ATTRIBUTE);
      if (attribute instanceof ModelAndView view) {
        this.modelAndView = view;
      }
      else if (requestContext.hasBinding()) {
        BindingContext bindingContext = requestContext.binding();
        if (bindingContext.hasModelAndView()) {
          ModelAndView modelAndView = bindingContext.getModelAndView();
          setModelAndView(modelAndView);
        }
        else {
          ModelMap model = bindingContext.getModel();
          Object viewNameAttribute = requestContext.getAttribute(VIEW_NAME_ATTRIBUTE);
          if (viewNameAttribute instanceof String viewName) {
            modelAndView = new ModelAndView(viewName, model);
          }
          else if (requestContext.getAttribute(VIEW_ATTRIBUTE) instanceof View view) {
            modelAndView = new ModelAndView(view, model);
          }
          else {
            modelAndView = new ModelAndView();
            modelAndView.addAllObjects(model);
          }
        }
      }
    }
    return this.modelAndView;
  }

  @Override
  public RedirectModel getFlashMap() {
    RedirectModel model = RequestContextUtils.getOutputRedirectModel(this.requestContext);
    if (model == null) {
      model = new RedirectModel();
      requestContext.setAttribute(RedirectModel.OUTPUT_ATTRIBUTE, model);
    }
    return model;
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
            "The asyncDispatch CountDownLatch was not set by the TestDispatcherHandler.");
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

  void setRequestContext(RequestContext maybeNew) {
    this.requestContext = maybeNew;
  }

  static DefaultMvcResult forContext(RequestContext request) {
    Object attribute = request.getAttribute(MVC_RESULT_ATTRIBUTE);
    if (attribute instanceof DefaultMvcResult mvcResult) {
      return mvcResult;
    }
    throw new IllegalStateException("No DefaultMvcResult");
  }

}
