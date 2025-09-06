/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.test.web.mock;

import java.io.Serial;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import infra.context.ApplicationContext;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.mock.api.MockException;
import infra.mock.api.MockRequest;
import infra.mock.api.MockResponse;
import infra.mock.api.http.HttpMockRequest;
import infra.mock.api.http.HttpMockResponse;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockAsyncContext;
import infra.web.RequestContext;
import infra.web.RequestContextHolder;
import infra.web.async.CallableProcessingInterceptor;
import infra.web.async.DeferredResult;
import infra.web.async.DeferredResultProcessingInterceptor;
import infra.web.handler.HandlerExecutionChain;
import infra.web.mock.MockDispatcher;
import infra.web.mock.MockRequestContext;
import infra.web.mock.MockUtils;
import infra.web.view.ModelAndView;

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
        Assert.notNull(mockRequest, "Expected MockHttpServletRequest");
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

//  @Override
//  protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response)
//          throws Exception {
//
//    DefaultMvcResult mvcResult = getMvcResult(request);
//    mvcResult.setModelAndView(mv);
//    super.render(mv, request, response);
//  }

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
