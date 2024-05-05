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

package cn.taketoday.web.handler.mvc;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import cn.taketoday.mock.api.MockContext;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.mock.api.RequestDispatcher;
import cn.taketoday.mock.api.Servlet;
import cn.taketoday.mock.api.MockConfig;
import cn.taketoday.mock.api.MockRequest;
import cn.taketoday.mock.api.MockResponse;
import cn.taketoday.mock.api.http.HttpMockRequest;
import cn.taketoday.mock.api.http.HttpMockResponse;
import cn.taketoday.web.mock.MockForwardingController;
import cn.taketoday.web.mock.MockRequestContext;
import cn.taketoday.web.mock.MockWrappingController;
import cn.taketoday.web.mock.support.StaticWebApplicationContext;
import cn.taketoday.web.view.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/8 17:32
 */
class ControllerTests {

  @Test
  public void parameterizableViewController() throws Throwable {
    String viewName = "viewName";
    ParameterizableViewController pvc = new ParameterizableViewController();
    pvc.setViewName(viewName);
    // We don't care about the params.
    StaticWebApplicationContext wac = new StaticWebApplicationContext();
    wac.refresh();
    MockRequestContext context = new MockRequestContext(wac, new HttpMockRequestImpl("GET", "foo.html"), new MockHttpResponseImpl());
    ModelAndView mv = (ModelAndView) pvc.handleRequest(context);
    assertThat(mv.getModel().size() == 0).as("model has no data").isTrue();
    assertThat(mv.getViewName().equals(viewName)).as("model has correct viewname").isTrue();
    assertThat(pvc.getViewName().equals(viewName)).as("getViewName matches").isTrue();
  }

  @Test
  public void servletForwardingController() throws Throwable {
    MockForwardingController sfc = new MockForwardingController();
    sfc.setServletName("action");
    doTestServletForwardingController(sfc, false);
  }

  @Test
  public void servletForwardingControllerWithInclude() throws Throwable {
    MockForwardingController sfc = new MockForwardingController();
    sfc.setServletName("action");
    doTestServletForwardingController(sfc, true);
  }

  @Test
  public void servletForwardingControllerWithBeanName() throws Throwable {
    MockForwardingController sfc = new MockForwardingController();
    sfc.setBeanName("action");
    doTestServletForwardingController(sfc, false);
  }

  private void doTestServletForwardingController(MockForwardingController sfc, boolean include)
          throws Throwable {

    HttpMockRequest request = mock(HttpMockRequest.class);
    HttpMockResponse response = mock(HttpMockResponse.class);
    MockContext context = mock(MockContext.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);

    given(request.getMethod()).willReturn("GET");
    given(context.getNamedDispatcher("action")).willReturn(dispatcher);
    if (include) {
      given(request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI)).willReturn("somePath");
    }
    else {
      given(request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI)).willReturn(null);
    }

    StaticWebApplicationContext sac = new StaticWebApplicationContext();
    sac.setMockContext(context);
    sfc.setApplicationContext(sac);

    sfc.setMockContext(context);

    MockRequestContext mockRequestContext = new MockRequestContext(sac, request, response);

    assertThat(sfc.handleRequest(mockRequestContext)).isNull();

    if (include) {
      verify(dispatcher).include(request, response);
    }
    else {
      verify(dispatcher).forward(request, response);
    }
  }

  @Test
  public void servletWrappingController() throws Throwable {
    HttpMockRequest request = new HttpMockRequestImpl("GET", "/somePath");
    HttpMockResponse response = new MockHttpResponseImpl();

    MockWrappingController swc = new MockWrappingController();
    swc.setServletClass(TestServlet.class);
    swc.setServletName("action");
    Properties props = new Properties();
    props.setProperty("config", "myValue");
    swc.setInitParameters(props);

    swc.afterPropertiesSet();
    assertThat(TestServlet.config).isNotNull();
    assertThat(TestServlet.config.getMockName()).isEqualTo("action");
    assertThat(TestServlet.config.getInitParameter("config")).isEqualTo("myValue");
    assertThat(TestServlet.request).isNull();
    assertThat(TestServlet.destroyed).isFalse();
    MockRequestContext mockRequestContext = new MockRequestContext(null, request, response);

    assertThat(swc.handleRequest(mockRequestContext)).isNull();
    assertThat(TestServlet.request).isEqualTo(request);
    assertThat(TestServlet.response).isEqualTo(response);
    assertThat(TestServlet.destroyed).isFalse();

    swc.destroy();
    assertThat(TestServlet.destroyed).isTrue();
  }

  @Test
  public void servletWrappingControllerWithBeanName() throws Throwable {
    HttpMockRequest request = new HttpMockRequestImpl("GET", "/somePath");
    HttpMockResponse response = new MockHttpResponseImpl();

    MockWrappingController swc = new MockWrappingController();
    swc.setServletClass(TestServlet.class);
    swc.setBeanName("action");

    swc.afterPropertiesSet();
    assertThat(TestServlet.config).isNotNull();
    assertThat(TestServlet.config.getMockName()).isEqualTo("action");
    assertThat(TestServlet.request).isNull();
    assertThat(TestServlet.destroyed).isFalse();
    MockRequestContext mockRequestContext = new MockRequestContext(null, request, response);

    assertThat(swc.handleRequest(mockRequestContext)).isNull();
    assertThat(TestServlet.request).isEqualTo(request);
    assertThat(TestServlet.response).isEqualTo(response);
    assertThat(TestServlet.destroyed).isFalse();

    swc.destroy();
    assertThat(TestServlet.destroyed).isTrue();
  }

  public static class TestServlet implements Servlet {

    private static MockConfig config;
    private static MockRequest request;
    private static MockResponse response;
    private static boolean destroyed;

    public TestServlet() {
      config = null;
      request = null;
      response = null;
      destroyed = false;
    }

    @Override
    public void init(MockConfig mockConfig) {
      config = mockConfig;
    }

    @Override
    public MockConfig getServletConfig() {
      return config;
    }

    @Override
    public void service(MockRequest mockRequest, MockResponse mockResponse) {
      request = mockRequest;
      response = mockResponse;
    }

    @Override
    public String getServletInfo() {
      return "TestServlet";
    }

    @Override
    public void destroy() {
      destroyed = true;
    }
  }

}
