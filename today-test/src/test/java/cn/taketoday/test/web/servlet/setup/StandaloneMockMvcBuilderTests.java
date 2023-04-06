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

package cn.taketoday.test.web.servlet.setup;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ser.impl.UnknownSerializer;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import cn.taketoday.http.converter.json.BeanFactoryHandlerInstantiator;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.handler.HandlerExecutionChain;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.handler.method.RequestMappingHandlerMapping;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.WebApplicationContext;
import cn.taketoday.web.servlet.filter.OncePerRequestFilter;
import cn.taketoday.web.servlet.support.WebApplicationContextUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
    HandlerExecutionChain chain = (HandlerExecutionChain) hm.getHandler(new ServletRequestContext(null,
            request, new MockHttpServletResponse()));

    assertThat(chain).isNotNull();
    assertThat(((HandlerMethod) chain.getRawHandler()).getMethod().getName()).isEqualTo("handleWithPlaceholders");
  }

  @Test
    // SPR-13637
  void suffixPatternMatch() throws Exception {
    TestStandaloneMockMvcBuilder builder = new TestStandaloneMockMvcBuilder(new PersonController());
    builder.build();

    RequestMappingHandlerMapping hm = builder.wac.getBean(RequestMappingHandlerMapping.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/persons");
    HandlerExecutionChain chain = (HandlerExecutionChain) hm.getHandler(new ServletRequestContext(null,
            request, new MockHttpServletResponse()));
    assertThat(chain).isNotNull();
    assertThat(((HandlerMethod) chain.getRawHandler()).getMethod().getName()).isEqualTo("persons");

    request = new MockHttpServletRequest("GET", "/persons.xml");
    chain = (HandlerExecutionChain) hm.getHandler(new ServletRequestContext(null,
            request, new MockHttpServletResponse()));
    assertThat(chain).isNull();
  }

  @Test
    // SPR-12553
  void applicationContextAttribute() {
    TestStandaloneMockMvcBuilder builder = new TestStandaloneMockMvcBuilder(new PlaceholderController());
    builder.addPlaceholderValue("sys.login.ajax", "/foo");
    WebApplicationContext wac = builder.initWebAppContext();
    assertThat(WebApplicationContextUtils.getRequiredWebApplicationContext(wac.getServletContext())).isEqualTo(wac);
  }

  @Test
  void addFiltersFiltersNull() {
    StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(new PersonController());
    assertThatIllegalArgumentException().isThrownBy(() ->
            builder.addFilters((Filter[]) null));
  }

  @Test
  void addFiltersFiltersContainsNull() {
    StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(new PersonController());
    assertThatIllegalArgumentException().isThrownBy(() ->
            builder.addFilters(new ContinueFilter(), null));
  }

  @Test
  void addFilterPatternsNull() {
    StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(new PersonController());
    assertThatIllegalArgumentException().isThrownBy(() ->
            builder.addFilter(new ContinueFilter(), (String[]) null));
  }

  @Test
  void addFilterPatternContainsNull() {
    StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(new PersonController());
    assertThatIllegalArgumentException().isThrownBy(() ->
            builder.addFilter(new ContinueFilter(), (String) null));
  }

  @Test  // SPR-13375
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

  private static class ContinueFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

      filterChain.doFilter(request, response);
    }
  }

}
