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

import java.io.Serial;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.web.MockAsyncContext;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.async.CallableProcessingInterceptor;
import cn.taketoday.web.async.DeferredResult;
import cn.taketoday.web.async.DeferredResultProcessingInterceptor;
import cn.taketoday.web.handler.HandlerExecutionChain;
import cn.taketoday.web.mock.DispatcherServlet;
import cn.taketoday.web.mock.MockRequestContext;
import cn.taketoday.web.mock.MockUtils;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.mock.api.MockException;
import cn.taketoday.mock.api.MockRequest;
import cn.taketoday.mock.api.MockResponse;
import cn.taketoday.mock.api.http.HttpMockRequest;
import cn.taketoday.mock.api.http.HttpMockResponse;

/**
 * A subclass of {@code DispatcherServlet} that saves the result in an
 * {@link MvcResult}. The {@code MvcResult} instance is expected to be available
 * as the request attribute {@link DefaultMvcResult#MVC_RESULT_ATTRIBUTE}.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class TestDispatcherServlet extends DispatcherServlet {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final String KEY = TestDispatcherServlet.class.getName() + ".interceptor";

  /**
   * Create a new instance with the given web application context.
   */
  public TestDispatcherServlet(ApplicationContext webApplicationContext) {
    super(webApplicationContext);
  }

  @Override
  public void service(MockRequest request, MockResponse response) throws MockException {
    RequestContext context = RequestContextHolder.getRequired();
    HttpMockRequest servletRequest = MockUtils.getServletRequest(context);
    HttpMockResponse servletResponse = MockUtils.getServletResponse(context);

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
    context.getAsyncManager().registerCallableInterceptor(KEY, interceptor);
    context.getAsyncManager().registerDeferredResultInterceptor(KEY, interceptor);
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
