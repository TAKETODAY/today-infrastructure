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

import java.io.Serial;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import infra.context.ApplicationContext;
import infra.lang.Assert;
import infra.mock.api.MockException;
import infra.mock.api.MockRequest;
import infra.mock.api.MockResponse;
import infra.mock.api.http.HttpMockRequest;
import infra.mock.api.http.HttpMockResponse;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockAsyncContext;
import infra.web.RequestContext;
import infra.web.RequestContextHolder;
import infra.web.ReturnValueHandler;
import infra.web.async.CallableProcessingInterceptor;
import infra.web.async.DeferredResult;
import infra.web.async.DeferredResultProcessingInterceptor;
import infra.web.handler.HandlerExecutionChain;
import infra.web.mock.MockDispatcher;
import infra.web.mock.MockRequestContext;
import infra.web.mock.MockUtils;
import infra.web.view.ModelAndView;
import infra.web.view.ViewRef;
import infra.web.view.ViewReturnValueHandler;

/**
 * A subclass of {@code DispatcherHandler} that saves the result in an
 * {@link MvcResult}. The {@code MvcResult} instance is expected to be available
 * as the request attribute {@link DefaultMvcResult#MVC_RESULT_ATTRIBUTE}.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class TestMockDispatcher extends MockDispatcher {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final String KEY = TestMockDispatcher.class.getName() + ".interceptor";

  /**
   * Create a new instance with the given web application context.
   */
  public TestMockDispatcher(ApplicationContext webApplicationContext) {
    super(webApplicationContext);
  }

  @Override
  public void service(MockRequest request, MockResponse response) throws MockException {
    RequestContext context = RequestContextHolder.getRequired();
    HttpMockRequest servletRequest = MockUtils.getMockRequest(context);
    HttpMockResponse servletResponse = MockUtils.getMockResponse(context);

    if (request != servletRequest && response != servletResponse) {
      context = new MockRequestContext(
              getApplicationContext(), (HttpMockRequest) request, (HttpMockResponse) response, this);
      RequestContextHolder.set(context);
    }

    registerAsyncResultInterceptors(context);

    super.service(request, response);

    if (request.getAsyncContext() != null) {
      MockAsyncContext asyncContext;
      if (request.getAsyncContext() instanceof MockAsyncContext mockAsyncContext) {
        asyncContext = mockAsyncContext;
      }
      else {
        var mockRequest = MockUtils.getNativeRequest(request, HttpMockRequestImpl.class);
        Assert.notNull(mockRequest, "Expected HttpMockRequestImpl");
        asyncContext = (MockAsyncContext) mockRequest.getAsyncContext();
        Assert.notNull(asyncContext, () ->
                "Outer request wrapper " + request.getClass().getName() + " has an AsyncContext," +
                        "but it is not a MockAsyncContext, while the nested " +
                        mockRequest.getClass().getName() + " does not have an AsyncContext at all.");
      }

      CountDownLatch dispatchLatch = new CountDownLatch(1);
      asyncContext.addDispatchHandler(dispatchLatch::countDown);

      getMvcResult(context).setAsyncDispatchLatch(dispatchLatch);
    }

  }

  private void registerAsyncResultInterceptors(RequestContext context) {
    var interceptor = new MvcResultProcessingInterceptor();
    context.asyncManager().registerCallableInterceptor(KEY, interceptor);
    context.asyncManager().registerDeferredResultInterceptor(KEY, interceptor);
  }

  private DefaultMvcResult getMvcResult(RequestContext request) {
    return DefaultMvcResult.forContext(request);
  }

  @Nullable
  @Override
  public Object lookupHandler(RequestContext context) throws Exception {
    Object handler = super.lookupHandler(context);
    if (handler instanceof HandlerExecutionChain chain) {
      DefaultMvcResult mvcResult = getMvcResult(context);
      mvcResult.setHandler(chain.getRawHandler());
      mvcResult.setInterceptors(chain.getInterceptors());
    }
    return handler;
  }

  @Nullable
  @Override
  protected Object processHandlerException(RequestContext request, @Nullable Object handler, Throwable ex) throws Throwable {
    Object mav = super.processHandlerException(request, handler, ex);

    // We got this far, exception was processed..
    DefaultMvcResult mvcResult = getMvcResult(request);
    mvcResult.setResolvedException(ex);

    if (mav instanceof ModelAndView modelAndView) {
      mvcResult.setModelAndView(modelAndView);
    }
    return mav;
  }

  @Override
  protected @Nullable Object handleUnresolvedException(RequestContext request, Throwable unresolved, @Nullable Object handler) throws Throwable {
    getMvcResult(request).setUnresolvedException(unresolved);
    return super.handleUnresolvedException(request, unresolved, handler);
  }

  @Override
  protected void handleReturnValue(ReturnValueHandler selected, RequestContext request, @Nullable Object handler, @Nullable Object returnValue) throws Exception {
    if (selected instanceof ViewReturnValueHandler) {
      if (returnValue instanceof ModelAndView mv) {
        request.setAttribute(MvcResult.MODEL_AND_VIEW_ATTRIBUTE, mv);
      }
      else if (returnValue instanceof String viewName) {
        request.setAttribute(MvcResult.VIEW_NAME_ATTRIBUTE, viewName);
      }
      else if (returnValue instanceof ViewRef viewRef) {
        String viewName = viewRef.getViewName();
        request.setAttribute(MvcResult.VIEW_NAME_ATTRIBUTE, viewName);
      }
    }
    super.handleReturnValue(selected, request, handler, returnValue);
  }

  class MvcResultProcessingInterceptor
          implements CallableProcessingInterceptor, DeferredResultProcessingInterceptor {

    @Override
    public <T> void postProcess(RequestContext context, Callable<T> task, Object value) {
      // We got the result, must also wait for the dispatch
      getMvcResult(context).setAsyncResult(value);
    }

    @Override
    public <T> void postProcess(RequestContext context, DeferredResult<T> result, Object value) {
      getMvcResult(context).setAsyncResult(value);
    }

  }

}
