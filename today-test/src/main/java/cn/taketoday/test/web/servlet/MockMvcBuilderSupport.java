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

import java.nio.charset.Charset;
import java.util.List;

import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.web.MockServletConfig;
import cn.taketoday.test.web.servlet.setup.DefaultMockMvcBuilder;
import cn.taketoday.web.context.WebApplicationContext;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;

/**
 * Base class for MockMvc builder implementations, providing the capability to
 * create a {@link MockMvc} instance.
 *
 * <p>{@link DefaultMockMvcBuilder},
 * which derives from this class, provides a concrete {@code build} method,
 * and delegates to abstract methods to obtain a {@link WebApplicationContext}.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.0
 */
public abstract class MockMvcBuilderSupport {

  /**
   * Delegates to {@link #createMockMvc(Filter[], MockServletConfig, WebApplicationContext, RequestBuilder, List, List, List)}
   * for creation of the {@link MockMvc} instance and configures that instance
   * with the supplied {@code defaultResponseCharacterEncoding}.
   *
   * @since 4.0
   */
  protected final MockMvc createMockMvc(Filter[] filters, MockServletConfig servletConfig,
          WebApplicationContext webAppContext, @Nullable RequestBuilder defaultRequestBuilder,
          @Nullable Charset defaultResponseCharacterEncoding,
          List<ResultMatcher> globalResultMatchers, List<ResultHandler> globalResultHandlers,
          @Nullable List<DispatcherServletCustomizer> dispatcherServletCustomizers) {

    MockMvc mockMvc = createMockMvc(filters, servletConfig, webAppContext, defaultRequestBuilder, globalResultMatchers, globalResultHandlers, dispatcherServletCustomizers);
    mockMvc.setDefaultResponseCharacterEncoding(defaultResponseCharacterEncoding);
    return mockMvc;
  }

  protected final MockMvc createMockMvc(Filter[] filters, MockServletConfig servletConfig,
          WebApplicationContext webAppContext, @Nullable RequestBuilder defaultRequestBuilder,
          List<ResultMatcher> globalResultMatchers, List<ResultHandler> globalResultHandlers,
          @Nullable List<DispatcherServletCustomizer> dispatcherServletCustomizers) {

    TestDispatcherServlet dispatcherServlet = new TestDispatcherServlet(webAppContext);
    if (dispatcherServletCustomizers != null) {
      for (DispatcherServletCustomizer customizers : dispatcherServletCustomizers) {
        customizers.customize(dispatcherServlet);
      }
    }
    try {
      dispatcherServlet.init(servletConfig);
    }
    catch (ServletException ex) {
      // should never happen..
      throw new MockMvcBuildException("Failed to initialize TestDispatcherServlet", ex);
    }

    MockMvc mockMvc = new MockMvc(dispatcherServlet, filters);
    mockMvc.setDefaultRequest(defaultRequestBuilder);
    mockMvc.setGlobalResultMatchers(globalResultMatchers);
    mockMvc.setGlobalResultHandlers(globalResultHandlers);

    return mockMvc;
  }

  @SuppressWarnings("serial")
  private static class MockMvcBuildException extends NestedRuntimeException {

    public MockMvcBuildException(String msg, Throwable cause) {
      super(msg, cause);
    }
  }

}
