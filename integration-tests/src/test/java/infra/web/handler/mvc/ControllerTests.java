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

package infra.web.handler.mvc;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import infra.web.handler.mvc.ParameterizableViewController;
import infra.mock.api.MockApi;
import infra.mock.api.MockContext;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.mock.api.RequestDispatcher;
import infra.mock.api.MockConfig;
import infra.mock.api.MockRequest;
import infra.mock.api.MockResponse;
import infra.mock.api.http.HttpMockRequest;
import infra.mock.api.http.HttpMockResponse;
import infra.web.mock.MockRequestContext;
import infra.web.mock.MockWrappingController;
import infra.web.mock.support.StaticWebApplicationContext;
import infra.web.view.ModelAndView;

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
  public void mockForwardingController() throws Throwable {
    MockForwardingController sfc = new MockForwardingController();
    sfc.setMockName("action");
    doTestMockForwardingController(sfc, false);
  }

  @Test
  public void mockForwardingControllerWithInclude() throws Throwable {
    MockForwardingController sfc = new MockForwardingController();
    sfc.setMockName("action");
    doTestMockForwardingController(sfc, true);
  }

  @Test
  public void mockForwardingControllerWithBeanName() throws Throwable {
    MockForwardingController sfc = new MockForwardingController();
    sfc.setBeanName("action");
    doTestMockForwardingController(sfc, false);
  }

  private void doTestMockForwardingController(MockForwardingController sfc, boolean include)
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

    Assertions.assertThat(sfc.handleRequest(mockRequestContext)).isNull();

    if (include) {
      verify(dispatcher).include(request, response);
    }
    else {
      verify(dispatcher).forward(request, response);
    }
  }

  @Test
  public void wrappingController() throws Throwable {
    HttpMockRequest request = new HttpMockRequestImpl("GET", "/somePath");
    HttpMockResponse response = new MockHttpResponseImpl();

    MockWrappingController swc = new MockWrappingController();
    swc.setMockClass(TestMockApi.class);
    swc.setMockName("action");
    Properties props = new Properties();
    props.setProperty("config", "myValue");
    swc.setInitParameters(props);

    swc.afterPropertiesSet();
    assertThat(TestMockApi.config).isNotNull();
    assertThat(TestMockApi.config.getMockName()).isEqualTo("action");
    assertThat(TestMockApi.config.getInitParameter("config")).isEqualTo("myValue");
    assertThat(TestMockApi.request).isNull();
    assertThat(TestMockApi.destroyed).isFalse();
    MockRequestContext mockRequestContext = new MockRequestContext(null, request, response);

    assertThat(swc.handleRequest(mockRequestContext)).isNull();
    assertThat(TestMockApi.request).isEqualTo(request);
    assertThat(TestMockApi.response).isEqualTo(response);
    assertThat(TestMockApi.destroyed).isFalse();

    swc.destroy();
    assertThat(TestMockApi.destroyed).isTrue();
  }

  @Test
  public void wrappingControllerWithBeanName() throws Throwable {
    HttpMockRequest request = new HttpMockRequestImpl("GET", "/somePath");
    HttpMockResponse response = new MockHttpResponseImpl();

    MockWrappingController swc = new MockWrappingController();
    swc.setMockClass(TestMockApi.class);
    swc.setBeanName("action");

    swc.afterPropertiesSet();
    assertThat(TestMockApi.config).isNotNull();
    assertThat(TestMockApi.config.getMockName()).isEqualTo("action");
    assertThat(TestMockApi.request).isNull();
    assertThat(TestMockApi.destroyed).isFalse();
    MockRequestContext mockRequestContext = new MockRequestContext(null, request, response);

    assertThat(swc.handleRequest(mockRequestContext)).isNull();
    assertThat(TestMockApi.request).isEqualTo(request);
    assertThat(TestMockApi.response).isEqualTo(response);
    assertThat(TestMockApi.destroyed).isFalse();

    swc.destroy();
    assertThat(TestMockApi.destroyed).isTrue();
  }

  public static class TestMockApi implements MockApi {

    private static MockConfig config;
    private static MockRequest request;
    private static MockResponse response;
    private static boolean destroyed;

    public TestMockApi() {
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
    public MockConfig getMockConfig() {
      return config;
    }

    @Override
    public void service(MockRequest mockRequest, MockResponse mockResponse) {
      request = mockRequest;
      response = mockResponse;
    }

    @Override
    public String getMockInfo() {
      return "TestServlet";
    }

    @Override
    public void destroy() {
      destroyed = true;
    }
  }

}
