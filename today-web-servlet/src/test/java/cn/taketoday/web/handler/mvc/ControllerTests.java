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

import cn.taketoday.web.servlet.ServletForwardingController;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.ServletWrappingController;
import cn.taketoday.web.servlet.support.StaticWebApplicationContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.web.mock.RequestDispatcher;
import cn.taketoday.web.mock.Servlet;
import cn.taketoday.web.mock.ServletConfig;
import cn.taketoday.web.mock.ServletContext;
import cn.taketoday.web.mock.ServletRequest;
import cn.taketoday.web.mock.ServletResponse;
import cn.taketoday.web.mock.http.HttpServletRequest;
import cn.taketoday.web.mock.http.HttpServletResponse;

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
    ServletRequestContext context = new ServletRequestContext(wac, new MockHttpServletRequest("GET", "foo.html"), new MockHttpServletResponse());
    ModelAndView mv = (ModelAndView) pvc.handleRequest(context);
    assertThat(mv.getModel().size() == 0).as("model has no data").isTrue();
    assertThat(mv.getViewName().equals(viewName)).as("model has correct viewname").isTrue();
    assertThat(pvc.getViewName().equals(viewName)).as("getViewName matches").isTrue();
  }

  @Test
  public void servletForwardingController() throws Throwable {
    ServletForwardingController sfc = new ServletForwardingController();
    sfc.setServletName("action");
    doTestServletForwardingController(sfc, false);
  }

  @Test
  public void servletForwardingControllerWithInclude() throws Throwable {
    ServletForwardingController sfc = new ServletForwardingController();
    sfc.setServletName("action");
    doTestServletForwardingController(sfc, true);
  }

  @Test
  public void servletForwardingControllerWithBeanName() throws Throwable {
    ServletForwardingController sfc = new ServletForwardingController();
    sfc.setBeanName("action");
    doTestServletForwardingController(sfc, false);
  }

  private void doTestServletForwardingController(ServletForwardingController sfc, boolean include)
          throws Throwable {

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    ServletContext context = mock(ServletContext.class);
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
    sac.setServletContext(context);
    sfc.setApplicationContext(sac);

    sfc.setServletContext(context);

    ServletRequestContext servletRequestContext = new ServletRequestContext(sac, request, response);

    assertThat(sfc.handleRequest(servletRequestContext)).isNull();

    if (include) {
      verify(dispatcher).include(request, response);
    }
    else {
      verify(dispatcher).forward(request, response);
    }
  }

  @Test
  public void servletWrappingController() throws Throwable {
    HttpServletRequest request = new MockHttpServletRequest("GET", "/somePath");
    HttpServletResponse response = new MockHttpServletResponse();

    ServletWrappingController swc = new ServletWrappingController();
    swc.setServletClass(TestServlet.class);
    swc.setServletName("action");
    Properties props = new Properties();
    props.setProperty("config", "myValue");
    swc.setInitParameters(props);

    swc.afterPropertiesSet();
    assertThat(TestServlet.config).isNotNull();
    assertThat(TestServlet.config.getServletName()).isEqualTo("action");
    assertThat(TestServlet.config.getInitParameter("config")).isEqualTo("myValue");
    assertThat(TestServlet.request).isNull();
    assertThat(TestServlet.destroyed).isFalse();
    ServletRequestContext servletRequestContext = new ServletRequestContext(null, request, response);

    assertThat(swc.handleRequest(servletRequestContext)).isNull();
    assertThat(TestServlet.request).isEqualTo(request);
    assertThat(TestServlet.response).isEqualTo(response);
    assertThat(TestServlet.destroyed).isFalse();

    swc.destroy();
    assertThat(TestServlet.destroyed).isTrue();
  }

  @Test
  public void servletWrappingControllerWithBeanName() throws Throwable {
    HttpServletRequest request = new MockHttpServletRequest("GET", "/somePath");
    HttpServletResponse response = new MockHttpServletResponse();

    ServletWrappingController swc = new ServletWrappingController();
    swc.setServletClass(TestServlet.class);
    swc.setBeanName("action");

    swc.afterPropertiesSet();
    assertThat(TestServlet.config).isNotNull();
    assertThat(TestServlet.config.getServletName()).isEqualTo("action");
    assertThat(TestServlet.request).isNull();
    assertThat(TestServlet.destroyed).isFalse();
    ServletRequestContext servletRequestContext = new ServletRequestContext(null, request, response);

    assertThat(swc.handleRequest(servletRequestContext)).isNull();
    assertThat(TestServlet.request).isEqualTo(request);
    assertThat(TestServlet.response).isEqualTo(response);
    assertThat(TestServlet.destroyed).isFalse();

    swc.destroy();
    assertThat(TestServlet.destroyed).isTrue();
  }

  public static class TestServlet implements Servlet {

    private static ServletConfig config;
    private static ServletRequest request;
    private static ServletResponse response;
    private static boolean destroyed;

    public TestServlet() {
      config = null;
      request = null;
      response = null;
      destroyed = false;
    }

    @Override
    public void init(ServletConfig servletConfig) {
      config = servletConfig;
    }

    @Override
    public ServletConfig getServletConfig() {
      return config;
    }

    @Override
    public void service(ServletRequest servletRequest, ServletResponse servletResponse) {
      request = servletRequest;
      response = servletResponse;
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
