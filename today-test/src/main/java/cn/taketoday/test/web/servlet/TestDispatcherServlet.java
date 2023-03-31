/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.web.MockAsyncContext;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.context.async.CallableProcessingInterceptor;
import cn.taketoday.web.context.async.DeferredResult;
import cn.taketoday.web.context.async.DeferredResultProcessingInterceptor;
import cn.taketoday.web.handler.HandlerExecutionChain;
import cn.taketoday.web.servlet.DispatcherServlet;
import cn.taketoday.web.servlet.WebApplicationContext;
import cn.taketoday.web.util.WebUtils;
import cn.taketoday.web.view.ModelAndView;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A subclass of {@code DispatcherServlet} that saves the result in an
 * {@link MvcResult}. The {@code MvcResult} instance is expected to be available
 * as the request attribute {@link MockMvc#MVC_RESULT_ATTRIBUTE}.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 4.0
 */
final class TestDispatcherServlet extends DispatcherServlet {

  private static final String KEY = TestDispatcherServlet.class.getName() + ".interceptor";

  /**
   * Create a new instance with the given web application context.
   */
  public TestDispatcherServlet(WebApplicationContext webApplicationContext) {
    super(webApplicationContext);
  }

  @Override
  protected void service(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {

    registerAsyncResultInterceptors(request);

    super.service(request, response);

    if (request.getAsyncContext() != null) {
      MockAsyncContext asyncContext;
      if (request.getAsyncContext() instanceof MockAsyncContext mockAsyncContext) {
        asyncContext = mockAsyncContext;
      }
      else {
        MockHttpServletRequest mockRequest = WebUtils.getNativeRequest(request, MockHttpServletRequest.class);
        Assert.notNull(mockRequest, "Expected MockHttpServletRequest");
        asyncContext = (MockAsyncContext) mockRequest.getAsyncContext();
        String requestClassName = request.getClass().getName();
        Assert.notNull(asyncContext, () ->
                "Outer request wrapper " + requestClassName + " has an AsyncContext," +
                        "but it is not a MockAsyncContext, while the nested " +
                        mockRequest.getClass().getName() + " does not have an AsyncContext at all.");
      }

      CountDownLatch dispatchLatch = new CountDownLatch(1);
      asyncContext.addDispatchHandler(dispatchLatch::countDown);
      getMvcResult(request).setAsyncDispatchLatch(dispatchLatch);
    }
  }

  private void registerAsyncResultInterceptors(HttpServletRequest request) {

    WebAsyncUtils.getAsyncManager(request).registerCallableInterceptor(KEY,
            new CallableProcessingInterceptor() {
              @Override
              public <T> void postProcess(NativeWebRequest r, Callable<T> task, Object value) {
                // We got the result, must also wait for the dispatch
                getMvcResult(request).setAsyncResult(value);
              }
            });

    WebAsyncUtils.getAsyncManager(request).registerDeferredResultInterceptor(KEY,
            new DeferredResultProcessingInterceptor() {
              @Override
              public <T> void postProcess(NativeWebRequest r, DeferredResult<T> result, Object value) {
                getMvcResult(request).setAsyncResult(value);
              }
            });
  }

  protected DefaultMvcResult getMvcResult(RequestContext request) {
    return (DefaultMvcResult) request.getAttribute(MockMvc.MVC_RESULT_ATTRIBUTE);
  }

  @Nullable
  @Override
  public Object lookupHandler(RequestContext context) throws Exception {
    Object handler = super.lookupHandler(context);
    if (handler instanceof HandlerExecutionChain chain) {
      DefaultMvcResult mvcResult = getMvcResult(context);
      mvcResult.setHandler(chain.getHandler());
      mvcResult.setInterceptors(chain.getInterceptors());
    }
    return handler;
  }

  @Override
  protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response)
          throws Exception {

    DefaultMvcResult mvcResult = getMvcResult(request);
    mvcResult.setModelAndView(mv);
    super.render(mv, request, response);
  }

  @Override
  protected ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response,
          @Nullable Object handler, Exception ex) throws Exception {

    ModelAndView mav = super.processHandlerException(request, response, handler, ex);

    // We got this far, exception was processed..
    DefaultMvcResult mvcResult = getMvcResult(request);
    mvcResult.setResolvedException(ex);
    mvcResult.setModelAndView(mav);

    return mav;
  }

}
