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

package cn.taketoday.annotation.config.web.servlet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cn.taketoday.annotation.config.context.PropertyPlaceholderAutoConfiguration;
import cn.taketoday.annotation.config.http.HttpMessageConvertersAutoConfiguration;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import cn.taketoday.framework.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import cn.taketoday.test.util.TestPropertyValues;
import cn.taketoday.test.web.servlet.MockServletWebServer.RegisteredFilter;
import jakarta.servlet.Filter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify the ordering of various filters that are auto-configured.
 *
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 */
class FilterOrderingIntegrationTests {

  private AnnotationConfigServletWebServerApplicationContext context;

  @AfterEach
  void cleanup() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  void testFilterOrdering() {
    load();
    List<RegisteredFilter> registeredFilters = this.context.getBean(MockServletWebServerFactory.class)
            .getWebServer().getRegisteredFilters();
    List<Filter> filters = new ArrayList<>(registeredFilters.size());
    for (RegisteredFilter registeredFilter : registeredFilters) {
      filters.add(registeredFilter.getFilter());
    }
    Iterator<Filter> iterator = filters.iterator();
    assertThat(iterator.next()).isInstanceOf(Filter.class);
    assertThat(iterator.next()).isInstanceOf(Filter.class);
  }

  private void load() {
    this.context = new AnnotationConfigServletWebServerApplicationContext();
    this.context.register(MockWebServerConfiguration.class,
            WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
            PropertyPlaceholderAutoConfiguration.class);
    TestPropertyValues.of("web.mvc.hiddenmethod.filter.enabled:true").applyTo(this.context);
    this.context.refresh();
  }

  @Configuration(proxyBeanMethods = false)
  static class MockWebServerConfiguration {

    @Bean
    MockServletWebServerFactory webServerFactory() {
      return new MockServletWebServerFactory();
    }

    @Bean
    WebServerFactoryCustomizerBeanPostProcessor ServletWebServerCustomizerBeanPostProcessor() {
      return new WebServerFactoryCustomizerBeanPostProcessor();
    }

  }

}
