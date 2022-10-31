/*
 * Copyright 2012-2022 the original author or authors.
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
