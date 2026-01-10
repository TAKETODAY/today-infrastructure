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

package infra.test.web.mock.setup;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.EnumSet;
import java.util.Map;

import infra.mock.api.DispatcherType;
import infra.mock.api.Filter;
import infra.mock.api.FilterConfig;
import infra.mock.api.MockException;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.stereotype.Controller;
import infra.web.annotation.RequestMapping;
import infra.web.handler.HandlerExecutionChain;
import infra.web.handler.method.HandlerMethod;
import infra.web.handler.method.RequestMappingHandlerMapping;
import infra.web.mock.MockRequestContext;
import infra.web.mock.WebApplicationContext;
import infra.web.mock.support.WebApplicationContextUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link StandaloneMockMvcBuilder}
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author Sebastien Deleuze
 */
class StandaloneMockMvcBuilderTests {

  @Test
  void placeHoldersInRequestMapping() throws Exception {
    TestStandaloneMockMvcBuilder builder = new TestStandaloneMockMvcBuilder(new PlaceholderController());
    builder.addPlaceholderValue("sys.login.ajax", "/foo");
    builder.build();

    RequestMappingHandlerMapping hm = builder.wac.getBean(RequestMappingHandlerMapping.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/foo");
    HandlerExecutionChain chain = (HandlerExecutionChain) hm.getHandler(new MockRequestContext(null,
            request, new MockHttpResponseImpl()));

    assertThat(chain).isNotNull();
    assertThat(((HandlerMethod) chain.getRawHandler()).getMethod().getName()).isEqualTo("handleWithPlaceholders");
  }

  @Test
  void suffixPatternMatch() throws Exception {
    TestStandaloneMockMvcBuilder builder = new TestStandaloneMockMvcBuilder(new PersonController());
    builder.build();

    RequestMappingHandlerMapping hm = builder.wac.getBean(RequestMappingHandlerMapping.class);

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/persons");
    HandlerExecutionChain chain = (HandlerExecutionChain) hm.getHandler(new MockRequestContext(null,
            request, new MockHttpResponseImpl()));
    assertThat(chain).isNotNull();
    assertThat(((HandlerMethod) chain.getRawHandler()).getMethod().getName()).isEqualTo("persons");

    request = new HttpMockRequestImpl("GET", "/persons.xml");
    chain = (HandlerExecutionChain) hm.getHandler(new MockRequestContext(null,
            request, new MockHttpResponseImpl()));
    assertThat(chain).isNull();
  }

  @Test
  void applicationContextAttribute() {
    TestStandaloneMockMvcBuilder builder = new TestStandaloneMockMvcBuilder(new PlaceholderController());
    builder.addPlaceholderValue("sys.login.ajax", "/foo");
    WebApplicationContext wac = builder.initWebAppContext();
    assertThat(WebApplicationContextUtils.getRequiredWebApplicationContext(wac.getMockContext())).isEqualTo(wac);
  }

  @Test
  void addFiltersFiltersNull() {
    StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(new PersonController());
    assertThatIllegalArgumentException().isThrownBy(() ->
            builder.addFilters((Filter[]) null));
  }

  @Test
  void addFilterWithInitParams() throws MockException {
    Filter filter = mock(Filter.class);
    ArgumentCaptor<FilterConfig> captor = ArgumentCaptor.forClass(FilterConfig.class);

    MockMvcBuilders.standaloneSetup(new PersonController())
            .addFilter(filter, null, Map.of("p", "v"), EnumSet.of(DispatcherType.REQUEST), "/")
            .build();

    verify(filter, times(1)).init(captor.capture());
    assertThat(captor.getValue().getInitParameter("p")).isEqualTo("v");
  }

  @Controller
  private static class PlaceholderController {

    @RequestMapping(value = "${sys.login.ajax}")
    private void handleWithPlaceholders() { }
  }

  private static class TestStandaloneMockMvcBuilder extends StandaloneMockMvcBuilder {

    private WebApplicationContext wac;

    private TestStandaloneMockMvcBuilder(Object... controllers) {
      super(controllers);
    }

    @Override
    protected WebApplicationContext initWebAppContext() {
      this.wac = super.initWebAppContext();
      return this.wac;
    }
  }

  @Controller
  private static class PersonController {

    @RequestMapping(value = "/persons")
    public String persons() {
      return null;
    }

    @RequestMapping(value = "/forward")
    public String forward() {
      return "forward:/persons";
    }
  }

}
