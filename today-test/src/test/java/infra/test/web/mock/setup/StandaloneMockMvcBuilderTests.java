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

package infra.test.web.mock.setup;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ser.impl.UnknownSerializer;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.EnumSet;
import java.util.Map;

import infra.http.converter.json.BeanFactoryHandlerInstantiator;
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

  @Test
  @SuppressWarnings("rawtypes")
  void springHandlerInstantiator() {
    TestStandaloneMockMvcBuilder builder = new TestStandaloneMockMvcBuilder(new PersonController());
    builder.build();
    BeanFactoryHandlerInstantiator instantiator = new BeanFactoryHandlerInstantiator(builder.wac.getAutowireCapableBeanFactory());
    JsonSerializer serializer = instantiator.serializerInstance(null, null, UnknownSerializer.class);
    assertThat(serializer).isNotNull();
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
